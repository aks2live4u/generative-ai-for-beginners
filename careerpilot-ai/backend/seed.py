"""Seeds a demo user (demo@careerpilot.ai / demo1234) with a filled-in Career
Profile tailored to a Cloud Operations / ITIL / Leadership background, pulls
in jobs from every connector, and pre-computes match scores so the app has
something to show the moment you log in.

Run with: python seed.py
"""

from app.agents import matcher_agent, scout_agent
from app.database import Base, SessionLocal, engine
from app.models import CareerProfile, RemotePreference, User
from app.security import hash_password
from app.services.ai_service import ai_service

DEMO_EMAIL = "demo@careerpilot.ai"
DEMO_PASSWORD = "demo1234"


def run():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        user = db.query(User).filter(User.email == DEMO_EMAIL).first()
        if not user:
            user = User(
                email=DEMO_EMAIL,
                hashed_password=hash_password(DEMO_PASSWORD),
                full_name="Demo Candidate",
            )
            db.add(user)
            db.commit()
            db.refresh(user)
            print(f"Created demo user {DEMO_EMAIL} / {DEMO_PASSWORD}")

        profile = db.query(CareerProfile).filter(CareerProfile.user_id == user.id).first()
        if not profile:
            profile = CareerProfile(user_id=user.id)
            db.add(profile)

        profile.resume_text = (
            "Cloud Operations Manager with 9+ years leading 24/7 operations and SRE teams. "
            "Ran incident, problem, and change management under ITIL v4. Owned multi-region "
            "AWS infrastructure, cut MTTR by 35%, and led a team of 12 engineers."
        )
        profile.skills = ["AWS", "ITIL", "Leadership", "Cloud Operations", "Incident Management", "Kubernetes"]
        profile.certifications = ["ITIL v4 Foundation", "AWS Certified SysOps Administrator"]
        profile.experience_years = 9
        profile.experience_summary = (
            "Cloud Operations Manager with 9+ years leading global, 24/7 operations teams "
            "across AWS and Azure, with a strong ITIL service management background."
        )
        profile.salary_expectation_min = 75000
        profile.salary_expectation_max = 100000
        profile.salary_currency = "GBP"
        profile.preferred_countries = ["United Kingdom", "Germany", "United Arab Emirates", "India"]
        profile.target_titles = [
            "Cloud Operations Manager",
            "Cloud Service Delivery Manager",
            "Site Reliability Engineering Manager",
            "Cloud Infrastructure Manager",
            "DevOps Manager",
            "IT Operations Manager",
        ]
        profile.industries = ["Cloud Infrastructure", "Fintech", "SaaS"]
        profile.technologies = ["AWS", "Azure", "Terraform", "Kubernetes", "Ansible"]
        profile.visa_status = "No sponsorship required (UK citizen)"
        profile.notice_period_days = 60
        profile.remote_preference = RemotePreference.remote

        try:
            profile.embedding = ai_service.embed(
                f"{profile.resume_text}\n{' '.join(profile.skills)}\n{profile.experience_summary}"
            )
        except Exception:
            pass

        db.commit()

        stats = scout_agent.refresh(db)
        print(f"Job fetch stats: {stats}")

        db.refresh(profile)
        jobs = scout_agent.search(db, limit=200)
        scored = 0
        for job in jobs:
            score = matcher_agent.score(profile, job)
            db.add(score)
            scored += 1
        db.commit()
        print(f"Pre-computed match scores for {scored} jobs")

    finally:
        db.close()


if __name__ == "__main__":
    run()
