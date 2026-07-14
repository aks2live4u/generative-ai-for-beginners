import { COLOR_FACE, FACE_COLOR } from "./constants";
import { CubeColor, CubeFaces, Face, FACES } from "./types";

/**
 * Physical cube geometry: which (face, within-face index) sticker positions
 * belong to each of the 8 corner slots and 12 edge slots, and the fixed
 * color identity of each physical corner/edge piece (a piece's colors never
 * change, no matter how the cube is scrambled).
 *
 * This table is transcribed from cubejs's own facelet definitions
 * (lib/cube.js: cornerFacelet/edgeFacelet/cornerColor/edgeColor), converted
 * from 1-indexed to 0-indexed within-face positions, so it is guaranteed to
 * agree with what the solver expects.
 */

export type FaceletRef = [Face, number];

export const CORNER_NAMES = ["URF", "UFL", "ULB", "UBR", "DFR", "DLF", "DBL", "DRB"] as const;

export const CORNER_FACELETS: FaceletRef[][] = [
  [["U", 8], ["R", 0], ["F", 2]], // URF
  [["U", 6], ["F", 0], ["L", 2]], // UFL
  [["U", 0], ["L", 0], ["B", 2]], // ULB
  [["U", 2], ["B", 0], ["R", 2]], // UBR
  [["D", 2], ["F", 8], ["R", 6]], // DFR
  [["D", 0], ["L", 8], ["F", 6]], // DLF
  [["D", 6], ["B", 8], ["L", 6]], // DBL
  [["D", 8], ["R", 8], ["B", 6]], // DRB
];

const CORNER_FACE_TRIPLES: Face[][] = [
  ["U", "R", "F"],
  ["U", "F", "L"],
  ["U", "L", "B"],
  ["U", "B", "R"],
  ["D", "F", "R"],
  ["D", "L", "F"],
  ["D", "B", "L"],
  ["D", "R", "B"],
];

/** The fixed (never-changing) set of 3 colors belonging to each physical corner piece. */
export const CORNER_COLOR_TRIPLES: CubeColor[][] = CORNER_FACE_TRIPLES.map((t) => t.map((f) => FACE_COLOR[f]));

export const EDGE_NAMES = ["UR", "UF", "UL", "UB", "DR", "DF", "DL", "DB", "FR", "FL", "BL", "BR"] as const;

export const EDGE_FACELETS: FaceletRef[][] = [
  [["U", 5], ["R", 1]], // UR
  [["U", 7], ["F", 1]], // UF
  [["U", 3], ["L", 1]], // UL
  [["U", 1], ["B", 1]], // UB
  [["D", 5], ["R", 7]], // DR
  [["D", 1], ["F", 7]], // DF
  [["D", 3], ["L", 7]], // DL
  [["D", 7], ["B", 7]], // DB
  [["F", 5], ["R", 3]], // FR
  [["F", 3], ["L", 5]], // FL
  [["B", 5], ["L", 3]], // BL
  [["B", 3], ["R", 5]], // BR
];

const EDGE_FACE_PAIRS: Face[][] = [
  ["U", "R"],
  ["U", "F"],
  ["U", "L"],
  ["U", "B"],
  ["D", "R"],
  ["D", "F"],
  ["D", "L"],
  ["D", "B"],
  ["F", "R"],
  ["F", "L"],
  ["B", "L"],
  ["B", "R"],
];

/** The fixed (never-changing) pair of colors belonging to each physical edge piece. */
export const EDGE_COLOR_PAIRS: CubeColor[][] = EDGE_FACE_PAIRS.map((p) => p.map((f) => FACE_COLOR[f]));

export const CENTER_FACELET_INDEX = 4;

/** Convert scanned face colors into the 54-character URFDLB facelet string cubejs expects. */
export function facesToFaceletString(faces: CubeFaces): string {
  return FACES.map((face) => faces[face].map((color) => COLOR_FACE[color]).join("")).join("");
}

/** Convert a 54-character URFDLB facelet string back into per-face color arrays. */
export function faceletStringToFaces(str: string): CubeFaces {
  const result = {} as CubeFaces;
  FACES.forEach((face, i) => {
    result[face] = str
      .slice(i * 9, i * 9 + 9)
      .split("")
      .map((letter) => FACE_COLOR[letter as Face]);
  });
  return result;
}

export function colorSetEquals(a: CubeColor[], b: CubeColor[]): boolean {
  if (a.length !== b.length) return false;
  const sa = [...a].sort().join(",");
  const sb = [...b].sort().join(",");
  return sa === sb;
}
