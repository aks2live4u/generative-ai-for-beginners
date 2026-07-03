import enum
from datetime import datetime

from sqlalchemy import (
    JSON,
    Boolean,
    DateTime,
    Enum,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class RemotePreference(str, enum.Enum):
    remote = "remote"
    hybrid = "hybrid"
    office = "office"
    any = "any"


class ApplicationStage(str, enum.Enum):
    saved = "saved"
    applied = "applied"
    hr_review = "hr_review"
    assessment = "assessment"
    interview = "interview"
    offer = "offer"
    rejected = "rejected"
    accepted = "accepted"


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    hashed_password: Mapped[str] = mapped_column(String(255))
    full_name: Mapped[str] = mapped_column(String(255), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    profile: Mapped["CareerProfile"] = relationship(
        back_populates="user", uselist=False, cascade="all, delete-orphan"
    )
    applications: Mapped[list["Application"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )


class CareerProfile(Base):
    """The single source of truth the AI builds and continuously updates."""

    __tablename__ = "career_profiles"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), unique=True)

    resume_text: Mapped[str] = mapped_column(Text, default="")
    skills: Mapped[list[str]] = mapped_column(JSON, default=list)
    certifications: Mapped[list[str]] = mapped_column(JSON, default=list)
    experience_years: Mapped[float] = mapped_column(Float, default=0)
    experience_summary: Mapped[str] = mapped_column(Text, default="")
    portfolio_url: Mapped[str] = mapped_column(String(500), default="")
    linkedin_url: Mapped[str] = mapped_column(String(500), default="")
    github_url: Mapped[str] = mapped_column(String(500), default="")
    publications: Mapped[list[str]] = mapped_column(JSON, default=list)

    salary_expectation_min: Mapped[float] = mapped_column(Float, default=0)
    salary_expectation_max: Mapped[float] = mapped_column(Float, default=0)
    salary_currency: Mapped[str] = mapped_column(String(10), default="USD")

    preferred_countries: Mapped[list[str]] = mapped_column(JSON, default=list)
    preferred_companies: Mapped[list[str]] = mapped_column(JSON, default=list)
    target_titles: Mapped[list[str]] = mapped_column(JSON, default=list)
    industries: Mapped[list[str]] = mapped_column(JSON, default=list)
    technologies: Mapped[list[str]] = mapped_column(JSON, default=list)

    visa_status: Mapped[str] = mapped_column(String(255), default="")
    notice_period_days: Mapped[int] = mapped_column(Integer, default=30)
    remote_preference: Mapped[RemotePreference] = mapped_column(
        Enum(RemotePreference), default=RemotePreference.remote
    )

    embedding: Mapped[list[float] | None] = mapped_column(JSON, nullable=True)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.utcnow, onupdate=datetime.utcnow
    )

    user: Mapped["User"] = relationship(back_populates="profile")


class Job(Base):
    """Normalized job posting, regardless of source connector."""

    __tablename__ = "jobs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    source: Mapped[str] = mapped_column(String(50), index=True)  # remoteok, arbeitnow, greenhouse:stripe, ...
    external_id: Mapped[str] = mapped_column(String(255), index=True)
    title: Mapped[str] = mapped_column(String(500))
    company: Mapped[str] = mapped_column(String(255), index=True)
    location: Mapped[str] = mapped_column(String(255), default="")
    country: Mapped[str] = mapped_column(String(100), default="")
    remote_type: Mapped[str] = mapped_column(String(20), default="unknown")  # remote/hybrid/office/unknown
    employment_type: Mapped[str] = mapped_column(String(50), default="")  # full_time/contract/part_time
    salary_min: Mapped[float | None] = mapped_column(Float, nullable=True)
    salary_max: Mapped[float | None] = mapped_column(Float, nullable=True)
    salary_currency: Mapped[str] = mapped_column(String(10), default="")
    description: Mapped[str] = mapped_column(Text, default="")
    tags: Mapped[list[str]] = mapped_column(JSON, default=list)
    url: Mapped[str] = mapped_column(String(1000), default="")
    posted_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    fetched_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    supports_auto_apply: Mapped[bool] = mapped_column(Boolean, default=False)
    is_flagged_suspicious: Mapped[bool] = mapped_column(Boolean, default=False)
    is_likely_ghost: Mapped[bool] = mapped_column(Boolean, default=False)
    content_hash: Mapped[str] = mapped_column(String(64), index=True, default="")

    embedding: Mapped[list[float] | None] = mapped_column(JSON, nullable=True)

    match_scores: Mapped[list["MatchScore"]] = relationship(
        back_populates="job", cascade="all, delete-orphan"
    )


