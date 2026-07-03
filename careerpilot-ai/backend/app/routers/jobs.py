from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.agents import scout_agent
from app.database import get_db
from app.models import Job, User
from app.schemas import JobOut
from app.security import get_current_user

router = APIRouter(prefix="/api/jobs", tags=["jobs"])


@router.post("/refresh")
def refresh_jobs(
    sources: str | None = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    source_list = [s.strip() for s in sources.split(",")] if sources else None
    stats = scout_agent.refresh(db, sources=source_list)
    return stats


@router.get("", response_model=list[JobOut])
def list_jobs(
    keyword: str | None = None,
    remote_type: str | None = None,
    country: str | None = None,
    employment_type: str | None = None,
    min_salary: float | None = None,
    limit: int = Query(50, le=200),
    offset: int = 0,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return scout_agent.search(
        db,
        keyword=keyword,
        remote_type=remote_type,
        country=country,
        employment_type=employment_type,
        min_salary=min_salary,
        limit=limit,
        offset=offset,
    )


@router.get("/{job_id}", response_model=JobOut)
def get_job(job_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    job = db.get(Job, job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    return job
