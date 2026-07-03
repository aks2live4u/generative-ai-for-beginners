from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.agents import cover_letter_agent
from app.database import get_db
from app.models import CareerProfile, CoverLetter, Job, User
from app.schemas import CoverLetterGenerateRequest, CoverLetterOut
from app.security import get_current_user
from app.services.export_service import markdown_to_docx_bytes, markdown_to_pdf_bytes

router = APIRouter(prefix="/api/cover-letters", tags=["cover-letter"])


@router.post("/generate", response_model=CoverLetterOut)
def generate_cover_letter(
    payload: CoverLetterGenerateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    job = db.get(Job, payload.job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    profile = db.query(CareerProfile).filter(CareerProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Create your Career Profile first")

    content = cover_letter_agent.generate(profile, job, tone=payload.tone)
    letter = CoverLetter(user_id=current_user.id, job_id=payload.job_id, content=content, tone=payload.tone)
    db.add(letter)
    db.commit()
    db.refresh(letter)
    return letter


@router.get("", response_model=list[CoverLetterOut])
def list_cover_letters(job_id: int | None = None, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    stmt = select(CoverLetter).where(CoverLetter.user_id == current_user.id)
    if job_id:
        stmt = stmt.where(CoverLetter.job_id == job_id)
    return list(db.scalars(stmt.order_by(CoverLetter.created_at.desc())).all())


def _get_owned(db: Session, letter_id: int, user_id: int) -> CoverLetter:
    letter = db.get(CoverLetter, letter_id)
    if not letter or letter.user_id != user_id:
        raise HTTPException(status_code=404, detail="Cover letter not found")
    return letter


@router.get("/{letter_id}/export/pdf")
def export_pdf(letter_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    letter = _get_owned(db, letter_id, current_user.id)
    pdf_bytes = markdown_to_pdf_bytes(letter.content)
    return StreamingResponse(
        iter([pdf_bytes]),
        media_type="application/pdf",
        headers={"Content-Disposition": f"attachment; filename=cover_letter_{letter.id}.pdf"},
    )


@router.get("/{letter_id}/export/docx")
def export_docx(letter_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    letter = _get_owned(db, letter_id, current_user.id)
    docx_bytes = markdown_to_docx_bytes(letter.content)
    return StreamingResponse(
        iter([docx_bytes]),
        media_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        headers={"Content-Disposition": f"attachment; filename=cover_letter_{letter.id}.docx"},
    )
