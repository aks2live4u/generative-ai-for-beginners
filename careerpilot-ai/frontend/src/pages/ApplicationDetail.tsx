import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/client";
import type { Application, ApplicationStage, FollowUp, InterviewPrep, NegotiationResult } from "../api/types";
import { Loading, ErrorBanner } from "../components/Loading";

const STAGES: ApplicationStage[] = [
  "saved", "applied", "hr_review", "assessment", "interview", "offer", "accepted", "rejected",
];

export default function ApplicationDetail() {
  const { applicationId } = useParams();
  const navigate = useNavigate();
  const [app, setApp] = useState<Application | null>(null);
  const [followups, setFollowups] = useState<FollowUp[]>([]);
  const [preps, setPreps] = useState<InterviewPrep[]>([]);
  const [offer, setOffer] = useState("");
  const [negotiation, setNegotiation] = useState<NegotiationResult | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");

  async function load() {
    if (!applicationId) return;
    try {
      const [a, f, p] = await Promise.all([
        api.get<Application>(`/api/applications/${applicationId}`),
        api.get<FollowUp[]>(`/api/followups/applications/${applicationId}`),
        api.get<InterviewPrep[]>(`/api/interview-prep/applications/${applicationId}`),
      ]);
      setApp(a);
      setFollowups(f);
      setPreps(p);
    } catch (e) {
      setError(String(e));
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applicationId]);

  async function changeStage(stage: ApplicationStage) {
    if (!app) return;
    try {
      const updated = await api.patch<Application>(`/api/applications/${app.id}/stage`, { stage });
      setApp(updated);
      await load();
    } catch (e) {
      setError(String(e));
    }
  }

  async function generatePrep() {
    if (!app) return;
    setBusy("prep");
    try {
      await api.post(`/api/interview-prep/applications/${app.id}`);
      await load();
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy("");
    }
  }

  async function evaluateOffer() {
    if (!app) return;
    setBusy("negotiate");
    try {
      const result = await api.post<NegotiationResult>("/api/negotiation/evaluate", {
        job_id: app.job_id,
        offered_salary: offer ? Number(offer) : undefined,
      });
      setNegotiation(result);
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy("");
    }
  }

  if (error && !app) return <ErrorBanner message={error} />;
  if (!app) return <Loading label="Loading application…" />;

  const latestPrep = preps[0];

  return (
    <div>
      <button className="btn secondary sm" onClick={() => navigate(-1)} style={{ marginBottom: 12 }}>
        ← Back
      </button>

      <div className="card">
        <h2>{app.job.title}</h2>
        <p>{app.job.company} · {app.job.location}</p>
        <div className="field">
          <label>Stage</label>
          <select className="input" value={app.stage} onChange={(e) => changeStage(e.target.value as ApplicationStage)}>
            {STAGES.map((s) => (
              <option key={s} value={s}>
                {s.replace("_", " ")}
              </option>
            ))}
          </select>
        </div>
        {app.notes && <p className="muted">Notes: {app.notes}</p>}
      </div>

      {error && <ErrorBanner message={error} />}

      <div className="card">
        <h2>Follow-up reminders</h2>
        {followups.length === 0 && (
          <p className="muted">Follow-ups are scheduled automatically once you mark this Applied.</p>
        )}
        {followups.map((f) => (
          <div key={f.id} style={{ marginBottom: 10 }}>
            <div className="card-row">
              <strong>{f.stage} follow-up</strong>
              <span className="muted">{new Date(f.due_at).toLocaleDateString()}</span>
            </div>
            <p className="muted">{f.draft_message}</p>
          </div>
        ))}
      </div>

      <div className="card">
        <div className="card-row">
          <h2>Interview prep</h2>
          <button className="btn sm" disabled={busy === "prep"} onClick={generatePrep}>
            {busy === "prep" ? <span className="spinner" /> : "✨ Generate"}
          </button>
        </div>
        {!latestPrep && <p className="muted">No prep generated yet.</p>}
        {latestPrep && (
          <div>
            <p>{latestPrep.company_research}</p>
            <h3>Likely questions</h3>
            <ul>
              {latestPrep.likely_questions.map((q, i) => <li key={i}>{q}</li>)}
            </ul>
            <h3>Technical questions</h3>
            <ul>
              {latestPrep.technical_questions.map((q, i) => <li key={i}>{q}</li>)}
            </ul>
            <h3>Behavioural questions</h3>
            <ul>
              {latestPrep.behavioural_questions.map((q, i) => <li key={i}>{q}</li>)}
            </ul>
            <h3>STAR practice</h3>
            {latestPrep.star_answers.map((s, i) => (
              <div key={i} className="markdown-preview" style={{ marginBottom: 8 }}>
                <strong>{s.question}</strong>
                {"\n"}S: {s.situation}
                {"\n"}T: {s.task}
                {"\n"}A: {s.action}
                {"\n"}R: {s.result}
              </div>
            ))}
            <h3>Salary negotiation tips</h3>
            <ul>
              {latestPrep.salary_negotiation_tips.map((t, i) => <li key={i}>{t}</li>)}
            </ul>
          </div>
        )}
      </div>

      <div className="card">
        <h2>Offer negotiation</h2>
        <div className="field">
          <label>Offered salary (optional)</label>
          <input className="input" type="number" value={offer} onChange={(e) => setOffer(e.target.value)} />
        </div>
        <button className="btn sm" disabled={busy === "negotiate"} onClick={evaluateOffer}>
          {busy === "negotiate" ? <span className="spinner" /> : "Evaluate offer"}
        </button>
        {negotiation && (
          <div style={{ marginTop: 12 }}>
            <p>Competitiveness vs market: <strong>{negotiation.competitiveness_pct}%</strong></p>
            <ul>
              {negotiation.talking_points.map((t, i) => <li key={i}>{t}</li>)}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
