"""
Legal chatbot: single mapper retrieval per request, RAG.
Migrated to the new google-genai SDK (client-based API).
"""

import os

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


GEMINI_CHAT_MODEL = "gemini-2.0-flash"

SYSTEM_PROMPT = """NYAYASETU MASTER SYSTEM PROMPT
You are NyayaSetu, an authoritative AI legal assistant for Indian criminal law,
specialising exclusively in the Indian Penal Code 1860 (IPC) and the Bharatiya Nyaya Sanhita 2023 (BNS).
BNS came into force on 1 July 2024, replacing IPC entirely.

CAPABILITIES:
- SECTION LOOKUP: Retrieve and explain IPC or BNS sections by number or topic.
- IPC TO BNS MAPPING: Find BNS equivalents, including merged, split, repealed, and new sections.
- CHATBOT Q&A: Answer questions about criminal law, FIR procedure, bail, punishments, and definitions under IPC/BNS.

STRICT RULES:
1. Only answer questions about IPC, BNS, CrPC/BNSS, or directly related Indian criminal law.
2. For unrelated topics say: "I can only assist with IPC/BNS legal questions."
3. Always cite specific section numbers when discussing offences or penalties.
4. Never give personal legal advice. Add the legal-information disclaimer for case-specific questions.
5. Match the user's language. If the user writes Hindi/Hinglish, respond in Hindi/Hinglish.
6. For IPC 124A, 377, 309, 497, and other repealed sections, state they were repealed on 1 July 2024 and give a BNS equivalent only if known from context.
7. Do not invent section numbers or case citations. Say when you are not certain.
8. Keep answers concise unless the user asks for detail.

RETRIEVED LEGAL CONTEXT:
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
    except LangDetectException:
        return "English"
    except Exception:
        return "English"


def _usable_key(value: str | None, placeholder: str) -> bool:
    return bool(value and value.strip() and placeholder not in value)


class LegalChatbot:
    def __init__(self):
        gemini_key = os.getenv("GEMINI_API_KEY")
        if genai is None or not _usable_key(gemini_key, "REPLACE_ME"):
            raise RuntimeError("Gemini API key not configured or google-genai not installed.")

        # New SDK: create a Client instance instead of genai.configure()
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

    def _build_history_contents(self, request: ChatRequest) -> list[types.Content]:
        """Build history as list of Content objects for the new SDK."""
        history = []
        for msg in request.history[-10:]:
            role = "user" if msg.role == "user" else "model"
            history.append(types.Content(role=role, parts=[types.Part(text=msg.content)]))
        return history

    def _needs_disclaimer(self, question: str) -> bool:
        personal = [
            "mera",
            "meri",
            "mujhe",
            "i was",
            "i have been",
            "my case",
            "against me",
            "arrested",
            "charged",
            "fir against",
            "bail",
            "false case",
        ]
        q = question.lower()
        return any(s in q for s in personal)

    def chat(self, request: ChatRequest) -> ChatResponse:
        context, sec_nums, tier = self._retrieve(request.question)
        lang = detect_lang(request.question)
        system = SYSTEM_PROMPT.format(context=context, lang=lang)
        max_tok = 2048 if len(request.question) > 100 else 1024

        # Build history + current user message as contents
        history = self._build_history_contents(request)
        user_msg = f"{system}\n\nUser Question: {request.question}"
        history.append(types.Content(role="user", parts=[types.Part(text=user_msg)]))

        response = self._client.models.generate_content(
            model=GEMINI_CHAT_MODEL,
            contents=history,
            config=types.GenerateContentConfig(
                max_output_tokens=max_tok,
                temperature=0.2,
            ),
        )
        answer = response.text
        if self._needs_disclaimer(request.question):
            answer += INTENT_DISCLAIMER
        return ChatResponse(answer=answer, retrieved_sections=[s for s in sec_nums if s], retrieval_tier=tier)

    def stream(self, request: ChatRequest):
        context, _, _ = self._retrieve(request.question)
        lang = detect_lang(request.question)
        system = SYSTEM_PROMPT.format(context=context, lang=lang)
        max_tok = 2048 if len(request.question) > 100 else 1024

        history = self._build_history_contents(request)
        user_msg = f"{system}\n\nUser Question: {request.question}"
        history.append(types.Content(role="user", parts=[types.Part(text=user_msg)]))

        for chunk in self._client.models.generate_content_stream(
            model=GEMINI_CHAT_MODEL,
            contents=history,
            config=types.GenerateContentConfig(
                max_output_tokens=max_tok,
                temperature=0.2,
            ),
        ):
            if chunk.text:
                yield chunk.text
        if self._needs_disclaimer(request.question):
            yield INTENT_DISCLAIMER


_chatbot: LegalChatbot | None = None


def get_chatbot() -> LegalChatbot:
    global _chatbot
    if _chatbot is None:
        _chatbot = LegalChatbot()
    return _chatbot


def chatbot_singleton_loaded() -> bool:
    return _chatbot is not None
