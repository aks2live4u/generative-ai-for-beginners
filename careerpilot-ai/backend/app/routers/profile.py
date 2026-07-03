from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import CareerProfile, User
from app.schemas import CareerProfileIn, CareerProfileOut
from app.security import get_current_user
from app.services.ai_service import ai_service

router = APIRouter(prefix="/api/profile", tags=["career-profile"])


@router.get("", response_model=CareerProfileOut)
def get_profile(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    profile = db.query(CareerProfile).filter(CareerProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Profile not found")
    return profile


@router.put("", response_model=CareerProfileOut)
def update_profile(
    payload: CareerProfileIn,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    profile = db.query(CareerProfile).filter(CareerProfile.user_id == current_user.id).first()
    if not profile:
        profile = CareerProfile(user_id=current_user.id)
        db.add(profile)

    for field, value in payload.model_dump().items():
        setattr(profile, field, value)

    embed_text = f"{profile.resume_text}\n{' '.join(profile.skills)}\n{profile.experience_summary}"
    try:
        profile.embedding = ai_service.embed(embed_text)
    except Exception:
        pass

    db.commit()
    db.refresh(profile)
    return profile
