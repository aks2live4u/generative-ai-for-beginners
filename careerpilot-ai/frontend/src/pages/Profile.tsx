import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { CareerProfile } from "../api/types";
import { Loading, ErrorBanner } from "../components/Loading";

const EMPTY: CareerProfile = {
  resume_text: "",
  skills: [],
  certifications: [],
  experience_years: 0,
  experience_summary: "",
  portfolio_url: "",
  linkedin_url: "",
  github_url: "",
  publications: [],
  salary_expectation_min: 0,
  salary_expectation_max: 0,
  salary_currency: "USD",
  preferred_countries: [],
  preferred_companies: [],
  target_titles: [],
  industries: [],
  technologies: [],
  visa_status: "",
  notice_period_days: 30,
  remote_preference: "remote",
};

function listToText(list: string[]) {
  return list.join(", ");
}
function textToList(text: string) {
  return text.split(",").map((s) => s.trim()).filter(Boolean);
}

export default function Profile() {
  const [profile, setProfile] = useState<CareerProfile>(EMPTY);
  const [loaded, setLoaded] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .get<CareerProfile>("/api/profile")
      .then((p) => setProfile(p))
      .catch(() => {})
      .finally(() => setLoaded(true));
  }, []);

  function update<K extends keyof CareerProfile>(key: K, value: CareerProfile[K]) {
    setProfile((prev) => ({ ...prev, [key]: value }));
    setSaved(false);
  }

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      const updated = await api.put<CareerProfile>("/api/profile", profile);
      setProfile(updated);
      setSaved(true);
    } catch (err) {
      setError(String(err));
    } finally {
      setSaving(false);
    }
  }

  if (!loaded) return <Loading label="Loading your profile…" />;

  return (
    <form onSubmit={save}>
      <div className="card">
        <h2>Resume & background</h2>
        <div className="field">
          <label>Resume text / experience</label>
          <textarea
            className="input"
            rows={6}
            value={profile.resume_text}
            onChange={(e) => update("resume_text", e.target.value)}
          />
        </div>
        <div className="field">
          <label>Experience summary (used in cover letters & resumes)</label>
          <textarea
            className="input"
            rows={3}
            value={profile.experience_summary}
            onChange={(e) => update("experience_summary", e.target.value)}
          />
        </div>
        <div className="field">
          <label>Years of experience</label>
          <input
            className="input"
            type="number"
            value={profile.experience_years}
            onChange={(e) => update("experience_years", Number(e.target.value))}
          />
        </div>
      </div>

      <div className="card">
        <h2>Skills & credentials</h2>
        <div className="field">
          <label>Skills (comma separated)</label>
          <input
            className="input"
            value={listToText(profile.skills)}
            onChange={(e) => update("skills", textToList(e.target.value))}
          />
        </div>
        <div className="field">
          <label>Certifications (comma separated)</label>
          <input
            className="input"
            value={listToText(profile.certifications)}
            onChange={(e) => update("certifications", textToList(e.target.value))}
          />
        </div>
        <div className="field">
          <label>Technologies (comma separated)</label>
          <input
            className="input"
            value={listToText(profile.technologies)}
            onChange={(e) => update("technologies", textToList(e.target.value))}
          />
        </div>
      </div>

      <div className="card">
        <h2>Links</h2>
        <div className="field">
          <label>Portfolio URL</label>
          <input className="input" value={profile.portfolio_url} onChange={(e) => update("portfolio_url", e.target.value)} />
        </div>
        <div className="field">
          <label>LinkedIn URL</label>
          <input className="input" value={profile.linkedin_url} onChange={(e) => update("linkedin_url", e.target.value)} />
        </div>
        <div className="field">
          <label>GitHub URL</label>
          <input className="input" value={profile.github_url} onChange={(e) => update("github_url", e.target.value)} />
        </div>
      </div>

      <div className="card">
        <h2>Preferences</h2>
        <div className="grid-2">
          <div className="field">
            <label>Min salary expectation</label>
            <input
              className="input"
              type="number"
              value={profile.salary_expectation_min}
              onChange={(e) => update("salary_expectation_min", Number(e.target.value))}
            />
          </div>
          <div className="field">
            <label>Max salary expectation</label>
            <input
              className="input"
              type="number"
              value={profile.salary_expectation_max}
              onChange={(e) => update("salary_expectation_max", Number(e.target.value))}
            />
          </div>
        </div>
        <div className="field">
          <label>Currency</label>
          <input className="input" value={profile.salary_currency} onChange={(e) => update("salary_currency", e.target.value)} />
        </div>
        <div className="field">
          <label>Remote preference</label>
          <select
            className="input"
            value={profile.remote_preference}
            onChange={(e) => update("remote_preference", e.target.value as CareerProfile["remote_preference"])}
          >
            <option value="remote">Remote</option>
            <option value="hybrid">Hybrid</option>
            <option value="office">Office</option>
            <option value="any">Any</option>
          </select>
        </div>
        <div className="field">
          <label>Preferred countries (comma separated)</label>
          <input
            className="input"
            value={listToText(profile.preferred_countries)}
            onChange={(e) => update("preferred_countries", textToList(e.target.value))}
          />
        </div>
        <div className="field">
          <label>Target titles (comma separated)</label>
          <input
            className="input"
            value={listToText(profile.target_titles)}
            onChange={(e) => update("target_titles", textToList(e.target.value))}
          />
        </div>
        <div className="field">
          <label>Visa status</label>
          <input className="input" value={profile.visa_status} onChange={(e) => update("visa_status", e.target.value)} />
        </div>
        <div className="field">
          <label>Notice period (days)</label>
          <input
            className="input"
            type="number"
            value={profile.notice_period_days}
            onChange={(e) => update("notice_period_days", Number(e.target.value))}
          />
        </div>
      </div>

      {error && <ErrorBanner message={error} />}
      <button className="btn block" type="submit" disabled={saving}>
        {saving ? <span className="spinner" /> : saved ? "Saved ✓" : "Save profile"}
      </button>
    </form>
  );
}
