"""
Summarizer Evaluation Script
=============================
Evaluates the BART-based legal case summarizer using:
  - ROUGE-1 / ROUGE-2 / ROUGE-L  (lexical overlap)
  - BLEU-4                        (n-gram precision)
  - Section extraction recall     (did we find the IPC/BNS sections?)
  - Structured field coverage     (how many required fields are populated?)
  - Latency per document

Usage
-----
    # From backend/ directory:
    python -m evaluation.eval_summarizer
    python -m evaluation.eval_summarizer --sample 20 --output results/summarizer_eval.json

Notes
-----
- Requires the training data at data/processed/bart_training/val.json
- If the fine-tuned model is absent, falls back to facebook/bart-large-cnn
- ROUGE/BLEU are computed with pure-Python implementations (no extra deps)
"""

import argparse
import json
import logging
import math
import re
import sys
import time
from collections import Counter
from pathlib import Path
from typing import Any

logging.basicConfig(level=logging.INFO, format="%(levelname)s  %(message)s")
logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).resolve().parents[1]
VAL_PATH = BASE_DIR / "data" / "processed" / "bart_training" / "val.json"


# ---------------------------------------------------------------------------
# Pure-Python ROUGE / BLEU (no rouge_score dependency needed)
# ---------------------------------------------------------------------------

def _tokenize(text: str) -> list[str]:
    return re.findall(r"\b\w+\b", text.lower())


def _ngrams(tokens: list[str], n: int) -> Counter:
    return Counter(tuple(tokens[i:i+n]) for i in range(len(tokens) - n + 1))


def rouge_n(hypothesis: str, reference: str, n: int = 1) -> dict[str, float]:
    hyp_ng = _ngrams(_tokenize(hypothesis), n)
    ref_ng = _ngrams(_tokenize(reference), n)
    overlap = sum((hyp_ng & ref_ng).values())
    precision = overlap / max(sum(hyp_ng.values()), 1)
    recall    = overlap / max(sum(ref_ng.values()), 1)
    f1 = 2 * precision * recall / max(precision + recall, 1e-9)
    return {"precision": precision, "recall": recall, "f1": f1}


def rouge_l(hypothesis: str, reference: str) -> dict[str, float]:
    """LCS-based ROUGE-L."""
    hyp_tok = _tokenize(hypothesis)
    ref_tok = _tokenize(reference)
    m, n = len(hyp_tok), len(ref_tok)
    # DP LCS (O(m*n) — acceptable for summaries)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if hyp_tok[i-1] == ref_tok[j-1]:
                dp[i][j] = dp[i-1][j-1] + 1
            else:
                dp[i][j] = max(dp[i-1][j], dp[i][j-1])
    lcs_len = dp[m][n]
    precision = lcs_len / max(m, 1)
    recall    = lcs_len / max(n, 1)
    f1 = 2 * precision * recall / max(precision + recall, 1e-9)
    return {"precision": precision, "recall": recall, "f1": f1}


def bleu4(hypothesis: str, reference: str) -> float:
    """Corpus BLEU-4 for a single sentence pair."""
    hyp_tok = _tokenize(hypothesis)
    ref_tok = _tokenize(reference)
    if not hyp_tok:
        return 0.0
    # Brevity penalty
    bp = 1.0 if len(hyp_tok) >= len(ref_tok) else math.exp(1 - len(ref_tok) / len(hyp_tok))
    log_score = 0.0
    for n in range(1, 5):
        hyp_ng = _ngrams(hyp_tok, n)
        ref_ng = _ngrams(ref_tok, n)
        clipped = sum(min(c, ref_ng[ng]) for ng, c in hyp_ng.items())
        total   = sum(hyp_ng.values())
        if total == 0 or clipped == 0:
            return 0.0
        log_score += math.log(clipped / total)
    return bp * math.exp(log_score / 4)


# ---------------------------------------------------------------------------
# Section extraction evaluation
# ---------------------------------------------------------------------------

SECTION_PATTERN = re.compile(
    r"\b(?:IPC|BNS)\s*[Ss]ec(?:tion)?\s*(\d+[A-Z]?(?:\(\d+\))?)",
    re.IGNORECASE,
)

def extract_sections(text: str) -> set[str]:
    return {m.group(1).upper() for m in SECTION_PATTERN.finditer(text)}


def section_recall(predicted_text: str, reference_text: str) -> float:
    """What fraction of reference sections appear in the prediction?"""
    ref_secs = extract_sections(reference_text)
    if not ref_secs:
        return 1.0   # nothing to recall
    pred_secs = extract_sections(predicted_text)
    return len(ref_secs & pred_secs) / len(ref_secs)


# ---------------------------------------------------------------------------
# Structured field coverage
# ---------------------------------------------------------------------------

REQUIRED_FIELDS = [
    "case_title", "court", "facts", "issues",
    "held", "ratio_decidendi", "order", "summary_short",
]

def field_coverage(summary_dict: dict) -> dict[str, Any]:
    filled = [f for f in REQUIRED_FIELDS if summary_dict.get(f)]
    return {
        "coverage_ratio": len(filled) / len(REQUIRED_FIELDS),
        "filled_fields": filled,
        "missing_fields": [f for f in REQUIRED_FIELDS if f not in filled],
    }


