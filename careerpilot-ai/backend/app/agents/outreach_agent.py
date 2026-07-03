from urllib.parse import quote

from app.models import CareerProfile, Job
from app.services.ai_service import ai_service


class OutreachAgent:
    """Drafts personalised recruiter/hiring-manager outreach. Messages are
    always created with approved=False -- a human reviews and explicitly
    approves before anything would be sent, since LinkedIn/email automation
    without consent is both a ToS risk and bad etiquette.
    """

    def draft_message(
        self,
        profile: CareerProfile,
        job: Job,
        channel: str,
        recipient_role: str,
        recipient_name: str = "",
    ) -> dict:
        if ai_service.is_available:
            try:
                body = ai_service.complete(
                    system_prompt=(
                        f"Write a short (under 120 words), warm but professional {channel} "
                        f"outreach message from a job candidate to a {recipient_role}. No "
                        "generic flattery, reference something concrete about the role/company. "
                        "Include a clear, low-pressure ask."
                    ),
                    user_prompt=(
                        f"Candidate summary: {profile.experience_summary}\n"
                        f"Candidate skills: {', '.join(profile.skills)}\n"
                        f"Recipient name (if known): {recipient_name or 'unknown, keep generic greeting'}\n"
                        f"Company: {job.company}\nRole: {job.title}\n"
                        f"Job description snippet: {job.description[:800]}"
                    ),
                    temperature=0.6,
                )
                subject = f"Interest in the {job.title} role at {job.company}"
                return {"subject": subject, "message": body}
            except Exception:
                pass

        return self._fallback(profile, job, recipient_role, recipient_name)

    def _fallback(self, profile: CareerProfile, job: Job, recipient_role: str, recipient_name: str) -> dict:
        greeting = f"Hi {recipient_name}," if recipient_name else "Hi,"
        top_skill = (profile.skills[0] if profile.skills else "relevant experience")
        message = (
            f"{greeting}\n\n"
            f"I just applied for the {job.title} role at {job.company} and wanted to reach out "
            f"directly. My background in {top_skill} lines up well with what the team is building, "
            f"and I'd love to learn more about what success looks like in this role.\n\n"
            f"Would you be open to a brief chat?\n\nThanks,\n(Set your name in Career Profile)"
        )
        return {"subject": f"Interest in the {job.title} role at {job.company}", "message": message}

    def find_referrals(self, profile: CareerProfile, job: Job) -> list[dict]:
        """Suggests referral search strategies. We don't have LinkedIn's
        social graph API, so instead of fabricating names we generate
        pre-built, correct search queries the user can run themselves.
        """
        company_q = quote(job.company)
        suggestions = [
            {
                "connection_type": "employee",
                "suggestion": f"Search LinkedIn for current employees at {job.company} in similar roles to yours.",
                "search_url": f"https://www.linkedin.com/search/results/people/?keywords={company_q}",
            },
            {
                "connection_type": "alumni",
                "suggestion": f"Filter {job.company} employees by your university via LinkedIn's alumni tool.",
                "search_url": f"https://www.linkedin.com/school/",
            },
            {
                "connection_type": "former_colleague",
                "suggestion": f"Check your existing LinkedIn connections for anyone who has worked at {job.company}.",
                "search_url": f"https://www.linkedin.com/search/results/people/?keywords={company_q}&network=%5B%22F%22%5D",
            },
        ]
        return suggestions


outreach_agent = OutreachAgent()
