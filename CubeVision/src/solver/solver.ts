import Cube from "cubejs";
import { facesToFaceletString, faceletStringToFaces } from "../cube/geometry";
import { CubeFaces } from "../cube/types";

let solverReady: Promise<void> | null = null;

/**
 * cubejs's pruning-table generation (`Cube.initSolver`) takes a few seconds
 * and runs synchronously on the JS thread — there is no native worker
 * thread available in Hermes/React Native. Call this once, early (e.g. on
 * the Home screen), and show a loading state while it resolves so the first
 * scan-to-solve flow doesn't stall on a blank screen.
 */
export function initSolver(): Promise<void> {
  if (!solverReady) {
    solverReady = new Promise((resolve) => {
      // Yield a tick so any pending UI update (e.g. a spinner) can paint first.
      setTimeout(() => {
        Cube.initSolver();
        resolve();
      }, 0);
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
