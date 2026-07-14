export type ColorPalette = typeof lightColors;

const lightColors = {
  background: "#FFFFFF",
  surface: "#F7F8FA",
  card: "#FFFFFF",
  primary: "#0A66FF",
  primaryDark: "#0047CC",
  text: "#111318",
  textMuted: "#6B7280",
  border: "#E5E7EB",
  success: "#16A34A",
  danger: "#DC2626",
  warning: "#D97706",
};

const darkColors: ColorPalette = {
  background: "#0B0D12",
  surface: "#151821",
  card: "#1B1F2A",
  primary: "#3B82F6",
  primaryDark: "#2563EB",
  text: "#F4F5F7",
  textMuted: "#9CA3AF",
  border: "#262B38",
  success: "#22C55E",
  danger: "#F87171",
  warning: "#FBBF24",
};

export const theme = {
  colors: lightColors,
  dark: darkColors,
  radius: {
    sm: 8,
    md: 14,
    lg: 20,
    xl: 28,
    pill: 999,
  },
  spacing: (n: number) => n * 4,
};

export type Theme = typeof theme;
