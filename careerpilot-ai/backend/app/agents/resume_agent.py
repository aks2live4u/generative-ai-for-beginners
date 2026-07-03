from app.models import CareerProfile, Job
from app.services.ai_service import ai_service
from app.services.scoring import ats_score, flesch_reading_ease, keyword_match


class ResumeAgent:
    """Rewrites/tailors the candidate's resume for a specific job description."""

    def generate(self, profile: CareerProfile, job: Job) -> dict:
        content = self._generate_content(profile, job)
        _, missing, keyword_pct = keyword_match(content, job.description)
        return {
            "content_markdown": content,
            "ats_score": ats_score(content, job.description),
            "keyword_match_pct": keyword_pct,
            "readability_score": flesch_reading_ease(content),
            "missing_keywords": missing[:10],
        }

    def _generate_content(self, profile: CareerProfile, job: Job) -> str:
        if ai_service.is_available:
            try:
                return ai_service.complete(
                    system_prompt=(
                        "You are an expert resume writer specializing in ATS optimisation. "
                        "Rewrite the candidate's resume, tailored specifically to the target "
                        "job description. Output clean Markdown with a # Name heading, "
                        "## Summary, ## Skills, ## Experience, ## Certifications sections. "
                        "Naturally weave in JD keywords the candidate genuinely has. Never "
                        "fabricate experience, employers, or credentials that aren't provided."
                    ),
                    user_prompt=(
                        f"Candidate profile:\n"
                        f"Resume/background: {profile.resume_text}\n"
                        f"Skills: {', '.join(profile.skills)}\n"
                        f"Certifications: {', '.join(profile.certifications)}\n"
                        f"Experience summary: {profile.experience_summary}\n"
                        f"Years of experience: {profile.experience_years}\n\n"
                        f"Target job:\nTitle: {job.title}\nCompany: {job.company}\n"
                        f"Description: {job.description[:3000]}"
                    ),
                    temperature=0.5,
                )
            except Exception:
                pass

        return self._fallback_template(profile, job)

    def _fallback_template(self, profile: CareerProfile, job: Job) -> str:
        skills = ", ".join(profile.skills) or "Add your core skills here"
        certs = "\n".join(f"- {c}" for c in profile.certifications) or "- Add certifications here"
        return (
            f"# Candidate Resume\n\n"
            f"## Summary\n"
            f"{profile.experience_summary or 'Experienced professional targeting the ' + job.title + ' role at ' + job.company + '.'}\n\n"
            f"## Skills\n{skills}\n\n"
            f"## Experience\n"
            f"{profile.resume_text or 'Paste your resume text into your Career Profile to auto-populate this section.'}\n\n"
            f"## Certifications\n{certs}\n\n"
            f"## Links\n"
            f"- Portfolio: {profile.portfolio_url or 'n/a'}\n"
            f"- LinkedIn: {profile.linkedin_url or 'n/a'}\n"
            f"- GitHub: {profile.github_url or 'n/a'}\n"
        )


resume_agent = ResumeAgent()
