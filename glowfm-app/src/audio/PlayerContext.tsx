import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import {
  createAudioPlayer,
  setAudioModeAsync,
  useAudioPlayerStatus,
  type AudioPlayer,
} from 'expo-audio';
import { getStationByUuid, registerClick } from '../api/radioBrowser';
import { useRadioStore } from '../store/useRadioStore';
import type { PlaybackState, SignalStrength, Station } from '../types';

const MAX_RETRIES = 2;
const STALL_TIMEOUT_MS = 8000;

type Toast = { message: string } | null;

type PlayerContextValue = {
  playbackState: PlaybackState;
  signal: SignalStrength;
  toast: Toast;
  dismissToast: () => void;
  playStation: (station: Station) => Promise<void>;
  togglePlayPause: () => void;
};

const PlayerContext = createContext<PlayerContextValue | null>(null);

export function PlayerProvider({ children }: { children: React.ReactNode }) {
  const [player] = useState<AudioPlayer>(() => createAudioPlayer(undefined, { updateInterval: 1000 }));
  const status = useAudioPlayerStatus(player);
  const [toast, setToast] = useState<Toast>(null);

  const retryCountRef = useRef(0);
  const stallTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const handledErrorRef = useRef<string | null>(null);

  const markUnreliable = useRadioStore((s) => s.markUnreliable);
  const nextSuggestedStation = useRadioStore((s) => s.nextSuggestedStation);
  const currentStation = useRadioStore((s) => s.currentStation);
  const setCurrentStation = useRadioStore((s) => s.setCurrentStation);

  useEffect(() => {
    setAudioModeAsync({
      playsInSilentMode: true,
      shouldPlayInBackground: true,
      interruptionMode: 'doNotMix',
    });
  }, []);

  const clearStallTimer = () => {
    if (stallTimerRef.current) {
      clearTimeout(stallTimerRef.current);
      stallTimerRef.current = null;
    }
  };

  const handleFailure = async (station: Station) => {
    retryCountRef.current += 1;
    if (retryCountRef.current <= MAX_RETRIES) {
      const latest = await getStationByUuid(station.stationuuid).catch(() => null);
      if (latest && latest.urlResolved !== station.urlResolved) {
        player.replace(latest.urlResolved);
        player.play();
        return;
      }
      // Same URL or lookup failed — retry the original URL once more.
      player.replace(station.urlResolved);
      player.play();
      return;
    }

    markUnreliable(station.stationuuid);
    setToast({ message: 'Station unavailable — try another' });
    const suggestion = nextSuggestedStation(station.stationuuid);
    if (suggestion) {
      retryCountRef.current = 0;
      await playStation(suggestion);
    }
  };

  useEffect(() => {
    if (!currentStation) return;
    if (status.error && status.error !== handledErrorRef.current) {
      handledErrorRef.current = status.error;
      handleFailure(currentStation);
    }
  }, [status.error, currentStation]);

  useEffect(() => {
    clearStallTimer();
    if (status.isBuffering && currentStation) {
      stallTimerRef.current = setTimeout(() => {
        handleFailure(currentStation);
      }, STALL_TIMEOUT_MS);
    }
    return clearStallTimer;
  }, [status.isBuffering, currentStation]);

  const playStation = async (station: Station) => {
    retryCountRef.current = 0;
    handledErrorRef.current = null;
    await setCurrentStation(station);
    player.replace(station.urlResolved);
    player.play();
    registerClick(station.stationuuid);
  };

  const togglePlayPause = () => {
    if (status.playing) {
      player.pause();
    } else if (currentStation) {
      player.play();
    }
  };

  const playbackState: PlaybackState = status.error
    ? 'error'
    : status.isBuffering
      ? 'buffering'
      : status.playing
        ? 'playing'
        : 'idle';

  const signal: SignalStrength = status.error ? 'weak' : status.isBuffering ? 'good' : 'excellent';

  const value: PlayerContextValue = {
    playbackState,
    signal,
    toast,
    dismissToast: () => setToast(null),
    playStation,
    togglePlayPause,
  };

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>;
}

export function usePlayer(): PlayerContextValue {
  const ctx = useContext(PlayerContext);
  if (!ctx) throw new Error('usePlayer must be used within a PlayerProvider');
  return ctx;
}
