from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.agents import outreach_agent
from app.database import get_db
from app.models import CareerProfile, Job, OutreachMessage, User
from app.schemas import OutreachApprove, OutreachGenerateRequest, OutreachOut
from app.security import get_current_user

router = APIRouter(prefix="/api/outreach", tags=["outreach"])


@router.post("/generate", response_model=OutreachOut)
def generate_outreach(
    payload: OutreachGenerateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    job = db.get(Job, payload.job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    profile = db.query(CareerProfile).filter(CareerProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Create your Career Profile first")

    drafted = outreach_agent.draft_message(
        profile, job, payload.channel, payload.recipient_role, payload.recipient_name
    )
    message = OutreachMessage(
        user_id=current_user.id,
        job_id=payload.job_id,
        channel=payload.channel,
        recipient_role=payload.recipient_role,
        recipient_name=payload.recipient_name,
        subject=drafted["subject"],
        message=drafted["message"],
        approved=False,
        sent=False,
    )
    db.add(message)
    db.commit()
    db.refresh(message)
    return message


@router.get("", response_model=list[OutreachOut])
def list_outreach(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    stmt = select(OutreachMessage).where(OutreachMessage.user_id == current_user.id).order_by(
        OutreachMessage.created_at.desc()
    )
    return list(db.scalars(stmt).all())


@router.patch("/{message_id}/approve", response_model=OutreachOut)
def approve_outreach(
    message_id: int,
    payload: OutreachApprove,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Approving only marks the draft as reviewed -- CareerPilot never sends
    messages on your behalf. Copy the approved text into LinkedIn/email yourself,
    or wire up your own mail/LinkedIn integration if automation is permitted."""
    message = db.get(OutreachMessage, message_id)
    if not message or message.user_id != current_user.id:
        raise HTTPException(status_code=404, detail="Message not found")
    message.approved = payload.approved
    db.commit()
    db.refresh(message)
    return message
