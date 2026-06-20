"""
Mapper Evaluation Script
========================
Measures the accuracy and quality of the 3-tier legal section retrieval system.

Metrics computed
----------------
- Top-1 accuracy      : correct BNS section appears as first result
- Top-3 / Top-5 accuracy : correct answer within first N results
- MRR (Mean Reciprocal Rank): average 1/rank of first correct hit
- Retrieval tier distribution : how many queries are resolved at each tier
- Confidence statistics : mean / std / histogram per tier
- Per-status accuracy : how well mapped vs repealed vs merged sections are found

Usage
-----
    # From backend/ directory:
    python -m evaluation.eval_mapper
    python -m evaluation.eval_mapper --top_k 5 --sample 100 --output results/mapper_eval.json
"""

import argparse
import json
import logging
import random
import sys
import time
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

logging.basicConfig(level=logging.INFO, format="%(levelname)s  %(message)s")
logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).resolve().parents[1]
PROCESSED_DIR = BASE_DIR / "data" / "processed"


# ---------------------------------------------------------------------------
# Ground-truth test set
# ---------------------------------------------------------------------------

def build_test_cases(mapping_db_path: Path, sample: int | None = None) -> list[dict]:
    """
    Build test cases directly from the mapping database.
    Each case has:
      - query   : the IPC section number (e.g. "302")
      - expected_ipc : normalised IPC section
      - expected_bns : normalised BNS section(s)
      - status  : mapped / repealed / merged / split / new_in_bns
    """
    with mapping_db_path.open("r", encoding="utf-8") as f:
        records = json.load(f)

    cases = []
    for rec in records:
        ipc = str(rec.get("ipc_section", "")).strip()
        bns = str(rec.get("bns_section", "")).strip()
        if not ipc or not bns:
            continue
        cases.append({
            "query": ipc,
            "expected_ipc": ipc,
            "expected_bns": bns,
            "status": rec.get("status", "unknown"),
            "ipc_heading": rec.get("ipc_heading", ""),
        })

    # Augment with heading-based queries (harder variant)
    heading_cases = []
    for rec in records:
        heading = str(rec.get("ipc_heading", "")).strip()
        if len(heading) < 10:
            continue
        heading_cases.append({
            "query": heading[:80],          # use first 80 chars of heading as query
            "expected_ipc": str(rec.get("ipc_section", "")),
            "expected_bns": str(rec.get("bns_section", "")),
            "status": rec.get("status", "unknown"),
            "ipc_heading": heading,
            "query_type": "heading",
        })

    all_cases = cases + heading_cases

    if sample and sample < len(all_cases):
        random.seed(42)
        all_cases = random.sample(all_cases, sample)

    logger.info("Test set: %d cases", len(all_cases))
    return all_cases


# ---------------------------------------------------------------------------
# Evaluation helpers
# ---------------------------------------------------------------------------

def _normalise_section(s: str) -> str:
    """Strip whitespace and normalise to uppercase for comparison."""
    return str(s).strip().upper().replace(" ", "")


def _result_hits(result_records: list[dict], expected_bns: str) -> list[bool]:
    """Return a bool list: does position i contain the expected BNS section?"""
    exp_norm = _normalise_section(expected_bns)
    hits = []
    for r in result_records:
        bns_norm = _normalise_section(r.get("bns_section", ""))
        hits.append(bns_norm == exp_norm or exp_norm in bns_norm or bns_norm in exp_norm)
    return hits


def reciprocal_rank(hits: list[bool]) -> float:
    for i, h in enumerate(hits):
        if h:
            return 1.0 / (i + 1)
    return 0.0


# ---------------------------------------------------------------------------
# Main evaluation loop
# ---------------------------------------------------------------------------

