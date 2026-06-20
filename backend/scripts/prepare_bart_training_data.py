"""
Step 1 of BART fine-tuning pipeline — NO external API required.

Reads all court judgment PDFs from data/training_pdfs/
Extracts text from each PDF (uses pdfplumber, falls back to OCR)
Generates summaries using extractive + regex methods (no Gemini API call)
Saves training data to data/processed/bart_training/train.json and val.json

The original script used Gemini API to generate "gold" summaries for BART training.
That dependency has been removed. Instead this script uses:
  1. Regex extraction of key fields (parties, court, sections, held, outcome)
  2. Extractive summarization (top-N scored sentences via TF-IDF-style scoring)
  3. Lead + tail heuristic for when sentence scoring fails

These summaries are "silver-standard" — not as polished as Gemini-generated ones,
but fully sufficient for domain fine-tuning of BART on Indian legal judgments.

Usage (from case_dhara_backend/ directory):
    python scripts/prepare_bart_training_data.py

Optional flags:
    --pdf_dir path/to/pdfs          (default: data/training_pdfs)
    --out_dir path/to/output        (default: data/processed/bart_training)
    --val_split 0.1                 (default: 10% validation)
    --max_pdfs 500                  (default: all PDFs found)
    --min_summary_len 60            (default: skip records with summary shorter than this)
"""

import argparse
import io
import json
import math
import re
import sys
from collections import Counter
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))


# ── PDF extraction ────────────────────────────────────────────────────────────

def extract_text_from_pdf(pdf_path: Path) -> str:
    """Extract text from a PDF file using pdfplumber, fall back to OCR."""
    pdf_bytes = pdf_path.read_bytes()
    try:
        import pdfplumber
        pages = []
        with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
            for page in pdf.pages:
                t = page.extract_text()
                if t:
                    pages.append(t)
        text = "\n\n".join(pages)
        if len(text.strip()) >= 200:
            return _clean(text)
    except Exception as e:
        print(f"    pdfplumber failed: {e}")

    try:
        import fitz
        import pytesseract
        from PIL import Image
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        pages = []
        for i in range(len(doc)):
            pix = doc[i].get_pixmap(dpi=200)
            img = Image.frombytes("RGB", [pix.width, pix.height], pix.samples)
            pages.append(pytesseract.image_to_string(img, lang="eng"))
        return _clean("\n\n".join(pages))
    except Exception as e:
        print(f"    OCR fallback failed: {e}")
        return ""


def _clean(text: str) -> str:
    text = re.sub(r"^\s*\d+\s*$", "", text, flags=re.MULTILINE)
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = re.sub(r"\x0c", "\n", text)
    text = re.sub(r"\xa0", " ", text)
    return text.strip()


# ── Smart truncation ──────────────────────────────────────────────────────────

def smart_truncate(text: str, max_chars: int = 4000) -> str:
    """Keep beginning + middle + end so BART sees a representative slice."""
    if len(text) <= max_chars:
        return text
    third = max_chars // 3
    mid = len(text) // 2
    half = third // 2
    return (
        text[:third]
        + "\n\n[...middle section...]\n\n"
        + text[max(0, mid - half): mid + half]
        + "\n\n[...end section...]\n\n"
        + text[-third:]
    )


# ── Extractive summarizer ─────────────────────────────────────────────────────

_LEGAL_STOP = {
    "the", "a", "an", "and", "or", "of", "to", "in", "is", "was", "are", "were",
    "be", "been", "being", "have", "has", "had", "do", "does", "did", "for",
    "on", "at", "by", "from", "with", "as", "that", "this", "it", "its",
    "he", "she", "they", "we", "i", "his", "her", "their", "our",
    "not", "but", "if", "then", "when", "which", "who", "what", "how",
    "any", "all", "also", "such", "under", "said", "shall", "may", "upon",
}

_LEGAL_IMPORTANT = {
    "held", "convicted", "acquitted", "appeal", "accused", "section", "ipc", "bns",
    "court", "judge", "offence", "sentence", "bail", "remand", "evidence",
    "prosecution", "defence", "defendant", "petitioner", "respondent", "appellant",
    "dismissed", "allowed", "disposed", "judgment", "order", "penalty", "fine",
}


def _tokenize(text: str) -> list:
    return [w.lower() for w in re.findall(r"[a-zA-Z]+", text)]


