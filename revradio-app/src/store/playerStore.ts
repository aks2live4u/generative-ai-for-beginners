import { create } from "zustand";
import TrackPlayer, { Capability, AppKilledPlaybackBehavior, State } from "react-native-track-player";
import { Station } from "@/types/station";
import { registerStationClick } from "@/api/radioBrowser";
import { useFavoritesStore } from "@/store/favoritesStore";

const MAX_RECONNECT_ATTEMPTS = 3;

interface PlayerState {
  isSetup: boolean;
  currentStation: Station | null;
  isLoading: boolean;
  errorMessage: string | null;
  reconnectAttempt: number;
  sleepTimerEndsAt: number | null; // "Pit Stop Timer" — §4.4
  sleepTimerHandle: ReturnType<typeof setTimeout> | null;

  ensureSetup: () => Promise<void>;
  play: (station: Station) => Promise<void>;
  togglePlayPause: () => Promise<void>;
  stop: () => Promise<void>;
  handleStreamDropped: () => Promise<void>;
  setSleepTimer: (minutes: number) => void;
  clearSleepTimer: () => void;
}

export const usePlayerStore = create<PlayerState>()((set, get) => ({
  isSetup: false,
  currentStation: null,
  isLoading: false,
  errorMessage: null,
  reconnectAttempt: 0,
  sleepTimerEndsAt: null,
  sleepTimerHandle: null,

  ensureSetup: async () => {
    if (get().isSetup) return;
    await TrackPlayer.setupPlayer({ autoHandleInterruptions: true });
    await TrackPlayer.updateOptions({
      capabilities: [Capability.Play, Capability.Pause, Capability.Stop, Capability.SeekTo],
      compactCapabilities: [Capability.Play, Capability.Pause, Capability.Stop],
      android: {
        appKilledPlaybackBehavior: AppKilledPlaybackBehavior.StopPlaybackAndRemoveNotification,
      },
    });
    set({ isSetup: true });
  },

  play: async (station) => {
    const { ensureSetup } = get();
    await ensureSetup();
    set({ isLoading: true, errorMessage: null, currentStation: station, reconnectAttempt: 0 });

    try {
      // Register the click (Radio-Browser popularity signal) and resolve
      // the canonical stream URL before handing it to the player.
      const clickResult = await registerStationClick(station.id);
      const streamUrl = clickResult.url || station.streamUrl;

      await TrackPlayer.reset();
      await TrackPlayer.add({
        id: station.id,
        url: streamUrl,
        title: station.name,
        artist: station.country,
        artwork: station.favicon ?? undefined,
        isLiveStream: true,
      });
      await TrackPlayer.play();
      useFavoritesStore.getState().recordPlay(station);
      set({ isLoading: false });
    } catch (err) {
      set({ isLoading: false, errorMessage: "Couldn't start this station." });
    }
  },

  togglePlayPause: async () => {
    const playbackState = await TrackPlayer.getPlaybackState();
    if (playbackState.state === State.Playing) {
      await TrackPlayer.pause();
    } else {
      await TrackPlayer.play();
    }
  },

  stop: async () => {
    get().clearSleepTimer();
    await TrackPlayer.reset();
    set({ currentStation: null, errorMessage: null });
  },

  /**
   * Buffering/reconnect logic with automatic stream-mirror fallback — §4.4.
   * Called when the RNTP PlaybackError event fires for the active station.
   */
  handleStreamDropped: async () => {
    const { currentStation, reconnectAttempt } = get();
    if (!currentStation || reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
      set({ errorMessage: "Signal lost — this station may be offline." });
      return;
    }
    set({ reconnectAttempt: reconnectAttempt + 1, isLoading: true });
    try {
      const clickResult = await registerStationClick(currentStation.id);
      const streamUrl = clickResult.url || currentStation.streamUrl;
      await TrackPlayer.reset();
      await TrackPlayer.add({
        id: currentStation.id,
        url: streamUrl,
        title: currentStation.name,
        artist: currentStation.country,
        artwork: currentStation.favicon ?? undefined,
        isLiveStream: true,
      });
      await TrackPlayer.play();
      set({ isLoading: false });
    } catch {
      set({ isLoading: false, errorMessage: "Reconnecting failed." });
    }
  },

  setSleepTimer: (minutes) => {
    get().clearSleepTimer();
    const endsAt = Date.now() + minutes * 60 * 1000;
    const handle = setTimeout(() => {
      TrackPlayer.pause();
      set({ sleepTimerEndsAt: null, sleepTimerHandle: null });
    }, minutes * 60 * 1000);
    set({ sleepTimerEndsAt: endsAt, sleepTimerHandle: handle });
  },

  clearSleepTimer: () => {
    const { sleepTimerHandle } = get();
    if (sleepTimerHandle) clearTimeout(sleepTimerHandle);
    set({ sleepTimerEndsAt: null, sleepTimerHandle: null });
  },
}));
