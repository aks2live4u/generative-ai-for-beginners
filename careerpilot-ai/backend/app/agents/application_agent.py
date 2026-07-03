from datetime import datetime

from sqlalchemy.orm import Session

from app.models import Application, ApplicationStage, Job


# Sources with a documented, ToS-compliant application API a user could
# eventually wire up (Greenhouse/Lever both offer application submission
# endpoints for boards that opt in). Nothing here actually submits anything
# automatically today -- supports_auto_apply is informational, surfaced to the
# user so they know which applications *could* be automated later versus
# platforms (LinkedIn/Indeed/Glassdoor) that prohibit it outright.
AUTO_APPLY_CAPABLE_PREFIXES = ("greenhouse:", "lever:")


class ApplicationAgent:
    """Prepares and tracks applications. Never submits anything without the
    user's explicit action, to stay compliant with each platform's Terms of
    Service -- see AUTO_APPLY_CAPABLE_PREFIXES above.
    """

    def create(
        self,
        db: Session,
        user_id: int,
        job: Job,
        resume_id: int | None,
        cover_letter_id: int | None,
        notes: str = "",
    ) -> Application:
        application = Application(
            user_id=user_id,
            job_id=job.id,
            resume_id=resume_id,
            cover_letter_id=cover_letter_id,
            stage=ApplicationStage.saved,
            notes=notes,
        )
        db.add(application)
        db.commit()
        db.refresh(application)
        return application

    def mark_applied(self, db: Session, application: Application) -> Application:
        application.stage = ApplicationStage.applied
        application.applied_at = datetime.utcnow()
        application.last_activity_at = datetime.utcnow()
        db.commit()
        db.refresh(application)
        return application

    def can_auto_apply(self, job: Job) -> bool:
        return job.source.startswith(AUTO_APPLY_CAPABLE_PREFIXES)


application_agent = ApplicationAgent()
