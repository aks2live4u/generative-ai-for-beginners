from collections import Counter

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import MatchScore
from app.services.learning_service import recommend_for_skill


class CareerCoachAgent:
    """Looks at recurring skill gaps across match scores and recommends how to close them."""

    def recommend(self, db: Session, user_id: int, top_n: int = 5) -> list[dict]:
        stmt = select(MatchScore).where(MatchScore.user_id == user_id)
        scores = db.scalars(stmt).all()

        counter: Counter[str] = Counter()
        for score in scores:
            counter.update(score.missing_keywords)

        recommendations = []
        for skill, count in counter.most_common(top_n):
            rec = recommend_for_skill(skill)
            recommendations.append({"skill": skill, "demand_count": count, **{k: v for k, v in rec.items() if k != "skill"}})
        return recommendations


career_coach_agent = CareerCoachAgent()
