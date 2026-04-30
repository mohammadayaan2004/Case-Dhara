"""
3-tier legal section retrieval: exact → keyword+synonym → semantic FAISS.
LRU cache on full search results.
"""

import json
import math
import os
import re
from pathlib import Path
from typing import Optional

os.environ.setdefault("TRANSFORMERS_NO_TF", "1")

import faiss
from sentence_transformers import SentenceTransformer

from schemas.mapper_schema import MapperResponse, MappingRecord
from services.cache_service import (
    get_cached_mapper,
    mapper_cache_key,
    set_cached_mapper,
)


BASE_DIR = Path(__file__).resolve().parents[1]
PROCESSED_DIR = BASE_DIR / "data" / "processed"
FAISS_DIR = PROCESSED_DIR / "faiss_index"
SYNONYMS_PATH = PROCESSED_DIR / "lookup_synonyms.json"
MODEL_NAME = "sentence-transformers/all-mpnet-base-v2"


def _norm(s: str) -> str:
    s = str(s).strip().upper()
    s = re.sub(r"\b(SECTION|SEC)\s*", "", s)
    s = re.sub(r"^(IPC|BNS)\s*", "", s)
    s = re.sub(r"\s*(IPC|BNS)\s*$", "", s)
    s = re.sub(r"\bU[/\\]S\b\s*", "", s)
    s = re.sub(r"\bUNDER\s*", "", s)
    s = re.sub(r"\s+", "", s)
    return s


def _query_keys(s: str) -> list[str]:
    norm = _norm(s)
    keys = [norm]
    base = re.split(r"[,(]", norm)[0]
    if base and base != norm:
        keys.append(base)
    return keys


def _extract_section_refs(text: str) -> list[str]:
    patterns = [
        r"\b(?:IPC|BNS)?\s*(\d+[A-Z]*(?:\(\d+\))?)\s*(?:IPC|BNS|r/w|read with|,|$)",
        r"\bu[/\\]s\s+(\d+[A-Z]*(?:\(\d+\))?)",
        r"\b[Ss]ection\s+(\d+[A-Z]*(?:\(\d+\))?)",
    ]
    found: set[str] = set()
    for pat in patterns:
        for m in re.findall(pat, text, flags=re.IGNORECASE):
            cleaned = re.sub(r"\s+", "", str(m).upper())
            if cleaned:
                found.add(cleaned)
    return list(found)


