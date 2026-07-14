export interface NormRect {
  /** All values normalized 0-1 relative to the camera preview / captured photo. */
  x: number;
  y: number;
  size: number;
}

/**
 * The 9 sticker sample points for a centered square overlay, as normalized
 * (0-1) coordinates within that square. Row-major, matching FaceColors
 * ordering (index 0 = top-left, 4 = center, 8 = bottom-right).
 *
 * Samples are taken from small regions at each cell's center and inset from
 * cell edges, so grout lines/glare at sticker borders don't get sampled.
 */
export function faceGridSamplePoints(cellInset = 0.28): NormRect[] {
  const points: NormRect[] = [];
  for (let row = 0; row < 3; row++) {
    for (let col = 0; col < 3; col++) {
      const cellSize = 1 / 3;
      points.push({
        x: col * cellSize + cellSize / 2,
        y: row * cellSize + cellSize / 2,
        size: cellSize * (1 - cellInset * 2),
      });
    }
  }
  return points;
}

/** Maps the overlay square (centered, given side length as a fraction of the
 * shorter screen dimension) into absolute pixel rects on a photo of given size. */
export function samplePointsToPixelRects(
  photoWidth: number,
  photoHeight: number,
  overlaySizeFraction: number
): { x: number; y: number; width: number; height: number }[] {
  const side = Math.min(photoWidth, photoHeight) * overlaySizeFraction;
  const originX = (photoWidth - side) / 2;
  const originY = (photoHeight - side) / 2;

  return faceGridSamplePoints().map((p) => ({
    x: originX + p.x * side - (p.size * side) / 2,
    y: originY + p.y * side - (p.size * side) / 2,
    width: p.size * side,
    height: p.size * side,
  }));
}
