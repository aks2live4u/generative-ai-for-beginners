export type Face = "U" | "R" | "F" | "D" | "L" | "B";

export const FACES: Face[] = ["U", "R", "F", "D", "L", "B"];

export type CubeColor = "white" | "yellow" | "red" | "orange" | "blue" | "green";

/** One face's 9 stickers, row-major (index 0 = top-left, index 4 = center, index 8 = bottom-right). */
export type FaceColors = CubeColor[];

/** Full scanned cube state: one 9-color array per face. */
export type CubeFaces = Record<Face, FaceColors>;

/** A cube state where some faces may not have been scanned yet. */
export type PartialCubeFaces = Partial<Record<Face, FaceColors>>;

export interface CubieState {
  /** Corner permutation: cp[slot] = index of the corner piece occupying that slot. */
  cp: number[];
  /** Corner orientation, 0-2, per slot. */
  co: number[];
  /** Edge permutation: ep[slot] = index of the edge piece occupying that slot. */
  ep: number[];
  /** Edge orientation, 0-1, per slot. */
  eo: number[];
}
