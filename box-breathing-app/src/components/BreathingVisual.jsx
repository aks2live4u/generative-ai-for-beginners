export default function BreathingVisual({ phaseKey, duration, paused, restartKey }) {
  const isGrow = phaseKey === 'inhale'
  const isShrink = phaseKey === 'exhale'
  const isHoldLarge = phaseKey === 'hold1'

  const classNames = ['breathing-circle']
  if (isGrow) classNames.push('anim-grow')
  if (isShrink) classNames.push('anim-shrink')

  const style = {
    animationPlayState: paused ? 'paused' : 'running',
  }
  if (isGrow || isShrink) {
    style.animationDuration = `${duration}s`
  } else {
    style.transform = isHoldLarge ? 'scale(1)' : 'scale(0.55)'
  }

  return (
    <div className="breathing-visual">
      <div className="breathing-ring" />
      <div key={restartKey} className={classNames.join(' ')} style={style} />
    </div>
  )
}
