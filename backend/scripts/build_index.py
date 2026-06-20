"""
Build exact lookup, keyword lookup, and FAISS semantic index.
"""

import argparse
import json
import os
import re
import sys
from pathlib import Path

os.environ.setdefault("TRANSFORMERS_NO_TF", "1")

import faiss
import numpy as np
from sentence_transformers import SentenceTransformer

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from scripts.preprocess import get_all_keys

BASE_DIR = Path(__file__).resolve().parents[1]
PROCESSED_DIR = BASE_DIR / "data" / "processed"
FAISS_DIR = PROCESSED_DIR / "faiss_index"
SYNONYMS_FILE = PROCESSED_DIR / "lookup_synonyms.json"
DEFAULT_MODEL_NAME = "sentence-transformers/all-mpnet-base-v2"

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
    "are",
    "was",
    "were",
    "has",
    "have",
    "india",
    "indian",
    "penal",
    "code",
    "sanhita",
    "nyaya",
    "bharatiya",
    "shall",
    "section",
    "every",
    "person",
    "liable",
    "punishment",
    "under",
    "ipc",
    "bns",
}


def build_exact_lookup(records: list[dict]) -> dict[str, int]:
    lookup: dict[str, int] = {}
    for rec in records:
        for key in get_all_keys(rec.get("ipc_section", "")):
            lookup.setdefault(key, rec["id"])
        for key in get_all_keys(rec.get("bns_section", "")):
            lookup.setdefault(key, rec["id"])
    return lookup


def build_keyword_index(records: list[dict], synonyms: dict[str, list[str]]) -> dict[str, list[int]]:
    kw_map: dict[str, list[int]] = {}

    def index_word(word: str, rec_id: int) -> None:
        if word not in STOP and len(word) > 3:
            kw_map.setdefault(word, [])
            if rec_id not in kw_map[word]:
                kw_map[word].append(rec_id)

    for rec in records:
        text = (
            f"{rec.get('ipc_heading', '')} {rec.get('bns_heading', '')} "
            f"{rec.get('ipc_section', '')} {rec.get('bns_section', '')} "
            f"{rec.get('ipc_description', '')[:200]}"
        )
        for word in re.findall(r"[a-zA-Z]+", text.lower()):
            index_word(word, rec["id"])

    for syn_term, heading_keywords in synonyms.items():
        for rec in records:
            heading_lower = (rec.get("ipc_heading", "") + " " + rec.get("bns_heading", "")).lower()
            if any(kw in heading_lower for kw in heading_keywords):
                for word in syn_term.split():
                    index_word(word, rec["id"])

    return kw_map


def build_faiss(records: list[dict], model_name: str) -> None:
    FAISS_DIR.mkdir(parents=True, exist_ok=True)
    print(f"Loading embedding model: {model_name}")
    model = SentenceTransformer(model_name)

    valid = [r for r in records if len((r.get("corpus_text") or "").strip()) > 50]
    texts = [r["corpus_text"] for r in valid]
    ids = [r["id"] for r in valid]

    embeddings = model.encode(texts, batch_size=32, show_progress_bar=True, normalize_embeddings=True)
    embeddings = np.asarray(embeddings, dtype="float32")

    dim = embeddings.shape[1]
    index = faiss.IndexFlatIP(dim)
    index.add(embeddings)

    faiss.write_index(index, str(FAISS_DIR / "index.faiss"))
    metadata = {str(i): rid for i, rid in enumerate(ids)}
    with (FAISS_DIR / "metadata.json").open("w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print(f"FAISS index created: vectors={index.ntotal}, dim={dim}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Build exact, keyword, and FAISS indexes.")
    parser.add_argument("--model", default=DEFAULT_MODEL_NAME, help="SentenceTransformer model path or name.")
    args = parser.parse_args()

    with (PROCESSED_DIR / "mapping_db.json").open("r", encoding="utf-8") as f:
        records = json.load(f)

    synonyms: dict = {}
    if SYNONYMS_FILE.exists():
        with SYNONYMS_FILE.open("r", encoding="utf-8") as f:
            synonyms = json.load(f)

    exact = build_exact_lookup(records)
    with (PROCESSED_DIR / "lookup_exact.json").open("w", encoding="utf-8") as f:
        json.dump(exact, f, indent=2)
    print(f"Exact keys: {len(exact)}")

    keywords = build_keyword_index(records, synonyms)
    with (PROCESSED_DIR / "lookup_keywords.json").open("w", encoding="utf-8") as f:
        json.dump(keywords, f, indent=2)
    print(f"Keyword entries: {len(keywords)}")

    build_faiss(records, model_name=args.model)
    print("All indexes built.")


if __name__ == "__main__":
    main()
