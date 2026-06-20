"""
Step 2 of BART fine-tuning pipeline.

Fine-tunes facebook/bart-large-cnn on your court judgment training data.
Training data must be prepared first by running prepare_bart_training_data.py.

Usage (from backend_fixed/ directory):
    python scripts/train_bart.py

Optional flags:
    --train_file   path to train.json  (default: data/processed/bart_training/train.json)
    --val_file     path to val.json    (default: data/processed/bart_training/val.json)
    --output_dir   where to save model (default: models/bart_finetuned)
    --epochs       number of epochs    (default: 3)
    --batch_size   per-device batch    (default: 2, use 1 if GPU OOM)
    --max_input    max input tokens    (default: 1024)
    --max_target   max summary tokens  (default: 256)
    --lr           learning rate       (default: 3e-5)
    --resume       resume from checkpoint if exists

Requirements (all in requirements.txt):
    transformers>=4.40.0
    torch>=2.0
    datasets (pip install datasets)
    evaluate (pip install evaluate)
    rouge_score (pip install rouge-score)
"""

import argparse
import json
import os
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))

os.environ.setdefault("TRANSFORMERS_NO_TF", "1")
os.environ.setdefault("KMP_DUPLICATE_LIB_OK", "TRUE")


def check_dependencies():
    missing = []
    for pkg in ["transformers", "torch", "datasets", "evaluate", "rouge_score"]:
        try:
            __import__(pkg.replace("-", "_").replace("rouge_score", "rouge_score"))
        except ImportError:
            missing.append(pkg)
    if missing:
        print(f"Missing packages: {', '.join(missing)}")
        print(f"Install with: pip install {' '.join(missing)}")
        sys.exit(1)


def load_json_dataset(path: Path) -> list:
    if not path.exists():
        print(f"File not found: {path}")
        print("Run prepare_bart_training_data.py first.")
        sys.exit(1)
    # errors="replace" handles non-UTF-8 bytes from PDF OCR output (e.g. Windows-1252 \x85)
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        data = json.load(f)
    print(f"  Loaded {len(data)} records from {path.name}")
    return data


