export function Loading({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="muted" style={{ display: "flex", alignItems: "center", gap: 8, padding: "16px 0" }}>
      <span className="spinner" /> {label}
    </div>
  );
}

export function ErrorBanner({ message }: { message: string }) {
  return <p className="error-text">{message}</p>;
}
