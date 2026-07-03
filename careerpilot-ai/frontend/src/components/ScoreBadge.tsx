export function ScoreBadge({ score, label }: { score: number; label?: string }) {
  const cls = score >= 75 ? "score-high" : score >= 50 ? "score-mid" : "score-low";
  return (
    <span className={`badge ${cls}`}>
      {label ? `${label}: ` : ""}
      {Math.round(score)}%
    </span>
  );
}
