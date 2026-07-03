from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.agents import career_coach_agent
from app.database import get_db
from app.models import User
from app.schemas import LearningRecommendationOut
from app.security import get_current_user

router = APIRouter(prefix="/api/learning", tags=["learning"])


@router.get("/recommendations", response_model=list[LearningRecommendationOut])
def get_recommendations(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return career_coach_agent.recommend(db, current_user.id)
