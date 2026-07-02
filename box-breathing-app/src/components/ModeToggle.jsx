export default function ModeToggle({ mode, onChange }) {
  return (
    <div className="mode-toggle" role="tablist" aria-label="Session length mode">
      <button
        type="button"
        role="tab"
        aria-selected={mode === 'rounds'}
        className={mode === 'rounds' ? 'mode-btn active' : 'mode-btn'}
        onClick={() => onChange('rounds')}
      >
        Set rounds
      </button>
      <button
        type="button"
        role="tab"
        aria-selected={mode === 'duration'}
        className={mode === 'duration' ? 'mode-btn active' : 'mode-btn'}
        onClick={() => onChange('duration')}
      >
        Set duration
      </button>
    </div>
  )
}
