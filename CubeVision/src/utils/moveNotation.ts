import { Face } from "../cube/types";

export interface ParsedMove {
  raw: string;
  face: Face;
  /** Number of clockwise quarter turns: 1, 2, or 3 (3 == counterclockwise quarter turn). */
  turns: 1 | 2 | 3;
}

export function parseMove(move: string): ParsedMove {
  const face = move[0] as Face;
  if (move.endsWith("2")) return { raw: move, face, turns: 2 };
  if (move.endsWith("'")) return { raw: move, face, turns: 3 };
  return { raw: move, face, turns: 1 };
}

export function parseAlgorithm(algorithm: string): ParsedMove[] {
  return algorithm
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map(parseMove);
}

/** The move that undoes `move`, e.g. R -> R', R' -> R, R2 -> R2. */
export function invertMove(move: string): string {
  if (move.endsWith("2")) return move;
  if (move.endsWith("'")) return move.slice(0, -1);
  return `${move}'`;
}

/** Signed clockwise-quarter-turn count in [-1, 2] range used for rotation math: 1 = CW, -1 = CCW, 2 = half turn. */
export function turnSign(turns: 1 | 2 | 3): number {
  if (turns === 1) return 1;
  if (turns === 3) return -1;
  return 2;
}
