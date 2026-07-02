import { useEffect, useRef, useState } from 'react'
import SetupScreen from './components/SetupScreen'
import SessionScreen from './components/SessionScreen'
import {
  DEFAULT_PHASES,
  DEFAULT_ROUNDS,
  DEFAULT_DURATION_MINUTES,
} from './constants'
import './App.css'

export default function App() {
  const [phases, setPhases] = useState(DEFAULT_PHASES)
  const [mode, setMode] = useState('rounds')
  const [rounds, setRounds] = useState(DEFAULT_ROUNDS)
  const [durationMinutes, setDurationMinutes] = useState(DEFAULT_DURATION_MINUTES)

  const [videoUrl, setVideoUrl] = useState(null)
  const [videoName, setVideoName] = useState('')
  const [audioUrl, setAudioUrl] = useState(null)
  const [audioName, setAudioName] = useState('')

  const [sessionId, setSessionId] = useState(0)
  const [sessionActive, setSessionActive] = useState(false)

  const videoUrlRef = useRef(null)
  const audioUrlRef = useRef(null)
  videoUrlRef.current = videoUrl
  audioUrlRef.current = audioUrl

  useEffect(() => {
    return () => {
      if (videoUrlRef.current) URL.revokeObjectURL(videoUrlRef.current)
      if (audioUrlRef.current) URL.revokeObjectURL(audioUrlRef.current)
    }
  }, [])

  const setPhase = (key, value) => {
    setPhases((prev) => ({ ...prev, [key]: value }))
  }

  const roundSeconds = phases.inhale + phases.hold1 + phases.exhale + phases.hold2
  const effectiveRounds =
    mode === 'rounds' ? rounds : Math.max(1, Math.floor((durationMinutes * 60) / roundSeconds))
  const totalSeconds = effectiveRounds * roundSeconds

  const handleVideoChange = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (videoUrl) URL.revokeObjectURL(videoUrl)
    setVideoUrl(URL.createObjectURL(file))
    setVideoName(file.name)
    e.target.value = ''
  }

  const handleVideoClear = () => {
    if (videoUrl) URL.revokeObjectURL(videoUrl)
    setVideoUrl(null)
    setVideoName('')
  }

  const handleAudioChange = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (audioUrl) URL.revokeObjectURL(audioUrl)
    setAudioUrl(URL.createObjectURL(file))
    setAudioName(file.name)
    e.target.value = ''
  }

  const handleAudioClear = () => {
    if (audioUrl) URL.revokeObjectURL(audioUrl)
    setAudioUrl(null)
    setAudioName('')
  }

  const handleStart = () => {
    setSessionId((id) => id + 1)
    setSessionActive(true)
  }

  const handleExit = () => {
    setSessionActive(false)
  }

  if (sessionActive) {
    return (
      <SessionScreen
        key={sessionId}
        phases={phases}
        totalRounds={effectiveRounds}
        videoUrl={videoUrl}
        audioUrl={audioUrl}
        onExit={handleExit}
      />
    )
  }

  return (
    <SetupScreen
      phases={phases}
      setPhase={setPhase}
      mode={mode}
      setMode={setMode}
      rounds={rounds}
      setRounds={setRounds}
      durationMinutes={durationMinutes}
      setDurationMinutes={setDurationMinutes}
      roundSeconds={roundSeconds}
      effectiveRounds={effectiveRounds}
      totalSeconds={totalSeconds}
      videoName={videoName}
      audioName={audioName}
      onVideoChange={handleVideoChange}
      onVideoClear={handleVideoClear}
      onAudioChange={handleAudioChange}
      onAudioClear={handleAudioClear}
      onStart={handleStart}
    />
  )
}
