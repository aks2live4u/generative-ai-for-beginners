declare module "cubejs" {
  export interface CubeJSON {
    cp: number[];
    co: number[];
    ep: number[];
    eo: number[];
  }

  class Cube {
    constructor(other?: Cube | CubeJSON);
    static random(): Cube;
    static fromString(facelets: string): Cube;
    static initSolver(): void;
    static asyncInit(workerScript: string, callback: () => void): void;
    static asyncSolve(cube: Cube, callback: (algorithm: string) => void): void;

    move(algorithm: string): void;
    randomize(): void;
    isSolved(): boolean;
    asString(): string;
    toJSON(): CubeJSON;
    solve(maxDepth?: number): string;
  }

  export default Cube;
}
