import { CubeColor, CubeFaces, Face } from "./types";

export type Vec3 = [number, number, number];

/**
 * Maps each of the 26 visible cubie positions (x,y,z each in {-1,0,1},
 * excluding the hidden core at 0,0,0) to the facelet index (0-8, row-major)
 * on each face it touches. Coordinate convention: +X = Right, +Y = Up,
 * +Z = Front — a standard right-handed frame matching cube notation
 * (R/U/F point along their respective positive axes).
 *
 * These formulas were derived directly from cubejs's own corner/edge
 * facelet tables (see geometry.ts) by solving for the (x,y,z) <-> facelet
 * pattern per face, not guessed — each was checked against all 4 corners
 * and 4 edges of every face.
 */
const faceletIndex = {
  U: (x: number, z: number) => (z + 1) * 3 + (x + 1),
  D: (x: number, z: number) => (1 - z) * 3 + (x + 1),
  F: (x: number, y: number) => (1 - y) * 3 + (x + 1),
  B: (x: number, y: number) => (1 - y) * 3 + (1 - x),
  L: (y: number, z: number) => (1 - y) * 3 + (z + 1),
  R: (y: number, z: number) => (1 - y) * 3 + (1 - z),
};

export interface CubieDef {
  id: string;
  basePosition: Vec3;
  /** Sticker color per outward-facing local face, e.g. stickers.R is only present when x === 1. */
  stickers: Partial<Record<Face, CubeColor>>;
}

export function buildCubies(faces: CubeFaces): CubieDef[] {
  const cubies: CubieDef[] = [];
  for (let x = -1; x <= 1; x++) {
    for (let y = -1; y <= 1; y++) {
      for (let z = -1; z <= 1; z++) {
        if (x === 0 && y === 0 && z === 0) continue;
        const stickers: Partial<Record<Face, CubeColor>> = {};
        if (y === 1) stickers.U = faces.U[faceletIndex.U(x, z)];
        if (y === -1) stickers.D = faces.D[faceletIndex.D(x, z)];
        if (z === 1) stickers.F = faces.F[faceletIndex.F(x, y)];
        if (z === -1) stickers.B = faces.B[faceletIndex.B(x, y)];
        if (x === -1) stickers.L = faces.L[faceletIndex.L(y, z)];
        if (x === 1) stickers.R = faces.R[faceletIndex.R(y, z)];
        cubies.push({ id: `${x}${y}${z}`, basePosition: [x, y, z], stickers });
      }
    }
  }
  return cubies;
}

/** Outward normal axis for each face — also the rotation axis for turning that face. */
export const FACE_AXIS: Record<Face, Vec3> = {
  U: [0, 1, 0],
  D: [0, -1, 0],
  F: [0, 0, 1],
  B: [0, 0, -1],
  L: [-1, 0, 0],
  R: [1, 0, 0],
};

/** Whether a cubie at the given position belongs to the layer that face-letter turns. */
export function isInLayer(face: Face, [x, y, z]: Vec3): boolean {
  switch (face) {
    case "U":
      return y === 1;
    case "D":
      return y === -1;
    case "F":
      return z === 1;
    case "B":
      return z === -1;
    case "L":
      return x === -1;
    case "R":
      return x === 1;
  }
}
