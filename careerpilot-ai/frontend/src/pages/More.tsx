import { Link } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

export default function More() {
  const { logout } = useAuth();

  const links = [
    { to: "/outreach", icon: "📨", label: "Outreach drafts", desc: "Recruiter & hiring manager messages awaiting your review" },
    { to: "/learning", icon: "📚", label: "Learning recommendations", desc: "Close the skill gaps that keep showing up in job descriptions" },
    { to: "/salary", icon: "💰", label: "Salary intelligence", desc: "Market rate lookup for any title & location" },
  ];

  return (
    <div>
      {links.map((l) => (
        <Link key={l.to} to={l.to} className="card" style={{ display: "block" }}>
          <div className="card-row">
            <div>
              <h3>{l.icon} {l.label}</h3>
              <p style={{ margin: 0 }}>{l.desc}</p>
            </div>
            <span className="muted">→</span>
          </div>
        </Link>
      ))}

      <div className="card">
        <h3>About auto-apply & automation</h3>
        <p>
          CareerPilot AI tailors resumes, cover letters, and outreach for you, but it never submits
          applications or sends messages automatically. LinkedIn, Indeed, and Glassdoor prohibit
          automated form-filling and scraping in their Terms of Service, so every send/submit action
          stays a manual, one-tap confirmation from you.
        </p>
      </div>

      <button className="btn secondary block" onClick={logout}>
        Log out
      </button>
    </div>
  );
}
