import { CORNER_COLOR_TRIPLES, CORNER_FACELETS, EDGE_COLOR_PAIRS, EDGE_FACELETS } from "./geometry";
import { CubeFaces, CubieState } from "./types";

function permutationParity(perm: number[]): 0 | 1 {
  const visited = new Array(perm.length).fill(false);
  let parity = 0;
  for (let i = 0; i < perm.length; i++) {
    if (visited[i]) continue;
    let len = 0;
    let j = i;
    while (!visited[j]) {
      visited[j] = true;
      j = perm[j];
      len++;
    }
    parity += len - 1;
  }
  return (parity % 2) as 0 | 1;
}

/**
 * Decode 54 scanned facelets into cubie-level permutation/orientation state.
 * Mirrors cubejs's own `Cube.fromString` decoding logic (piece identity from
 * its color set, orientation from which facelet holds the U/D-or-axis color),
 * but implemented independently so validation can run before a solver
 * instance even exists.
 */
export function decodeCubieState(faces: CubeFaces): CubieState | null {
  const cp: number[] = [];
  const co: number[] = [];
  for (const triple of CORNER_FACELETS) {
    const colors = triple.map(([face, idx]) => faces[face][idx]);
    // Orientation reference axis is whichever facelet shows a white/yellow (U/D) sticker.
    let ori = colors.findIndex((c) => c === "white" || c === "yellow");
    if (ori === -1) return null; // no U/D color present: physically impossible piece
    const rotated = [colors[ori], colors[(ori + 1) % 3], colors[(ori + 2) % 3]];
    const pieceIndex = CORNER_COLOR_TRIPLES.findIndex(
      (t) => t[0] === rotated[0] && t[1] === rotated[1] && t[2] === rotated[2]
    );
    if (pieceIndex === -1) return null; // colors don't form a valid physical corner
    cp.push(pieceIndex);
    co.push(ori);
  }

  const ep: number[] = [];
  const eo: number[] = [];
  for (const pair of EDGE_FACELETS) {
    const colors = pair.map(([face, idx]) => faces[face][idx]);
    let matched = false;
    for (let j = 0; j < EDGE_COLOR_PAIRS.length; j++) {
      const [c1, c2] = EDGE_COLOR_PAIRS[j];
      if (colors[0] === c1 && colors[1] === c2) {
        ep.push(j);
        eo.push(0);
        matched = true;
        break;
      }
      if (colors[0] === c2 && colors[1] === c1) {
        ep.push(j);
        eo.push(1);
        matched = true;
        break;
      }
    }
    if (!matched) return null; // colors don't form a valid physical edge
  }

  return { cp, co, ep, eo };
}

export interface CubeValidityResult {
  valid: boolean;
  reasons: string[];
}

/**
 * Standard Rubik's Cube solvability constraints:
 *  - corner permutation parity must equal edge permutation parity
 *  - sum of corner orientations must be divisible by 3
 *  - sum of edge orientations must be divisible by 2
 *  - every piece slot must be filled by a distinct piece (no duplicates)
 */
export function checkCubieValidity(state: CubieState): CubeValidityResult {
  const reasons: string[] = [];

  const cpUnique = new Set(state.cp).size === state.cp.length;
  const epUnique = new Set(state.ep).size === state.ep.length;
  if (!cpUnique) reasons.push("Two corner pieces have the same color combination — a sticker was likely misread.");
  if (!epUnique) reasons.push("Two edge pieces have the same color combination — a sticker was likely misread.");

  if (cpUnique && epUnique) {
    const cornerParity = permutationParity(state.cp);
    const edgeParity = permutationParity(state.ep);
    if (cornerParity !== edgeParity) {
      reasons.push(
        "Corner and edge arrangement parity don't match. This cube state is unsolvable — two stickers were likely swapped during scanning."
      );
    }
  }

  const coSum = state.co.reduce((a, b) => a + b, 0);
  if (coSum % 3 !== 0) {
    reasons.push("Corner orientation is off. One or more corner stickers were likely misread or a corner is twisted in an impossible way.");
  }

  const eoSum = state.eo.reduce((a, b) => a + b, 0);
  if (eoSum % 2 !== 0) {
    reasons.push("Edge orientation is off. One or more edge stickers were likely misread or flipped in an impossible way.");
  }

  return { valid: reasons.length === 0, reasons };
}
