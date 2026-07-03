from app.models import CareerProfile, Job, MatchScore
from app.services.ai_service import ai_service, cosine_similarity
from app.services.scoring import keyword_match


class MatcherAgent:
    """Scores fit between a career profile and a job across several dimensions."""

    def score(self, profile: CareerProfile, job: Job) -> MatchScore:
        resume_text = profile.resume_text or " ".join(profile.skills + profile.technologies)
        matched, missing, keyword_pct = keyword_match(resume_text, job.description)

        resume_match = self._resume_match(profile, job)
        skill_match = self._skill_match(profile, job, matched)
        salary_fit = self._salary_fit(profile, job)
        growth_score = self._growth_score(job)
        competition_score = self._competition_score(job)

        overall = round(
            0.35 * resume_match
            + 0.25 * skill_match
            + 0.15 * salary_fit
            + 0.15 * growth_score
            + 0.10 * (100 - competition_score),
            1,
        )
        interview_probability = round(min(95.0, max(5.0, overall * 0.85 - competition_score * 0.1)), 1)

        rationale = self._rationale(profile, job, matched, missing, overall)

        return MatchScore(
            job_id=job.id,
            user_id=profile.user_id,
            overall_score=overall,
            resume_match=resume_match,
            skill_match=skill_match,
            salary_fit=salary_fit,
            growth_score=growth_score,
            competition_score=competition_score,
            interview_probability=interview_probability,
            matched_keywords=matched[:15],
            missing_keywords=missing[:10],
            rationale=rationale,
        )

    def _resume_match(self, profile: CareerProfile, job: Job) -> float:
        if profile.embedding and job.embedding:
            similarity = cosine_similarity(profile.embedding, job.embedding)
            return round(similarity * 100, 1)
        # No embeddings available yet: fall back to keyword overlap.
        matched, missing, pct = keyword_match(profile.resume_text, job.description)
        return pct

    def _skill_match(self, profile: CareerProfile, job: Job, matched_keywords: list[str]) -> float:
        profile_terms = {s.lower() for s in (profile.skills + profile.technologies + profile.certifications)}
        job_terms = {t.lower() for t in job.tags} | set(matched_keywords)
        if not job_terms:
            return 50.0
        overlap = profile_terms & job_terms
        return round(min(100.0, len(overlap) / max(1, len(job_terms)) * 100), 1)

    def _salary_fit(self, profile: CareerProfile, job: Job) -> float:
        if not job.salary_max or not profile.salary_expectation_min:
            return 60.0  # unknown, neutral-ish score
        if job.salary_max >= profile.salary_expectation_min:
            surplus_ratio = min(1.5, job.salary_max / max(1, profile.salary_expectation_min))
            return round(min(100.0, 60 + surplus_ratio * 25), 1)
        return round(max(10.0, (job.salary_max / profile.salary_expectation_min) * 60), 1)

    def _growth_score(self, job: Job) -> float:
        score = 55.0
        text = job.description.lower()
        for signal, points in [
            ("leadership", 10),
            ("manager", 8),
            ("architect", 8),
            ("senior", 6),
            ("strategy", 6),
            ("director", 10),
        ]:
            if signal in text or signal in job.title.lower():
                score += points
        return round(min(100.0, score), 1)

    def _competition_score(self, job: Job) -> float:
        # Heuristic: fully-remote, well-known-sounding roles draw more applicants.
        score = 40.0
        if job.remote_type == "remote":
            score += 20
        if job.salary_max and job.salary_max > 0:
            score += 10
        return round(min(100.0, score), 1)

    def _rationale(
        self, profile: CareerProfile, job: Job, matched: list[str], missing: list[str], overall: float
    ) -> str:
        if ai_service.is_available:
            try:
                return ai_service.complete(
                    system_prompt=(
                        "You are a recruiting analyst. In 2 short sentences, explain why this "
                        "candidate is or isn't a strong fit for this job. Be concrete and cite skills."
                    ),
                    user_prompt=(
                        f"Candidate skills: {', '.join(profile.skills)}\n"
                        f"Candidate experience summary: {profile.experience_summary}\n"
                        f"Job title: {job.title} at {job.company}\n"
                        f"Job description: {job.description[:1500]}\n"
                        f"Matched keywords: {', '.join(matched[:10])}\n"
                        f"Missing keywords: {', '.join(missing[:5])}\n"
                        f"Overall score: {overall}/100"
                    ),
                )
            except Exception:
                pass

        strengths = ", ".join(matched[:5]) or "general background overlap"
        gaps = ", ".join(missing[:3]) or "no major gaps detected"
        return (
            f"{overall:.0f}% match based on keyword and skill overlap. "
            f"Strengths: {strengths}. Missing: {gaps}."
        )


matcher_agent = MatcherAgent()
