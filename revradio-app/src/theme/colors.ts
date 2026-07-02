/**
 * "Cockpit" design language palette — see RevRadio App Plan §3.
 * Dark mode is the default cockpit theme; "Daylight" is the optional light theme.
 */
export const cockpitDark = {
  mode: "dark" as const,
  racingRed: "#E10600",
  carbonBlack: "#0B0B0D",
  titaniumGrey: "#8A8D91",
  signalAmber: "#FFB100",

  background: "#0B0B0D",
  surface: "#17181B",
  surfaceRaised: "#202226",
  border: "#2B2D31",
  hairline: "#1F2023",

  textPrimary: "#F5F6F7",
  textSecondary: "#A6A9AE",
  textMuted: "#6C6F75",

  accent: "#E10600",
  accentAlt: "#FFB100",
  success: "#2ECC71",
  danger: "#E10600",
  warning: "#FFB100",

  tabBarBackground: "#101114",
  tabBarActive: "#E10600",
  tabBarInactive: "#6C6F75",
};

export const daylightLight = {
  mode: "light" as const,
  racingRed: "#D0021B",
  carbonBlack: "#1A1B1E",
  titaniumGrey: "#6B6E73",
  signalAmber: "#CC8400",

  background: "#F4F4F5",
  surface: "#FFFFFF",
  surfaceRaised: "#FFFFFF",
  border: "#E1E2E4",
  hairline: "#ECEDEF",

  textPrimary: "#141518",
  textSecondary: "#4B4D52",
  textMuted: "#84868C",

  accent: "#D0021B",
  accentAlt: "#CC8400",
  success: "#1E9E52",
  danger: "#D0021B",
  warning: "#CC8400",

  tabBarBackground: "#FFFFFF",
  tabBarActive: "#D0021B",
  tabBarInactive: "#9A9CA1",
};

export type CockpitPalette = typeof cockpitDark | typeof daylightLight;
