"""
End-to-End Pipeline Benchmark
==============================
Runs Mapper and Summarizer evaluations and produces a unified report
with a readiness score.

Usage
-----
    # From backend/ directory:
    python -m evaluation.run_all_evals
    python -m evaluation.run_all_evals --output results/full_eval.json
    python -m evaluation.run_all_evals --sample 50
    python -m evaluation.run_all_evals --skip_summarizer
"""

import argparse
import importlib
import json
import logging
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any

logging.basicConfig(level=logging.INFO, format="%(levelname)s  %(message)s")
logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).resolve().parents[1]
RESULTS_DIR = BASE_DIR / "evaluation" / "results"


# ---------------------------------------------------------------------------
# Thresholds
# ---------------------------------------------------------------------------

@dataclass
class Thresholds:
    mapper_top1_acc:             float = 0.80
    mapper_mrr:                  float = 0.85
    summarizer_rouge1:           float = 0.35
    summarizer_field_coverage:   float = 0.75


THRESHOLDS = Thresholds()


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _run_module(module_name: str, args_ns) -> dict[str, Any] | None:
    try:
        mod = importlib.import_module(f"evaluation.{module_name}")
        return mod.run_evaluation(args_ns)
    except Exception as exc:
        logger.error("Eval module '%s' failed: %s", module_name, exc)
        return None


def _pass_fail(value: float, threshold: float) -> str:
    return "PASS ✓" if value >= threshold else "FAIL ✗"


# ---------------------------------------------------------------------------
# Readiness score  (Mapper 55 %, Summarizer 45 %)
# ---------------------------------------------------------------------------

def compute_readiness(mapper: dict | None, summarizer: dict | None) -> float:
    scores = []

    if mapper:
        s = mapper["summary"]
        m_score = (
            0.5 * s.get("top1_accuracy", 0) +
            0.3 * s.get("mrr", 0) +
            0.2 * s.get("top3_accuracy", 0)
        )
        scores.append((m_score, 0.55))

    if summarizer:
        s = summarizer["summary"]
        sum_score = (
            0.4 * min(s.get("rouge1_f1", 0) / 0.45, 1.0) +
            0.3 * s.get("section_extraction_recall", 0) +
            0.3 * s.get("field_coverage_ratio", 0)
        )
        scores.append((sum_score, 0.45))

    if not scores:
        return 0.0

    total_weight = sum(w for _, w in scores)
    return sum(v * w for v, w in scores) / total_weight


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def run_all(args) -> dict[str, Any]:
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)

    class SubArgs:
        sample = args.sample
        top_k  = 5

    mapper_results     = None
    summarizer_results = None

    t0 = time.perf_counter()

    if not args.skip_mapper:
        logger.info("\n━━━  Running Mapper Evaluation  ━━━")
        mapper_results = _run_module("eval_mapper", SubArgs())

    if not args.skip_summarizer:
        logger.info("\n━━━  Running Summarizer Evaluation  ━━━")
        summarizer_results = _run_module("eval_summarizer", SubArgs())

    total_time = round(time.perf_counter() - t0, 2)
    readiness  = compute_readiness(mapper_results, summarizer_results)

    # ── Threshold checks ────────────────────────────────────────────────────
    checks = []
    if mapper_results:
        ms = mapper_results["summary"]
        checks.append({
            "check": "Mapper Top-1 Accuracy",
            "value": ms.get("top1_accuracy", 0),
            "threshold": THRESHOLDS.mapper_top1_acc,
            "status": _pass_fail(ms.get("top1_accuracy", 0), THRESHOLDS.mapper_top1_acc),
        })
        checks.append({
            "check": "Mapper MRR",
            "value": ms.get("mrr", 0),
            "threshold": THRESHOLDS.mapper_mrr,
            "status": _pass_fail(ms.get("mrr", 0), THRESHOLDS.mapper_mrr),
        })
    if summarizer_results:
        ss = summarizer_results["summary"]
        checks.append({
            "check": "Summarizer ROUGE-1",
            "value": ss.get("rouge1_f1", 0),
            "threshold": THRESHOLDS.summarizer_rouge1,
            "status": _pass_fail(ss.get("rouge1_f1", 0), THRESHOLDS.summarizer_rouge1),
        })
        checks.append({
            "check": "Summarizer Field Coverage",
            "value": ss.get("field_coverage_ratio", 0),
            "threshold": THRESHOLDS.summarizer_field_coverage,
            "status": _pass_fail(ss.get("field_coverage_ratio", 0), THRESHOLDS.summarizer_field_coverage),
        })

    passed   = sum(1 for c in checks if "PASS" in c["status"])
    all_pass = passed == len(checks)

    return {
        "readiness_score":    round(readiness, 4),
        "all_checks_passed":  all_pass,
        "checks_passed":      f"{passed}/{len(checks)}",
        "threshold_checks":   checks,
        "mapper":             mapper_results,
        "summarizer":         summarizer_results,
        "total_eval_time_s":  total_time,
    }


def print_full_report(report: dict) -> None:
    print("\n" + "=" * 60)
    print("  CASE DHARA — Pipeline Evaluation Report")
    print("=" * 60)
    print(f"  Readiness Score   : {report['readiness_score']:.2%}")
    print(f"  Checks Passed     : {report['checks_passed']}")
    print(f"  All Checks Pass   : {'YES ✓' if report['all_checks_passed'] else 'NO  ✗'}")
    print(f"  Total Eval Time   : {report['total_eval_time_s']} s")
    print()
    print("  Threshold Checks:")
    for c in report["threshold_checks"]:
        print(f"    {c['check']:<35} {c['value']:.4f} >= {c['threshold']}  → {c['status']}")
    print()

    if report.get("mapper"):
        ms = report["mapper"]["summary"]
        print("  MAPPER")
        print(f"    Top-1  : {ms['top1_accuracy']:.2%}   Top-3: {ms['top3_accuracy']:.2%}   MRR: {ms['mrr']:.4f}")
        print(f"    Latency: {ms['latency_ms_mean']:.1f} ms mean / {ms['latency_ms_p95']:.1f} ms p95")

    if report.get("summarizer"):
        ss = report["summarizer"]["summary"]
        print("  SUMMARIZER")
        print(f"    ROUGE-1: {ss['rouge1_f1']:.4f}   ROUGE-L: {ss['rougeL_f1']:.4f}   BLEU-4: {ss['bleu4']:.4f}")
        print(f"    Field coverage: {ss['field_coverage_ratio']:.2%}   Section recall: {ss['section_extraction_recall']:.2%}")
        print(f"    Latency: {ss['latency_ms_mean']:.1f} ms mean")

    print("=" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Run Case Dhara model evaluations")
    parser.add_argument("--sample",          type=int, default=None)
    parser.add_argument("--skip_mapper",     action="store_true")
    parser.add_argument("--skip_summarizer", action="store_true")
    parser.add_argument("--output",          type=str, default=None)
    args = parser.parse_args()

    sys.path.insert(0, str(BASE_DIR))

    report = run_all(args)
    print_full_report(report)

    out_path = Path(args.output) if args.output else RESULTS_DIR / "full_eval.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("w") as f:
        json.dump(report, f, indent=2)
    print(f"\n  Results saved → {out_path}")