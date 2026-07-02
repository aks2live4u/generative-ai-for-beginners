import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { EqPresetId } from "@/constants/genres";

export type ThemeMode = "cockpit" | "daylight";
export type AudioQuality = "auto" | "data-saver" | "high";

interface SettingsState {
  themeMode: ThemeMode;
  engineSoundOnLaunch: boolean;
  hasCompletedOnboarding: boolean;
  preferredLanguages: string[];
  preferredGenreTags: string[];
  defaultSleepTimerMinutes: number;
  audioQuality: AudioQuality;
  equalizerPreset: EqPresetId;
  cellularWarningEnabled: boolean;

  setThemeMode: (mode: ThemeMode) => void;
  toggleEngineSound: () => void;
  completeOnboarding: (languages: string[], genreTags: string[]) => void;
  resetOnboarding: () => void;
  setDefaultSleepTimer: (minutes: number) => void;
  setAudioQuality: (quality: AudioQuality) => void;
  setEqualizerPreset: (preset: EqPresetId) => void;
  toggleCellularWarning: () => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      themeMode: "cockpit",
      engineSoundOnLaunch: true,
      hasCompletedOnboarding: false,
      preferredLanguages: [],
      preferredGenreTags: [],
      defaultSleepTimerMinutes: 30,
      audioQuality: "auto",
      equalizerPreset: "flat",
      cellularWarningEnabled: true,

      setThemeMode: (mode) => set({ themeMode: mode }),
      toggleEngineSound: () => set((s) => ({ engineSoundOnLaunch: !s.engineSoundOnLaunch })),
      completeOnboarding: (languages, genreTags) =>
        set({ hasCompletedOnboarding: true, preferredLanguages: languages, preferredGenreTags: genreTags }),
      resetOnboarding: () => set({ hasCompletedOnboarding: false }),
      setDefaultSleepTimer: (minutes) => set({ defaultSleepTimerMinutes: minutes }),
      setAudioQuality: (quality) => set({ audioQuality: quality }),
      setEqualizerPreset: (preset) => set({ equalizerPreset: preset }),
      toggleCellularWarning: () => set((s) => ({ cellularWarningEnabled: !s.cellularWarningEnabled })),
    }),
    { name: "revradio:settings", storage: createJSONStorage(() => AsyncStorage) }
  )
);
