from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr

from app.models import ApplicationStage, RemotePreference


# ---- Auth ----
class UserCreate(BaseModel):
    email: EmailStr
    password: str
    full_name: str = ""


class UserLogin(BaseModel):
    email: EmailStr
    password: str


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    email: EmailStr
    full_name: str


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"


# ---- Career Profile ----
class CareerProfileIn(BaseModel):
    resume_text: str = ""
    skills: list[str] = []
    certifications: list[str] = []
    experience_years: float = 0
    experience_summary: str = ""
    portfolio_url: str = ""
    linkedin_url: str = ""
    github_url: str = ""
    publications: list[str] = []
    salary_expectation_min: float = 0
    salary_expectation_max: float = 0
    salary_currency: str = "USD"
    preferred_countries: list[str] = []
    preferred_companies: list[str] = []
    target_titles: list[str] = []
    industries: list[str] = []
    technologies: list[str] = []
    visa_status: str = ""
    notice_period_days: int = 30
    remote_preference: RemotePreference = RemotePreference.remote


class CareerProfileOut(CareerProfileIn):
    model_config = ConfigDict(from_attributes=True)
    id: int
    updated_at: datetime


# ---- Jobs ----
class JobOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    source: str
    title: str
    company: str
    location: str
    country: str
    remote_type: str
    employment_type: str
    salary_min: float | None
    salary_max: float | None
    salary_currency: str
    description: str
    tags: list[str]
    url: str
    posted_at: datetime | None
    supports_auto_apply: bool
    is_flagged_suspicious: bool
    is_likely_ghost: bool


class JobFilter(BaseModel):
    remote_type: str | None = None
    country: str | None = None
    keyword: str | None = None
    min_salary: float | None = None
    employment_type: str | None = None
    limit: int = 50
    offset: int = 0


# ---- Match Score ----
class MatchScoreOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    job_id: int
    overall_score: float
    resume_match: float
    skill_match: float
    salary_fit: float
    growth_score: float
    competition_score: float
    interview_probability: float
    matched_keywords: list[str]
    missing_keywords: list[str]
    rationale: str
    created_at: datetime


class JobWithScore(BaseModel):
    job: JobOut
    score: MatchScoreOut | None = None


# ---- Resume ----
class ResumeGenerateRequest(BaseModel):
    job_id: int
    template: str = "modern"


class ResumeOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    job_id: int | None
    version: int
    content_markdown: str
    ats_score: float
    keyword_match_pct: float
    readability_score: float
    template: str
    created_at: datetime


# ---- Cover Letter ----
class CoverLetterGenerateRequest(BaseModel):
    job_id: int
    tone: str = "professional"


class CoverLetterOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    job_id: int
    content: str
    tone: str
    created_at: datetime


# ---- Applications ----
class ApplicationCreate(BaseModel):
    job_id: int
    resume_id: int | None = None
    cover_letter_id: int | None = None
    notes: str = ""


class ApplicationStageUpdate(BaseModel):
    stage: ApplicationStage


class ApplicationOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    job_id: int
    resume_id: int | None
    cover_letter_id: int | None
    stage: ApplicationStage
    applied_at: datetime | None
    last_activity_at: datetime
    notes: str
    job: JobOut


# ---- Outreach ----
class OutreachGenerateRequest(BaseModel):
    job_id: int
    channel: str = "linkedin"
    recipient_role: str = "Hiring Manager"
    recipient_name: str = ""


class OutreachOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    job_id: int
    channel: str
    recipient_role: str
    recipient_name: str
    subject: str
    message: str
    approved: bool
    sent: bool
    created_at: datetime


class OutreachApprove(BaseModel):
    approved: bool


# ---- Referrals ----
class ReferralOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    job_id: int
    connection_type: str
    suggestion: str
    search_url: str
    created_at: datetime


# ---- Interview Prep ----
class InterviewPrepOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    application_id: int
    company_research: str
    likely_questions: list[str]
    star_answers: list[dict]
    technical_questions: list[str]
    behavioural_questions: list[str]
    salary_negotiation_tips: list[str]
    created_at: datetime


# ---- Follow-ups ----
class FollowUpOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    application_id: int
    stage: str
    due_at: datetime
    sent: bool
    draft_message: str


# ---- Salary Intelligence ----
class SalaryInsightRequest(BaseModel):
    title: str
    country: str = ""
    city: str = ""


class SalaryInsightOut(BaseModel):
    title: str
    location: str
    estimated_min: float
    estimated_median: float
    estimated_max: float
    currency: str
    market_demand: str
    commentary: str


# ---- Learning Recommendations ----
class LearningRecommendationOut(BaseModel):
    skill: str
    demand_count: int
    courses: list[str]
    projects: list[str]


# ---- Analytics ----
class AnalyticsOut(BaseModel):
    applications_sent: int
    interview_rate: float
    avg_resume_ats_score: float
    avg_response_time_days: float | None
    stage_counts: dict[str, int]
    top_missing_skills: list[str]
    weekly_applications: list[dict]
