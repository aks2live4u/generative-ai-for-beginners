import hashlib
import re

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import Job
from app.services.ai_service import ai_service
from app.services.job_connectors import ALL_CONNECTORS, NormalizedJob

SCAM_KEYWORDS = [
    "wire transfer",
    "send money",
    "processing fee",
    "buy equipment yourself",
    "western union",
    "crypto payment",
    "pay upfront",
]

GHOST_SIGNALS = [
    "no experience necessary",
    "unlimited earning potential",
    "work from home, no interview",
]


def _content_hash(job: NormalizedJob) -> str:
    key = f"{job.title.strip().lower()}|{job.company.strip().lower()}|{job.location.strip().lower()}"
    return hashlib.sha256(key.encode()).hexdigest()


def _looks_suspicious(job: NormalizedJob) -> bool:
    text = f"{job.title} {job.description}".lower()
    return any(keyword in text for keyword in SCAM_KEYWORDS)


def _looks_like_ghost(job: NormalizedJob) -> bool:
    text = f"{job.title} {job.description}".lower()
    if any(signal in text for signal in GHOST_SIGNALS):
        return True
    # Very short descriptions with no concrete requirements are a weak ghost signal.
    word_count = len(re.findall(r"\w+", job.description or ""))
    return word_count < 15


def fetch_and_store_jobs(db: Session, sources: list[str] | None = None) -> dict[str, int]:
    """Runs the Scout Agent's data collection step: pulls from every connector,
    deduplicates against what's already stored, flags suspicious/ghost listings,
    and upserts the normalized result into the jobs table.
    """
    stats = {"fetched": 0, "new": 0, "duplicates": 0, "flagged": 0}
    seen_hashes: set[str] = set(db.scalars(select(Job.content_hash)).all())

    for connector in ALL_CONNECTORS:
        if sources and connector.name not in sources:
            continue
        try:
            normalized_jobs = connector.fetch()
        except Exception:
            continue

        for nj in normalized_jobs:
            stats["fetched"] += 1
            content_hash = _content_hash(nj)
            if content_hash in seen_hashes:
                stats["duplicates"] += 1
                continue
            seen_hashes.add(content_hash)

            suspicious = _looks_suspicious(nj)
            ghost = _looks_like_ghost(nj)
            if suspicious or ghost:
                stats["flagged"] += 1

            embedding = None
            try:
                embedding = ai_service.embed(f"{nj.title}\n{nj.description}")
            except Exception:
                embedding = None

            job = Job(
                source=nj.source,
                external_id=nj.external_id,
                title=nj.title,
                company=nj.company,
                location=nj.location,
                country=nj.country,
                remote_type=nj.remote_type,
                employment_type=nj.employment_type,
                salary_min=nj.salary_min,
                salary_max=nj.salary_max,
                salary_currency=nj.salary_currency,
                description=nj.description,
                tags=nj.tags,
                url=nj.url,
                posted_at=nj.posted_at,
                supports_auto_apply=nj.supports_auto_apply,
                is_flagged_suspicious=suspicious,
                is_likely_ghost=ghost,
                content_hash=content_hash,
                embedding=embedding,
            )
            db.add(job)
            stats["new"] += 1

    db.commit()
    return stats
