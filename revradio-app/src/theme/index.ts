import { cockpitDark, daylightLight, CockpitPalette } from "./colors";
import { type, fontFamily } from "./typography";
import { useSettingsStore } from "@/store/settingsStore";

export { cockpitDark, daylightLight, type, fontFamily };
export type { CockpitPalette };

export const radius = { sm: 8, md: 14, lg: 20, xl: 28, pill: 999 };
export const spacing = { xs: 4, sm: 8, md: 16, lg: 24, xl: 32, xxl: 48 };

/** Reads the user's theme preference and returns the matching cockpit palette. */
export function useTheme(): CockpitPalette {
  const themeMode = useSettingsStore((s) => s.themeMode);
  return themeMode === "daylight" ? daylightLight : cockpitDark;
}
