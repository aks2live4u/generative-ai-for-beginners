from collections import Counter
from datetime import datetime, timedelta

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import Application, ApplicationStage, MatchScore, Resume, User
from app.schemas import AnalyticsOut
from app.security import get_current_user

router = APIRouter(prefix="/api/analytics", tags=["analytics"])

INTERVIEW_STAGES = {ApplicationStage.interview, ApplicationStage.offer, ApplicationStage.accepted}


@router.get("", response_model=AnalyticsOut)
def get_analytics(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    applications = list(
        db.scalars(select(Application).where(Application.user_id == current_user.id)).all()
    )
    applied = [a for a in applications if a.applied_at is not None]
    interviewed = [a for a in applied if a.stage in INTERVIEW_STAGES]

    interview_rate = round((len(interviewed) / len(applied)) * 100, 1) if applied else 0.0

    resumes = list(db.scalars(select(Resume).where(Resume.user_id == current_user.id)).all())
    avg_ats = round(sum(r.ats_score for r in resumes) / len(resumes), 1) if resumes else 0.0

    response_days = [
        (a.last_activity_at - a.applied_at).days
        for a in applied
        if a.stage != ApplicationStage.applied and a.last_activity_at and a.applied_at
    ]
    avg_response_time = round(sum(response_days) / len(response_days), 1) if response_days else None

    stage_counts = dict(Counter(a.stage.value for a in applications))

    scores = list(db.scalars(select(MatchScore).where(MatchScore.user_id == current_user.id)).all())
    missing_counter: Counter[str] = Counter()
    for s in scores:
        missing_counter.update(s.missing_keywords)
    top_missing_skills = [skill for skill, _ in missing_counter.most_common(8)]

    now = datetime.utcnow()
    weekly_applications = []
    for week_offset in range(7, -1, -1):
        week_start = now - timedelta(weeks=week_offset + 1)
        week_end = now - timedelta(weeks=week_offset)
        count = sum(1 for a in applied if a.applied_at and week_start <= a.applied_at < week_end)
        weekly_applications.append({"week_starting": week_start.date().isoformat(), "count": count})

    return AnalyticsOut(
        applications_sent=len(applied),
        interview_rate=interview_rate,
        avg_resume_ats_score=avg_ats,
        avg_response_time_days=avg_response_time,
        stage_counts=stage_counts,
        top_missing_skills=top_missing_skills,
        weekly_applications=weekly_applications,
    )
