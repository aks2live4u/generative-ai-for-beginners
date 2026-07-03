"""Thin wrapper around whichever AI provider is configured (OpenAI or Gemini).

Every agent in app/agents/ calls into this module instead of a provider SDK
directly. That gives us one place to swap/add providers, and one place where
the "no API key configured" fallback lives -- the app must stay fully usable
without a key so it can be cloned and run immediately; agents fall back to
deterministic, template-based output (see each agent's `_fallback_*` method)
when `AiService.is_available` is False or a call errors out.
"""

import hashlib
import json
import math

from app.config import get_settings

settings = get_settings()


class AiService:
    def __init__(self) -> None:
        self.provider = settings.ai_provider  # "openai" | "gemini" | "none"
        self.is_available = self.provider in ("openai", "gemini")
        self._client = None

        if self.provider == "openai":
            from openai import OpenAI

            self._client = OpenAI(api_key=settings.openai_api_key)
        elif self.provider == "gemini":
            from google import genai

            self._client = genai.Client(api_key=settings.gemini_api_key)

    def complete(
        self, system_prompt: str, user_prompt: str, json_mode: bool = False, temperature: float = 0.4
    ) -> str:
        if self.provider == "openai":
            return self._complete_openai(system_prompt, user_prompt, json_mode, temperature)
        if self.provider == "gemini":
            return self._complete_gemini(system_prompt, user_prompt, json_mode, temperature)
        raise RuntimeError("No AI provider configured (set OPENAI_API_KEY or GEMINI_API_KEY)")

    def _complete_openai(self, system_prompt: str, user_prompt: str, json_mode: bool, temperature: float) -> str:
        response = self._client.chat.completions.create(
            model=settings.openai_chat_model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            temperature=temperature,
            response_format={"type": "json_object"} if json_mode else {"type": "text"},
        )
        return response.choices[0].message.content or ""

    def _complete_gemini(self, system_prompt: str, user_prompt: str, json_mode: bool, temperature: float) -> str:
        from google.genai import types

        response = self._client.models.generate_content(
            model=settings.gemini_chat_model,
            contents=user_prompt,
            config=types.GenerateContentConfig(
                system_instruction=system_prompt,
                temperature=temperature,
                response_mime_type="application/json" if json_mode else "text/plain",
            ),
        )
        return response.text or ""

    def complete_json(self, system_prompt: str, user_prompt: str, temperature: float = 0.4) -> dict:
        raw = self.complete(system_prompt, user_prompt, json_mode=True, temperature=temperature)
        return json.loads(raw)

    def embed(self, text: str) -> list[float]:
        if self.provider == "openai":
            response = self._client.embeddings.create(
                model=settings.openai_embedding_model, input=text[:8000]
            )
            return response.data[0].embedding
        if self.provider == "gemini":
            response = self._client.models.embed_content(
                model=settings.gemini_embedding_model, contents=text[:8000]
            )
            return list(response.embeddings[0].values)
        return hashing_embedding(text)


def hashing_embedding(text: str, dims: int = 256) -> list[float]:
    """Deterministic bag-of-words hashing embedding used when no API key is set.

    Not as good as a real embedding model, but gives cosine similarity between
    resumes/JDs that share vocabulary a real, non-random signal, so matching
    still works end-to-end without any external dependency.
    """
    vector = [0.0] * dims
    tokens = "".join(c.lower() if c.isalnum() else " " for c in text).split()
    for token in tokens:
        digest = hashlib.sha256(token.encode()).hexdigest()
        index = int(digest, 16) % dims
        sign = 1.0 if int(digest, 16) % 2 == 0 else -1.0
        vector[index] += sign

    norm = math.sqrt(sum(v * v for v in vector)) or 1.0
    return [v / norm for v in vector]


def cosine_similarity(a: list[float], b: list[float]) -> float:
    if not a or not b or len(a) != len(b):
        return 0.0
    dot = sum(x * y for x, y in zip(a, b))
    norm_a = math.sqrt(sum(x * x for x in a)) or 1.0
    norm_b = math.sqrt(sum(y * y for y in b)) or 1.0
    return max(0.0, min(1.0, dot / (norm_a * norm_b)))


ai_service = AiService()
