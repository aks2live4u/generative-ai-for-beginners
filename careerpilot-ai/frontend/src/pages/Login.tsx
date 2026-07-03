import { useState } from "react";
import { ApiError, register } from "../api/client";
import { useAuth } from "../hooks/useAuth";

export default function Login() {
  const { login } = useAuth();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("demo@careerpilot.ai");
  const [password, setPassword] = useState("demo1234");
  const [fullName, setFullName] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      if (mode === "register") {
        await register(email, password, fullName);
      }
      await login(email, password);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="center-page">
      <div className="card" style={{ width: "100%", maxWidth: 380 }}>
        <h1 style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 20 }}>
          <span className="brand-dot" /> CareerPilot AI
        </h1>
        <p className="muted" style={{ marginBottom: 18 }}>
          Your autonomous AI career agent — finds jobs, scores fit, tailors resumes, and preps you
          for interviews.
        </p>
        <form onSubmit={submit}>
          {mode === "register" && (
            <div className="field">
              <label>Full name</label>
              <input className="input" value={fullName} onChange={(e) => setFullName(e.target.value)} />
            </div>
          )}
          <div className="field">
            <label>Email</label>
            <input
              className="input"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label>Password</label>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <button className="btn block" type="submit" disabled={busy}>
            {busy ? <span className="spinner" /> : mode === "login" ? "Log in" : "Create account"}
          </button>
        </form>
        <p className="muted" style={{ marginTop: 14, textAlign: "center" }}>
          {mode === "login" ? "New here?" : "Already have an account?"}{" "}
          <a href="#" onClick={(e) => { e.preventDefault(); setMode(mode === "login" ? "register" : "login"); }}>
            {mode === "login" ? "Create an account" : "Log in"}
          </a>
        </p>
        <p className="muted" style={{ marginTop: 10, fontSize: 12 }}>
          Demo credentials are pre-filled: demo@careerpilot.ai / demo1234 (run backend/seed.py first).
        </p>
      </div>
    </div>
  );
}
