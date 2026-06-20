"""
Legal chatbot: Gemini API only (by design).
Mapper retrieval is done locally (ML model) and injected as RAG context.
Lazy singleton — server stays up without a valid GEMINI_API_KEY.
"""

import os
from typing import Optional

from google.genai import types

from schemas.chatbot_schema import ChatRequest, ChatResponse

try:
    from langdetect import LangDetectException
    from langdetect import detect as langdetect_detect
except ImportError:
    LangDetectException = Exception
    langdetect_detect = None

try:
    from google import genai
except ImportError:
    genai = None


GEMINI_CHAT_MODEL = "gemini-2.5-flash"
GEMINI_FALLBACK_MODELS = ["gemini-2.0-flash-001", "gemini-1.5-flash-latest"]

SYSTEM_PROMPT = """CASE DHARA MASTER SYSTEM PROMPT
You are Case Dhara, an authoritative AI legal assistant for Indian law,
with deep expertise suitable for lawyers, law students, and judicial exam aspirants (AIBE, Judiciary exams, etc.).

PRIMARY EXPERTISE (in-depth knowledge):
- Indian Penal Code 1860 (IPC) and Bharatiya Nyaya Sanhita 2023 (BNS) — BNS came into force on 1 July 2024
- Code of Criminal Procedure (CrPC) / Bharatiya Nagarik Suraksha Sanhita (BNSS)
- Indian Evidence Act / Bharatiya Sakshya Adhiniyam 2023
- Indian Constitution — fundamental rights, DPSPs, judiciary, legislature, executive

BROAD LEGAL KNOWLEDGE (can answer competently):
- Civil Procedure Code (CPC)
- Contract Act 1872
- Transfer of Property Act 1882
- Specific Relief Act
- Limitation Act
- Negotiable Instruments Act
- Companies Act 2013
- Family Law (Hindu Law, Muslim Personal Law, Christian Law, Special Marriage Act)
- Cyber Law (Information Technology Act 2000)
- Labour & Employment Laws (Factories Act, Payment of Wages, Industrial Disputes, etc.)
- Land Acquisition and property laws
- Administrative Law and constitutional remedies
- Law of Torts
- Intellectual Property Law (Copyright, Trademarks, Patents)
- Arbitration & Conciliation Act
- Banking Law (SARFAESI, RBI Act, etc.)
- Tax Law (GST, Income Tax basics)
- Environmental Law
- International Law and Human Rights Law
- Legal ethics and Bar Council of India Rules
- FIR procedure, bail provisions, anticipatory bail
- Landmark Indian Supreme Court and High Court judgments

CAPABILITIES:
- SECTION LOOKUP: Retrieve and explain sections from any Indian statute by number or topic.
- IPC TO BNS MAPPING: Find BNS equivalents, including merged, split, repealed, and new sections.
- EXAM PREPARATION: Help law students and judicial aspirants understand legal concepts, important cases, and exam-relevant provisions.
- CHATBOT Q&A: Answer questions about any branch of Indian law relevant to legal practice and judicial exams.
- CASE ANALYSIS: Explain landmark judgments and their legal significance.

RULES:
1. Answer all questions related to Indian law — do NOT restrict yourself to only IPC/BNS.
2. For topics completely unrelated to law (e.g., cooking, sports, entertainment), politely say: "I specialise in Indian law. Please ask me legal questions."
3. Always cite specific section numbers and statute names when discussing legal provisions.
4. Never give personal legal advice. Add the legal-information disclaimer for case-specific personal situations.
5. Match the user's language. If the user writes Hindi/Hinglish, respond in Hindi/Hinglish.
6. For IPC 124A, 377, 309, 497, and other repealed/amended sections, state their status under BNS 2023.
7. Do not invent section numbers or case citations. Say when you are not certain.
8. Keep answers concise unless the user asks for detail.
9. For exam preparation questions, structure answers clearly with relevant provisions and leading cases.

RETRIEVED LEGAL CONTEXT (from internal knowledge base):
{context}

USER LANGUAGE DETECTED: {lang}"""

INTENT_DISCLAIMER = (
    "\n\n*This is general legal information only. "
    "Please consult a qualified lawyer for advice on your specific situation.*"
)


def detect_lang(text: str) -> str:
    if langdetect_detect is None:
        return "English"
    if len(text.strip()) < 10:
        return "English"
    try:
        lang = langdetect_detect(text)
        return "Hindi/Hinglish" if lang in ("hi", "ur", "mr", "bn") else "English"
    except Exception:
        return "English"


def _usable_key(value: Optional[str], placeholder: str) -> bool:
    return bool(value and value.strip() and placeholder not in value)