def run_evaluation(args) -> dict[str, Any]:
    # Lazy import so the script can be run standalone
    sys.path.insert(0, str(BASE_DIR))
    from models.mapper import LegalMapper

    logger.info("Loading LegalMapper…")
    t0 = time.perf_counter()
    mapper = LegalMapper()
    load_time = time.perf_counter() - t0
    logger.info("Mapper loaded in %.2fs", load_time)

    test_cases = build_test_cases(PROCESSED_DIR / "mapping_db.json", sample=args.sample)

    # ── Per-case metrics ────────────────────────────────────────────────────
    top1_hits = []
    top3_hits = []
    top5_hits = []
    mrr_scores = []
    tier_counts = Counter()
    confidence_by_tier: dict[int, list[float]] = defaultdict(list)
    latency_ms: list[float] = []
    status_results: dict[str, list[bool]] = defaultdict(list)
    failed_cases: list[dict] = []

    for i, case in enumerate(test_cases):
        if i % 50 == 0:
            logger.info("  Evaluating %d/%d…", i, len(test_cases))

        try:
            t_start = time.perf_counter()
            response = mapper.search(case["query"], top_k=args.top_k)
            elapsed_ms = (time.perf_counter() - t_start) * 1000
            latency_ms.append(elapsed_ms)

            records = [r.model_dump() for r in response.results]
            hits = _result_hits(records, case["expected_bns"])

            top1 = any(hits[:1])
            top3 = any(hits[:3])
            top5 = any(hits[:5])
            rr   = reciprocal_rank(hits)

            top1_hits.append(top1)
            top3_hits.append(top3)
            top5_hits.append(top5)
            mrr_scores.append(rr)
            tier_counts[response.retrieval_tier] += 1

            for r in records:
                tier = r.get("retrieval_tier", 0)
                conf = r.get("confidence", 0.0)
                confidence_by_tier[tier].append(conf)

            status = case.get("status", "unknown")
            status_results[status].append(top1)

            if not top1:
                failed_cases.append({
                    "query": case["query"],
                    "expected_bns": case["expected_bns"],
                    "got_bns": records[0].get("bns_section") if records else None,
                    "status": status,
                    "tier": response.retrieval_tier,
                })

        except Exception as exc:
            logger.warning("Error on query %r: %s", case["query"], exc)
            top1_hits.append(False)
            top3_hits.append(False)
            top5_hits.append(False)
            mrr_scores.append(0.0)

    # ── Aggregate ────────────────────────────────────────────────────────────
    n = len(top1_hits)
    avg = lambda lst: sum(lst) / len(lst) if lst else 0.0

    conf_stats = {}
    for tier, confs in confidence_by_tier.items():
        conf_stats[f"tier_{tier}"] = {
            "mean": round(avg(confs), 4),
            "std":  round((sum((c - avg(confs))**2 for c in confs) / len(confs)) ** 0.5, 4) if confs else 0.0,
            "min":  round(min(confs), 4) if confs else 0.0,
            "max":  round(max(confs), 4) if confs else 0.0,
            "count": len(confs),
        }

    status_accuracy = {
        status: {
            "accuracy": round(avg(hits), 4),
            "count": len(hits),
            "correct": sum(hits),
        }
        for status, hits in status_results.items()
    }

    results = {
        "summary": {
            "total_cases": n,
            "top1_accuracy": round(avg(top1_hits), 4),
            "top3_accuracy": round(avg(top3_hits), 4),
            "top5_accuracy": round(avg(top5_hits), 4),
            "mrr": round(avg(mrr_scores), 4),
            "latency_ms_mean": round(avg(latency_ms), 2),
            "latency_ms_p95": round(sorted(latency_ms)[int(len(latency_ms) * 0.95)] if latency_ms else 0, 2),
        },
        "retrieval_tier_distribution": dict(tier_counts),
        "confidence_stats_by_tier": conf_stats,
        "accuracy_by_status": status_accuracy,
        "failed_cases_sample": failed_cases[:20],   # first 20 failures for inspection
        "model_load_time_s": round(load_time, 3),
    }

    return results


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def print_report(r: dict) -> None:
    s = r["summary"]
    print("\n" + "=" * 60)
    print("  CASE DHARA — Mapper Evaluation Report")
    print("=" * 60)
    print(f"  Total test cases   : {s['total_cases']}")
    print(f"  Top-1 Accuracy     : {s['top1_accuracy']:.2%}")
    print(f"  Top-3 Accuracy     : {s['top3_accuracy']:.2%}")
    print(f"  Top-5 Accuracy     : {s['top5_accuracy']:.2%}")
    print(f"  MRR                : {s['mrr']:.4f}")
    print(f"  Latency (mean)     : {s['latency_ms_mean']:.1f} ms")
    print(f"  Latency (p95)      : {s['latency_ms_p95']:.1f} ms")
    print(f"  Model load time    : {r['model_load_time_s']} s")
    print()
    print("  Retrieval tier distribution:")
    for tier, count in sorted(r["retrieval_tier_distribution"].items()):
        print(f"    Tier {tier}: {count} queries")
    print()
    print("  Accuracy by section status:")
    for status, info in r["accuracy_by_status"].items():
        print(f"    {status:<15} : {info['accuracy']:.2%}  (n={info['count']})")
    print()
    if r["failed_cases_sample"]:
        print("  First 5 failures:")
        for case in r["failed_cases_sample"][:5]:
            print(f"    query={case['query']!r}  expected_bns={case['expected_bns']!r}  "
                  f"got={case.get('got_bns')!r}  status={case['status']}")
    print("=" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Evaluate the Case Dhara legal section mapper")
    parser.add_argument("--top_k",  type=int, default=5, help="Results per query (default: 5)")
    parser.add_argument("--sample", type=int, default=None, help="Random sample size (default: all)")
    parser.add_argument("--output", type=str, default=None, help="Save JSON results to this path")
    parser.add_argument("--seed",   type=int, default=42)
    args = parser.parse_args()
    random.seed(args.seed)

    results = run_evaluation(args)
    print_report(results)

    if args.output:
        out = Path(args.output)
        out.parent.mkdir(parents=True, exist_ok=True)
        with out.open("w") as f:
            json.dump(results, f, indent=2)
        print(f"\n  Results saved → {out}")