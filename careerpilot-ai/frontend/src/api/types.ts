export interface CareerProfile {
  resume_text: string;
  skills: string[];
  certifications: string[];
  experience_years: number;
  experience_summary: string;
  portfolio_url: string;
  linkedin_url: string;
  github_url: string;
  publications: string[];
  salary_expectation_min: number;
  salary_expectation_max: number;
  salary_currency: string;
  preferred_countries: string[];
  preferred_companies: string[];
  target_titles: string[];
  industries: string[];
  technologies: string[];
  visa_status: string;
  notice_period_days: number;
  remote_preference: "remote" | "hybrid" | "office" | "any";
}

export interface Job {
  id: number;
  source: string;
  title: string;
  company: string;
  location: string;
  country: string;
  remote_type: string;
  employment_type: string;
  salary_min: number | null;
  salary_max: number | null;
  salary_currency: string;
  description: string;
  tags: string[];
  url: string;
  posted_at: string | null;
  supports_auto_apply: boolean;
  is_flagged_suspicious: boolean;
  is_likely_ghost: boolean;
}

export interface MatchScore {
  id: number;
  job_id: number;
  overall_score: number;
  resume_match: number;
  skill_match: number;
  salary_fit: number;
  growth_score: number;
  competition_score: number;
  interview_probability: number;
  matched_keywords: string[];
  missing_keywords: string[];
  rationale: string;
  created_at: string;
}

export interface JobWithScore {
  job: Job;
  score: MatchScore | null;
}

export interface Resume {
  id: number;
  job_id: number | null;
  version: number;
  content_markdown: string;
  ats_score: number;
  keyword_match_pct: number;
  readability_score: number;
  template: string;
  created_at: string;
}

export interface CoverLetter {
  id: number;
  job_id: number;
  content: string;
  tone: string;
  created_at: string;
}

export type ApplicationStage =
  | "saved"
  | "applied"
  | "hr_review"
  | "assessment"
  | "interview"
  | "offer"
  | "rejected"
  | "accepted";

export interface Application {
  id: number;
  job_id: number;
  resume_id: number | null;
  cover_letter_id: number | null;
  stage: ApplicationStage;
  applied_at: string | null;
  last_activity_at: string;
  notes: string;
  job: Job;
}

export interface OutreachMessage {
  id: number;
  job_id: number;
  channel: string;
  recipient_role: string;
  recipient_name: string;
  subject: string;
  message: string;
  approved: boolean;
  sent: boolean;
  created_at: string;
}

export interface Referral {
  id: number;
  job_id: number;
  connection_type: string;
  suggestion: string;
  search_url: string;
  created_at: string;
}

export interface InterviewPrep {
  id: number;
  application_id: number;
  company_research: string;
  likely_questions: string[];
  star_answers: { question: string; situation: string; task: string; action: string; result: string }[];
  technical_questions: string[];
  behavioural_questions: string[];
  salary_negotiation_tips: string[];
  created_at: string;
}

export interface FollowUp {
  id: number;
  application_id: number;
  stage: string;
  due_at: string;
  sent: boolean;
  draft_message: string;
}

export interface SalaryInsight {
  title: string;
  location: string;
  estimated_min: number;
  estimated_median: number;
  estimated_max: number;
  currency: string;
  market_demand: string;
  commentary: string;
}

export interface NegotiationResult {
  market: SalaryInsight | Record<string, unknown>;
  offered_salary: number;
  competitiveness_pct: number;
  talking_points: string[];
}

export interface LearningRecommendation {
  skill: string;
  demand_count: number;
  courses: string[];
  projects: string[];
}

export interface Analytics {
  applications_sent: number;
  interview_rate: number;
  avg_resume_ats_score: number;
  avg_response_time_days: number | null;
  stage_counts: Record<string, number>;
  top_missing_skills: string[];
  weekly_applications: { week_starting: string; count: number }[];
}
