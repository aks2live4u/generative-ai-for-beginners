import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { OutreachMessage } from "../api/types";
import { Loading, ErrorBanner } from "../components/Loading";

export default function Outreach() {
  const [messages, setMessages] = useState<OutreachMessage[] | null>(null);
  const [error, setError] = useState("");

  function load() {
    api.get<OutreachMessage[]>("/api/outreach").then(setMessages).catch((e) => setError(String(e)));
  }

  useEffect(load, []);

  async function toggleApprove(id: number, approved: boolean) {
    try {
      await api.patch(`/api/outreach/${id}/approve`, { approved: !approved });
      load();
    } catch (e) {
      setError(String(e));
    }
  }

  if (error) return <ErrorBanner message={error} />;
  if (!messages) return <Loading />;
  if (messages.length === 0) {
    return <div className="empty-state">No outreach drafts yet. Generate one from a job's Outreach tab.</div>;
  }

  return (
    <div>
      {messages.map((m) => (
        <div key={m.id} className="card">
          <div className="card-row">
            <strong>{m.subject}</strong>
            <span className={`badge ${m.approved ? "score-high" : ""}`}>
              {m.approved ? "Approved" : "Pending review"}
            </span>
          </div>
          <div className="markdown-preview">{m.message}</div>
          <button className="btn secondary sm" style={{ marginTop: 8 }} onClick={() => toggleApprove(m.id, m.approved)}>
            {m.approved ? "Mark as not sent" : "Mark reviewed & approved"}
          </button>
        </div>
      ))}
    </div>
  );
}
