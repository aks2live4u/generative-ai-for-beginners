from datetime import datetime, timedelta, timezone

from .base import JobConnector, NormalizedJob

# LinkedIn, Indeed, Glassdoor and Wellfound do not provide a public jobs API,
# and scraping them violates their Terms of Service, so they are not wired up
# as live connectors. This connector stands in for "jobs you found manually
# on those sites and want tracked" -- in a real deployment you'd replace this
# with a small CSV/JSON importer (File > Import Job) fed by the browser
# extension mentioned in the product spec. It also guarantees the app has
# data to demo with when outbound network access to job boards is restricted.
_SAMPLE_JOBS = [
    dict(
        title="Cloud Operations Manager",
        company="Meridian Cloud Systems",
        location="Remote - UK",
        country="United Kingdom",
        remote_type="remote",
        employment_type="full_time",
        salary_min=70000,
        salary_max=90000,
        salary_currency="GBP",
        description=(
            "Lead a 24/7 cloud operations team, own incident management and ITIL "
            "processes, drive service delivery for enterprise cloud infrastructure. "
            "Requires AWS, ITIL v4, leadership experience, Terraform, cloud cost "
            "optimisation, SRE practices."
        ),
        tags=["AWS", "ITIL", "Leadership", "Cloud Operations", "Terraform"],
    ),
    dict(
        title="Site Reliability Engineering Manager",
        company="Northgate Digital",
        location="Remote - Europe",
        country="Germany",
        remote_type="remote",
        employment_type="full_time",
        salary_min=75000,
        salary_max=95000,
        salary_currency="EUR",
        description=(
            "Manage an SRE team responsible for uptime, observability and incident "
            "response across a multi-region Kubernetes platform. Kubernetes, "
            "Terraform, Prometheus, on-call leadership, ITIL background a plus."
        ),
        tags=["Kubernetes", "SRE", "Terraform", "Observability"],
    ),
    dict(
        title="Cloud Service Delivery Manager",
        company="Bluepeak Technologies",
        location="London, UK (Hybrid)",
        country="United Kingdom",
        remote_type="hybrid",
        employment_type="full_time",
        salary_min=65000,
        salary_max=85000,
        salary_currency="GBP",
        description=(
            "Own service delivery for enterprise cloud clients, manage SLAs, "
            "stakeholder relationships and a distributed operations team. ITIL v4 "
            "certification required, cloud operations background, strong "
            "leadership and client-facing skills."
        ),
        tags=["ITIL", "Service Delivery", "Leadership", "Cloud"],
    ),
    dict(
        title="DevOps Manager",
        company="Skyline Fintech",
        location="Remote - India",
        country="India",
        remote_type="remote",
        employment_type="full_time",
        salary_min=2800000,
        salary_max=3800000,
        salary_currency="INR",
        description=(
            "Lead the DevOps function for a fintech scale-up, own CI/CD, cloud "
            "infrastructure and platform reliability. AWS, Terraform, Kubernetes, "
            "team leadership, ITIL exposure preferred."
        ),
        tags=["AWS", "Kubernetes", "DevOps", "Leadership"],
    ),
    dict(
        title="IT Operations Manager",
        company="Falcon Gulf Holdings",
        location="Dubai, UAE",
        country="United Arab Emirates",
        remote_type="office",
        employment_type="full_time",
        salary_min=180000,
        salary_max=240000,
        salary_currency="AED",
        description=(
            "Oversee IT operations, service desk and cloud infrastructure teams "
            "across the region. ITIL v4, incident/problem/change management, "
            "vendor management, strong leadership."
        ),
        tags=["ITIL", "IT Operations", "Leadership", "Cloud"],
    ),
    dict(
        title="Cloud Infrastructure Manager",
        company="Vantage Remote Works",
        location="Remote - Global",
        country="",
        remote_type="remote",
        employment_type="full_time",
        salary_min=80000,
        salary_max=100000,
        salary_currency="USD",
        description=(
            "Fully remote-first company seeking a Cloud Infrastructure Manager to "
            "run multi-cloud operations (AWS/Azure), lead a global on-call "
            "rotation, and drive automation with Terraform and Ansible."
        ),
        tags=["AWS", "Azure", "Terraform", "Ansible", "Leadership"],
    ),
]


class MockConnector(JobConnector):
    name = "manual_import"

    def fetch(self) -> list[NormalizedJob]:
        jobs = []
        for i, item in enumerate(_SAMPLE_JOBS):
            jobs.append(
                NormalizedJob(
                    source=self.name,
                    external_id=f"sample-{i}",
                    title=item["title"],
                    company=item["company"],
                    location=item["location"],
                    country=item["country"],
                    remote_type=item["remote_type"],
                    employment_type=item["employment_type"],
                    salary_min=item["salary_min"],
                    salary_max=item["salary_max"],
                    salary_currency=item["salary_currency"],
                    description=item["description"],
                    tags=item["tags"],
                    url="",
                    posted_at=datetime.now(timezone.utc) - timedelta(days=i),
                    supports_auto_apply=False,
                )
            )
        return jobs
