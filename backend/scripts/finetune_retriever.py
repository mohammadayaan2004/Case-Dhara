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

from sentence_transformers import SentenceTransformer, SentenceTransformerTrainer, SentenceTransformerTrainingArguments, evaluation, losses
from sentence_transformers.training_args import BatchSamplers
from datasets import Dataset as HFDataset

BASE_DIR = Path(__file__).resolve().parents[1]
AUGMENTED = BASE_DIR / "data" / "processed" / "augmented_pairs.json"
RAW_CSV = BASE_DIR / "data" / "raw" / "ipc_bns_data.csv"
RAW_FALLBACK = BASE_DIR.parent / "dataset" / "ipc_bns_data.csv"
OUTPUT_DIR = BASE_DIR / "models" / "finetuned_retriever"
BASE_MODEL = "sentence-transformers/all-mpnet-base-v2"
EPOCHS = 8
BATCH_SIZE = 32
WARMUP_STEPS = 100


def load_pairs() -> list[dict]:
    if AUGMENTED.exists():
        print("Using augmented pairs...")
        data = json.loads(AUGMENTED.read_text(encoding="utf-8"))
        pairs = [{"anchor": item["query"], "positive": item["corpus"]} for item in data]
        print(f"  Loaded {len(pairs)} augmented pairs")
        return pairs

    csv_path = RAW_CSV if RAW_CSV.exists() else RAW_FALLBACK
    if not csv_path.exists():
        raise FileNotFoundError(f"No CSV at {RAW_CSV} or {RAW_FALLBACK}")

    print("Using original CSV pairs...")
    pairs: list[dict] = []
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
                    pairs.append({"anchor": prompt, "positive": answer})
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
    train_pairs = pairs[:split]
    val_pairs = pairs[split:]

    train_dataset = HFDataset.from_list(train_pairs)
    val_dataset = HFDataset.from_list(val_pairs) if val_pairs else None

    loss = losses.MultipleNegativesRankingLoss(model=model)

    args = SentenceTransformerTrainingArguments(
        output_dir=str(OUTPUT_DIR),
        num_train_epochs=EPOCHS,
        per_device_train_batch_size=BATCH_SIZE,
        warmup_steps=WARMUP_STEPS,
        save_strategy="epoch",
        save_total_limit=1,
        batch_sampler=BatchSamplers.NO_DUPLICATES,
        logging_steps=50,
    )

    trainer = SentenceTransformerTrainer(
        model=model,
        args=args,
        train_dataset=train_dataset,
        eval_dataset=val_dataset,
        loss=loss,
    )
    trainer.train()

    model.save_pretrained(str(OUTPUT_DIR))
    print(f"Fine-tuned model saved -> {OUTPUT_DIR}")
    print("Next: python backend/scripts/build_index.py --model backend/models/finetuned_retriever")


if __name__ == "__main__":
    main()
