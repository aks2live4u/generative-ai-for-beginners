import { Platform } from "react-native";

/**
 * Bold condensed sans-serif for numbers & headers (speedometer feel).
 * Falls back to system condensed/bold fonts — swap in Orbitron / Eurostile
 * via expo-font if custom typefaces are added later.
 */
export const fontFamily = {
  gauge: Platform.select({ ios: "Menlo-Bold", android: "sans-serif-condensed", default: "monospace" }),
  heading: Platform.select({ ios: "System", android: "sans-serif-condensed", default: "System" }),
  body: Platform.select({ ios: "System", android: "sans-serif", default: "System" }),
};

export const type = {
  gaugeXL: { fontFamily: fontFamily.gauge, fontSize: 56, fontWeight: "800" as const, letterSpacing: 1 },
  gaugeL: { fontFamily: fontFamily.gauge, fontSize: 34, fontWeight: "800" as const, letterSpacing: 0.5 },
  h1: { fontFamily: fontFamily.heading, fontSize: 28, fontWeight: "800" as const, letterSpacing: 0.3 },
  h2: { fontFamily: fontFamily.heading, fontSize: 20, fontWeight: "700" as const, letterSpacing: 0.2 },
  h3: { fontFamily: fontFamily.heading, fontSize: 16, fontWeight: "700" as const },
  body: { fontFamily: fontFamily.body, fontSize: 15, fontWeight: "400" as const },
  bodyStrong: { fontFamily: fontFamily.body, fontSize: 15, fontWeight: "600" as const },
  caption: { fontFamily: fontFamily.body, fontSize: 12, fontWeight: "500" as const, letterSpacing: 0.4 },
  micro: { fontFamily: fontFamily.body, fontSize: 10, fontWeight: "600" as const, letterSpacing: 0.6 },
};
