from datetime import datetime, timezone

import httpx

from app.config import get_settings

from .base import JobConnector, NormalizedJob

settings = get_settings()


class ArbeitnowConnector(JobConnector):
    """https://www.arbeitnow.com/api/job-board-api - public, free, no API key required."""

    name = "arbeitnow"

    def fetch(self) -> list[NormalizedJob]:
        try:
            response = httpx.get(settings.arbeitnow_api_url, timeout=15)
            response.raise_for_status()
            payload = response.json()
        except (httpx.HTTPError, ValueError):
            return []

        jobs: list[NormalizedJob] = []
        for item in payload.get("data", []):
            posted_at = None
            if item.get("created_at"):
                try:
                    posted_at = datetime.fromtimestamp(int(item["created_at"]), tz=timezone.utc)
                except (TypeError, ValueError, OSError):
                    posted_at = None

            job_types = item.get("job_types", []) or []
            employment_type = job_types[0] if job_types else ""

            jobs.append(
                NormalizedJob(
                    source=self.name,
                    external_id=item.get("slug", ""),
                    title=item.get("title", ""),
                    company=item.get("company_name", ""),
                    location=item.get("location", ""),
                    country="",
                    remote_type="remote" if item.get("remote") else "office",
                    employment_type=employment_type,
                    description=item.get("description", ""),
                    tags=item.get("tags", []) or [],
                    url=item.get("url", ""),
                    posted_at=posted_at,
                    supports_auto_apply=False,
                )
            )
        return jobs
