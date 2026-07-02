export default function MediaPicker({
  videoName,
  audioName,
  onVideoChange,
  onVideoClear,
  onAudioChange,
  onAudioClear,
}) {
  return (
    <div className="media-picker">
      <div className="media-row">
        <label className="file-btn">
          <span className="file-icon" aria-hidden="true">🎥</span>
          <span className="file-text">{videoName || 'Add background video'}</span>
          <input type="file" accept="video/*" onChange={onVideoChange} hidden />
        </label>
        {videoName && (
          <button type="button" className="clear-btn" onClick={onVideoClear}>
            Remove
          </button>
        )}
      </div>
      <div className="media-row">
        <label className="file-btn">
          <span className="file-icon" aria-hidden="true">🎵</span>
          <span className="file-text">{audioName || 'Add background music'}</span>
          <input type="file" accept="audio/*" onChange={onAudioChange} hidden />
        </label>
        {audioName && (
          <button type="button" className="clear-btn" onClick={onAudioClear}>
            Remove
          </button>
        )}
      </div>
      <p className="media-hint">Files are picked from your device and never uploaded anywhere.</p>
    </div>
  )
}
