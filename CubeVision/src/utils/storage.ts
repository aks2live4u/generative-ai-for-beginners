import AsyncStorage from "@react-native-async-storage/async-storage";

const KEYS = {
  settings: "cubevision.settings",
  history: "cubevision.history",
} as const;

export interface AppSettings {
  darkMode: boolean;
  voiceGuidance: boolean;
  hapticFeedback: boolean;
}

export const DEFAULT_SETTINGS: AppSettings = {
  darkMode: false,
  voiceGuidance: false,
  hapticFeedback: true,
};

export async function loadSettings(): Promise<AppSettings> {
  const raw = await AsyncStorage.getItem(KEYS.settings);
  if (!raw) return DEFAULT_SETTINGS;
  try {
    return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export async function saveSettings(settings: AppSettings): Promise<void> {
  await AsyncStorage.setItem(KEYS.settings, JSON.stringify(settings));
}

export interface SolveHistoryEntry {
  id: string;
  timestamp: number;
  moveCount: number;
  durationMs: number | null;
}

export async function loadHistory(): Promise<SolveHistoryEntry[]> {
  const raw = await AsyncStorage.getItem(KEYS.history);
  if (!raw) return [];
  try {
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

export async function appendHistory(entry: SolveHistoryEntry): Promise<SolveHistoryEntry[]> {
  const history = await loadHistory();
  const next = [entry, ...history].slice(0, 200);
  await AsyncStorage.setItem(KEYS.history, JSON.stringify(next));
  return next;
}

export async function clearHistory(): Promise<void> {
  await AsyncStorage.removeItem(KEYS.history);
}
