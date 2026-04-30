"""
Court judgment summarizer: Gemini JSON extraction.
Migrated to the new google-genai SDK (client-based API).
"""

import json
import os
import re
import time

try:
    from google import genai
    from google.genai import types
except ImportError:
    genai = None
    types = None

from schemas.summarizer_schema import CaseSummary, SectionReference
from services.cache_service import (
    get_cached_summary,
    set_cached_summary,
    summary_cache_key,
)
from services.pdf_extractor import extract_text_from_pdf, find_sections_in_text


GEMINI_SUMMARY_MODEL = "gemini-1.5-pro"
MAX_TEXT_LEN = 90_000

SYSTEM_PROMPT = """You are NyayaSetu, an expert Indian legal assistant.
Analyze court judgments with precision. Extract ONLY what is explicitly present.
For missing fields use null. Respond ONLY with valid JSON and no markdown fences."""

EXTRACTION_PROMPT = """Analyze this Indian court judgment and return this exact JSON.
Detect whether the matter is primarily criminal/civil/constitutional where possible.

{text}

Return:
{{
  "case_title": "...",
  "citation": "... or null",
  "court": "Court name (note criminal/civil/constitutional if clear)",
  "bench": "Judge(s) or null",
  "date": "YYYY-MM-DD or null",
  "parties": {{"appellant": "...", "respondent": "..."}},
  "raw_sections": ["302", "376AB"],
  "facts": "2-3 paragraphs on facts",
  "issues": ["Issue 1"],
  "arguments": {{"prosecution": "...", "defense": "..."}},
  "evidence": "...",
  "held": "...",
  "ratio": "...",
  "order": "...",
  "summary": "Plain-language summary under 200 words",
  "outcome": "conviction|acquittal|remand|disposed|appeal_allowed|appeal_dismissed|null"
}}"""


def smart_truncate(text: str, max_len: int) -> str:
    if len(text) <= max_len:
        return text
    third = max_len // 3
    midpoint = len(text) // 2
    half_middle = third // 2
    return (
        text[:third]
        + "\n\n[...truncated for length...]\n\n"
        + text[max(0, midpoint - half_middle) : midpoint + half_middle]
        + "\n\n[...truncated for length...]\n\n"
        + text[-third:]
    )


def _usable_key(value: str | None, placeholder: str) -> bool:
    return bool(value and value.strip() and placeholder not in value)


def _strip_json_fences(raw: str) -> str:
    raw = raw.strip()
    if "```" not in raw:
        return raw
    raw = re.sub(r"^[\s\S]*?```(?:json)?\s*", "", raw, flags=re.IGNORECASE)
    raw = re.sub(r"```[\s\S]*$", "", raw)
    return raw.strip()


class CaseSummarizer:
    def __init__(self):
        gemini_key = os.getenv("GEMINI_API_KEY")
        self._use_gemini = genai is not None and _usable_key(gemini_key, "REPLACE_ME")
        if self._use_gemini:
            # New SDK: create a Client instance instead of genai.configure()
            self._client = genai.Client(api_key=gemini_key)
        else:
            raise RuntimeError("Gemini API key not configured or google-genai not installed.")

        from models.mapper import get_mapper

        self._mapper = get_mapper()

    def from_pdf(self, pdf_bytes: bytes) -> CaseSummary:
        key = summary_cache_key(pdf_bytes)
        cached = get_cached_summary(key)
        if cached is not None:
            return cached
        text = extract_text_from_pdf(pdf_bytes)
        result = self._process(text)
        set_cached_summary(key, result)
        return result

    def from_text(self, text: str) -> CaseSummary:
        return self._process(text)

    def _process(self, text: str) -> CaseSummary:
        text = smart_truncate(text, MAX_TEXT_LEN)
        regex_sections = find_sections_in_text(text)
        prompt = EXTRACTION_PROMPT.format(text=text)

        data: dict = {}
        last_err: Exception | None = None
        for attempt in range(3):
            try:
                # New SDK: call client.models.generate_content() directly
                response = self._client.models.generate_content(
                    model=GEMINI_SUMMARY_MODEL,
                    contents=prompt,
                    config=types.GenerateContentConfig(
                        system_instruction=SYSTEM_PROMPT,
                        response_mime_type="application/json",
                        max_output_tokens=4096,
                        temperature=0.1,
                    ),
                )
                raw = response.text
                data = json.loads(_strip_json_fences(raw))
                break
            except Exception as exc:
                last_err = exc
                time.sleep(2**attempt)
        else:
            raise ValueError(f"Summarization failed after retries: {last_err}")

        all_sections = list(set((data.get("raw_sections") or []) + regex_sections))
        mapped: list[SectionReference] = []
        for sec in all_sections:
            try:
                result = self._mapper.search(query=str(sec), top_k=1)
                if result.results and result.results[0].confidence > 0.65:
                    r = result.results[0]
                    mapped.append(
                        SectionReference(
                            raw_ref=str(sec),
                            ipc_section=r.ipc_section,
                            ipc_heading=r.ipc_heading,
                            bns_section=r.bns_section,
                            bns_heading=r.bns_heading,
                            status=r.status,
                        )
                    )
            except Exception:
                continue

        return CaseSummary(
            case_title=data.get("case_title") or "",
            citation=data.get("citation"),
            court=data.get("court") or "",
            bench=data.get("bench"),
            date=data.get("date"),
            parties=data.get("parties") or {},
            sections_invoked=mapped,
            facts=data.get("facts") or "",
            issues=data.get("issues") or [],
            arguments=data.get("arguments") or {},
            evidence=data.get("evidence") or "",
            held=data.get("held") or "",
            ratio_decidendi=data.get("ratio") or "",
            order=data.get("order") or "",
            summary_short=data.get("summary") or "",
            outcome=data.get("outcome"),
        )


_summarizer: CaseSummarizer | None = None


def get_summarizer() -> CaseSummarizer:
    global _summarizer
    if _summarizer is None:
        _summarizer = CaseSummarizer()
    return _summarizer


def summarizer_singleton_loaded() -> bool:
    return _summarizer is not None
