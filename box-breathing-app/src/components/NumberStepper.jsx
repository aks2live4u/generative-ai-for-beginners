import { clamp } from '../utils'

export default function NumberStepper({ label, value, min, max, step = 1, suffix, onChange }) {
  const dec = () => onChange(clamp(value - step, min, max))
  const inc = () => onChange(clamp(value + step, min, max))

  const handleInput = (e) => {
    const raw = e.target.value
    if (raw === '') return
    const num = Number(raw)
    if (Number.isNaN(num)) return
    onChange(clamp(Math.round(num), min, max))
  }

  return (
    <div className="stepper">
      <span className="stepper-label">{label}</span>
      <div className="stepper-controls">
        <button
          type="button"
          className="stepper-btn"
          onClick={dec}
          disabled={value <= min}
          aria-label={`Decrease ${label}`}
        >
          −
        </button>
        <div className="stepper-value">
          <input
            type="number"
            inputMode="numeric"
            value={value}
            min={min}
            max={max}
            step={step}
            onChange={handleInput}
          />
          {suffix && <span className="stepper-suffix">{suffix}</span>}
        </div>
        <button
          type="button"
          className="stepper-btn"
          onClick={inc}
          disabled={value >= max}
          aria-label={`Increase ${label}`}
        >
          +
        </button>
      </div>
    </div>
  )
}
