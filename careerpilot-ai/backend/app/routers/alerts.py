from datetime import datetime, timedelta

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import Job, MatchScore, User
from app.schemas import JobWithScore
from app.security import get_current_user

router = APIRouter(prefix="/api/alerts", tags=["alerts"])


@router.get("/daily-digest", response_model=list[JobWithScore])
def daily_digest(
    min_score: float = Query(80.0),
    since_hours: int = Query(48),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Instead of flooding you with every posting, this returns only the
    handful of jobs genuinely worth applying to right now."""
    since = datetime.utcnow() - timedelta(hours=since_hours)
    stmt = (
        select(Job, MatchScore)
        .join(MatchScore, MatchScore.job_id == Job.id)
        .where(
            MatchScore.user_id == current_user.id,
            MatchScore.overall_score >= min_score,
            Job.is_flagged_suspicious.is_(False),
            Job.is_likely_ghost.is_(False),
            MatchScore.created_at >= since,
        )
        .order_by(MatchScore.overall_score.desc())
        .limit(15)
    )
    rows = db.execute(stmt).all()
    return [JobWithScore(job=job, score=score) for job, score in rows]
