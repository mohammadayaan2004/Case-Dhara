"""
3-tier legal section retrieval: exact → keyword+synonym → semantic FAISS.
Uses sentence-transformers/all-mpnet-base-v2 ML model for semantic search.
LRU cache on full search results.
"""
import logging
import json
import math
import os
import re
from pathlib import Path
from typing import Optional

os.environ.setdefault("TRANSFORMERS_NO_TF", "1")

import faiss
import numpy as np
from sentence_transformers import SentenceTransformer

from schemas.mapper_schema import MapperResponse, MappingRecord
from services.cache_service import (
    get_cached_mapper,
    mapper_cache_key,
    set_cached_mapper,
)

logger = logging.getLogger("case_dhara.mapper")


BASE_DIR = Path(__file__).resolve().parents[1]
PROCESSED_DIR = BASE_DIR / "data" / "processed"
FAISS_DIR = PROCESSED_DIR / "faiss_index"
SYNONYMS_PATH = PROCESSED_DIR / "lookup_synonyms.json"

MODEL_NAME = "sentence-transformers/all-mpnet-base-v2"

_VALID_LAW_PREFIXES = {"IPC", "BNS"}
_INVALID_PREFIX_RE = re.compile(r"^([A-Z]{2,6})\s+\d", re.IGNORECASE)
_LAW_PREFIX_RE = re.compile(r"^(IPC|BNS)\s*\d", re.IGNORECASE)


def _detect_prefix(s: str) -> Optional[str]:
    m = _LAW_PREFIX_RE.match(s.strip())
    return m.group(1).upper() if m else None


def _has_invalid_prefix(s: str) -> bool:
    m = _INVALID_PREFIX_RE.match(s.strip())
    if not m:
        return False
    return m.group(1).upper() not in _VALID_LAW_PREFIXES


def _norm(s: str) -> str:
    s = str(s).strip().upper()
    s = re.sub(r"\b(SECTION|SEC)\s*", "", s)
    s = re.sub(r"^(IPC|BNS)\s*", "", s)
    s = re.sub(r"\s*(IPC|BNS)\s*$", "", s)
    s = re.sub(r"\bU[/\\]S\b\s*", "", s)
    s = re.sub(r"\bUNDER\s*", "", s)
    s = re.sub(r"\s+", "", s)
    return s


def _query_keys(s: str) -> list:
    norm = _norm(s)
    keys = [norm]
    base = re.split(r"[,(]", norm)[0]
    if base and base != norm:
        keys.append(base)
    return keys


def _extract_section_refs(text: str) -> list:
    patterns = [
        r"\b(?:IPC|BNS)?\s*(\d+[A-Z]*(?:\(\d+\))?)[\s,]*(?:IPC|BNS|r/w|read with|$)",
        r"\bu[/\\]s\s+(\d+[A-Z]*(?:\(\d+\))?)",
        r"\b[Ss]ection\s+(\d+[A-Z]*(?:\(\d+\))?)",
    ]
    found: set = set()
    for pat in patterns:
        for m in re.findall(pat, text, flags=re.IGNORECASE):
            cleaned = re.sub(r"\s+", "", str(m).upper())
            if cleaned:
                found.add(cleaned)
    return list(found)


