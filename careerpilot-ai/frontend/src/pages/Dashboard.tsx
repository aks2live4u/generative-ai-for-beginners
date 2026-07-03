import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import type { Analytics, JobWithScore } from "../api/types";
import { Loading, ErrorBanner } from "../components/Loading";
import { ScoreBadge } from "../components/ScoreBadge";

export default function Dashboard() {
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [digest, setDigest] = useState<JobWithScore[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<Analytics>("/api/analytics").then(setAnalytics).catch((e) => setError(String(e)));
    api
      .get<JobWithScore[]>("/api/alerts/daily-digest?min_score=50&since_hours=720")
      .then(setDigest)
      .catch(() => setDigest([]));
  }, []);

  if (error) return <ErrorBanner message={error} />;
  if (!analytics) return <Loading label="Loading your dashboard…" />;

  const maxWeekly = Math.max(1, ...analytics.weekly_applications.map((w) => w.count));

  return (
    <div>
      <div className="grid-3" style={{ marginBottom: 16 }}>
        <div className="stat-tile">
          <div className="value">{analytics.applications_sent}</div>
          <div className="label">Applications</div>
        </div>
        <div className="stat-tile">
          <div className="value">{analytics.interview_rate}%</div>
          <div className="label">Interview rate</div>
        </div>
        <div className="stat-tile">
          <div className="value">{analytics.avg_resume_ats_score}</div>
          <div className="label">Avg ATS score</div>
        </div>
      </div>

      <div className="card">
        <h2>Weekly application activity</h2>
        <div style={{ display: "flex", alignItems: "flex-end", gap: 6, height: 80 }}>
          {analytics.weekly_applications.map((w) => (
            <div
              key={w.week_starting}
              title={`${w.week_starting}: ${w.count}`}
              style={{
                flex: 1,
                background: "var(--accent)",
                opacity: w.count === 0 ? 0.2 : 1,
                height: `${Math.max(6, (w.count / maxWeekly) * 100)}%`,
                borderRadius: 4,
              }}
            />
          ))}
        </div>
      </div>

      <div className="card">
        <div className="card-row">
          <h2>Worth applying to today</h2>
          <Link to="/jobs" className="muted">
            See all →
          </Link>
        </div>
        {digest === null && <Loading />}
        {digest && digest.length === 0 && (
          <p className="muted">
            No standout matches yet. Go to Jobs → Refresh, then Score jobs to populate this.
          </p>
        )}
        {digest &&
          digest.slice(0, 5).map(({ job, score }) => (
            <Link
              key={job.id}
              to={`/jobs/${job.id}`}
              style={{ display: "block", padding: "10px 0", borderBottom: "1px solid var(--border)" }}
            >
              <div className="card-row">
                <div>
                  <strong>{job.title}</strong>
                  <p className="muted" style={{ margin: 0 }}>
                    {job.company} · {job.location}
                  </p>
                </div>
                {score && <ScoreBadge score={score.overall_score} />}
              </div>
            </Link>
          ))}
      </div>

      {analytics.top_missing_skills.length > 0 && (
        <div className="card">
          <h2>Recurring skill gaps</h2>
          <div className="tag-row">
            {analytics.top_missing_skills.map((s) => (
              <span key={s} className="badge">
                {s}
              </span>
            ))}
          </div>
          <Link to="/learning" className="btn secondary sm" style={{ marginTop: 8 }}>
            See learning recommendations
          </Link>
        </div>
      )}
    </div>
  );
}