class LegalMapper:
    def __init__(self) -> None:
        with (PROCESSED_DIR / "mapping_db.json").open("r", encoding="utf-8") as f:
            self._records = json.load(f)
        self._records_by_id = {r["id"]: r for r in self._records}

        with (PROCESSED_DIR / "lookup_exact.json").open("r", encoding="utf-8") as f:
            self._exact = json.load(f)

        with (PROCESSED_DIR / "lookup_keywords.json").open("r", encoding="utf-8") as f:
            self._keywords = json.load(f)

        self._synonyms: dict[str, list[str]] = {}
        if SYNONYMS_PATH.exists():
            with SYNONYMS_PATH.open("r", encoding="utf-8") as f:
                self._synonyms = json.load(f)

        self._faiss = faiss.read_index(str(FAISS_DIR / "index.faiss"))
        with (FAISS_DIR / "metadata.json").open("r", encoding="utf-8") as f:
            self._faiss_meta = json.load(f)
        self._validate_faiss_meta()

        ft_path = BASE_DIR / "models" / "finetuned_retriever"
        model_path = str(ft_path) if ft_path.exists() else os.getenv("EMBEDDING_MODEL_NAME", MODEL_NAME)
        print(f"  Loading retriever: {model_path}")
        self._model = SentenceTransformer(model_path)
        print("  Mapper ready.")

    def _validate_faiss_meta(self) -> None:
        n_vectors = self._faiss.ntotal
        n_meta = len(self._faiss_meta)
        if n_vectors != n_meta:
            raise RuntimeError(
                f"FAISS index has {n_vectors} vectors but metadata has {n_meta} entries. "
                "Rebuild index with scripts/build_index.py"
            )

    def _status_or_default(self, value: str) -> str:
        allowed = {"mapped", "repealed", "new_in_bns", "merged", "split"}
        return value if value in allowed else "mapped"

    def _to_record_model(self, rec: dict, score: float, tier: int) -> MappingRecord:
        return MappingRecord(
            id=rec["id"],
            ipc_section=rec.get("ipc_section", ""),
            ipc_heading=rec.get("ipc_heading", ""),
            ipc_description=rec.get("ipc_description", ""),
            bns_section=rec.get("bns_section", ""),
            bns_heading=rec.get("bns_heading", ""),
            bns_description=rec.get("bns_description", ""),
            status=self._status_or_default(rec.get("status", "mapped")),
            confidence=max(0.0, min(1.0, round(float(score), 4))),
            retrieval_tier=tier,
        )

    def _law_ok(self, rec: dict, law_filter: Optional[str]) -> bool:
        if not law_filter:
            return True
        law = law_filter.lower().strip()
        if law == "ipc":
            return bool((rec.get("ipc_section") or "").strip())
        if law == "bns":
            return bool((rec.get("bns_section") or "").strip())
        return True

    def _tier2_confidence(self, hits: int, max_hits: int) -> float:
        """Keyword tier: dampen misleading 1.0 scores; cap at 0.85."""
        if max_hits <= 0:
            return 0.5
        linear = hits / max_hits
        damped = 0.55 + 0.30 * math.log1p(hits) / math.log1p(max_hits)
        raw = min(linear, damped)
        return min(0.85, raw)

    def search(self, query: str, top_k: int = 5, law_filter: Optional[str] = None) -> MapperResponse:
        query = query.strip()
        cache_key = mapper_cache_key(query, top_k, law_filter)
        cached = get_cached_mapper(cache_key)
        if cached is not None:
            return cached

        # Tier 1A — exact (two-key via _query_keys)
        for key in _query_keys(query):
            if key in self._exact:
                rec = self._records_by_id[self._exact[key]]
                if self._law_ok(rec, law_filter):
                    msg = "Exact section match."
                    if rec.get("status") == "repealed":
                        ipc = rec.get("ipc_section", "")
                        msg = (
                            f"IPC {ipc} has been REPEALED under BNS 2023 "
                            "(correspondence shows repealed / no direct equivalent)."
                        )
                    result = MapperResponse(
                        query=query,
                        results=[self._to_record_model(rec, 1.0, 1)],
                        total=1,
                        retrieval_tier=1,
                        message=msg,
                    )
                    set_cached_mapper(cache_key, result)
                    return result

        # Tier 1B — FIR-style embedded refs (try base keys per section)
        embedded = _extract_section_refs(query)
        if embedded:
            tier1b: list[MappingRecord] = []
            for sec in embedded:
                rid = None
                for k in _query_keys(sec):
                    if k in self._exact:
                        rid = self._exact[k]
                        break
                if rid is None:
                    continue
                rec = self._records_by_id.get(rid)
                if rec and self._law_ok(rec, law_filter):
                    tier1b.append(self._to_record_model(rec, 0.98, 1))
                    if len(tier1b) >= top_k:
                        break
            if tier1b:
                result = MapperResponse(
                    query=query,
                    results=tier1b,
                    total=len(tier1b),
                    retrieval_tier=1,
                    message="Section references extracted from text.",
                )
                set_cached_mapper(cache_key, result)
                return result

        # Tier 2 — keyword + synonym expansion
        STOP = {
            "of",
            "the",
            "a",
            "an",
            "and",
            "or",
            "to",
            "by",
            "in",
            "for",
            "with",
            "on",
            "at",
            "from",
            "is",
            "its",
            "not",
            "be",
            "such",
            "any",
            "when",
            "which",
            "that",
            "this",
            "what",
            "does",
            "say",
            "new",
            "old",
            "law",
            "section",
            "ipc",
            "bns",
            "under",
            "india",
            "indian",
            "penal",
            "code",
            "sanhita",
            "shall",
            "every",
            "person",
            "liable",
            "punishment",
        }
        words = [
            w
            for w in re.findall(r"[a-zA-Z]+", query.lower())
            if w not in STOP and len(w) > 3
        ]
        expanded = list(words)
        for w in words:
            for syn_key, heading_kws in self._synonyms.items():
                key_parts = syn_key.split()
                if w in key_parts:
                    expanded.extend(key_parts)
                    continue
                for hk in heading_kws:
                    if w in hk or hk.startswith(w[: min(4, len(w))]):
                        expanded.extend(key_parts)
                        break

        if expanded:
            score_map: dict[int, float] = {}
            for w in expanded:
                for rid in self._keywords.get(w, []):
                    score_map[rid] = score_map.get(rid, 0) + 1.0
            if score_map:
                max_hits = max(score_map.values())
                ranked = sorted(score_map.keys(), key=lambda x: -score_map[x])
                out: list[MappingRecord] = []
                for rid in ranked:
                    rec = self._records_by_id.get(rid)
                    if not rec or not self._law_ok(rec, law_filter):
                        continue
                    conf = self._tier2_confidence(int(score_map[rid]), int(max_hits))
                    out.append(self._to_record_model(rec, conf, 2))
                    if len(out) >= top_k:
                        break
                if out:
                    result = MapperResponse(
                        query=query,
                        results=out,
                        total=len(out),
                        retrieval_tier=2,
                        message=f"Keyword match: {', '.join(expanded[:5])}",
                    )
                    set_cached_mapper(cache_key, result)
                    return result

        # Tier 3 — semantic FAISS
        embedding = self._model.encode([query], normalize_embeddings=True).astype("float32")
        scores, positions = self._faiss.search(embedding, top_k * 3)

        out = []
        for score, pos in zip(scores[0], positions[0]):
            if pos < 0:
                continue
            rid = int(self._faiss_meta.get(str(pos), -1))
            if rid < 0:
                continue
            rec = self._records_by_id.get(rid)
            if rec and self._law_ok(rec, law_filter):
                out.append(self._to_record_model(rec, float(score), 3))
                if len(out) >= top_k:
                    break

        result = MapperResponse(
            query=query,
            results=out,
            total=len(out),
            retrieval_tier=3,
            message="Semantic search results.",
        )
        set_cached_mapper(cache_key, result)
        return result

    def batch_search(self, queries: list[str], top_k: int = 1, law_filter: Optional[str] = None) -> list[MapperResponse]:
        return [self.search(q, top_k=top_k, law_filter=law_filter) for q in queries]

    def list_repealed(self, limit: int = 50) -> list[MapperResponse]:
        out: list[MapperResponse] = []
        for r in self._records:
            if r.get("status") != "repealed":
                continue
            ipc = r.get("ipc_section", "")
            out.append(
                MapperResponse(
                    query=ipc,
                    results=[self._to_record_model(r, 1.0, 1)],
                    total=1,
                    retrieval_tier=1,
                    message="Repealed section.",
                )
            )
            if len(out) >= limit:
                break
        return out


_mapper_instance: Optional[LegalMapper] = None


def get_mapper() -> LegalMapper:
    global _mapper_instance
    if _mapper_instance is None:
        _mapper_instance = LegalMapper()
    return _mapper_instance


def mapper_singleton_loaded() -> bool:
    return _mapper_instance is not None
