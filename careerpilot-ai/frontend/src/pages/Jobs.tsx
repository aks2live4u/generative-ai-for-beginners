import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import type { Job, MatchScore } from "../api/types";
import { Loading, ErrorBanner } from "../components/Loading";
import { ScoreBadge } from "../components/ScoreBadge";

export default function Jobs() {
  const [jobs, setJobs] = useState<Job[] | null>(null);
  const [scores, setScores] = useState<Record<number, MatchScore>>({});
  const [keyword, setKeyword] = useState("");
  const [remoteType, setRemoteType] = useState("");
  const [error, setError] = useState("");
  const [refreshing, setRefreshing] = useState(false);
  const [scoring, setScoring] = useState(false);

  async function load() {
    const params = new URLSearchParams();
    if (keyword) params.set("keyword", keyword);
    if (remoteType) params.set("remote_type", remoteType);
    try {
      const data = await api.get<Job[]>(`/api/jobs?${params.toString()}`);
      setJobs(data);
      const bestMatches = await api.get<{ job: Job; score: MatchScore }[]>(
        "/api/match/best?min_score=0&limit=200"
      );
      const map: Record<number, MatchScore> = {};
      bestMatches.forEach(({ job, score }) => {
        if (score) map[job.id] = score;
      });
      setScores(map);
    } catch (e) {
      setError(String(e));
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleRefresh() {
    setRefreshing(true);
    try {
      await api.post("/api/jobs/refresh");
      await load();
    } catch (e) {
      setError(String(e));
    } finally {
      setRefreshing(false);
    }
  }

  async function handleScoreAll() {
    setScoring(true);
    try {
      await api.post("/api/match/refresh-all");
      await load();
    } catch (e) {
      setError(String(e));
    } finally {
      setScoring(false);
    }
  }

  return (
    <div>
      <div className="card">
        <div className="field" style={{ marginBottom: 10 }}>
          <input
            className="input"
            placeholder="Search title, company, keyword…"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && load()}
          />
        </div>
        <div className="card-row" style={{ gap: 8 }}>
          <select className="input" value={remoteType} onChange={(e) => { setRemoteType(e.target.value); }}>
            <option value="">Any location type</option>
            <option value="remote">Remote</option>
            <option value="hybrid">Hybrid</option>
            <option value="office">Office</option>
          </select>
          <button className="btn secondary sm" onClick={load}>
            Filter
          </button>
        </div>
        <div className="card-row" style={{ marginTop: 12, gap: 8 }}>
          <button className="btn secondary sm" onClick={handleRefresh} disabled={refreshing}>
            {refreshing ? <span className="spinner" /> : "🔄 Refresh jobs"}
          </button>
          <button className="btn sm" onClick={handleScoreAll} disabled={scoring}>
            {scoring ? <span className="spinner" /> : "⚡ Score all jobs"}
          </button>
        </div>
      </div>

      {error && <ErrorBanner message={error} />}
      {!jobs && !error && <Loading label="Loading jobs…" />}
      {jobs && jobs.length === 0 && (
        <div className="empty-state">
          No jobs yet. Tap <strong>Refresh jobs</strong> to pull from RemoteOK, Arbeitnow, Greenhouse,
          Lever, and your sample listings.
        </div>
      )}

      {jobs &&
        jobs
          .slice()
          .sort((a, b) => (scores[b.id]?.overall_score ?? -1) - (scores[a.id]?.overall_score ?? -1))
          .map((job) => (
            <Link key={job.id} to={`/jobs/${job.id}`} className="card" style={{ display: "block" }}>
              <div className="card-row">
                <div>
                  <h3>{job.title}</h3>
                  <p style={{ margin: 0 }}>
                    {job.company} · {job.location}
                  </p>
                </div>
                {scores[job.id] ? (
                  <ScoreBadge score={scores[job.id].overall_score} />
                ) : (
                  <span className="badge">Not scored</span>
                )}
              </div>
              <div className="tag-row">
                {job.tags.slice(0, 4).map((t) => (
                  <span key={t} className="badge">
                    {t}
                  </span>
                ))}
              </div>
              {(job.is_flagged_suspicious || job.is_likely_ghost) && (
                <p className="muted" style={{ color: "var(--warning)" }}>
                  ⚠ Flagged: {job.is_flagged_suspicious ? "possible scam" : "possible ghost posting"}
                </p>
              )}
            </Link>
          ))}
    </div>
  );
}
