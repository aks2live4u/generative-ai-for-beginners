import re
from datetime import datetime

import httpx

from app.config import get_settings

from .base import JobConnector, NormalizedJob

settings = get_settings()


def _strip_html(html: str) -> str:
    text = re.sub(r"<[^>]+>", " ", html or "")
    return re.sub(r"\s+", " ", text).strip()


class GreenhouseConnector(JobConnector):
    """https://boards-api.greenhouse.io/v1/boards/{company}/jobs - public per-company job board API.

    Configure companies via the GREENHOUSE_COMPANIES env var (comma separated
    board slugs, e.g. "stripe,gitlab"). Each company using Greenhouse exposes
    this endpoint publicly with no authentication.
    """

    name = "greenhouse"

    def fetch(self) -> list[NormalizedJob]:
        jobs: list[NormalizedJob] = []
        for company in settings.greenhouse_companies:
            jobs.extend(self._fetch_company(company))
        return jobs

    def _fetch_company(self, company: str) -> list[NormalizedJob]:
        url = settings.greenhouse_api_url.format(company=company) + "?content=true"
        try:
            response = httpx.get(url, timeout=15)
            response.raise_for_status()
            payload = response.json()
        except (httpx.HTTPError, ValueError):
            return []

        jobs: list[NormalizedJob] = []
        for item in payload.get("jobs", []):
            posted_at = None
            if item.get("updated_at"):
                try:
                    posted_at = datetime.fromisoformat(item["updated_at"].replace("Z", "+00:00"))
                except ValueError:
                    posted_at = None

            location_name = (item.get("location") or {}).get("name", "")

            jobs.append(
                NormalizedJob(
                    source=f"greenhouse:{company}",
                    external_id=str(item.get("id", "")),
                    title=item.get("title", ""),
                    company=company,
                    location=location_name,
                    country="",
                    remote_type="remote" if "remote" in location_name.lower() else "unknown",
                    employment_type="",
                    description=_strip_html(item.get("content", "")),
                    tags=[d.get("name", "") for d in item.get("departments", [])],
                    url=item.get("absolute_url", ""),
                    posted_at=posted_at,
                    supports_auto_apply=False,
                )
            )
        return jobs
