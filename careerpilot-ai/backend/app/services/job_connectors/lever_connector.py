from datetime import datetime, timezone

import httpx

from app.config import get_settings

from .base import JobConnector, NormalizedJob

settings = get_settings()


class LeverConnector(JobConnector):
    """https://api.lever.co/v0/postings/{company}?mode=json - public per-company job board API.

    Configure companies via the LEVER_COMPANIES env var.
    """

    name = "lever"

    def fetch(self) -> list[NormalizedJob]:
        jobs: list[NormalizedJob] = []
        for company in settings.lever_companies:
            jobs.extend(self._fetch_company(company))
        return jobs

    def _fetch_company(self, company: str) -> list[NormalizedJob]:
        url = settings.lever_api_url.format(company=company)
        try:
            response = httpx.get(url, timeout=15)
            response.raise_for_status()
            payload = response.json()
        except (httpx.HTTPError, ValueError):
            return []

        jobs: list[NormalizedJob] = []
        for item in payload:
            categories = item.get("categories", {}) or {}
            posted_at = None
            if item.get("createdAt"):
                try:
                    posted_at = datetime.fromtimestamp(int(item["createdAt"]) / 1000, tz=timezone.utc)
                except (TypeError, ValueError, OSError):
                    posted_at = None

            workplace_type = (item.get("workplaceType") or "").lower()
            remote_type = {"remote": "remote", "hybrid": "hybrid", "on-site": "office"}.get(
                workplace_type, "unknown"
            )

            jobs.append(
                NormalizedJob(
                    source=f"lever:{company}",
                    external_id=str(item.get("id", "")),
                    title=item.get("text", ""),
                    company=company,
                    location=categories.get("location", ""),
                    country="",
                    remote_type=remote_type,
                    employment_type=categories.get("commitment", ""),
                    description=item.get("descriptionPlain", "") or item.get("description", ""),
                    tags=[categories.get("team", "")] if categories.get("team") else [],
                    url=item.get("hostedUrl", ""),
                    posted_at=posted_at,
                    supports_auto_apply=False,
                )
            )
        return jobs
