from app.models import CareerProfile, Job
from app.services.ai_service import ai_service


class InterviewAgent:
    """Builds interview prep once an application is shortlisted."""

    def prepare(self, profile: CareerProfile, job: Job) -> dict:
        if ai_service.is_available:
            try:
                data = ai_service.complete_json(
                    system_prompt=(
                        "You are an interview coach. Return a JSON object with keys: "
                        "company_research (string, 3-4 sentences), likely_questions (5 strings), "
                        "star_answers (3 objects each with question, situation, task, action, result "
                        "grounded in the candidate's real background), technical_questions (5 strings), "
                        "behavioural_questions (5 strings), salary_negotiation_tips (4 strings)."
                    ),
                    user_prompt=(
                        f"Candidate background: {profile.experience_summary}\n"
                        f"Skills: {', '.join(profile.skills)}\n"
                        f"Company: {job.company}\nRole: {job.title}\n"
                        f"Job description: {job.description[:2000]}"
                    ),
                )
                return {
                    "company_research": data.get("company_research", ""),
                    "likely_questions": data.get("likely_questions", []),
                    "star_answers": data.get("star_answers", []),
                    "technical_questions": data.get("technical_questions", []),
                    "behavioural_questions": data.get("behavioural_questions", []),
                    "salary_negotiation_tips": data.get("salary_negotiation_tips", []),
                }
            except Exception:
                pass

        return self._fallback(profile, job)

    def _fallback(self, profile: CareerProfile, job: Job) -> dict:
        top_skills = profile.skills[:3] or ["your core technical skill"]
        return {
            "company_research": (
                f"Research {job.company}'s recent product launches, engineering blog, and news before "
                f"the interview. Understand how the {job.title} role contributes to their roadmap."
            ),
            "likely_questions": [
                f"Walk me through your experience relevant to {job.title}.",
                "Tell me about a time you led a team through an incident or major change.",
                "How do you prioritize competing operational demands?",
                f"Why do you want to work at {job.company}?",
                "Where do you see this role in 2 years?",
            ],
            "star_answers": [
                {
                    "question": f"Describe a time you improved {skill}-related processes.",
                    "situation": "Fill in a specific situation from your experience.",
                    "task": "What was your responsibility?",
                    "action": "What specific actions did you take?",
                    "result": "What was the measurable outcome?",
                }
                for skill in top_skills
            ],
            "technical_questions": [
                "How would you design an on-call rotation for a global team?",
                "Explain how you'd troubleshoot a cascading service outage.",
                "How do you measure and improve infrastructure cost efficiency?",
            ],
            "behavioural_questions": [
                "Tell me about a conflict with a stakeholder and how you resolved it.",
                "Describe your leadership style.",
                "How do you handle underperforming team members?",
            ],
            "salary_negotiation_tips": [
                "Anchor to a specific number backed by market data, not a vague range.",
                "Let the employer state a number first if possible.",
                "Negotiate the full package (bonus, equity, remote flexibility), not just base salary.",
                "Get any verbal offer changes confirmed in writing.",
            ],
        }


interview_agent = InterviewAgent()
