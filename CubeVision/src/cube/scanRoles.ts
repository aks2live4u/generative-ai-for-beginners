import { FACE_COLOR, SCAN_ORDER } from "./constants";
import { EDGE_FACELETS } from "./geometry";
import { CubeColor, Face, FACES } from "./types";

/**
 * For each face, which neighboring face's color must be at the top edge
 * (facelet index 1) of the grid for a capture to line up with our canonical
 * row-major facelet indexing. Derived from the verified EDGE_FACELETS table
 * rather than hand-reasoned, so it can't silently disagree with the solver's
 * own geometry.
 */
export const REQUIRED_TOP_NEIGHBOR: Record<Face, Face> = FACES.reduce((acc, face) => {
  const edge = EDGE_FACELETS.find(([a, b]) => (a[0] === face && a[1] === 1) || (b[0] === face && b[1] === 1))!;
  const neighbor = edge[0][0] === face ? edge[1][0] : edge[0][0];
  acc[face] = neighbor;
  return acc;
}, {} as Record<Face, Face>);

export interface ScanStep {
  color: CubeColor;
  face: Face;
  topNeighborColor: CubeColor;
}

/**
 * The 6 guided scan steps, in the order the spec asks for (White, Yellow,
 * Red, Orange, Blue, Green). Each step is self-contained: it doesn't depend
 * on what was scanned before, it just tells the user which color must sit
 * at the top edge of the face currently facing the camera.
 */
export const SCAN_STEPS: ScanStep[] = SCAN_ORDER.map((color) => {
  const face = Object.entries(FACE_COLOR).find(([, c]) => c === color)![0] as Face;
  return { color, face, topNeighborColor: FACE_COLOR[REQUIRED_TOP_NEIGHBOR[face]] };
});
