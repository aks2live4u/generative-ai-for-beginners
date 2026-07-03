from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import Job
from app.services.job_search_service import fetch_and_store_jobs


class ScoutAgent:
    """Finds jobs: pulls from every connector and applies filters on request."""

    def refresh(self, db: Session, sources: list[str] | None = None) -> dict:
        return fetch_and_store_jobs(db, sources=sources)

    def search(
        self,
        db: Session,
        keyword: str | None = None,
        remote_type: str | None = None,
        country: str | None = None,
        employment_type: str | None = None,
        min_salary: float | None = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[Job]:
        stmt = select(Job)
        if keyword:
            like = f"%{keyword.lower()}%"
            stmt = stmt.where(
                (Job.title.ilike(like))
                | (Job.company.ilike(like))
                | (Job.description.ilike(like))
            )
        if remote_type:
            stmt = stmt.where(Job.remote_type == remote_type)
        if country:
            stmt = stmt.where(Job.country.ilike(f"%{country}%"))
        if employment_type:
            stmt = stmt.where(Job.employment_type == employment_type)
        if min_salary:
            stmt = stmt.where(Job.salary_max.isnot(None), Job.salary_max >= min_salary)

        stmt = stmt.where(Job.is_flagged_suspicious.is_(False))
        stmt = stmt.order_by(Job.posted_at.desc().nullslast()).offset(offset).limit(limit)
        return list(db.scalars(stmt).all())


scout_agent = ScoutAgent()
