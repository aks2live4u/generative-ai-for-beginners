from app.models import CareerProfile, Job
from app.services.ai_service import ai_service


class CoverLetterAgent:
    """Writes a unique cover letter per company, referencing the specific role."""

    def generate(self, profile: CareerProfile, job: Job, tone: str = "professional") -> str:
        if ai_service.is_available:
            try:
                return ai_service.complete(
                    system_prompt=(
                        f"You are an expert cover letter writer. Write a {tone}, specific, "
                        "non-generic cover letter (under 350 words) referencing the company "
                        "name, the role, and 2-3 concrete pieces of the candidate's experience "
                        "that map to the job description. No filler like 'I am writing to "
                        "express my interest'. Never fabricate facts not given to you."
                    ),
                    user_prompt=(
                        f"Candidate summary: {profile.experience_summary}\n"
                        f"Candidate skills: {', '.join(profile.skills)}\n"
                        f"Candidate certifications: {', '.join(profile.certifications)}\n\n"
                        f"Company: {job.company}\nRole: {job.title}\n"
                        f"Job description: {job.description[:2500]}"
                    ),
                    temperature=0.6,
                )
            except Exception:
                pass

        return self._fallback_template(profile, job, tone)

    def _fallback_template(self, profile: CareerProfile, job: Job, tone: str) -> str:
        top_skills = ", ".join(profile.skills[:4]) or "relevant technical and leadership skills"
        return (
            f"Dear Hiring Team at {job.company},\n\n"
            f"I'm applying for the {job.title} role. {profile.experience_summary or 'My background'} "
            f"lines up closely with what you're looking for, particularly around {top_skills}.\n\n"
            f"In my recent work I've focused on the kind of problems this role tackles, and I'd bring "
            f"that same ownership to your team from day one.\n\n"
            f"I'd welcome the chance to talk about how I can contribute to {job.company}'s goals.\n\n"
            f"Best regards,\n(Set your name in Career Profile)"
        )


cover_letter_agent = CoverLetterAgent()