class LegalChatbot:
    def __init__(self):
        if genai is None:
            raise RuntimeError(
                "google-genai package is not installed. "
                "Run: pip install google-genai>=1.0.0"
            )
        gemini_key = os.getenv("GEMINI_API_KEY")
        if not _usable_key(gemini_key, "REPLACE_ME"):
            raise RuntimeError(
                "GEMINI_API_KEY is not set or is a placeholder. "
                "Add a valid key to your .env file to use the chatbot."
            )

        self._client = genai.Client(api_key=gemini_key)

        from models.mapper import get_mapper
        self._mapper = get_mapper()

    def _retrieve(self, question: str):
        """Single mapper.search call; returns (context_str, section_ids, tier)."""
        result = self._mapper.search(query=question, top_k=5)
        if not result.results:
            return "No specific sections retrieved for this query.", [], 3

        parts: list[str] = []
        sec_nums: list[str] = []
        for i, r in enumerate(result.results, 1):
            p = f"[{i}]"
            if r.ipc_section and r.ipc_description not in ("Repealed", ""):
                p += f" IPC {r.ipc_section} ({r.ipc_heading}): {r.ipc_description[:500]}"
            if r.bns_section and r.bns_description not in ("Repealed in BNS", ""):
                p += f" | BNS {r.bns_section} ({r.bns_heading}): {r.bns_description[:500]}"
            if r.status == "repealed":
                p += " | STATUS: REPEALED IN BNS 2023"
            elif r.status == "new_in_bns":
                p += " | STATUS: NEW IN BNS (no IPC equivalent)"
            parts.append(p)
            sec_nums.append(r.ipc_section or r.bns_section or "")

        return "\n---\n".join(parts), sec_nums, result.retrieval_tier

    def _build_contents(self, request: ChatRequest, system: str) -> list[types.Content]:
        """
        Build the full contents list for the Gemini API call.
        Structure: [system_turn(user), system_ack(model), ...history..., current_user_msg]
        """
        contents: list[types.Content] = []

        contents.append(types.Content(role="user", parts=[types.Part(text=system)]))
        contents.append(types.Content(
            role="model",
            parts=[types.Part(text="Understood. I am Case Dhara, ready to assist with all Indian law questions.")],
        ))

        for msg in request.history[-10:]:
            role = "user" if msg.role == "user" else "model"
            contents.append(types.Content(role=role, parts=[types.Part(text=msg.content)]))

        contents.append(types.Content(role="user", parts=[types.Part(text=request.question)]))
        return contents

    def _needs_disclaimer(self, question: str) -> bool:
        personal = [
            "mera", "meri", "mujhe", "i was", "i have been",
            "my case", "against me", "arrested", "charged",
            "fir against", "bail", "false case",
        ]
        q = question.lower()
        return any(s in q for s in personal)

    def _generate_with_fallback(self, contents: list, max_tok: int) -> str:
        """Try GEMINI_CHAT_MODEL first, then fallbacks if 404."""
        last_error: Exception = RuntimeError("No models tried.")
        for model in [GEMINI_CHAT_MODEL] + GEMINI_FALLBACK_MODELS:
            try:
                response = self._client.models.generate_content(
                    model=model,
                    contents=contents,
                    config=types.GenerateContentConfig(
                        max_output_tokens=max_tok,
                        temperature=0.2,
                    ),
                )
                return response.text
            except Exception as e:
                last_error = e
                if "404" in str(e) or "NOT_FOUND" in str(e):
                    continue
                raise
        raise RuntimeError(f"All Gemini models unavailable. Last error: {last_error}")

    def chat(self, request: ChatRequest) -> ChatResponse:
        context, sec_nums, tier = self._retrieve(request.question)
        lang = detect_lang(request.question)
        system = SYSTEM_PROMPT.format(context=context, lang=lang)
        max_tok = 2048 if len(request.question) > 100 else 1024

        contents = self._build_contents(request, system)
        answer = self._generate_with_fallback(contents, max_tok)

        if self._needs_disclaimer(request.question):
            answer += INTENT_DISCLAIMER

        return ChatResponse(
            answer=answer,
            retrieved_sections=[s for s in sec_nums if s],
            retrieval_tier=tier,
        )

    def stream(self, request: ChatRequest):
        context, _, _ = self._retrieve(request.question)
        lang = detect_lang(request.question)
        system = SYSTEM_PROMPT.format(context=context, lang=lang)
        max_tok = 2048 if len(request.question) > 100 else 1024

        contents = self._build_contents(request, system)

        last_error: Exception = RuntimeError("No models tried.")
        for model in [GEMINI_CHAT_MODEL] + GEMINI_FALLBACK_MODELS:
            try:
                for chunk in self._client.models.generate_content_stream(
                    model=model,
                    contents=contents,
                    config=types.GenerateContentConfig(
                        max_output_tokens=max_tok,
                        temperature=0.2,
                    ),
                ):
                    if chunk.text:
                        yield chunk.text
                if self._needs_disclaimer(request.question):
                    yield INTENT_DISCLAIMER
                return
            except Exception as e:
                last_error = e
                if "404" in str(e) or "NOT_FOUND" in str(e):
                    continue
                raise
        raise RuntimeError(f"All Gemini models unavailable for streaming. Last error: {last_error}")


# ── Lazy singleton ─────────────────────────────────────────────────────────────

_chatbot: Optional[LegalChatbot] = None
_chatbot_error: Optional[str] = None


def get_chatbot() -> LegalChatbot:
    global _chatbot, _chatbot_error
    if _chatbot is None:
        try:
            _chatbot = LegalChatbot()
            _chatbot_error = None
        except Exception as e:
            _chatbot_error = str(e)
            raise
    return _chatbot


def chatbot_singleton_loaded() -> bool:
    return _chatbot is not None
