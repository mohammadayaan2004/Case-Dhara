import logging
"""
Court judgment summarizer: facebook/bart-large-cnn for summarization,
plus regex-based structured field extraction.
100% local ML inference — no external API calls.
Fine-tuned weights used automatically if models/bart_finetuned/ exists.
"""

import os
import re
from pathlib import Path

import torch
from transformers import pipeline

from schemas.summarizer_schema import CaseSummary, SectionReference
from services.cache_service import (
    get_cached_summary,
    set_cached_summary,
    summary_cache_key,
)
from services.pdf_extractor import extract_text_from_pdf, find_sections_in_text

logger = logging.getLogger("case_dhara.summarizer")


# ── Model config ──────────────────────────────────────────────────────────────
BART_MODEL_NAME = "facebook/bart-large-cnn"
MAX_INPUT_TOKENS = 1024
MAX_SUMMARY_TOKENS = 256
FINE_TUNED_MODEL_DIR = Path(__file__).resolve().parents[1] / "models" / "bart_finetuned"


def _get_device() -> str:
    return "cuda:0" if torch.cuda.is_available() else "cpu"


def _load_summarizer_pipeline():
    """
    Load the BART summarization pipeline.
    Uses fine-tuned weights from models/bart_finetuned/ if present,
    otherwise falls back to pretrained facebook/bart-large-cnn.
    """
    has_finetuned = (
        FINE_TUNED_MODEL_DIR.exists()
        and (FINE_TUNED_MODEL_DIR / "config.json").exists()
        and (FINE_TUNED_MODEL_DIR / "model.safetensors").exists()
    )
    model_path = str(FINE_TUNED_MODEL_DIR) if has_finetuned else BART_MODEL_NAME
    logger.info("Loading BART summarizer: %s", model_path)

    # Repair tokenizer if missing or corrupted (version mismatch from Colab training)
    if has_finetuned:
        tok_path = FINE_TUNED_MODEL_DIR / "tokenizer.json"
        tok_config = FINE_TUNED_MODEL_DIR / "tokenizer_config.json"
        if not tok_path.exists() or tok_path.stat().st_size < 1000:
            logger.warning("tokenizer.json missing or corrupted — re-downloading from base model")
            from transformers import BartTokenizerFast
            tok = BartTokenizerFast.from_pretrained(BART_MODEL_NAME)
            tok.save_pretrained(str(FINE_TUNED_MODEL_DIR))
            logger.info("Tokenizer repaired.")

    summ = pipeline(
        "summarization",
        model=model_path,
        tokenizer=model_path,
        device=_get_device(),
        framework="pt",
    )
    logger.info("BART summarizer ready.")
    return summ


# ── Text helpers ──────────────────────────────────────────────────────────────

def smart_truncate(text: str, max_chars: int = 6000) -> str:
    """Keep beginning + middle + end so BART sees a representative slice."""
    if len(text) <= max_chars:
        return text
    third = max_chars // 3
    midpoint = len(text) // 2
    half_middle = third // 2
    return (
        text[:third]
        + "\n\n[...truncated...]\n\n"
        + text[max(0, midpoint - half_middle): midpoint + half_middle]
        + "\n\n[...truncated...]\n\n"
        + text[-third:]
    )


def chunk_text_for_bart(text: str, tokenizer, max_tokens: int = MAX_INPUT_TOKENS) -> list:
    """Split text into BART-sized chunks (max 1024 tokens each)."""
    tokens = tokenizer.encode(text, add_special_tokens=False)
    chunks = []
    for i in range(0, len(tokens), max_tokens):
        chunk_tokens = tokens[i: i + max_tokens]
        chunks.append(tokenizer.decode(chunk_tokens, skip_special_tokens=True))
    return chunks if chunks else [text]


# ── Regex-based structured extraction ─────────────────────────────────────────

_DATE_RE = re.compile(
    r"\b(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4}|\d{4}[\/\-\.]\d{2}[\/\-\.]\d{2})\b"
)
_VS_RE = re.compile(
    r"^(.*?)\s+v(?:s\.?|ersus\.?)\s+(.*?)$", re.IGNORECASE | re.MULTILINE
)
_COURT_KEYWORDS = [
    "Supreme Court", "High Court", "Sessions Court", "District Court",
    "Magistrate Court", "Fast Track Court", "Family Court",
]


def _extract_case_title(text: str) -> str:
    m = _VS_RE.search(text[:2000])
    if m:
        return f"{m.group(1).strip()} v. {m.group(2).strip()}"
    for line in text.splitlines()[:20]:
        line = line.strip()
        if 10 < len(line) < 150 and line[0].isupper():
            return line
    return "Unknown Case"


