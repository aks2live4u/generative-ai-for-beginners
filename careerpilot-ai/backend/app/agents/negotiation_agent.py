from app.models import CareerProfile, Job
from app.services.ai_service import ai_service
from app.services.salary_service import estimate_salary


class NegotiationAgent:
    """Evaluates an offer against market data and drafts negotiation talking points."""

    def evaluate(self, profile: CareerProfile, job: Job, offered_salary: float | None = None) -> dict:
        market = estimate_salary(job.title, job.country)
        offered = offered_salary or job.salary_max or 0

        if offered and market["estimated_median"]:
            competitiveness = round(min(150.0, offered / market["estimated_median"] * 100), 1)
        else:
            competitiveness = 0.0

        talking_points = self._talking_points(profile, job, market, offered, competitiveness)

        return {
            "market": market,
            "offered_salary": offered,
            "competitiveness_pct": competitiveness,
            "talking_points": talking_points,
        }

    def _talking_points(self, profile, job, market, offered, competitiveness) -> list[str]:
        if ai_service.is_available:
            try:
                text = ai_service.complete(
                    system_prompt=(
                        "You are a compensation negotiation coach. Give 4 short, concrete, "
                        "numbered negotiation talking points as plain sentences (no markdown), one per line."
                    ),
                    user_prompt=(
                        f"Role: {job.title} at {job.company}\n"
                        f"Offered salary: {offered} {market['currency']}\n"
                        f"Market median: {market['estimated_median']} {market['currency']}\n"
                        f"Candidate salary expectation: {profile.salary_expectation_min}-"
                        f"{profile.salary_expectation_max} {profile.salary_currency}\n"
                        f"Competitiveness vs market: {competitiveness}%"
                    ),
                    temperature=0.5,
                )
                return [line.strip("- ") for line in text.splitlines() if line.strip()]
            except Exception:
                pass

        points = []
        if competitiveness and competitiveness < 90:
            points.append(
                f"This offer is below the market median (~{market['estimated_median']} {market['currency']}) "
                "for this title in this location -- use that as your anchor to counter."
            )
        elif competitiveness:
            points.append("This offer is at or above market median -- focus negotiation on non-salary terms.")
        points += [
            "Ask for time to review before responding; never accept on the spot.",
            "Negotiate the full package: signing bonus, equity, remote flexibility, PTO.",
            "Get the final offer confirmed in writing before resigning from your current role.",
        ]
        return points


negotiation_agent = NegotiationAgent()
