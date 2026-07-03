from dataclasses import dataclass, field
from datetime import datetime


@dataclass
class NormalizedJob:
    source: str
    external_id: str
    title: str
    company: str
    location: str = ""
    country: str = ""
    remote_type: str = "unknown"  # remote | hybrid | office | unknown
    employment_type: str = ""
    salary_min: float | None = None
    salary_max: float | None = None
    salary_currency: str = ""
    description: str = ""
    tags: list[str] = field(default_factory=list)
    url: str = ""
    posted_at: datetime | None = None
    supports_auto_apply: bool = False


class JobConnector:
    """Every connector implements fetch() and returns NormalizedJob objects.

    Only ToS-compliant, public/free job board APIs are wired up here
    (RemoteOK, Arbeitnow, Greenhouse, Lever job boards). LinkedIn, Indeed and
    Glassdoor do not offer a public jobs API and scraping them breaks their
    Terms of Service, so they are intentionally not implemented as automated
    connectors -- see MockConnector for a stand-in you can replace with a
    manual CSV/JSON import of jobs you found yourself on those sites.
    """

    name: str = "base"

    def fetch(self) -> list[NormalizedJob]:
        raise NotImplementedError
