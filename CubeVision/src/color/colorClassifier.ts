import { COLOR_HEX, COLORS } from "../cube/constants";
import { CubeColor, FaceColors } from "../cube/types";
import { hexToRgb, labDistance, LabColor, rgbToLab, RGB } from "./labColor";

const DEFAULT_REFERENCE_LAB: Record<CubeColor, LabColor> = Object.fromEntries(
  COLORS.map((color) => [color, rgbToLab(hexToRgb(COLOR_HEX[color]))])
) as Record<CubeColor, LabColor>;

export interface ClassifiedSticker {
  color: CubeColor;
  /** Distance to the matched reference centroid — lower is more confident. */
  distance: number;
  /** True when the sample is roughly equidistant between two colors and should be flagged for user review. */
  ambiguous: boolean;
}

/**
 * Learns per-lighting-condition reference colors as the user confirms faces,
 * so later faces classify more accurately than the first one. Centroids
 * start at fixed swatch values and drift toward what the camera actually
 * saw, via a simple exponential moving average — this is what makes
 * detection "lighting independent" in practice rather than only in theory.
 */
export class ColorProfile {
  private centroids: Record<CubeColor, LabColor>;
  private samplesSeen: Record<CubeColor, number>;

  constructor() {
    this.centroids = { ...DEFAULT_REFERENCE_LAB };
    this.samplesSeen = Object.fromEntries(COLORS.map((c) => [c, 0])) as Record<CubeColor, number>;
  }

  classify(rgb: RGB): ClassifiedSticker {
    const lab = rgbToLab(rgb);
    const distances = COLORS.map((color) => ({ color, distance: labDistance(lab, this.centroids[color]) })).sort(
      (a, b) => a.distance - b.distance
    );
    const [best, second] = distances;
    const ambiguous = second !== undefined && second.distance - best.distance < 6;
    return { color: best.color, distance: best.distance, ambiguous };
  }

  classifyFace(samples: RGB[]): ClassifiedSticker[] {
    return samples.map((s) => this.classify(s));
  }

  /** Call once the user has confirmed (or corrected) a face's 9 colors, to refine future classification. */
  learnFromConfirmedFace(samples: RGB[], confirmedColors: FaceColors) {
    const alpha = 0.35; // weight given to the new observation
    samples.forEach((sample, i) => {
      const color = confirmedColors[i];
      const lab = rgbToLab(sample);
      const current = this.centroids[color];
      this.centroids[color] = {
        L: current.L * (1 - alpha) + lab.L * alpha,
        a: current.a * (1 - alpha) + lab.a * alpha,
        b: current.b * (1 - alpha) + lab.b * alpha,
      };
      this.samplesSeen[color] += samples.length;
    });
  }

  reset() {
    this.centroids = { ...DEFAULT_REFERENCE_LAB };
    this.samplesSeen = Object.fromEntries(COLORS.map((c) => [c, 0])) as Record<CubeColor, number>;
  }
}
