"""
Fine-tune sentence-transformer on IPC/BNS pairs.
Uses augmented_pairs.json if present, else data/raw ipc_bns_data.csv (or dataset fallback).

Run: python backend/scripts/finetune_retriever.py
Then: python backend/scripts/build_index.py --model backend/models/finetuned_retriever
"""

import ast
import csv
import json
import os
from pathlib import Path

os.environ.setdefault("TRANSFORMERS_NO_TF", "1")

from sentence_transformers import InputExample, SentenceTransformer, evaluation, losses
from torch.utils.data import DataLoader

BASE_DIR = Path(__file__).resolve().parents[1]
AUGMENTED = BASE_DIR / "data" / "processed" / "augmented_pairs.json"
RAW_CSV = BASE_DIR / "data" / "raw" / "ipc_bns_data.csv"
RAW_FALLBACK = BASE_DIR.parent / "dataset" / "ipc_bns_data.csv"
OUTPUT_DIR = BASE_DIR / "models" / "finetuned_retriever"
BASE_MODEL = "sentence-transformers/all-mpnet-base-v2"
EPOCHS = 8
BATCH_SIZE = 32
WARMUP_STEPS = 100


def load_pairs() -> list[InputExample]:
    if AUGMENTED.exists():
        print("Using augmented pairs...")
        data = json.loads(AUGMENTED.read_text(encoding="utf-8"))
        pairs = [InputExample(texts=[item["query"], item["corpus"]]) for item in data]
        print(f"  Loaded {len(pairs)} augmented pairs")
        return pairs

    csv_path = RAW_CSV if RAW_CSV.exists() else RAW_FALLBACK
    if not csv_path.exists():
        raise FileNotFoundError(f"No CSV at {RAW_CSV} or {RAW_FALLBACK}")

    print("Using original CSV pairs...")
    pairs: list[InputExample] = []
    with csv_path.open("r", encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            try:
                r = ast.literal_eval(row.get("response", "{}"))
                parts: list[str] = []
                ipc_desc = r.get("IPC Descriptions", "").strip()
                bns_desc = r.get("BNS description", "").strip()
                ipc_sec = r.get("IPC Section", "")
                bns_sec = r.get("BNS Section", "")
                if ipc_desc not in ("Repealed", ""):
                    parts.append(f"IPC Section {ipc_sec} ({r.get('IPC Heading', '')}): {ipc_desc}")
                if bns_desc not in ("Repealed in BNS", ""):
                    parts.append(f"BNS Section {bns_sec} ({r.get('BNS Heading', '')}): {bns_desc}")
                answer = "\n\n".join(parts)
                prompt = (row.get("prompts") or "").strip()
                if answer.strip() and prompt:
                    pairs.append(InputExample(texts=[prompt, answer]))
            except Exception:
                continue
    print(f"  Loaded {len(pairs)} original pairs")
    return pairs


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    model = SentenceTransformer(BASE_MODEL)
    pairs = load_pairs()
    if not pairs:
        raise SystemExit("No training pairs found.")

    split = int(len(pairs) * 0.9)
    train_loader = DataLoader(pairs[:split], shuffle=True, batch_size=BATCH_SIZE)
    val_pairs = pairs[split:]

    loss = losses.MultipleNegativesRankingLoss(model=model)
    evaluator = evaluation.EmbeddingSimilarityEvaluator(
        sentences1=[p.texts[0] for p in val_pairs],
        sentences2=[p.texts[1] for p in val_pairs],
        scores=[1.0] * len(val_pairs),
        name="legal_val",
    )

    model.fit(
        train_objectives=[(train_loader, loss)],
        evaluator=evaluator,
        epochs=EPOCHS,
        warmup_steps=WARMUP_STEPS,
        output_path=str(OUTPUT_DIR),
        save_best_model=True,
        show_progress_bar=True,
    )
    print(f"Fine-tuned model saved -> {OUTPUT_DIR}")
    print("Next: python backend/scripts/build_index.py --model backend/models/finetuned_retriever")


if __name__ == "__main__":
    main()
