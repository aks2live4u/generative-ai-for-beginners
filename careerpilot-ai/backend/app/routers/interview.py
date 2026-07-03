from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.agents import interview_agent
from app.database import get_db
from app.models import Application, CareerProfile, InterviewPrep, User
from app.schemas import InterviewPrepOut
from app.security import get_current_user

router = APIRouter(prefix="/api/interview-prep", tags=["interview"])


@router.post("/applications/{application_id}", response_model=InterviewPrepOut)
def generate_prep(application_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    application = db.get(Application, application_id)
    if not application or application.user_id != current_user.id:
        raise HTTPException(status_code=404, detail="Application not found")
    profile = db.query(CareerProfile).filter(CareerProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Create your Career Profile first")

    data = interview_agent.prepare(profile, application.job)
    prep = InterviewPrep(application_id=application_id, **data)
    db.add(prep)
    db.commit()
    db.refresh(prep)
    return prep


@router.get("/applications/{application_id}", response_model=list[InterviewPrepOut])
def list_prep(application_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    application = db.get(Application, application_id)
    if not application or application.user_id != current_user.id:
        raise HTTPException(status_code=404, detail="Application not found")
    stmt = select(InterviewPrep).where(InterviewPrep.application_id == application_id).order_by(
        InterviewPrep.created_at.desc()
    )
    return list(db.scalars(stmt).all())
