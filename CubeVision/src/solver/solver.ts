import Cube from "cubejs";
import { facesToFaceletString, faceletStringToFaces } from "../cube/geometry";
import { CubeFaces } from "../cube/types";

let solverReady: Promise<void> | null = null;

/**
 * cubejs's pruning-table generation (`Cube.initSolver`) takes a few seconds
 * on a desktop and can take meaningfully longer on a phone's JS engine —
 * there is no native worker thread available in Hermes/React Native. Call
 * this once, early (e.g. on the Home screen), and show a loading state
 * while it resolves so the first scan-to-solve flow doesn't stall on a
 * blank screen.
 *
 * Important: the `setTimeout` callback below runs outside the Promise
 * executor's synchronous scope, so a thrown error inside it does NOT
 * automatically reject the promise — without the explicit try/catch here,
 * any failure in `Cube.initSolver()` would leave this promise pending
 * forever with no error and no way to retry.
 */
export function initSolver(): Promise<void> {
  if (!solverReady) {
    solverReady = new Promise<void>((resolve, reject) => {
      // Yield a tick so any pending UI update (e.g. a spinner) can paint first.
      setTimeout(() => {
        try {
          Cube.initSolver();
          resolve();
        } catch (e) {
          reject(e);
        }
      }, 0);
    }).catch((e) => {
      // Don't cache a failed attempt — let the caller retry.
      solverReady = null;
      throw e;
    });
  }
  return solverReady;
}

export interface SolveResult {
  moves: string[];
  moveCount: number;
}

export async function solveCube(faces: CubeFaces): Promise<SolveResult> {
  await initSolver();
  const facelets = facesToFaceletString(faces);
  const cube = Cube.fromString(facelets);
  const algorithm = cube.solve();
  const moves = algorithm.trim().length > 0 ? algorithm.trim().split(/\s+/) : [];
  return { moves, moveCount: moves.length };
}

export function isCubeSolved(faces: CubeFaces): boolean {
  const facelets = facesToFaceletString(faces);
  return Cube.fromString(facelets).isSolved();
}

/** Generates a random, guaranteed-scrambled cube state for Practice Mode / the scramble generator. */
export async function generateScramble(): Promise<{ faces: CubeFaces; algorithm: string[] }> {
  await initSolver();
  const cube = Cube.random();
  const solution = cube.solve();
  return {
    faces: faceletStringToFaces(cube.asString()),
    algorithm: solution.trim().length > 0 ? solution.trim().split(/\s+/) : [],
  };
}
