export const PHASES = [
  { key: 'inhale', label: 'Breathe In' },
  { key: 'hold1', label: 'Hold' },
  { key: 'exhale', label: 'Breathe Out' },
  { key: 'hold2', label: 'Hold' },
]

export const MIN_PHASE_SECONDS = 1
export const MAX_PHASE_SECONDS = 60

export const MIN_ROUNDS = 1
export const MAX_ROUNDS = 100

export const MIN_DURATION_MINUTES = 1
export const MAX_DURATION_MINUTES = 120

export const DEFAULT_PHASES = { inhale: 4, hold1: 4, exhale: 4, hold2: 4 }
export const DEFAULT_ROUNDS = 10
export const DEFAULT_DURATION_MINUTES = 10
