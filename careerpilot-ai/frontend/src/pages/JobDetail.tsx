import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, downloadFile } from "../api/client";
import type {
  CoverLetter,
  Job,
  MatchScore,
  OutreachMessage,
  Referral,
  Resume,
} from "../api/types";
import { Loading, ErrorBanner } from "../components/Loading";
import { ScoreBadge } from "../components/ScoreBadge";

type Tab = "overview" | "resume" | "cover-letter" | "outreach" | "referrals";

export default function JobDetail() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const [job, setJob] = useState<Job | null>(null);
  const [score, setScore] = useState<MatchScore | null>(null);
  const [resumes, setResumes] = useState<Resume[]>([]);
  const [letters, setLetters] = useState<CoverLetter[]>([]);
  const [outreach, setOutreach] = useState<OutreachMessage[]>([]);
  const [referrals, setReferrals] = useState<Referral[]>([]);
  const [tab, setTab] = useState<Tab>("overview");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState<string>("");

  async function loadAll() {
    if (!jobId) return;
    try {
      const jobData = await api.get<Job>(`/api/jobs/${jobId}`);
      setJob(jobData);
      const [best, resumeList, letterList, outreachList, referralList] = await Promise.all([
        api.get<{ job: Job; score: MatchScore }[]>("/api/match/best?min_score=0&limit=200"),
        api.get<Resume[]>(`/api/resumes?job_id=${jobId}`),
        api.get<CoverLetter[]>(`/api/cover-letters?job_id=${jobId}`),
        api.get<OutreachMessage[]>("/api/outreach"),
        api.get<Referral[]>(`/api/referrals?job_id=${jobId}`),
      ]);
      const match = best.find((b) => b.job.id === Number(jobId));
      setScore(match?.score ?? null);
      setResumes(resumeList);
      setLetters(letterList);
      setOutreach(outreachList.filter((o) => o.job_id === Number(jobId)));
      setReferrals(referralList);
    } catch (e) {
      setError(String(e));
    }
  }

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobId]);

  async function runAction(key: string, fn: () => Promise<unknown>) {
    setBusy(key);
    setError("");
    try {
      await fn();
      await loadAll();
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy("");
    }
  }

  async function handleApply() {
    if (!job) return;
    const latestResume = resumes[0];
    const latestLetter = letters[0];
    await runAction("apply", async () => {
      const application = await api.post<{ id: number }>("/api/applications", {
        job_id: job.id,
        resume_id: latestResume?.id ?? null,
        cover_letter_id: latestLetter?.id ?? null,
      });
      await api.patch(`/api/applications/${application.id}/stage`, { stage: "applied" });
      navigate(`/applications/${application.id}`);
    });
  }

  if (error && !job) return <ErrorBanner message={error} />;
  if (!job) return <Loading label="Loading job…" />;

  return (
    <div>
      <button className="btn secondary sm" onClick={() => navigate(-1)} style={{ marginBottom: 12 }}>
        ← Back
      </button>

      <div className="card">
        <div className="card-row">
          <div>
            <h2 style={{ marginBottom: 2 }}>{job.title}</h2>
            <p style={{ margin: 0 }}>
              {job.company} · {job.location}
            </p>
          </div>
          {score && <ScoreBadge score={score.overall_score} />}
        </div>
        <div className="tag-row">
          {job.tags.map((t) => (
            <span key={t} className="badge">
              {t}
            </span>
          ))}
        </div>
        {job.salary_min || job.salary_max ? (
          <p>
            💰 {job.salary_min?.toLocaleString()} - {job.salary_max?.toLocaleString()} {job.salary_currency}
          </p>
        ) : null}

        <div className="card-row" style={{ marginTop: 10, gap: 8, flexWrap: "wrap" }}>
          {!score && (
            <button
              className="btn sm"
              disabled={busy === "score"}
              onClick={() => runAction("score", () => api.post(`/api/match/jobs/${job.id}`))}
            >
              {busy === "score" ? <span className="spinner" /> : "⚡ Score this job"}
            </button>
          )}
          <button className="btn sm" disabled={busy === "apply"} onClick={handleApply}>
            {busy === "apply" ? <span className="spinner" /> : "✅ Save & Apply"}
          </button>
          {job.url && (
            <a className="btn secondary sm" href={job.url} target="_blank" rel="noreferrer">
              View original posting ↗
            </a>
          )}
        </div>
      </div>

      {score && (
        <div className="card">
          <h2>Match breakdown</h2>
          <div className="grid-3" style={{ marginBottom: 10 }}>
            <div className="stat-tile">
              <div className="value">{Math.round(score.resume_match)}%</div>
              <div className="label">Resume match</div>
            </div>
            <div className="stat-tile">
              <div className="value">{Math.round(score.skill_match)}%</div>
              <div className="label">Skill match</div>
            </div>
            <div className="stat-tile">
              <div className="value">{Math.round(score.interview_probability)}%</div>
              <div className="label">Interview odds</div>
            </div>
          </div>
          <p>{score.rationale}</p>
          {score.missing_keywords.length > 0 && (
            <>
              <p className="muted">Missing:</p>
              <div className="tag-row">
                {score.missing_keywords.map((k) => (
                  <span key={k} className="badge score-low">
                    {k}
                  </span>
                ))}
              </div>
            </>
          )}
        </div>
      )}

      <div className="card">
        <h2>Job description</h2>
        <p style={{ whiteSpace: "pre-wrap" }}>{job.description}</p>
      </div>

      <div className="kanban" style={{ marginBottom: 6 }}>
        {(["overview", "resume", "cover-letter", "outreach", "referrals"] as Tab[]).map((t) => (
          <button
            key={t}
            className={`nav-item ${tab === t ? "active" : ""}`}
            style={{ minWidth: "auto", flex: "none" }}
            onClick={() => setTab(t)}
          >
            {t.replace("-", " ")}
          </button>
        ))}
      </div>

      {error && <ErrorBanner message={error} />}

      {tab === "resume" && (
        <div className="card">
          <div className="card-row">
            <h2>Tailored resumes</h2>
            <button
              className="btn sm"
              disabled={busy === "resume"}
              onClick={() => runAction("resume", () => api.post("/api/resumes/generate", { job_id: job.id }))}
            >
              {busy === "resume" ? <span className="spinner" /> : "✨ Generate new version"}
            </button>
          </div>
          {resumes.length === 0 && <p className="muted">No resume generated yet.</p>}
          {resumes.map((r) => (
            <div key={r.id} style={{ marginBottom: 14 }}>
              <div className="card-row">
                <strong>v{r.version}</strong>
                <div className="tag-row" style={{ margin: 0 }}>
                  <span className="badge">ATS {r.ats_score}</span>
                  <span className="badge">Keywords {r.keyword_match_pct}%</span>
                </div>
              </div>
              <div className="markdown-preview">{r.content_markdown}</div>
              <div className="card-row" style={{ marginTop: 6, gap: 8 }}>
                <button
                  className="btn secondary sm"
                  onClick={() => downloadFile(`/api/resumes/${r.id}/export/pdf`, `resume_v${r.version}.pdf`)}
                >
                  Export PDF
                </button>
                <button
                  className="btn secondary sm"
                  onClick={() => downloadFile(`/api/resumes/${r.id}/export/docx`, `resume_v${r.version}.docx`)}
                >
                  Export DOCX
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === "cover-letter" && (
        <div className="card">
          <div className="card-row">
            <h2>Cover letters</h2>
            <button
              className="btn sm"
              disabled={busy === "letter"}
              onClick={() => runAction("letter", () => api.post("/api/cover-letters/generate", { job_id: job.id }))}
            >
              {busy === "letter" ? <span className="spinner" /> : "✨ Generate"}
            </button>
          </div>
          {letters.length === 0 && <p className="muted">No cover letter generated yet.</p>}
          {letters.map((l) => (
            <div key={l.id} style={{ marginBottom: 14 }}>
              <div className="markdown-preview">{l.content}</div>
              <div className="card-row" style={{ marginTop: 6, gap: 8 }}>
                <button
                  className="btn secondary sm"
                  onClick={() => downloadFile(`/api/cover-letters/${l.id}/export/pdf`, `cover_letter.pdf`)}
                >
                  Export PDF
                </button>
                <button
                  className="btn secondary sm"
                  onClick={() => downloadFile(`/api/cover-letters/${l.id}/export/docx`, `cover_letter.docx`)}
                >
                  Export DOCX
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === "outreach" && (
        <div className="card">
          <div className="card-row">
            <h2>Recruiter outreach</h2>
            <button
              className="btn sm"
              disabled={busy === "outreach"}
              onClick={() =>
                runAction("outreach", () =>
                  api.post("/api/outreach/generate", {
                    job_id: job.id,
                    channel: "linkedin",
                    recipient_role: "Hiring Manager",
                  })
                )
              }
            >
              {busy === "outreach" ? <span className="spinner" /> : "✨ Draft message"}
            </button>
          </div>
          <p className="muted">
            CareerPilot never sends messages automatically — review, edit, and send yourself.
          </p>
          {outreach.length === 0 && <p className="muted">No drafts yet.</p>}
          {outreach.map((o) => (
            <div key={o.id} className="markdown-preview" style={{ marginBottom: 10 }}>
              <strong>{o.subject}</strong>
              {"\n\n"}
              {o.message}
            </div>
          ))}
        </div>
      )}

      {tab === "referrals" && (
        <div className="card">
          <div className="card-row">
            <h2>Referral finder</h2>
            <button
              className="btn sm"
              disabled={busy === "referrals"}
              onClick={() => runAction("referrals", () => api.post(`/api/referrals/jobs/${job.id}`))}
            >
              {busy === "referrals" ? <span className="spinner" /> : "🔎 Find referrals"}
            </button>
          </div>
          {referrals.length === 0 && <p className="muted">No suggestions yet.</p>}
          {referrals.map((r) => (
            <div key={r.id} style={{ marginBottom: 10 }}>
              <p style={{ marginBottom: 2 }}>{r.suggestion}</p>
              <a href={r.search_url} target="_blank" rel="noreferrer" className="muted">
                Open search ↗
              </a>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
