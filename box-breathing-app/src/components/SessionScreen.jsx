import { useEffect, useMemo, useRef, useState } from 'react'
import BreathingVisual from './BreathingVisual'
import { PHASES } from '../constants'
import { formatTime } from '../utils'

const TICK_MS = 200

function buildSequence(phases, totalRounds) {
  const sequence = []
  for (let round = 1; round <= totalRounds; round += 1) {
    for (const phase of PHASES) {
      sequence.push({ round, phaseKey: phase.key, duration: phases[phase.key] })
    }
  }
  const prefix = []
  let sum = 0
  for (const step of sequence) {
    prefix.push(sum)
    sum += step.duration
  }
  return { sequence, prefix, totalDuration: sum }
}

export default function SessionScreen({ phases, totalRounds, videoUrl, audioUrl, onExit }) {
  const { sequence, prefix, totalDuration } = useMemo(
    () => buildSequence(phases, totalRounds),
    [phases, totalRounds],
  )

  const [stepIndex, setStepIndex] = useState(0)
  const [remaining, setRemaining] = useState(sequence[0].duration)
  const [paused, setPaused] = useState(false)
  const [finished, setFinished] = useState(false)
  const [restartTick, setRestartTick] = useState(0)

  const stepIndexRef = useRef(0)
  const stepEndRef = useRef(Date.now() + sequence[0].duration * 1000)
  const pausedRef = useRef(false)
  const remainingAtPauseRef = useRef(null)
  const intervalRef = useRef(null)
  const audioRef = useRef(null)
  const videoRef = useRef(null)

  useEffect(() => {
    if (audioUrl && audioRef.current) {
      audioRef.current.play().catch(() => {})
    }

    intervalRef.current = setInterval(() => {
      if (pausedRef.current) return

      const now = Date.now()
      const rem = stepEndRef.current - now

      if (rem <= 0) {
        const nextIndex = stepIndexRef.current + 1
        if (nextIndex >= sequence.length) {
          clearInterval(intervalRef.current)
          audioRef.current?.pause()
          setFinished(true)
          return
        }
        stepIndexRef.current = nextIndex
        stepEndRef.current = now + sequence[nextIndex].duration * 1000
        setStepIndex(nextIndex)
        setRestartTick((t) => t + 1)
        setRemaining(sequence[nextIndex].duration)
      } else {
        setRemaining(Math.ceil(rem / 1000))
      }
    }, TICK_MS)

    return () => {
      clearInterval(intervalRef.current)
      audioRef.current?.pause()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const togglePause = () => {
    if (paused) {
      const now = Date.now()
      stepEndRef.current = now + (remainingAtPauseRef.current ?? 0)
      pausedRef.current = false
      setPaused(false)
      audioRef.current?.play().catch(() => {})
      videoRef.current?.play().catch(() => {})
    } else {
      remainingAtPauseRef.current = stepEndRef.current - Date.now()
      pausedRef.current = true
      setPaused(true)
      audioRef.current?.pause()
      videoRef.current?.pause()
    }
  }

  const handleStop = () => {
    clearInterval(intervalRef.current)
    audioRef.current?.pause()
    onExit()
  }

  const currentStep = sequence[stepIndex]
  const currentPhase = PHASES.find((p) => p.key === currentStep.phaseKey)
  const elapsedInStep = currentStep.duration - remaining
  const totalRemaining = totalDuration - prefix[stepIndex] - elapsedInStep

  return (
    <div className="session-screen">
      {videoUrl && (
        <video
          ref={videoRef}
          className="bg-video"
          src={videoUrl}
          autoPlay
          muted
          loop
          playsInline
        />
      )}
      <div className="session-scrim" />

      <div className="session-overlay">
        <div className="session-top">
          <span>Round {currentStep.round} / {totalRounds}</span>
          <span>{formatTime(totalRemaining)} left</span>
        </div>

        <BreathingVisual
          phaseKey={currentStep.phaseKey}
          duration={currentStep.duration}
          paused={paused}
          restartKey={restartTick}
        />

        <div className="phase-label">{currentPhase.label}</div>
        <div className="phase-countdown">{remaining}</div>

        <div className="session-controls">
          <button type="button" className="control-btn" onClick={togglePause}>
            {paused ? 'Resume' : 'Pause'}
          </button>
          <button type="button" className="control-btn control-btn-secondary" onClick={handleStop}>
            Stop
          </button>
        </div>
      </div>

      {audioUrl && <audio ref={audioRef} src={audioUrl} loop />}

      {finished && (
        <div className="session-finished-overlay">
          <h2>Session complete</h2>
          <p>
            You completed {totalRounds} round{totalRounds === 1 ? '' : 's'} ({formatTime(totalDuration)}).
          </p>
          <button type="button" className="start-btn" onClick={onExit}>
            Done
          </button>
        </div>
      )}
    </div>
  )
}
