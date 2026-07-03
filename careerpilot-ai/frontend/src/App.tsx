import { NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { useAuth } from "./hooks/useAuth";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Jobs from "./pages/Jobs";
import JobDetail from "./pages/JobDetail";
import Applications from "./pages/Applications";
import ApplicationDetail from "./pages/ApplicationDetail";
import Profile from "./pages/Profile";
import More from "./pages/More";
import Outreach from "./pages/Outreach";
import Learning from "./pages/Learning";
import SalaryTool from "./pages/SalaryTool";

function ProtectedLayout() {
  const location = useLocation();
  const tabs = [
    { to: "/", icon: "🏠", label: "Home", match: "/" },
    { to: "/jobs", icon: "🔍", label: "Jobs", match: "/jobs" },
    { to: "/applications", icon: "🗂️", label: "Tracker", match: "/applications" },
    { to: "/profile", icon: "👤", label: "Profile", match: "/profile" },
    { to: "/more", icon: "✨", label: "More", match: "/more" },
  ];

  return (
    <div className="app-shell">
      <header className="topbar">
        <h1>
          <span className="brand-dot" /> CareerPilot AI
        </h1>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/jobs" element={<Jobs />} />
          <Route path="/jobs/:jobId" element={<JobDetail />} />
          <Route path="/applications" element={<Applications />} />
          <Route path="/applications/:applicationId" element={<ApplicationDetail />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/more" element={<More />} />
          <Route path="/outreach" element={<Outreach />} />
          <Route path="/learning" element={<Learning />} />
          <Route path="/salary" element={<SalaryTool />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <nav className="bottom-nav">
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={() =>
              "nav-item " + (location.pathname === tab.match ? "active" : "")
            }
          >
            <span className="icon">{tab.icon}</span>
            {tab.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}

export default function App() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return (
      <Routes>
        <Route path="*" element={<Login />} />
      </Routes>
    );
  }

  return <ProtectedLayout />;
}
