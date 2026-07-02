import NumberStepper from './NumberStepper'
import ModeToggle from './ModeToggle'
import MediaPicker from './MediaPicker'
import { formatTime } from '../utils'
import {
  MIN_PHASE_SECONDS,
  MAX_PHASE_SECONDS,
  MIN_ROUNDS,
  MAX_ROUNDS,
  MIN_DURATION_MINUTES,
  MAX_DURATION_MINUTES,
} from '../constants'

export default function SetupScreen({
  phases,
  setPhase,
  mode,
  setMode,
  rounds,
  setRounds,
  durationMinutes,
  setDurationMinutes,
  roundSeconds,
  effectiveRounds,
  totalSeconds,
  videoName,
  audioName,
  onVideoChange,
  onVideoClear,
  onAudioChange,
  onAudioClear,
  onStart,
}) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Box Breathing</h1>
        <p className="subtitle">A calm, steady rhythm for your mind.</p>
      </header>

      <main className="setup-card">
        <section className="section">
          <h2>Session length</h2>
          <ModeToggle mode={mode} onChange={setMode} />

          {mode === 'rounds' ? (
            <>
              <NumberStepper
                label="Rounds"
                value={rounds}
                min={MIN_ROUNDS}
                max={MAX_ROUNDS}
                onChange={setRounds}
                suffix="round(s)"
              />
              <p className="computed-line">
                Total session time: <strong>{formatTime(totalSeconds)}</strong>
              </p>
            </>
          ) : (
            <>
              <NumberStepper
                label="Duration"
                value={durationMinutes}
                min={MIN_DURATION_MINUTES}
                max={MAX_DURATION_MINUTES}
                onChange={setDurationMinutes}
                suffix="min"
              />
              <p className="computed-line">
                ≈ <strong>{effectiveRounds}</strong> round(s) · actual time{' '}
                <strong>{formatTime(totalSeconds)}</strong>
              </p>
            </>
          )}
        </section>

        <section className="section">
          <h2>Breathing pattern</h2>
          <div className="phase-grid">
            <NumberStepper
              label="Breathe In"
              value={phases.inhale}
              min={MIN_PHASE_SECONDS}
              max={MAX_PHASE_SECONDS}
              onChange={(v) => setPhase('inhale', v)}
              suffix="sec"
            />
            <NumberStepper
              label="Hold"
              value={phases.hold1}
              min={MIN_PHASE_SECONDS}
              max={MAX_PHASE_SECONDS}
              onChange={(v) => setPhase('hold1', v)}
              suffix="sec"
            />
            <NumberStepper
              label="Breathe Out"
              value={phases.exhale}
              min={MIN_PHASE_SECONDS}
              max={MAX_PHASE_SECONDS}
              onChange={(v) => setPhase('exhale', v)}
              suffix="sec"
            />
            <NumberStepper
              label="Hold"
              value={phases.hold2}
              min={MIN_PHASE_SECONDS}
              max={MAX_PHASE_SECONDS}
              onChange={(v) => setPhase('hold2', v)}
              suffix="sec"
            />
          </div>
          <p className="computed-line">
            One round = <strong>{roundSeconds}s</strong>
          </p>
        </section>

        <section className="section">
          <h2>Background (optional)</h2>
          <MediaPicker
            videoName={videoName}
            audioName={audioName}
            onVideoChange={onVideoChange}
            onVideoClear={onVideoClear}
            onAudioChange={onAudioChange}
            onAudioClear={onAudioClear}
          />
        </section>

        <button type="button" className="start-btn" onClick={onStart}>
          Start Session
        </button>
      </main>
    </div>
  )
}