def main():
    check_dependencies()

    parser = argparse.ArgumentParser(description="Fine-tune BART on court judgment summaries")
    parser.add_argument("--train_file",  default=str(BASE_DIR / "data/processed/bart_training/train.json"))
    parser.add_argument("--val_file",    default=str(BASE_DIR / "data/processed/bart_training/val.json"))
    parser.add_argument("--output_dir",  default=str(BASE_DIR / "models/bart_finetuned"))
    parser.add_argument("--epochs",      type=int,   default=3)
    parser.add_argument("--batch_size",  type=int,   default=2,    help="Reduce to 1 if GPU runs out of memory")
    parser.add_argument("--max_input",   type=int,   default=1024, help="Max input tokens (BART limit is 1024)")
    parser.add_argument("--max_target",  type=int,   default=256,  help="Max summary output tokens")
    parser.add_argument("--lr",          type=float, default=3e-5)
    parser.add_argument("--resume",      action="store_true", help="Resume training from checkpoint")
    args = parser.parse_args()

    import torch
    from datasets import Dataset
    from transformers import (
        BartTokenizer,
        BartForConditionalGeneration,
        Seq2SeqTrainer,
        Seq2SeqTrainingArguments,
        DataCollatorForSeq2Seq,
        EarlyStoppingCallback,
    )
    import evaluate

    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"\nDevice: {device}")
    if device == "cpu":
        print("NOTE: Training on CPU is very slow. Expect 10-30 min per epoch on small datasets.")
        print("Consider using Google Colab (free GPU) — see README comment at bottom of this file.")

    # ── Load data ─────────────────────────────────────────────────────────────
    print("\nLoading training data...")
    train_data = load_json_dataset(Path(args.train_file))
    val_data   = load_json_dataset(Path(args.val_file))

    if len(train_data) == 0:
        print("No training data found. Run prepare_bart_training_data.py first.")
        sys.exit(1)

    train_dataset = Dataset.from_list(train_data)
    val_dataset   = Dataset.from_list(val_data)

    # ── Load model + tokenizer ────────────────────────────────────────────────
    MODEL_NAME = "facebook/bart-large-cnn"
    print(f"\nLoading model: {MODEL_NAME}")
    print("(Downloads ~1.6 GB on first run — cached afterwards)")

    tokenizer = BartTokenizer.from_pretrained(MODEL_NAME)
    model = BartForConditionalGeneration.from_pretrained(MODEL_NAME)
    model.to(device)

    print(f"Model parameters: {sum(p.numel() for p in model.parameters()):,}")

    # ── Tokenization ──────────────────────────────────────────────────────────
    def tokenize_batch(batch):
        # Tokenize the judgment text (input to BART encoder)
        model_inputs = tokenizer(
            batch["text"],
            max_length=args.max_input,
            truncation=True,
            padding="max_length",
        )
        # Tokenize the summary (target for BART decoder)
        labels = tokenizer(
            text_target=batch["summary"],
            max_length=args.max_target,
            truncation=True,
            padding="max_length",
        )
        # Replace padding token id in labels with -100 (ignored in loss)
        label_ids = [
            [(l if l != tokenizer.pad_token_id else -100) for l in lbl]
            for lbl in labels["input_ids"]
        ]
        model_inputs["labels"] = label_ids
        return model_inputs

    print("\nTokenizing dataset...")
    # num_proc=1: avoids Python 3.12 + Windows multiprocessing crash (_thread.RLock bug)
    tokenized_train = train_dataset.map(tokenize_batch, batched=True, batch_size=16,
                                         remove_columns=train_dataset.column_names, num_proc=1)
    tokenized_val   = val_dataset.map(tokenize_batch, batched=True, batch_size=16,
                                       remove_columns=val_dataset.column_names, num_proc=1)

    # ── ROUGE metric for evaluation ───────────────────────────────────────────
    rouge = evaluate.load("rouge")

    def compute_metrics(eval_pred):
        predictions, labels = eval_pred
        import numpy as np

        # ── int32 cast (unconditional) ────────────────────────────────────────
        # HuggingFace's fast tokenizer Rust backend requires int32.  The trainer
        # delivers predictions as int64 (numpy default) even with fp16=True,
        # which causes: OverflowError: out of range integral type conversion.
        # Clip to valid vocab range first to guard against any sentinel values
        # (-1, large negatives) that beam search may leave in padding positions.
        predictions = np.clip(np.array(predictions), 0, tokenizer.vocab_size - 1).astype(np.int32)

        # Decode predictions
        decoded_preds = tokenizer.batch_decode(predictions, skip_special_tokens=True)
        # Replace -100 in labels (training padding sentinel) then also cast to int32
        labels = np.where(np.array(labels) == -100, tokenizer.pad_token_id, labels).astype(np.int32)
        decoded_labels = tokenizer.batch_decode(labels, skip_special_tokens=True)
        # Strip whitespace
        decoded_preds  = [p.strip() for p in decoded_preds]
        decoded_labels = [l.strip() for l in decoded_labels]
        result = rouge.compute(predictions=decoded_preds, references=decoded_labels, use_stemmer=True)
        return {k: round(v * 100, 2) for k, v in result.items()}

    # ── Training arguments ────────────────────────────────────────────────────
    use_fp16 = device == "cuda"

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    training_args = Seq2SeqTrainingArguments(
        output_dir=str(output_dir),

        # Training schedule
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        gradient_accumulation_steps=4,       # effective batch = batch_size * 4
        warmup_steps=max(50, len(train_data) // 10),
        learning_rate=args.lr,
        weight_decay=0.01,

        # Evaluation & saving
        eval_strategy="epoch",
        save_strategy="epoch",
        load_best_model_at_end=True,
        metric_for_best_model="rougeL",
        greater_is_better=True,
        save_total_limit=2,                  # keep only 2 best checkpoints

        # Generation settings for eval
        predict_with_generate=True,
        generation_max_length=args.max_target,

        # Performance
        fp16=use_fp16,
        dataloader_num_workers=0,            # 0 = safer on Windows/Docker

        # Logging
        logging_steps=max(1, len(train_data) // (args.batch_size * 5)),
        logging_dir=str(output_dir / "logs"),
        report_to="none",                    # disable wandb/tensorboard unless you want it

        # Resume
        resume_from_checkpoint=args.resume,
    )

    # ── Trainer ───────────────────────────────────────────────────────────────
    data_collator = DataCollatorForSeq2Seq(tokenizer, model=model, padding=True)

    trainer = Seq2SeqTrainer(
    model=model,
    args=training_args,
    train_dataset=tokenized_train,
    eval_dataset=tokenized_val,
    processing_class=tokenizer,   # ✅ FIXED
    data_collator=data_collator,
    compute_metrics=compute_metrics,
    callbacks=[EarlyStoppingCallback(early_stopping_patience=2)],
    )

    # ── Train ─────────────────────────────────────────────────────────────────
    print(f"\nStarting training:")
    print(f"  Epochs:           {args.epochs}")
    print(f"  Batch size:       {args.batch_size} (effective: {args.batch_size * 4} with grad accumulation)")
    print(f"  Training samples: {len(train_data)}")
    print(f"  Val samples:      {len(val_data)}")
    print(f"  Output dir:       {output_dir}")
    print(f"  FP16:             {use_fp16}")
    print()

    trainer.train()

    # ── Save final model ──────────────────────────────────────────────────────
    print(f"\nSaving best model to: {output_dir}")
    trainer.save_model(str(output_dir))
    tokenizer.save_pretrained(str(output_dir))

    # ── Quick test on a val sample ────────────────────────────────────────────
    if val_data:
        print("\nTest generation on first validation sample:")
        sample_text = val_data[0]["text"]
        gold_summary = val_data[0]["summary"]

        inputs = tokenizer(sample_text, return_tensors="pt",
                          max_length=args.max_input, truncation=True).to(device)
        generated = model.generate(
            **inputs,
            max_length=args.max_target,
            num_beams=4,
            early_stopping=True,
        )
        pred_summary = tokenizer.decode(generated[0], skip_special_tokens=True)

        print(f"\n  Gold:      {gold_summary[:200]}")
        print(f"  Generated: {pred_summary[:200]}")

    print(f"\nDone! Fine-tuned model saved to: {output_dir}")
    print("The backend will automatically load it on next startup.")
    print("(summarizer.py checks for models/bart_finetuned/ at load time)")


if __name__ == "__main__":
    main()

# ─────────────────────────────────────────────────────────────────────────────
# RUNNING ON GOOGLE COLAB (free GPU — recommended if you have no local GPU)
# ─────────────────────────────────────────────────────────────────────────────
# 1. Upload your backend_fixed/ folder to Google Drive
# 2. Open a new Colab notebook, set Runtime → Change runtime type → GPU (T4)
# 3. Mount Drive:
#       from google.colab import drive
#       drive.mount('/content/drive')
# 4. Install deps:
#       !pip install transformers datasets evaluate rouge_score torch pdfplumber PyMuPDF pytesseract google-genai
# 5. Run preparation:
#       !python /content/drive/MyDrive/case_dhara_backend/scripts/prepare_bart_training_data.py \
#           --pdf_dir /content/drive/MyDrive/case_dhara_backend/data/training_pdfs
# 6. Run training:
#       !python /content/drive/MyDrive/case_dhara_backend/scripts/train_bart.py \
#           --output_dir /content/drive/MyDrive/case_dhara_backend/models/bart_finetuned
# 7. The fine-tuned model saves back to your Drive automatically.
# ─────────────────────────────────────────────────────────────────────────────