# ---------------------------------------------------------------------------
# Main evaluation loop
# ---------------------------------------------------------------------------

def load_val_cases(path: Path, sample: int | None) -> list[dict]:
    with path.open("r", encoding="utf-8") as f:
        data = json.load(f)
    if sample and sample < len(data):
        import random
        random.seed(42)
        data = random.sample(data, sample)
    logger.info("Validation cases: %d", len(data))
    return data


def run_evaluation(args) -> dict[str, Any]:
    sys.path.insert(0, str(BASE_DIR))
    from models.summarizer import get_summarizer

    logger.info("Loading Summarizer…")
    t0 = time.perf_counter()
    summarizer = get_summarizer()
    load_time = time.perf_counter() - t0
    logger.info("Summarizer loaded in %.2fs", load_time)

    cases = load_val_cases(VAL_PATH, sample=args.sample)

    rouge1_f1 = []
    rouge2_f1 = []
    rougeL_f1 = []
    bleu_scores = []
    sec_recall_scores = []
    field_coverages = []
    latency_ms = []
    failed = []

    for i, case in enumerate(cases):
        if i % 10 == 0:
            logger.info("  %d/%d", i, len(cases))

        input_text = case.get("input", case.get("text", ""))
        reference  = case.get("output", case.get("summary", ""))
        if not input_text or not reference:
            continue

        try:
            t_start = time.perf_counter()
            result = summarizer.from_text(input_text)
            elapsed_ms = (time.perf_counter() - t_start) * 1000
            latency_ms.append(elapsed_ms)

            # Use summary_short for text metrics; full dict for field coverage
            if hasattr(result, "model_dump"):
                result_dict = result.model_dump()
            elif isinstance(result, dict):
                result_dict = result
            else:
                result_dict = {}

            predicted = result_dict.get("summary_short", str(result_dict))

            rouge1_f1.append(rouge_n(predicted, reference, 1)["f1"])
            rouge2_f1.append(rouge_n(predicted, reference, 2)["f1"])
            rougeL_f1.append(rouge_l(predicted, reference)["f1"])
            bleu_scores.append(bleu4(predicted, reference))
            sec_recall_scores.append(section_recall(predicted, reference))
            fc = field_coverage(result_dict)
            field_coverages.append(fc["coverage_ratio"])

        except Exception as exc:
            logger.warning("Case %d failed: %s", i, exc)
            failed.append({"index": i, "error": str(exc)})

    avg = lambda lst: sum(lst) / len(lst) if lst else 0.0

    return {
        "summary": {
            "total_cases": len(cases),
            "evaluated":   len(rouge1_f1),
            "failed":      len(failed),
            "rouge1_f1":   round(avg(rouge1_f1), 4),
            "rouge2_f1":   round(avg(rouge2_f1), 4),
            "rougeL_f1":   round(avg(rougeL_f1), 4),
            "bleu4":       round(avg(bleu_scores), 4),
            "section_extraction_recall": round(avg(sec_recall_scores), 4),
            "field_coverage_ratio":      round(avg(field_coverages), 4),
            "latency_ms_mean": round(avg(latency_ms), 2),
            "latency_ms_p95":  round(
                sorted(latency_ms)[int(len(latency_ms) * 0.95)] if latency_ms else 0, 2
            ),
        },
        "model_load_time_s": round(load_time, 3),
        "failures_sample": failed[:10],
    }


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def print_report(r: dict) -> None:
    s = r["summary"]
    print("\n" + "=" * 60)
    print("  CASE DHARA — Summarizer Evaluation Report")
    print("=" * 60)
    print(f"  Cases evaluated    : {s['evaluated']} / {s['total_cases']}")
    print(f"  ROUGE-1 F1         : {s['rouge1_f1']:.4f}")
    print(f"  ROUGE-2 F1         : {s['rouge2_f1']:.4f}")
    print(f"  ROUGE-L F1         : {s['rougeL_f1']:.4f}")
    print(f"  BLEU-4             : {s['bleu4']:.4f}")
    print(f"  Section recall     : {s['section_extraction_recall']:.2%}")
    print(f"  Field coverage     : {s['field_coverage_ratio']:.2%}")
    print(f"  Latency mean       : {s['latency_ms_mean']:.1f} ms")
    print(f"  Latency p95        : {s['latency_ms_p95']:.1f} ms")
    print(f"  Model load time    : {r['model_load_time_s']} s")
    if r.get("failures_sample"):
        print(f"\n  Failures ({len(r['failures_sample'])} shown):")
        for f in r["failures_sample"][:3]:
            print(f"    Case {f['index']}: {f['error']}")
    print("=" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Evaluate the Case Dhara BART summarizer")
    parser.add_argument("--sample", type=int, default=None,
                        help="Number of val cases to use (default: all)")
    parser.add_argument("--output", type=str, default=None,
                        help="Save JSON results to this path")
    args = parser.parse_args()

    results = run_evaluation(args)
    print_report(results)

    if args.output:
        out = Path(args.output)
        out.parent.mkdir(parents=True, exist_ok=True)
        with out.open("w") as f:
            json.dump(results, f, indent=2)
        print(f"\n  Results saved → {out}")