import { useState } from "react";
import { api } from "../api/client";
import type { SalaryInsight } from "../api/types";
import { ErrorBanner } from "../components/Loading";

export default function SalaryTool() {
  const [title, setTitle] = useState("Cloud Operations Manager");
  const [country, setCountry] = useState("United Kingdom");
  const [city, setCity] = useState("");
  const [result, setResult] = useState<SalaryInsight | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function lookup(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const insight = await api.post<SalaryInsight>("/api/salary/insight", { title, country, city });
      setResult(insight);
    } catch (err) {
      setError(String(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <form className="card" onSubmit={lookup}>
        <h2>Salary intelligence</h2>
        <div className="field">
          <label>Job title</label>
          <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Country</label>
            <input className="input" value={country} onChange={(e) => setCountry(e.target.value)} />
          </div>
          <div className="field">
            <label>City (optional)</label>
            <input className="input" value={city} onChange={(e) => setCity(e.target.value)} />
          </div>
        </div>
        <button className="btn block" type="submit" disabled={busy}>
          {busy ? <span className="spinner" /> : "Look up market rate"}
        </button>
      </form>

      {error && <ErrorBanner message={error} />}

      {result && (
        <div className="card">
          <h2>{result.title} — {result.location}</h2>
          <div className="grid-3">
            <div className="stat-tile">
              <div className="value">{result.estimated_min.toLocaleString()}</div>
              <div className="label">Min ({result.currency})</div>
            </div>
            <div className="stat-tile">
              <div className="value">{result.estimated_median.toLocaleString()}</div>
              <div className="label">Median</div>
            </div>
            <div className="stat-tile">
              <div className="value">{result.estimated_max.toLocaleString()}</div>
              <div className="label">Max</div>
            </div>
          </div>
          <p style={{ marginTop: 12 }}>Market demand: <strong>{result.market_demand}</strong></p>
          <p>{result.commentary}</p>
        </div>
      )}
    </div>
  );
}
