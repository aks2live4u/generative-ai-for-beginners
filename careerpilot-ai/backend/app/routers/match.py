from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.agents import matcher_agent
from app.database import get_db
from app.models import CareerProfile, Job, MatchScore, User
from app.schemas import JobWithScore, MatchScoreOut
from app.security import get_current_user

router = APIRouter(prefix="/api/match", tags=["match"])


def _get_profile(db: Session, user_id: int) -> CareerProfile:
    profile = db.query(CareerProfile).filter(CareerProfile.user_id == user_id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Create your Career Profile first")
    return profile


@router.post("/jobs/{job_id}", response_model=MatchScoreOut)
def score_job(job_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    job = db.get(Job, job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    profile = _get_profile(db, current_user.id)

    score = matcher_agent.score(profile, job)
    db.add(score)
    db.commit()
    db.refresh(score)
    return score


@router.post("/refresh-all")
def score_all_unscored(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    profile = _get_profile(db, current_user.id)
    scored_job_ids = {
        row[0]
        for row in db.query(MatchScore.job_id).filter(MatchScore.user_id == current_user.id).all()
    }
    all_jobs = db.scalars(select(Job)).all()
    created = 0
    for job in all_jobs:
        if job.id in scored_job_ids:
            continue
        score = matcher_agent.score(profile, job)
        db.add(score)
        created += 1
    db.commit()
    return {"newly_scored": created}


@router.get("/best", response_model=list[JobWithScore])
def best_matches(
    min_score: float = Query(75.0),
    limit: int = Query(20, le=500),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    stmt = (
        select(Job, MatchScore)
        .join(MatchScore, MatchScore.job_id == Job.id)
        .where(MatchScore.user_id == current_user.id, MatchScore.overall_score >= min_score)
        .order_by(MatchScore.overall_score.desc())
        .limit(limit)
    )
    rows = db.execute(stmt).all()
    return [JobWithScore(job=job, score=score) for job, score in rows]
