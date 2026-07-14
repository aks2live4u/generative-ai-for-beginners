import { manipulateAsync, SaveFormat } from "expo-image-manipulator";
import { RGB } from "../color/labColor";
import { averagePngColor } from "./pngDecode";

export interface PixelRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

/** Small output size for each cropped sample — big enough to smooth over JPEG noise, small enough to decode fast. */
const SAMPLE_OUTPUT_SIZE = 8;

/**
 * Reads the average color under each sample rect of a captured photo.
 *
 * Approach: crop each sticker's sample region out of the photo and resize it
 * down to a tiny PNG via `expo-image-manipulator` (this does the averaging
 * for us via its resize filter), then decode that PNG ourselves — see
 * pngDecode.ts — to get raw pixel bytes. This avoids depending on native GL
 * texture upload/readback, and the decoder itself is unit-tested against
 * hand-built PNGs covering every PNG filter type (see
 * scripts/test-png-decode.js), so the only untested part on a real device is
 * expo-image-manipulator's own crop/resize, which is a widely-used,
 * actively-maintained Expo module.
 */
export async function samplePixelColors(photoUri: string, _photoHeight: number, rects: PixelRect[]): Promise<RGB[]> {
  const results: RGB[] = [];

  for (const rect of rects) {
    const originX = Math.max(0, Math.round(rect.x));
    const originY = Math.max(0, Math.round(rect.y));
    const width = Math.max(1, Math.round(rect.width));
    const height = Math.max(1, Math.round(rect.height));

    const result = await manipulateAsync(
      photoUri,
      [{ crop: { originX, originY, width, height } }, { resize: { width: SAMPLE_OUTPUT_SIZE, height: SAMPLE_OUTPUT_SIZE } }],
      { base64: true, format: SaveFormat.PNG }
    );

    if (!result.base64) throw new Error("expo-image-manipulator did not return base64 data");
    results.push(averagePngColor(result.base64));
  }

  return results;
}
