"""
PDF/text extraction helpers for legal judgments.
"""

import io
import re

import fitz
import pdfplumber
import pytesseract
from PIL import Image


def extract_text_from_pdf(pdf_bytes: bytes) -> str:
    text = _extract_with_pdfplumber(pdf_bytes)
    if len(text.strip()) < 200:
        text = _extract_with_ocr(pdf_bytes)
    return _clean_legal_text(text)


def _extract_with_pdfplumber(pdf_bytes: bytes) -> str:
    pages: list[str] = []
    with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
        for page in pdf.pages:
            page_text = page.extract_text()
            if page_text:
                pages.append(page_text)
    return "\n\n".join(pages)


def _extract_with_ocr(pdf_bytes: bytes) -> str:
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    pages: list[str] = []
    for page_num in range(len(doc)):
        page = doc[page_num]
        pix = page.get_pixmap(dpi=200)
        img = Image.frombytes("RGB", [pix.width, pix.height], pix.samples)
        pages.append(pytesseract.image_to_string(img, lang="eng"))
    return "\n\n".join(pages)


def _clean_legal_text(text: str) -> str:
    text = re.sub(r"^\s*\d+\s*$", "", text, flags=re.MULTILINE)
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = re.sub(r"\x0c", "\n", text)
    text = re.sub(r"\xa0", " ", text)
    return text.strip()


def find_sections_in_text(text: str) -> list[str]:
    patterns = [
        r"\bSection\s+(\d+[A-Z]*(?:\(\d+\))?)\s+(?:IPC|BNS|of\s+IPC|of\s+BNS)",
        r"\bS\.\s*(\d+[A-Z]*(?:\(\d+\))?)\s+(?:IPC|BNS)",
        r"\bu[/\\]s\s+(\d+[A-Z]*(?:\(\d+\))?)",
        r"\b(\d+[A-Z]+)\s+(?:IPC|BNS)",
        r"\bIPC\s+(\d+[A-Z]*(?:\(\d+\))?)",
        r"\bBNS\s+(\d+[A-Z]*(?:\(\d+\))?)",
    ]
    found: set[str] = set()
    for pattern in patterns:
        matches = re.findall(pattern, text, flags=re.IGNORECASE)
        for match in matches:
            found.add(str(match).strip().upper())
    return list(found)


def extract_sections_from_text(text: str) -> list[str]:
    # Backward compatibility alias.
    return find_sections_in_text(text)
