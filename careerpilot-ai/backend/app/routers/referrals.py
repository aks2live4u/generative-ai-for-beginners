from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.agents import outreach_agent
from app.database import get_db
from app.models import CareerProfile, Job, Referral, User
from app.schemas import ReferralOut
from app.security import get_current_user

router = APIRouter(prefix="/api/referrals", tags=["referrals"])


@router.post("/jobs/{job_id}", response_model=list[ReferralOut])
def find_referrals(job_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    job = db.get(Job, job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    profile = db.query(CareerProfile).filter(CareerProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Create your Career Profile first")

    suggestions = outreach_agent.find_referrals(profile, job)
    records = []
    for s in suggestions:
        referral = Referral(user_id=current_user.id, job_id=job_id, **s)
        db.add(referral)
        records.append(referral)
    db.commit()
    for r in records:
        db.refresh(r)
    return records


@router.get("", response_model=list[ReferralOut])
def list_referrals(job_id: int | None = None, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    stmt = select(Referral).where(Referral.user_id == current_user.id)
    if job_id:
        stmt = stmt.where(Referral.job_id == job_id)
    return list(db.scalars(stmt.order_by(Referral.created_at.desc())).all())
