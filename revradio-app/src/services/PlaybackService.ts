import TrackPlayer, { Event } from "react-native-track-player";

/**
 * Headless playback service, registered in the root index.js. Handles
 * lock-screen / notification remote-control events so playback keeps
 * working while RevRadio is backgrounded (Plan §4.4).
 */
export const PlaybackService = async function () {
  TrackPlayer.addEventListener(Event.RemotePlay, () => TrackPlayer.play());
  TrackPlayer.addEventListener(Event.RemotePause, () => TrackPlayer.pause());
  TrackPlayer.addEventListener(Event.RemoteStop, () => TrackPlayer.stop());

  TrackPlayer.addEventListener(Event.RemoteDuck, async (event) => {
    if (event.paused) {
      await TrackPlayer.pause();
    } else if (event.permanent === false) {
      await TrackPlayer.play();
    }
  });

  TrackPlayer.addEventListener(Event.PlaybackError, (event) => {
    // The player store's reconnect logic listens for state changes and
    // handles mirror/stream fallback; this just prevents an unhandled
    // rejection from surfacing as a crash.
    console.warn("RevRadio playback error:", event);
  });
};