def _sentence_score(sentence: str, word_idf: dict) -> float:
    words = _tokenize(sentence)
    if not words:
        return 0.0
    score = 0.0
    for w in words:
        if w in _LEGAL_STOP:
            continue
        score += word_idf.get(w, 0.0)
        if w in _LEGAL_IMPORTANT:
            score += 2.0
    # Prefer sentences of moderate length (30-120 words)
    length_factor = 1.0
    n = len(words)
    if n < 10:
        length_factor = 0.4
    elif n > 150:
        length_factor = 0.7
    return score * length_factor


def extractive_summary(text: str, n_sentences: int = 6) -> str:
    """
    Select the top-N most informative sentences using TF-IDF-style scoring.
    Preserves document order of selected sentences.
    """
    # Split into sentences
    raw_sentences = re.split(r"(?<=[.!?])\s+", text.replace("\n", " "))
    sentences = [s.strip() for s in raw_sentences if len(s.strip()) > 30]

    if not sentences:
        return text[:400]

    if len(sentences) <= n_sentences:
        return " ".join(sentences)

    # Build IDF from this document (document = set of sentences as mini-docs)
    N = len(sentences)
    doc_freq: Counter = Counter()
    for sent in sentences:
        words = set(_tokenize(sent)) - _LEGAL_STOP
        doc_freq.update(words)

    word_idf: dict = {w: math.log((N + 1) / (df + 1)) for w, df in doc_freq.items()}

    # Score sentences
    scored = [(i, _sentence_score(s, word_idf), s) for i, s in enumerate(sentences)]
    scored.sort(key=lambda x: -x[1])

    # Take top-N, but always include first sentence (gives context)
    top_indices = {scored[0][0]}  # first sentence always included
    for i, _score, _sent in scored[:n_sentences]:
        top_indices.add(i)
        if len(top_indices) >= n_sentences:
            break

    # Restore document order
    selected = [s for i, _sc, s in scored if i in top_indices]
    # Re-sort by original position
    order = {idx: pos for pos, (idx, _, __) in enumerate(scored)}
    selected_with_pos = [(i, s) for i, _, s in scored if i in top_indices]
    selected_with_pos.sort(key=lambda x: x[0])

    return " ".join(s for _, s in selected_with_pos)


# ── Regex-based structured summary ───────────────────────────────────────────

def regex_summary(text: str) -> str:
    """
    Generate a rule-based summary from the judgment text.
    Extracts: court, parties, sections, held, outcome.
    """
    parts = []

    # Case title / parties
    vs = re.search(r"^(.*?)\s+v(?:s\.?|ersus\.?)\s+(.*?)$", text[:2000], re.IGNORECASE | re.MULTILINE)
    if vs:
        parts.append(f"Case: {vs.group(1).strip()} v. {vs.group(2).strip()}.")

    # Court
    for kw in ["Supreme Court", "High Court", "Sessions Court", "District Court",
               "Magistrate Court", "Fast Track Court"]:
        if kw.lower() in text[:3000].lower():
            parts.append(f"Court: {kw}.")
            break

    # Sections invoked
    secs = re.findall(
        r"\b(?:IPC|BNS|Section|u[/\\]s)\s*(\d+[A-Z]*(?:\(\d+\))?)\b",
        text[:8000], re.IGNORECASE
    )
    if secs:
        unique_secs = list(dict.fromkeys(secs[:6]))
        parts.append(f"Sections invoked: {', '.join(unique_secs)}.")

    # Held
    held = re.search(
        r"(?:HELD|held\s*[:;\-]|the court held)\s*[:\-]?\s*(.{80,500})",
        text, re.IGNORECASE | re.DOTALL
    )
    if held:
        parts.append(f"Held: {held.group(1).strip()[:300]}")

    # Outcome
    t = text.lower()
    if "acquitted" in t:
        parts.append("Outcome: acquittal.")
    elif "convicted" in t:
        parts.append("Outcome: conviction.")
    elif "appeal allowed" in t:
        parts.append("Outcome: appeal allowed.")
    elif "appeal dismissed" in t:
        parts.append("Outcome: appeal dismissed.")
    elif "remand" in t:
        parts.append("Outcome: case remanded.")

    return " ".join(parts) if parts else ""


# ── Combined summary (regex + extractive) ────────────────────────────────────

