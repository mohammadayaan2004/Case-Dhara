"""
Generate augmented query–corpus pairs using rule-based templates.
No API key needed. Runs instantly. Run after preprocess.py.

Usage: python backend/scripts/augment_dataset.py
"""

import json
import random
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
DB_PATH  = BASE_DIR / "data" / "processed" / "mapping_db.json"
OUT_PATH = BASE_DIR / "data" / "processed" / "augmented_pairs.json"


def generate_queries(rec: dict) -> list[str]:
    ipc = rec.get("ipc_section", "")
    bns = rec.get("bns_section", "")
    ipc_h = rec.get("ipc_heading", "")
    bns_h = rec.get("bns_heading", "")
    ipc_d = (rec.get("ipc_description") or "")[:200].strip()
    bns_d = (rec.get("bns_description") or "")[:200].strip()
    heading = ipc_h or bns_h or ""

    queries = []

    # 1. Direct section number lookups
    if ipc:
        queries += [
            f"IPC section {ipc}",
            f"Section {ipc} IPC",
            f"Indian Penal Code section {ipc}",
            f"what is IPC {ipc}",
            f"IPC {ipc} punishment",
            f"IPC {ipc} bailable or non bailable",
        ]
    if bns:
        queries += [
            f"BNS section {bns}",
            f"Section {bns} BNS",
            f"Bharatiya Nyaya Sanhita section {bns}",
            f"what is BNS {bns}",
        ]

    # 2. IPC vs BNS comparison queries
    if ipc and bns:
        queries += [
            f"IPC {ipc} equivalent in BNS",
            f"BNS {bns} replaced IPC {ipc}",
            f"difference between IPC {ipc} and BNS {bns}",
            f"IPC {ipc} to BNS conversion",
        ]

    # 3. Heading-based keyword queries
    if heading:
        queries += [
            heading,
            f"{heading} IPC",
            f"{heading} BNS",
            f"law related to {heading.lower()}",
            f"section for {heading.lower()}",
            f"punishment for {heading.lower()}",
            f"FIR under {heading.lower()}",
            f"case under {heading.lower()}",
            f"offence of {heading.lower()}",
        ]

    # 4. Description snippet queries
    for desc in [ipc_d, bns_d]:
        if len(desc) > 30:
            # Take first meaningful phrase (up to first comma or 60 chars)
            snippet = desc.split(",")[0][:60].strip()
            if snippet:
                queries.append(snippet)
                queries.append(f"{snippet} section")

    # 5. FIR / practical scenario style
    if heading:
        h = heading.lower()
        queries += [
            f"which section applies for {h}",
            f"accused of {h} which IPC section",
            f"FIR filed under {h}",
            f"bail in {h} case",
            f"sentence for {h} in India",
        ]

    # 6. Hindi/Hinglish queries (simple transliterations)
    if heading:
        h = heading.lower()
        queries += [
            f"{h} ki dhara konsi hai",
            f"{h} mein kaun si dhara lagti hai",
        ]

    # Deduplicate while preserving order
    seen = set()
    unique = []
    for q in queries:
        q = q.strip()
        if q and q not in seen:
            seen.add(q)
            unique.append(q)

    return unique


def main() -> None:
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)

    if not DB_PATH.exists():
        raise SystemExit(f"Run preprocess first; missing {DB_PATH}")

    with DB_PATH.open("r", encoding="utf-8") as f:
        db = json.load(f)

    result: list[dict] = []

    for i, rec in enumerate(db):
        ipc = rec.get("ipc_section", "?")
        print(f"[{i + 1}/{len(db)}] Augmenting IPC {ipc}...")
        queries  = generate_queries(rec)
        corpus   = rec.get("corpus_text") or ""
        rec_id   = rec.get("id", i)
        for q in queries:
            result.append({"query": q, "corpus": corpus, "record_id": rec_id})

    with OUT_PATH.open("w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)

    print(f"\nDone! Saved {len(result)} augmented pairs → {OUT_PATH}")
    print(f"Average {len(result) / max(len(db), 1):.1f} queries per record")


if __name__ == "__main__":
    main()