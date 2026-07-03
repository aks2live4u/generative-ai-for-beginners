from datetime import datetime, timedelta

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import Application, FollowUp, Job
from app.services.ai_service import ai_service

# No response after 5 days -> first follow-up. 10 days -> second. 15 days -> close out.
SCHEDULE = [("first", 5), ("second", 10), ("close", 15)]


class FollowUpAgent:
    """Schedules and drafts follow-up messages so nothing falls through the cracks."""

    def schedule_for_application(self, db: Session, application: Application, job: Job) -> list[FollowUp]:
        if not application.applied_at:
            return []

        created = []
        for stage, days in SCHEDULE:
            due_at = application.applied_at + timedelta(days=days)
            draft = self._draft(job, stage)
            followup = FollowUp(
                application_id=application.id,
                stage=stage,
                due_at=due_at,
                draft_message=draft,
            )
            db.add(followup)
            created.append(followup)

        db.commit()
        for f in created:
            db.refresh(f)
        return created

    def due_now(self, db: Session) -> list[FollowUp]:
        now = datetime.utcnow()
        stmt = select(FollowUp).where(FollowUp.due_at <= now, FollowUp.sent.is_(False))
        return list(db.scalars(stmt).all())

    def mark_sent(self, db: Session, followup: FollowUp) -> FollowUp:
        followup.sent = True
        db.commit()
        db.refresh(followup)
        return followup

    def _draft(self, job: Job, stage: str) -> str:
        if stage == "close":
            return (
                f"No response after 15 days from {job.company} regarding the {job.title} role. "
                "Recommended action: mark this application as closed and redirect energy to "
                "higher-probability leads."
            )

        ordinal = "first" if stage == "first" else "second"
        if ai_service.is_available:
            try:
                return ai_service.complete(
                    system_prompt=(
                        f"Write a brief, polite {ordinal} follow-up email (under 90 words) to a "
                        "recruiter after applying for a job with no response yet. Confident, not pushy."
                    ),
                    user_prompt=f"Role: {job.title}\nCompany: {job.company}",
                    temperature=0.5,
                )
            except Exception:
                pass

        return (
            f"Hi, I wanted to follow up on my application for the {job.title} role at {job.company}. "
            "I'm still very interested and happy to provide any additional information that would help "
            "with your decision. Thank you for your time."
        )


followup_agent = FollowUpAgent()
