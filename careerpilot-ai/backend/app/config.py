import os
from functools import lru_cache


class Settings:
    """Application configuration, sourced entirely from environment variables.

    Nothing here has a hardcoded secret. Copy backend/.env.example to backend/.env
    and fill in real values (OPENAI_API_KEY, DATABASE_URL, ...).
    """

    app_name: str = "CareerPilot AI"

    database_url: str = os.getenv(
        "DATABASE_URL", "sqlite:///./careerpilot.db"
    )

    openai_api_key: str = os.getenv("OPENAI_API_KEY", "")
    openai_chat_model: str = os.getenv("OPENAI_CHAT_MODEL", "gpt-4o-mini")
    openai_embedding_model: str = os.getenv(
        "OPENAI_EMBEDDING_MODEL", "text-embedding-3-small"
    )

    # Public, ToS-compliant job board APIs (no auth required).
    remoteok_api_url: str = "https://remoteok.com/api"
    arbeitnow_api_url: str = "https://www.arbeitnow.com/api/job-board-api"
    greenhouse_api_url: str = "https://boards-api.greenhouse.io/v1/boards/{company}/jobs"
    lever_api_url: str = "https://api.lever.co/v0/postings/{company}?mode=json"

    # Comma-separated company slugs to poll on Greenhouse/Lever (customize freely).
    greenhouse_companies: list[str] = [
        c.strip()
        for c in os.getenv("GREENHOUSE_COMPANIES", "stripe,gitlab,cloudflare").split(",")
        if c.strip()
    ]
    lever_companies: list[str] = [
        c.strip()
        for c in os.getenv("LEVER_COMPANIES", "netflix").split(",")
        if c.strip()
    ]

    cors_origins: list[str] = [
        o.strip()
        for o in os.getenv("CORS_ORIGINS", "http://localhost:5173").split(",")
        if o.strip()
    ]

    jwt_secret: str = os.getenv("JWT_SECRET", "dev-secret-change-me")
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = int(os.getenv("JWT_EXPIRE_MINUTES", "10080"))  # 7 days


@lru_cache
def get_settings() -> Settings:
    return Settings()
