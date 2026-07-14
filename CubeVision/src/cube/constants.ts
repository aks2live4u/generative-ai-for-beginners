import { CubeColor, Face, FACES } from "./types";

/**
 * Standard Western ("BOY") color scheme: the color scheme cubejs's letters
 * assume when a face's letter is treated as "the color of that face solved".
 */
export const FACE_COLOR: Record<Face, CubeColor> = {
  U: "white",
  D: "yellow",
  R: "red",
  L: "orange",
  F: "green",
  B: "blue",
};

export const COLOR_FACE: Record<CubeColor, Face> = Object.fromEntries(
  Object.entries(FACE_COLOR).map(([face, color]) => [color, face])
) as Record<CubeColor, Face>;

export const COLORS: CubeColor[] = FACES.map((f) => FACE_COLOR[f]);

export const OPPOSITE_FACE: Record<Face, Face> = { U: "D", D: "U", R: "L", L: "R", F: "B", B: "F" };

export const OPPOSITE_COLOR: Record<CubeColor, CubeColor> = Object.fromEntries(
  FACES.map((f) => [FACE_COLOR[f], FACE_COLOR[OPPOSITE_FACE[f]]])
) as Record<CubeColor, CubeColor>;

/** Hex swatches for UI rendering. */
export const COLOR_HEX: Record<CubeColor, string> = {
  white: "#F5F5F5",
  yellow: "#FFD500",
  red: "#C41E3A",
  orange: "#FF8C00",
  blue: "#0051BA",
  green: "#009E60",
};

export const FACE_LABEL: Record<Face, string> = {
  U: "Up",
  D: "Down",
  F: "Front",
  B: "Back",
  L: "Left",
  R: "Right",
};

/** Order the spec asks the user to scan faces in. */
export const SCAN_ORDER: CubeColor[] = ["white", "yellow", "red", "orange", "blue", "green"];
