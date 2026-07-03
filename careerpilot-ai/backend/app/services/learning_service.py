"""Static skill -> learning resource mapping used by CareerCoachAgent.

Curated once, not fetched live, since there is no single free API that
reliably indexes courses/certifications across providers.
"""

_RESOURCE_MAP = {
    "terraform": {
        "courses": [
            "HashiCorp Terraform Associate Certification (official)",
            "Terraform for Beginners to Advanced (Udemy)",
        ],
        "projects": ["Provision a 3-tier VPC on AWS with Terraform modules"],
    },
    "kubernetes": {
        "courses": ["Certified Kubernetes Administrator (CKA)", "Kubernetes for Developers (Linux Foundation)"],
        "projects": ["Deploy a self-healing microservice on a local kind/minikube cluster"],
    },
    "aws": {
        "courses": ["AWS Certified Solutions Architect - Associate", "AWS Certified SysOps Administrator"],
        "projects": ["Design a highly available multi-AZ architecture on AWS Free Tier"],
    },
    "azure": {
        "courses": ["Microsoft Certified: Azure Administrator Associate (AZ-104)"],
        "projects": ["Migrate a sample on-prem app to Azure App Service + Azure SQL"],
    },
    "itil": {
        "courses": ["ITIL 4 Foundation Certification (AXELOS/PeopleCert)"],
        "projects": ["Document an incident + problem management runbook for a sample service"],
    },
    "ansible": {
        "courses": ["Ansible for the Absolute Beginner (Udemy)"],
        "projects": ["Automate configuration of 3 VMs with an Ansible playbook"],
    },
    "python": {
        "courses": ["Python for Everybody Specialization (Coursera)"],
        "projects": ["Build a CLI tool that automates a repetitive ops task"],
    },
    "sre": {
        "courses": ["Site Reliability Engineering: Measuring and Managing Reliability (Coursera)"],
        "projects": ["Define SLOs/SLIs and an error budget policy for a sample service"],
    },
}

_DEFAULT = {
    "courses": ["Search LinkedIn Learning / Coursera for this exact keyword"],
    "projects": ["Build a small portfolio project demonstrating this skill"],
}


def recommend_for_skill(skill: str) -> dict:
    key = skill.strip().lower()
    resources = _RESOURCE_MAP.get(key, _DEFAULT)
    return {"skill": skill, "courses": resources["courses"], "projects": resources["projects"]}
