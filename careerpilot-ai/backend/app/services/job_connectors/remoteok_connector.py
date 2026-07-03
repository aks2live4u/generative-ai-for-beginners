from datetime import datetime

import httpx

from app.config import get_settings

from .base import JobConnector, NormalizedJob

settings = get_settings()


class RemoteOkConnector(JobConnector):
    """https://remoteok.com/api - public, free, no API key required."""

    name = "remoteok"

    def fetch(self) -> list[NormalizedJob]:
        headers = {"User-Agent": "CareerPilotAI/1.0 (+personal job search agent)"}
        try:
            response = httpx.get(settings.remoteok_api_url, headers=headers, timeout=15)
            response.raise_for_status()
            raw = response.json()
        except (httpx.HTTPError, ValueError):
            return []

        jobs: list[NormalizedJob] = []
        for item in raw:
            # RemoteOK's first array element is a "legal" notice, not a job.
            if not isinstance(item, dict) or "id" not in item or "position" not in item:
                continue

            posted_at = None
            if item.get("date"):
                try:
                    posted_at = datetime.fromisoformat(item["date"].replace("Z", "+00:00"))
                except ValueError:
                    posted_at = None

            jobs.append(
                NormalizedJob(
                    source=self.name,
                    external_id=str(item["id"]),
                    title=item.get("position", ""),
                    company=item.get("company", ""),
                    location=item.get("location", "Remote"),
                    country="",
                    remote_type="remote",
                    employment_type="full_time",
                    salary_min=_to_float(item.get("salary_min")),
                    salary_max=_to_float(item.get("salary_max")),
                    salary_currency="USD",
                    description=item.get("description", ""),
                    tags=item.get("tags", []) or [],
                    url=item.get("url") or item.get("apply_url", ""),
                    posted_at=posted_at,
                    supports_auto_apply=False,
                )
            )
        return jobs


def _to_float(value) -> float | None:
    try:
        return float(value) if value not in (None, "") else None
    except (TypeError, ValueError):
        return None
