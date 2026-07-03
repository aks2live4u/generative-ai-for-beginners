from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.database import Base, engine
from app.routers import (
    alerts,
    analytics,
    applications,
    auth,
    cover_letter,
    followups,
    interview,
    jobs,
    learning,
    match,
    negotiation,
    outreach,
    profile,
    referrals,
    resume,
    salary,
)

settings = get_settings()

Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="CareerPilot AI",
    description="An autonomous AI career agent: finds jobs, scores fit, tailors resumes and "
    "cover letters, tracks applications, and preps you for interviews.",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

for router in (
    auth.router,
    profile.router,
    jobs.router,
    match.router,
    resume.router,
    cover_letter.router,
    applications.router,
    outreach.router,
    referrals.router,
    interview.router,
    salary.router,
    negotiation.router,
    followups.router,
    analytics.router,
    alerts.router,
    learning.router,
):
    app.include_router(router)


@app.get("/api/health")
def health_check():
    return {"status": "ok", "ai_configured": bool(settings.openai_api_key)}