def generate_summary(text: str) -> str:
    """
    Combine structured regex extraction with extractive sentences for a richer summary.
    This replaces the Gemini API call — no external network call needed.
    """
    # Part 1: regex-extracted structured facts
    struct = regex_summary(text)

    # Part 2: extractive summary of the body
    # Skip the first 500 chars (header/title) to avoid duplicating case name
    body = text[500:] if len(text) > 1000 else text
    extractive = extractive_summary(body, n_sentences=5)

    # Combine
    if struct and extractive:
        combined = struct + " " + extractive
    elif struct:
        combined = struct
    elif extractive:
        combined = extractive
    else:
        combined = text[:400]

    # Hard-cap at 300 words (approximate)
    words = combined.split()
    if len(words) > 300:
        combined = " ".join(words[:300]) + "..."

    return combined.strip()


# ── Main pipeline ─────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Prepare BART training data from PDF judgments (no external API)"
    )
    parser.add_argument("--pdf_dir", default=str(BASE_DIR / "data" / "training_pdfs"),
                        help="Folder containing court judgment PDFs")
    parser.add_argument("--out_dir", default=str(BASE_DIR / "data" / "processed" / "bart_training"),
                        help="Output folder for train.json and val.json")
    parser.add_argument("--val_split", type=float, default=0.1,
                        help="Fraction to use as validation set (default: 0.1)")
    parser.add_argument("--max_pdfs", type=int, default=0,
                        help="Max PDFs to process (0 = all)")
    parser.add_argument("--min_summary_len", type=int, default=60,
                        help="Skip records whose summary is shorter than this (default: 60)")
    args = parser.parse_args()

    pdf_dir = Path(args.pdf_dir)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    pdf_files = sorted([f for f in pdf_dir.iterdir() if f.suffix.lower() == ".pdf"])
    if not pdf_files:
        print(f"No PDF files found in: {pdf_dir}")
        print("Place your court judgment PDFs there and re-run.")
        sys.exit(1)

    if args.max_pdfs > 0:
        pdf_files = pdf_files[:args.max_pdfs]

    print(f"Found {len(pdf_files)} PDFs in {pdf_dir}")
    print("Generating summaries locally (extractive + regex — no API calls).\n")

    records = []
    failed = []

    for i, pdf_path in enumerate(pdf_files):
        print(f"[{i+1}/{len(pdf_files)}] {pdf_path.name}")

        text = extract_text_from_pdf(pdf_path)
        if len(text.strip()) < 100:
            print(f"    SKIP: could not extract text.")
            failed.append(pdf_path.name)
            continue

        summary = generate_summary(text)

        if not summary or len(summary) < args.min_summary_len:
            print(f"    SKIP: summary too short ({len(summary)} chars).")
            failed.append(pdf_path.name)
            continue

        bart_input = smart_truncate(text, max_chars=4000)

        records.append({
            "id": i,
            "source_file": pdf_path.name,
            "text": bart_input,
            "summary": summary,
            "summary_method": "extractive+regex",
        })
        print(f"    OK  summary ({len(summary)} chars): {summary[:80]}...")

    print(f"\nProcessed: {len(records)} records  |  Failed/skipped: {len(failed)}")

    if not records:
        print("No valid records generated. Check your PDFs.")
        sys.exit(1)

    split_idx = max(1, int(len(records) * (1 - args.val_split)))
    train_records = records[:split_idx]
    val_records = records[split_idx:]

    def to_training_format(recs):
        return [{"text": r["text"], "summary": r["summary"]} for r in recs]

    train_path = out_dir / "train.json"
    val_path = out_dir / "val.json"
    meta_path = out_dir / "metadata.json"

    with open(train_path, "w", encoding="utf-8", errors="replace") as f:
        json.dump(to_training_format(train_records), f, indent=2, ensure_ascii=True)
    with open(val_path, "w", encoding="utf-8", errors="replace") as f:
        json.dump(to_training_format(val_records), f, indent=2, ensure_ascii=True)
    meta_path.write_text(json.dumps({
        "total_records": len(records),
        "train_records": len(train_records),
        "val_records": len(val_records),
        "failed_pdfs": failed,
        "summary_method": "extractive+regex (no external API)",
    }, indent=2))

    print(f"\nSaved:")
    print(f"  Train: {train_path}  ({len(train_records)} records)")
    print(f"  Val:   {val_path}  ({len(val_records)} records)")
    print(f"  Meta:  {meta_path}")
    if failed:
        print(f"\nFailed PDFs ({len(failed)}):")
        for f in failed:
            print(f"  - {f}")
    print("\nNext step: python scripts/train_bart.py")


if __name__ == "__main__":
    main()
