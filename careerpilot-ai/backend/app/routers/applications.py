from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.agents import application_agent, followup_agent
from app.database import get_db
from app.models import Application, ApplicationStage, Job, User
from app.schemas import ApplicationCreate, ApplicationOut, ApplicationStageUpdate
from app.security import get_current_user

router = APIRouter(prefix="/api/applications", tags=["applications"])


@router.post("", response_model=ApplicationOut)
def create_application(
    payload: ApplicationCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    job = db.get(Job, payload.job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    application = application_agent.create(
        db, current_user.id, job, payload.resume_id, payload.cover_letter_id, payload.notes
    )
    return application


@router.get("", response_model=list[ApplicationOut])
def list_applications(
    stage: ApplicationStage | None = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    stmt = select(Application).where(Application.user_id == current_user.id)
    if stage:
        stmt = stmt.where(Application.stage == stage)
    return list(db.scalars(stmt.order_by(Application.last_activity_at.desc())).all())


def _get_owned(db: Session, application_id: int, user_id: int) -> Application:
    application = db.get(Application, application_id)
    if not application or application.user_id != user_id:
        raise HTTPException(status_code=404, detail="Application not found")
    return application


@router.patch("/{application_id}/stage", response_model=ApplicationOut)
def update_stage(
    application_id: int,
    payload: ApplicationStageUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from datetime import datetime

    application = _get_owned(db, application_id, current_user.id)
    application.stage = payload.stage
    application.last_activity_at = datetime.utcnow()

    if payload.stage == ApplicationStage.applied and not application.applied_at:
        application.applied_at = datetime.utcnow()
        db.commit()
        db.refresh(application)
        followup_agent.schedule_for_application(db, application, application.job)
    else:
        db.commit()
        db.refresh(application)

    return application


@router.get("/{application_id}", response_model=ApplicationOut)
def get_application(application_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return _get_owned(db, application_id, current_user.id)