class LegalMapper:
    def __init__(self) -> None:
        # ── Load mapping database ───────────────────────────────────────────────
        with (PROCESSED_DIR / "mapping_db.json").open("r", encoding="utf-8") as f:
            self._records = json.load(f)

        self._records_by_id: dict = {}
        for r in self._records:
            rid = r.get("id")
            if rid is not None:
                self._records_by_id[int(rid)] = r

        # ── IPC exact lookup (prebuilt, keyed by IPC section number) ───────────
        with (PROCESSED_DIR / "lookup_exact.json").open("r", encoding="utf-8") as f:
            self._exact: dict = json.load(f)

        # ── BNS reverse lookup (built at startup from mapping_db) ──────────────
        # lookup_exact.json is indexed by IPC section numbers only.
        # "bns 35" must find the record whose bns_section == "35" (IPC 97),
        # not the record whose ipc_section happens to be "35" (different section).
        # Multiple records can share the same BNS number (split/merged cases),
        # so we store a list of record ids per normalised BNS key.
        self._bns_exact: dict = {}
        for r in self._records:
            bns_sec = (r.get("bns_section") or "").strip()
            if not bns_sec or bns_sec.upper() == "REPEALED IN BNS":
                continue
            key = re.sub(r"\s+", "", bns_sec.upper())
            self._bns_exact.setdefault(key, []).append(int(r["id"]))

        # ── Keyword lookup ─────────────────────────────────────────────────────
        with (PROCESSED_DIR / "lookup_keywords.json").open("r", encoding="utf-8") as f:
            raw_kw = json.load(f)
        self._keywords: dict = {kw: [int(i) for i in ids] for kw, ids in raw_kw.items()}

        # ── Synonym lookup ─────────────────────────────────────────────────────
        self._synonyms: dict = {}
        if SYNONYMS_PATH.exists():
            with SYNONYMS_PATH.open("r", encoding="utf-8") as f:
                self._synonyms = json.load(f)

        # ── FAISS semantic index ───────────────────────────────────────────────
        self._faiss = faiss.read_index(str(FAISS_DIR / "index.faiss"))
        with (FAISS_DIR / "metadata.json").open("r", encoding="utf-8") as f:
            raw_meta = json.load(f)
        self._faiss_meta: dict = {str(k): int(v) for k, v in raw_meta.items()}
        self._validate_faiss_meta()

        # ── ML embedding model ─────────────────────────────────────────────────
        ft_path = BASE_DIR / "models" / "finetuned_retriever"
        if ft_path.exists() and (ft_path / "config.json").exists():
            model_path = str(ft_path)
        else:
            model_path = os.getenv("EMBEDDING_MODEL_NAME", MODEL_NAME)
        logger.info("Loading retriever ML model: %s", model_path)
        self._model = SentenceTransformer(model_path)
        logger.info("Mapper ready.")

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
            id=int(rec["id"]),
            ipc_section=rec.get("ipc_section") or "",
            ipc_heading=rec.get("ipc_heading") or "",
            ipc_description=rec.get("ipc_description") or "",
            bns_section=rec.get("bns_section") or "",
            bns_heading=rec.get("bns_heading") or "",
            bns_description=rec.get("bns_description") or "",
            status=self._status_or_default(rec.get("status") or "mapped"),
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
        if max_hits <= 0:
            return 0.5
        damped = 0.55 + 0.30 * math.log1p(hits) / math.log1p(max_hits)
        linear = hits / max_hits
        return min(0.85, min(linear, damped))

    def _lookup_bns_section(self, query: str, top_k: int) -> list:
        """
        Look up records by BNS section number using _bns_exact.
        Returns a list of MappingRecord models (up to top_k), confidence 1.0.
        Handles split/merged sections where multiple records share one BNS number.
        """
        results = []
        for key in _query_keys(query):
            rids = self._bns_exact.get(key)
            if rids:
                for rid in rids:
                    rec = self._records_by_id.get(rid)
                    if rec:
                        results.append(self._to_record_model(rec, 1.0, 1))
                    if len(results) >= top_k:
                        break
                break
        return results

    def search(self, query: str, top_k: int = 5, law_filter: Optional[str] = None) -> MapperResponse:
        query = query.strip()

        # ── Guard: reject unrecognised law-code prefixes ───────────────────────
        # Queries like "bms 43", "bnss 35", "crpc 200" look like law references
        # but use codes we don't know. Return a clear error rather than silently
        # returning wrong results from a plain-number lookup.
        if _has_invalid_prefix(query):
            bad = _INVALID_PREFIX_RE.match(query).group(1).upper()
            sec = query.split()[-1]
            return MapperResponse(
                query=query,
                results=[],
                total=0,
                retrieval_tier=1,
                message=(
                    f'"{bad}" is not a recognised law code. '
                    f'Did you mean "IPC {sec}" or "BNS {sec}"?'
                ),
            )

        # ── Auto-apply law filter from explicit query prefix ───────────────────
        # "BNS 35" typed in the All panel must search BNS-side records.
        # "IPC 101" typed in the All panel must search IPC-side records.
        detected_prefix = _detect_prefix(query)
        if detected_prefix == "IPC":
            law_filter = "ipc"
        elif detected_prefix == "BNS":
            law_filter = "bns"

        cache_key = mapper_cache_key(query, top_k, law_filter)
        cached = get_cached_mapper(cache_key)
        if cached is not None:
            return cached

        # ── Tier 1A: Exact section match ──────────────────────────────────────
        #
        # lookup_exact.json is keyed by IPC section numbers.
        # When law_filter == "bns" we MUST use the BNS reverse lookup (_bns_exact)
        # so that "bns 35" returns the record with bns_section="35" (IPC 97),
        # not the record with ipc_section="35" (a completely different provision).
        #
        # Lookup order:
        #   law_filter == "bns"  → BNS reverse lookup first
        #   law_filter == "ipc" or None → IPC prebuilt lookup first

        if law_filter == "bns":
            bns_hits = self._lookup_bns_section(query, top_k)
            if bns_hits:
                result = MapperResponse(
                    query=query,
                    results=bns_hits,
                    total=len(bns_hits),
                    retrieval_tier=1,
                    message="Exact BNS section match.",
                )
                set_cached_mapper(cache_key, result)
                return result
            # No exact BNS match — fall through to keyword / semantic tiers
        elif law_filter == "ipc":
            # IPC-only: search IPC-keyed exact lookup
            for key in _query_keys(query):
                if key in self._exact:
                    rid = int(self._exact[key])
                    rec = self._records_by_id.get(rid)
                    if rec and self._law_ok(rec, law_filter):
                        msg = "Exact IPC section match."
                        if rec.get("status") == "repealed":
                            ipc = rec.get("ipc_section", "")
                            msg = (
                                f"IPC {ipc} has been REPEALED under BNS 2023 "
                                "(no direct equivalent)."
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
        else:
            # law_filter is None (All): search BOTH IPC and BNS exact lookups for
            # full bidirectional support — IPC section numbers and BNS section
            # numbers can differ, so we must check both sides.
            merged: dict = {}  # rid → MappingRecord, deduped
            for key in _query_keys(query):
                # IPC-keyed lookup
                if key in self._exact:
                    rid = int(self._exact[key])
                    rec = self._records_by_id.get(rid)
                    if rec and rid not in merged:
                        merged[rid] = self._to_record_model(rec, 1.0, 1)
                # BNS-keyed lookup
                bns_rids = self._bns_exact.get(key)
                if bns_rids:
                    for bns_rid in bns_rids:
                        bns_rec = self._records_by_id.get(bns_rid)
                        if bns_rec and bns_rid not in merged:
                            merged[bns_rid] = self._to_record_model(bns_rec, 1.0, 1)
            if merged:
                hits = list(merged.values())[:top_k]
                msg = "Exact section match." if len(hits) == 1 else f"{len(hits)} exact section matches (IPC & BNS)."
                result = MapperResponse(
                    query=query,
                    results=hits,
                    total=len(hits),
                    retrieval_tier=1,
                    message=msg,
                )
                set_cached_mapper(cache_key, result)
                return result

        # ── Tier 1B: FIR-style embedded section refs ──────────────────────────
        # Skip for BNS-only queries: embedded refs are resolved via the IPC-keyed
        # exact lookup so they would return the wrong sections for BNS queries.
        if law_filter != "bns":
            embedded = _extract_section_refs(query)
            if embedded:
                tier1b: list = []
                for sec in embedded:
                    rid = None
                    for k in _query_keys(sec):
                        if k in self._exact:
                            rid = int(self._exact[k])
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

        # ── Tier 2: Keyword + synonym expansion ───────────────────────────────
        STOP = {
            "of", "the", "a", "an", "and", "or", "to", "by", "in", "for",
            "with", "on", "at", "from", "is", "its", "not", "be", "such",
            "any", "when", "which", "that", "this", "what", "does", "say",
            "new", "old", "law", "section", "ipc", "bns", "under", "india",
            "indian", "penal", "code", "sanhita", "shall", "every", "person",
            "liable", "punishment",
        }
        words = [
            w for w in re.findall(r"[a-zA-Z]+", query.lower())
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
            score_map: dict = {}
            for w in expanded:
                for rid in self._keywords.get(w, []):
                    rid_int = int(rid)
                    score_map[rid_int] = score_map.get(rid_int, 0) + 1.0
            if score_map:
                max_hits = max(score_map.values())
                ranked = sorted(score_map.keys(), key=lambda x: -score_map[x])
                out: list = []
                for rid in ranked:
                    rec = self._records_by_id.get(int(rid))
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
                        message="Keyword match.",
                    )
                    set_cached_mapper(cache_key, result)
                    return result

        # ── Tier 3: Semantic FAISS ─────────────────────────────────────────────
        embedding = self._model.encode(
            [query], normalize_embeddings=True
        ).astype("float32")
        scores, positions = self._faiss.search(embedding, top_k * 3)

        out = []
        for score, pos in zip(scores[0], positions[0]):
            if pos < 0:
                continue
            rid = self._faiss_meta.get(str(int(pos)), -1)
            if rid < 0:
                continue
            rec = self._records_by_id.get(int(rid))
            if rec and self._law_ok(rec, law_filter):
                out.append(self._to_record_model(rec, float(score), 3))
                if len(out) >= top_k:
                    break

        result = MapperResponse(
            query=query,
            results=out,
            total=len(out),
            retrieval_tier=3,
            message="Semantic search results (ML model).",
        )
        set_cached_mapper(cache_key, result)
        return result

    def batch_search(self, queries: list, top_k: int = 1, law_filter: Optional[str] = None) -> list:
        return [self.search(q, top_k=top_k, law_filter=law_filter) for q in queries]

    def list_repealed(self, limit: int = 50) -> list:
        out: list = []
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