class MatchScore(Base):
    __tablename__ = "match_scores"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    job_id: Mapped[int] = mapped_column(ForeignKey("jobs.id"))
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))

    overall_score: Mapped[float] = mapped_column(Float, default=0)
    resume_match: Mapped[float] = mapped_column(Float, default=0)
    skill_match: Mapped[float] = mapped_column(Float, default=0)
    salary_fit: Mapped[float] = mapped_column(Float, default=0)
    growth_score: Mapped[float] = mapped_column(Float, default=0)
    competition_score: Mapped[float] = mapped_column(Float, default=0)
    interview_probability: Mapped[float] = mapped_column(Float, default=0)

    matched_keywords: Mapped[list[str]] = mapped_column(JSON, default=list)
    missing_keywords: Mapped[list[str]] = mapped_column(JSON, default=list)
    rationale: Mapped[str] = mapped_column(Text, default="")

    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    job: Mapped["Job"] = relationship(back_populates="match_scores")


class Resume(Base):
    __tablename__ = "resumes"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    job_id: Mapped[int | None] = mapped_column(ForeignKey("jobs.id"), nullable=True)
    version: Mapped[int] = mapped_column(Integer, default=1)
    content_markdown: Mapped[str] = mapped_column(Text)
    ats_score: Mapped[float] = mapped_column(Float, default=0)
    keyword_match_pct: Mapped[float] = mapped_column(Float, default=0)
    readability_score: Mapped[float] = mapped_column(Float, default=0)
    template: Mapped[str] = mapped_column(String(50), default="modern")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class CoverLetter(Base):
    __tablename__ = "cover_letters"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    job_id: Mapped[int] = mapped_column(ForeignKey("jobs.id"))
    content: Mapped[str] = mapped_column(Text)
    tone: Mapped[str] = mapped_column(String(50), default="professional")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class Application(Base):
    __tablename__ = "applications"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    job_id: Mapped[int] = mapped_column(ForeignKey("jobs.id"))
    resume_id: Mapped[int | None] = mapped_column(ForeignKey("resumes.id"), nullable=True)
    cover_letter_id: Mapped[int | None] = mapped_column(ForeignKey("cover_letters.id"), nullable=True)

    stage: Mapped[ApplicationStage] = mapped_column(
        Enum(ApplicationStage), default=ApplicationStage.saved
    )
    applied_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    last_activity_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    notes: Mapped[str] = mapped_column(Text, default="")

    user: Mapped["User"] = relationship(back_populates="applications")
    job: Mapped["Job"] = relationship()
    followups: Mapped[list["FollowUp"]] = relationship(
        back_populates="application", cascade="all, delete-orphan"
    )
    interview_preps: Mapped[list["InterviewPrep"]] = relationship(
        back_populates="application", cascade="all, delete-orphan"
    )


class FollowUp(Base):
    __tablename__ = "followups"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    application_id: Mapped[int] = mapped_column(ForeignKey("applications.id"))
    stage: Mapped[str] = mapped_column(String(50))  # first, second, close
    due_at: Mapped[datetime] = mapped_column(DateTime)
    sent: Mapped[bool] = mapped_column(Boolean, default=False)
    draft_message: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    application: Mapped["Application"] = relationship(back_populates="followups")


class InterviewPrep(Base):
    __tablename__ = "interview_preps"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    application_id: Mapped[int] = mapped_column(ForeignKey("applications.id"))
    company_research: Mapped[str] = mapped_column(Text, default="")
    likely_questions: Mapped[list[str]] = mapped_column(JSON, default=list)
    star_answers: Mapped[list[dict]] = mapped_column(JSON, default=list)
    technical_questions: Mapped[list[str]] = mapped_column(JSON, default=list)
    behavioural_questions: Mapped[list[str]] = mapped_column(JSON, default=list)
    salary_negotiation_tips: Mapped[list[str]] = mapped_column(JSON, default=list)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    application: Mapped["Application"] = relationship(back_populates="interview_preps")


class OutreachMessage(Base):
    __tablename__ = "outreach_messages"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    job_id: Mapped[int] = mapped_column(ForeignKey("jobs.id"))
    channel: Mapped[str] = mapped_column(String(20))  # linkedin, email
    recipient_role: Mapped[str] = mapped_column(String(100), default="")  # HR, Hiring Manager, ...
    recipient_name: Mapped[str] = mapped_column(String(255), default="")
    subject: Mapped[str] = mapped_column(String(500), default="")
    message: Mapped[str] = mapped_column(Text)
    approved: Mapped[bool] = mapped_column(Boolean, default=False)
    sent: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class Referral(Base):
    __tablename__ = "referrals"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    job_id: Mapped[int] = mapped_column(ForeignKey("jobs.id"))
    connection_type: Mapped[str] = mapped_column(String(50))  # alumni, former_colleague, mutual, employee
    suggestion: Mapped[str] = mapped_column(Text)
    search_url: Mapped[str] = mapped_column(String(1000), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
