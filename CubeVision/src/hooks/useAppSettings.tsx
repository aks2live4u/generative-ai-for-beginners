import React, { createContext, PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState } from "react";
import * as Haptics from "expo-haptics";
import * as Speech from "expo-speech";
import { AppSettings, DEFAULT_SETTINGS, loadSettings, saveSettings } from "../utils/storage";
import { ColorPalette, theme } from "../theme/theme";
import { moveVoiceDescription } from "../solver/moveDescriptions";

export type HapticStyle = "light" | "success" | "warning";

interface AppSettingsContextValue {
  settings: AppSettings;
  loaded: boolean;
  colors: ColorPalette;
  updateSetting: <K extends keyof AppSettings>(key: K, value: AppSettings[K]) => void;
  triggerHaptic: (style: HapticStyle) => void;
  speak: (text: string) => void;
  speakMove: (move: string) => void;
}

const AppSettingsContext = createContext<AppSettingsContextValue | null>(null);

export function AppSettingsProvider({ children }: PropsWithChildren<{}>) {
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    loadSettings().then((s) => {
      setSettings(s);
      setLoaded(true);
    });
  }, []);

  const updateSetting = useCallback(<K extends keyof AppSettings>(key: K, value: AppSettings[K]) => {
    setSettings((prev) => {
      const next = { ...prev, [key]: value };
      saveSettings(next);
      return next;
    });
  }, []);

  const triggerHaptic = useCallback(
    (style: HapticStyle) => {
      if (!settings.hapticFeedback) return;
      if (style === "light") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      else if (style === "success") Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      else Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning);
    },
    [settings.hapticFeedback]
  );

  const speak = useCallback(
    (text: string) => {
      if (!settings.voiceGuidance) return;
      Speech.stop();
      Speech.speak(text, { rate: 0.95 });
    },
    [settings.voiceGuidance]
  );

  const speakMove = useCallback((move: string) => speak(moveVoiceDescription(move)), [speak]);

  const colors = settings.darkMode ? theme.dark : theme.colors;

  const value = useMemo(
    () => ({ settings, loaded, colors, updateSetting, triggerHaptic, speak, speakMove }),
    [settings, loaded, colors, updateSetting, triggerHaptic, speak, speakMove]
  );

  return <AppSettingsContext.Provider value={value}>{children}</AppSettingsContext.Provider>;
}

export function useAppSettings(): AppSettingsContextValue {
  const ctx = useContext(AppSettingsContext);
  if (!ctx) throw new Error("useAppSettings must be used within an AppSettingsProvider");
  return ctx;
}