def _extract_court(text: str) -> str:
    for kw in _COURT_KEYWORDS:
        if kw.lower() in text[:3000].lower():
            return kw
    return "Court Not Identified"


def _extract_date(text: str):
    m = _DATE_RE.search(text[:5000])
    return m.group(0) if m else None


def _extract_outcome(text: str):
    t = text.lower()
    if "acquitted" in t or "acquittal" in t:
        return "acquittal"
    if "convicted" in t or "conviction" in t:
        return "conviction"
    if "remand" in t:
        return "remand"
    if "appeal allowed" in t:
        return "appeal_allowed"
    if "appeal dismissed" in t:
        return "appeal_dismissed"
    if "disposed" in t:
        return "disposed"
    return None


def _extract_parties(text: str) -> dict:
    m = _VS_RE.search(text[:2000])
    if m:
        return {"appellant": m.group(1).strip(), "respondent": m.group(2).strip()}
    return {"appellant": "Not identified", "respondent": "Not identified"}


def _extract_issues(text: str) -> list:
    patterns = [
        r"(?:issues?|question[s]? (?:of law|before(?: the)? court)|point[s]? (?:for )?(?:consideration|determination))[:\-–]\s*(.+?)(?=\n\n|\Z)",
        r"(?:whether|the (?:main |core )?(?:issue|question) (?:is|was|before))[:\s]+(.+?)(?=\n|\Z)",
    ]
    issues = []
    for pat in patterns:
        for m in re.findall(pat, text, re.IGNORECASE | re.DOTALL):
            sentences = [s.strip() for s in re.split(r'[;\n]', m) if len(s.strip()) > 20]
            issues.extend(sentences[:4])
        if issues:
            break
    if not issues:
        for sent in re.split(r'(?<=[.?!])\s+', text):
            if re.search(r'\bwhether\b', sent, re.IGNORECASE) and len(sent) > 30:
                issues.append(sent.strip())
            if len(issues) >= 4:
                break
    return issues[:6]


def _extract_evidence(text: str) -> str:
    patterns = [
        r"(?:evidence(?:[:\-–\s])|exhibit[s]?(?:[:\-–\s])|proved(?:[:\-–\s])|witness(?:es)?(?:[:\-–\s]))(.{100,600}?)(?=\n\n|\Z)",
        r"(?:on the basis of|relying upon|placing reliance on)(.{60,400}?)(?=[.]\s|\n\n|\Z)",
    ]
    for pat in patterns:
        m = re.search(pat, text, re.IGNORECASE | re.DOTALL)
        if m:
            return m.group(1).strip()[:600]
    return ""


def _extract_ratio_decidendi(text: str) -> str:
    patterns = [
        r"(?:ratio(?: decidendi)?|the (?:legal )?principle[s]? (?:laid down|established)|law (?:laid down|declared))[:\-–\s]+(.{80,600}?)(?=\n\n|\Z)",
        r"(?:we (?:hold|are of the view)|it is held|this court holds)[:\s]+(.{80,500}?)(?=[.]\s|\n\n|\Z)",
    ]
    for pat in patterns:
        m = re.search(pat, text, re.IGNORECASE | re.DOTALL)
        if m:
            return m.group(1).strip()[:600]
    held = _extract_held(text)
    return held if held else ""


def _extract_final_judgment(text: str) -> str:
    patterns = [
        r"(?:in the result|accordingly|for the(?:se)? reasons?|in view of the above)[,\s]+(.{60,500}?)(?=[.]\s|\n\n|\Z)",
        r"(?:the (?:appeal|petition|suit|revision) (?:is|are|stand[s]?)(?: hereby)? (?:allowed|dismissed|disposed|upheld|set aside))(.{0,300}?)(?=[.]\s|\n\n|\Z)",
        r"(?:order|ordered)[:\-–\s]+(.{60,400}?)(?=[.]\s|\n\n|\Z)",
    ]
    for pat in patterns:
        m = re.search(pat, text, re.IGNORECASE | re.DOTALL)
        if m:
            return (m.group(0) if m.lastindex == 0 else m.group(1)).strip()[:600]
    return ""


def _extract_held(text: str) -> str:
    pat = re.compile(
        r"(?:HELD|held\s*[:;\-]|the\s+court\s+held)\s*[:\-]?\s*(.{100,800})",
        re.IGNORECASE | re.DOTALL,
    )
    m = pat.search(text)
    return m.group(1).strip() if m else "Refer to judgment for holding."


