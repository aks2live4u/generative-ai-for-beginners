from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.agents import resume_agent
from app.database import get_db
from app.models import CareerProfile, Job, Resume, User
from app.schemas import ResumeGenerateRequest, ResumeOut
from app.security import get_current_user
from app.services.export_service import markdown_to_docx_bytes, markdown_to_pdf_bytes

router = APIRouter(prefix="/api/resumes", tags=["resume"])


@router.post("/generate", response_model=ResumeOut)
def generate_resume(
    payload: ResumeGenerateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    job = db.get(Job, payload.job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    profile = db.query(CareerProfile).filter(CareerProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Create your Career Profile first")

    existing_versions = db.scalar(
        select(Resume)
        .where(Resume.user_id == current_user.id, Resume.job_id == payload.job_id)
        .order_by(Resume.version.desc())
    )
    next_version = (existing_versions.version + 1) if existing_versions else 1

    result = resume_agent.generate(profile, job)
    resume = Resume(
        user_id=current_user.id,
        job_id=payload.job_id,
        version=next_version,
        content_markdown=result["content_markdown"],
        ats_score=result["ats_score"],
        keyword_match_pct=result["keyword_match_pct"],
        readability_score=result["readability_score"],
        template=payload.template,
    )
    db.add(resume)
    db.commit()
    db.refresh(resume)
    return resume


@router.get("", response_model=list[ResumeOut])
def list_resumes(job_id: int | None = None, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    stmt = select(Resume).where(Resume.user_id == current_user.id)
    if job_id:
        stmt = stmt.where(Resume.job_id == job_id)
    return list(db.scalars(stmt.order_by(Resume.created_at.desc())).all())


def _get_owned_resume(db: Session, resume_id: int, user_id: int) -> Resume:
    resume = db.get(Resume, resume_id)
    if not resume or resume.user_id != user_id:
        raise HTTPException(status_code=404, detail="Resume not found")
    return resume


@router.get("/{resume_id}/export/pdf")
def export_pdf(resume_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    resume = _get_owned_resume(db, resume_id, current_user.id)
    pdf_bytes = markdown_to_pdf_bytes(resume.content_markdown)
    return StreamingResponse(
        iter([pdf_bytes]),
        media_type="application/pdf",
        headers={"Content-Disposition": f"attachment; filename=resume_v{resume.version}.pdf"},
    )


@router.get("/{resume_id}/export/docx")
def export_docx(resume_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    resume = _get_owned_resume(db, resume_id, current_user.id)
    docx_bytes = markdown_to_docx_bytes(resume.content_markdown)
    return StreamingResponse(
        iter([docx_bytes]),
        media_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        headers={"Content-Disposition": f"attachment; filename=resume_v{resume.version}.docx"},
    )
