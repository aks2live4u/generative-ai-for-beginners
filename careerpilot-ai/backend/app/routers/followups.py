from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.agents import followup_agent
from app.database import get_db
from app.models import Application, FollowUp, User
from app.schemas import FollowUpOut
from app.security import get_current_user

router = APIRouter(prefix="/api/followups", tags=["followups"])


@router.get("/due", response_model=list[FollowUpOut])
def due_followups(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    all_due = followup_agent.due_now(db)
    owned_application_ids = {
        row[0] for row in db.query(Application.id).filter(Application.user_id == current_user.id).all()
    }
    return [f for f in all_due if f.application_id in owned_application_ids]


@router.get("/applications/{application_id}", response_model=list[FollowUpOut])
def list_followups(application_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    application = db.get(Application, application_id)
    if not application or application.user_id != current_user.id:
        raise HTTPException(status_code=404, detail="Application not found")
    stmt = select(FollowUp).where(FollowUp.application_id == application_id).order_by(FollowUp.due_at)
    return list(db.scalars(stmt).all())


@router.patch("/{followup_id}/mark-sent", response_model=FollowUpOut)
def mark_sent(followup_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    followup = db.get(FollowUp, followup_id)
    if not followup:
        raise HTTPException(status_code=404, detail="Follow-up not found")
    application = db.get(Application, followup.application_id)
    if not application or application.user_id != current_user.id:
        raise HTTPException(status_code=404, detail="Follow-up not found")
    return followup_agent.mark_sent(db, followup)