def _extract_bench(text: str):
    pat = re.compile(
        r"(?:before|coram|bench)\s*[:\-]?\s*(?:Hon['\.']?ble\s+)?(?:Mr\.?|Ms\.?|Justice\s+)"
        r"([A-Z][a-zA-Z\s,\.]+?)(?:\n|and|,\s*J)",
        re.IGNORECASE,
    )
    m = pat.search(text[:3000])
    return m.group(0).strip() if m else None


# ── Extractive fallback (when BART output is empty or very short) ──────────────

def _extractive_summary(text: str, max_sentences: int = 5) -> str:
    """
    Simple extractive summary: picks the first few meaningful sentences.
    Used only as a last-resort fallback if BART produces unusable output.
    """
    sentences = re.split(r"(?<=[.!?])\s+", text.replace("\n", " "))
    good = [s.strip() for s in sentences if len(s.strip()) > 40]
    return " ".join(good[:max_sentences]) if good else text[:400]


# ── Main summarizer class ─────────────────────────────────────────────────────

class CaseSummarizer:
    def __init__(self):
        self._pipeline = _load_summarizer_pipeline()
        self._tokenizer = self._pipeline.tokenizer

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

    def _generate_summary(self, text: str) -> str:
        """Run BART summarization with automatic chunking for long texts."""
        chunks = chunk_text_for_bart(text, self._tokenizer)
        chunk_summaries = []
        for chunk in chunks:
            if not chunk.strip():
                continue
            try:
                out = self._pipeline(
                    chunk,
                    max_length=MAX_SUMMARY_TOKENS,
                    min_length=40,
                    do_sample=False,
                    truncation=True,
                )
                chunk_text = out[0]["summary_text"]
                if chunk_text and len(chunk_text.strip()) > 20:
                    chunk_summaries.append(chunk_text)
            except Exception as e:
                logger.error("BART chunk error: %s", e)
                # Fallback: use extractive snippet for this chunk
                chunk_summaries.append(_extractive_summary(chunk, max_sentences=2))

        combined = " ".join(chunk_summaries)

        # Final merge pass for multi-chunk documents
        if len(chunks) > 1 and combined.strip():
            try:
                final_tokens = self._tokenizer.encode(combined, add_special_tokens=False)
                if len(final_tokens) > MAX_INPUT_TOKENS:
                    combined = self._tokenizer.decode(
                        final_tokens[:MAX_INPUT_TOKENS], skip_special_tokens=True
                    )
                out = self._pipeline(
                    combined,
                    max_length=MAX_SUMMARY_TOKENS,
                    min_length=40,
                    do_sample=False,
                    truncation=True,
                )
                merged = out[0]["summary_text"]
                if merged and len(merged.strip()) > 20:
                    return merged
            except Exception as e:
                logger.error("BART merge pass error: %s", e)

        # If we still have nothing useful, fall back to extractive
        if not combined or len(combined.strip()) < 20:
            return _extractive_summary(text)

        return combined

    def _process(self, text: str) -> CaseSummary:
        case_title = _extract_case_title(text)
        court = _extract_court(text)
        date = _extract_date(text)
        outcome = _extract_outcome(text)
        parties = _extract_parties(text)
        issues = _extract_issues(text)
        held = _extract_held(text)
        bench = _extract_bench(text)
        evidence_text = _extract_evidence(text)
        ratio_text = _extract_ratio_decidendi(text)
        final_judgment_text = _extract_final_judgment(text)

        truncated = smart_truncate(text, max_chars=6000)
        summary_short = self._generate_summary(truncated)

        regex_sections = find_sections_in_text(text)
        raw_secs = list(set(regex_sections))

        mapped: list = []
        for sec in raw_secs:
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
            case_title=case_title,
            citation=None,
            court=court,
            bench=bench,
            date=date,
            parties=parties,
            sections_invoked=mapped,
            facts=truncated[:1000],
            issues=issues,
            arguments={"prosecution": "", "defense": ""},
            evidence=evidence_text or "Refer to full judgment for evidence details.",
            held=held,
            ratio_decidendi=ratio_text or "Refer to full judgment for ratio decidendi.",
            order=final_judgment_text or "Refer to judgment order section.",
            summary_short=summary_short,
            outcome=outcome,
        )


_summarizer: CaseSummarizer | None = None


def get_summarizer() -> CaseSummarizer:
    global _summarizer
    if _summarizer is None:
        _summarizer = CaseSummarizer()
    return _summarizer


def summarizer_singleton_loaded() -> bool:
    return _summarizer is not None
