import { checkCubieValidity, decodeCubieState } from "../cube/cubieState";
import { COLORS, FACE_LABEL } from "../cube/constants";
import { CubeColor, CubeFaces, Face, FACES } from "../cube/types";

export interface ValidationIssue {
  message: string;
  /** Faces the user should consider rescanning to fix this issue, if known. */
  suggestFaces?: Face[];
}

export interface ValidationResult {
  valid: boolean;
  issues: ValidationIssue[];
}

/** Every color must appear exactly 9 times across the whole cube. */
function checkColorCounts(faces: CubeFaces): ValidationIssue[] {
  const counts: Record<CubeColor, number> = {
    white: 0,
    yellow: 0,
    red: 0,
    orange: 0,
    blue: 0,
    green: 0,
  };
  const facesByColor: Record<CubeColor, Set<Face>> = {
    white: new Set(),
    yellow: new Set(),
    red: new Set(),
    orange: new Set(),
    blue: new Set(),
    green: new Set(),
  };

  for (const face of FACES) {
    for (const color of faces[face]) {
      counts[color]++;
      facesByColor[color].add(face);
    }
  }

  const issues: ValidationIssue[] = [];
  for (const color of COLORS) {
    if (counts[color] !== 9) {
      const diff = counts[color] - 9;
      issues.push({
        message:
          diff > 0
            ? `Found ${counts[color]} ${color} stickers, but a cube only has 9. Some other color was likely misread as ${color}.`
            : `Found only ${counts[color]} ${color} stickers — a cube needs 9. Some ${color} stickers were likely misread as another color.`,
        suggestFaces: Array.from(facesByColor[color]),
      });
    }
  }
  return issues;
}

/** Each face's center sticker must be a unique color (centers never move relative to each other). */
function checkCenters(faces: CubeFaces): ValidationIssue[] {
  const seen = new Map<CubeColor, Face>();
  const issues: ValidationIssue[] = [];
  for (const face of FACES) {
    const center = faces[face][4];
    const existing = seen.get(center);
    if (existing) {
      issues.push({
        message: `The ${FACE_LABEL[face]} and ${FACE_LABEL[existing]} faces both have a ${center} center. Each face center must be a different color.`,
        suggestFaces: [face, existing],
      });
    } else {
      seen.set(center, face);
    }
  }
  return issues;
}

/**
 * Full validation pipeline, run before handing the cube to the solver:
 * 1. exactly nine stickers of every color
 * 2. six distinct face centers
 * 3. every corner/edge sticker combination forms a real physical piece
 * 4. correct permutation parity, corner orientation, and edge orientation
 */
export function validateCube(faces: CubeFaces): ValidationResult {
  const issues: ValidationIssue[] = [...checkColorCounts(faces), ...checkCenters(faces)];

  if (issues.length > 0) {
    // Count/center problems make piece-level decoding meaningless; report those first.
    return { valid: false, issues };
  }

  const cubieState = decodeCubieState(faces);
  if (!cubieState) {
    return {
      valid: false,
      issues: [
        {
          message:
            "Some stickers don't form a real Rubik's Cube piece (e.g. a corner showing two colors from the same opposite pair). Please recheck each face.",
        },
      ],
    };
  }

  const { valid, reasons } = checkCubieValidity(cubieState);
  if (!valid) {
    return { valid: false, issues: reasons.map((message) => ({ message })) };
  }

  return { valid: true, issues: [] };
}
