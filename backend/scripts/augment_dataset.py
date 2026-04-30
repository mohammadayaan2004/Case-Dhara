"""
Generate augmented query–corpus pairs via Gemini for better retriever fine-tuning.
Requires GEMINI_API_KEY. Run after preprocess.py.
Migrated to the new google-genai SDK (client-based API).

Usage: python backend/scripts/augment_dataset.py
"""

import json
import os
import time
from pathlib import Path

from google import genai
from google.genai import types

BASE_DIR = Path(__file__).resolve().parents[1]
DB_PATH = BASE_DIR / "data" / "processed" / "mapping_db.json"
OUT_PATH = BASE_DIR / "data" / "processed" / "augmented_pairs.json"

AUGMENT_PROMPT = """You are generating training data for an Indian legal search system.

Given this IPC/BNS section mapping:
IPC {ipc_sec} ({ipc_heading}): {ipc_desc_short}
BNS {bns_sec} ({bns_heading}): {bns_desc_short}

Generate exactly 10 DIVERSE search queries a user might type to find this section.
Include: section number queries, keyword queries, scenario queries, FIR-style text,
comparison queries (IPC vs BNS), and 2 queries in Hindi/Hinglish.

Return ONLY a JSON array of 10 strings. No explanation."""


def augment_record(client: genai.Client, rec: dict) -> list[str]:
    ipc_desc_short = (rec.get("ipc_description") or "")[:300]
    bns_desc_short = (rec.get("bns_description") or "")[:300]
    if not ipc_desc_short and not bns_desc_short:
        return []
    prompt = AUGMENT_PROMPT.format(
        ipc_sec=rec.get("ipc_section", ""),
        ipc_heading=rec.get("ipc_heading", ""),
        ipc_desc_short=ipc_desc_short,
        bns_sec=rec.get("bns_section", ""),
        bns_heading=rec.get("bns_heading", ""),
        bns_desc_short=bns_desc_short,
    )
    try:
        response = client.models.generate_content(
            model="gemini-1.5-pro",
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                max_output_tokens=500,
            ),
        )
        text = response.text.strip()
        data = json.loads(text)
        if isinstance(data, list):
            return [str(x).strip() for x in data if str(x).strip()]
    except Exception as exc:
        print(f"  augment error IPC {rec.get('ipc_section')}: {exc}")
    return []


def main() -> None:
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    if not DB_PATH.exists():
        raise SystemExit(f"Run preprocess first; missing {DB_PATH}")

    with DB_PATH.open("r", encoding="utf-8") as f:
        db = json.load(f)

    gemini_key = os.getenv("GEMINI_API_KEY")
    if not gemini_key:
        raise SystemExit("Missing GEMINI_API_KEY")

    # New SDK: create a Client instance instead of genai.configure()
    client = genai.Client(api_key=gemini_key)

    result: list[dict] = []

    for i, rec in enumerate(db):
        print(f"[{i + 1}/{len(db)}] Augmenting IPC {rec.get('ipc_section')}...")
        queries = augment_record(client, rec)
        corpus = rec.get("corpus_text") or ""
        for q in queries:
            result.append({"query": q, "corpus": corpus, "record_id": rec["id"]})
        time.sleep(0.3)

    with OUT_PATH.open("w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)
    print(f"Saved {len(result)} augmented pairs -> {OUT_PATH}")


if __name__ == "__main__":
    main()
