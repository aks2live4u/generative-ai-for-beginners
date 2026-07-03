import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { LearningRecommendation } from "../api/types";
import { Loading, ErrorBanner } from "../components/Loading";

export default function Learning() {
  const [recs, setRecs] = useState<LearningRecommendation[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<LearningRecommendation[]>("/api/learning/recommendations").then(setRecs).catch((e) => setError(String(e)));
  }, []);

  if (error) return <ErrorBanner message={error} />;
  if (!recs) return <Loading />;
  if (recs.length === 0) {
    return (
      <div className="empty-state">
        No recurring skill gaps yet. Score more jobs to build up data here.
      </div>
    );
  }

  return (
    <div>
      {recs.map((r) => (
        <div key={r.skill} className="card">
          <div className="card-row">
            <h3>{r.skill}</h3>
            <span className="badge">Seen in {r.demand_count} jobs</span>
          </div>
          <p className="muted">Courses</p>
          <ul>
            {r.courses.map((c, i) => <li key={i}>{c}</li>)}
          </ul>
          <p className="muted">Practice projects</p>
          <ul>
            {r.projects.map((p, i) => <li key={i}>{p}</li>)}
          </ul>
        </div>
      ))}
    </div>
  );
}
