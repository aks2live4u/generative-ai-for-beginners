import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import type { Application, ApplicationStage } from "../api/types";
import { Loading, ErrorBanner } from "../components/Loading";

const STAGES: { key: ApplicationStage; label: string }[] = [
  { key: "saved", label: "Saved" },
  { key: "applied", label: "Applied" },
  { key: "hr_review", label: "HR Review" },
  { key: "assessment", label: "Assessment" },
  { key: "interview", label: "Interview" },
  { key: "offer", label: "Offer" },
  { key: "accepted", label: "Accepted" },
  { key: "rejected", label: "Rejected" },
];

export default function Applications() {
  const [apps, setApps] = useState<Application[] | null>(null);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    api
      .get<Application[]>("/api/applications")
      .then(setApps)
      .catch((e) => setError(String(e)));
  }, []);

  async function moveStage(id: number, stage: ApplicationStage) {
    try {
      const updated = await api.patch<Application>(`/api/applications/${id}/stage`, { stage });
      setApps((prev) => (prev ? prev.map((a) => (a.id === id ? updated : a)) : prev));
    } catch (e) {
      setError(String(e));
    }
  }

  if (error) return <ErrorBanner message={error} />;
  if (!apps) return <Loading label="Loading your pipeline…" />;

  if (apps.length === 0) {
    return (
      <div className="empty-state">
        No applications tracked yet. Open a job and tap <strong>Save & Apply</strong> to add one.
      </div>
    );
  }

  return (
    <div className="kanban">
      {STAGES.map((stage) => {
        const stageApps = apps.filter((a) => a.stage === stage.key);
        return (
          <div key={stage.key} className="kanban-column">
            <h3>
              {stage.label} <span className="muted">{stageApps.length}</span>
            </h3>
            {stageApps.map((app) => {
              const stageIndex = STAGES.findIndex((s) => s.key === app.stage);
              const next = STAGES[stageIndex + 1];
              return (
                <div key={app.id} className="kanban-card" onClick={() => navigate(`/applications/${app.id}`)}>
                  <strong>{app.job.title}</strong>
                  <p className="muted" style={{ margin: "2px 0" }}>
                    {app.job.company}
                  </p>
                  {next && (
                    <button
                      className="btn secondary sm"
                      style={{ marginTop: 6, width: "100%" }}
                      onClick={(e) => {
                        e.stopPropagation();
                        moveStage(app.id, next.key);
                      }}
                    >
                      Move to {next.label} →
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        );
      })}
    </div>
  );
}
