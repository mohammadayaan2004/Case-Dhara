"""
Parse ipc_bns_data.csv into clean JSON records.
Run: python backend/scripts/preprocess.py
Output: backend/data/processed/mapping_db.json, lookup_synonyms.json
"""

import ast
import csv
import json
import re
import shutil
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parents[1]
_RAW_PRIMARY = BASE_DIR / "data" / "raw" / "ipc_bns_data.csv"
_RAW_FALLBACK = BASE_DIR.parent / "dataset" / "ipc_bns_data.csv"
OUT_PATH = BASE_DIR / "data" / "processed" / "mapping_db.json"
SYNONYMS_OUT = BASE_DIR / "data" / "processed" / "lookup_synonyms.json"

# Legal synonyms: search term -> heading keywords matched in corpus headings
LEGAL_SYNONYMS = {
    "homicide": ["murder", "culpable homicide"],
    "sexual assault": ["rape", "outraging modesty"],
    "robbery": ["dacoity", "theft"],
    "fraud": ["cheating", "forgery"],
    "extortion": ["extortion"],
    "kidnapping": ["kidnapping", "abduction"],
    "assault": ["assault", "hurt", "grievous"],
    "defamation": ["defamation"],
    "conspiracy": ["criminal conspiracy"],
    "bribery": ["bribery", "public servant"],
}


def normalize(s: str) -> str:
    s = str(s).strip().upper()
    s = re.sub(r"\b(SECTION|SEC)\s*", "", s)
    s = re.sub(r"^(IPC|BNS)\s*", "", s)
    s = re.sub(r"\s*(IPC|BNS)\s*$", "", s)
    s = re.sub(r"\bU[/\\]S\b\s*", "", s)
    s = re.sub(r"\bUNDER\s*", "", s)
    s = re.sub(r"\s+", "", s)
    return s


def get_all_keys(section_str: str) -> list[str]:
    norm = normalize(section_str)
    keys = [norm]
    base = re.split(r"[,(]", norm)[0]
    if base and base != norm:
        keys.append(base)
    return [k for k in keys if k and k not in ("REPEALDINBNS", "NEW", "REPEALED", "-", "")]


def clean_legal_text(text: str) -> str:
    """Remove common PDF artefacts from IPC text."""
    text = re.sub(r"\d+\[.*?\]", "", text)
    text = text.replace("\xa0", " ")
    text = re.sub(r"\s{2,}", " ", text)
    return text.strip()


def derive_status(ipc_sec: str, bns_sec: str) -> str:
    if "Repealed" in bns_sec or bns_sec.strip() == "":
        return "repealed"
    if bns_sec.strip() in ("New", "New Section", "-"):
        return "new_in_bns"
    if "Repealed" in ipc_sec:
        return "repealed"
    if "," in bns_sec or "&" in bns_sec:
        return "split"
    if "," in ipc_sec or "&" in ipc_sec:
        return "merged"
    return "mapped"


def build_corpus_text(rec: dict) -> str:
    parts: list[str] = []
    if rec["ipc_description"] not in ("Repealed", ""):
        parts.append(
            f"IPC Section {rec['ipc_section']} {rec['ipc_heading']}: {rec['ipc_description'][:800]}"
        )
    if rec["bns_description"] not in ("Repealed in BNS", ""):
        parts.append(
            f"BNS Section {rec['bns_section']} {rec['bns_heading']}: {rec['bns_description'][:800]}"
        )
    return "\n".join(parts)


def resolve_raw_csv_path() -> Path:
    _RAW_PRIMARY.parent.mkdir(parents=True, exist_ok=True)
    if _RAW_PRIMARY.exists():
        return _RAW_PRIMARY
    if _RAW_FALLBACK.exists():
        shutil.copy2(_RAW_FALLBACK, _RAW_PRIMARY)
        print(f"Copied dataset -> {_RAW_PRIMARY}")
        return _RAW_PRIMARY
    raise FileNotFoundError(
        f"No ipc_bns_data.csv at {_RAW_PRIMARY} or {_RAW_FALLBACK}"
    )


def main() -> None:
    raw_path = resolve_raw_csv_path()
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)

    records: list[dict] = []
    with raw_path.open("r", encoding="utf-8", newline="") as f:
        for idx, row in enumerate(csv.DictReader(f)):
            try:
                r = ast.literal_eval(row["response"])
                ipc_desc = clean_legal_text(r.get("IPC Descriptions", "").strip())
                bns_desc = r.get("BNS description", "").strip()
                ipc_sec = r.get("IPC Section", "").strip()
                bns_sec = r.get("BNS Section", "").strip()

                rec = {
                    "id": idx,
                    "prompt": row["prompts"],
                    "ipc_section": ipc_sec,
                    "ipc_heading": r.get("IPC Heading", "").strip(),
                    "ipc_description": ipc_desc,
                    "bns_section": bns_sec,
                    "bns_heading": r.get("BNS Heading", "").strip(),
                    "bns_description": bns_desc,
                }
                rec["status"] = derive_status(ipc_sec, bns_sec)
                rec["corpus_text"] = build_corpus_text(rec)
                records.append(rec)
            except Exception as exc:
                print(f"skip row {idx}: {exc}")

    with OUT_PATH.open("w", encoding="utf-8") as f:
        json.dump(records, f, indent=2, ensure_ascii=False)
    print(f"Saved {len(records)} records -> {OUT_PATH}")

    with SYNONYMS_OUT.open("w", encoding="utf-8") as f:
        json.dump(LEGAL_SYNONYMS, f, indent=2, ensure_ascii=False)
    print(f"Saved synonyms -> {SYNONYMS_OUT}")


if __name__ == "__main__":
    main()
