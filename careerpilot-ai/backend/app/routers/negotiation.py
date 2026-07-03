from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.agents import negotiation_agent
from app.database import get_db
from app.models import CareerProfile, Job, User
from app.security import get_current_user

router = APIRouter(prefix="/api/negotiation", tags=["negotiation"])


class NegotiationRequest(BaseModel):
    job_id: int
    offered_salary: float | None = None


@router.post("/evaluate")
def evaluate_offer(
    payload: NegotiationRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    job = db.get(Job, payload.job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    profile = db.query(CareerProfile).filter(CareerProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Create your Career Profile first")

    return negotiation_agent.evaluate(profile, job, payload.offered_salary)
