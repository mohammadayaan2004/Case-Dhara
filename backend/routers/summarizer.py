from fastapi import APIRouter, Depends, File, HTTPException, Request, UploadFile

from middleware.rate_limiter import limiter
from models.summarizer import CaseSummarizer, get_summarizer
from schemas.summarizer_schema import CaseSummary, SummarizeTextRequest

router = APIRouter(prefix="/api/v1/summarize", tags=["summarizer"])


@router.post("/pdf", response_model=CaseSummary)
@limiter.limit("20/minute")
async def summarize_pdf(
    request: Request,
    file: UploadFile = File(..., description="Court judgment PDF"),
    summarizer: CaseSummarizer = Depends(get_summarizer),
):
    if not (file.filename or "").lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF files are accepted.")
    pdf_bytes = await file.read()
    if len(pdf_bytes) > 50 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="File too large. Max 50MB.")

    try:
        return summarizer.from_pdf(pdf_bytes)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Summarization failed: {exc}") from exc


@router.post("/text", response_model=CaseSummary)
@limiter.limit("30/minute")
async def summarize_text(
    request: Request,
    body: SummarizeTextRequest,
    summarizer: CaseSummarizer = Depends(get_summarizer),
):
    if len(body.text.strip()) < 100:
        raise HTTPException(status_code=400, detail="Text too short to summarize.")
    try:
        return summarizer.from_text(body.text)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Summarization failed: {exc}") from exc
