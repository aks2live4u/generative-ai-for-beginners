import { useCallback, useState } from "react";
import { solveCube } from "../solver/solver";
import { CubeFaces } from "../cube/types";

export type SolverStatus = "idle" | "solving" | "solved" | "error";

export function useSolver() {
  const [status, setStatus] = useState<SolverStatus>("idle");
  const [moves, setMoves] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  const solve = useCallback(async (faces: CubeFaces) => {
    setStatus("solving");
    setError(null);
    try {
      const result = await solveCube(faces);
      setMoves(result.moves);
      setStatus("solved");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not solve this cube.");
      setStatus("error");
    }
  }, []);

  const reset = useCallback(() => {
    setStatus("idle");
    setMoves([]);
    setError(null);
  }, []);

  return { status, moves, error, solve, reset };
}
