import pako from "pako";
import { fromByteArray, toByteArray } from "base64-js";
import { RGB } from "../color/labColor";

const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];

interface Ihdr {
  width: number;
  height: number;
  bitDepth: number;
  colorType: number;
}

function readUint32(bytes: Uint8Array, offset: number): number {
  return (bytes[offset] << 24) | (bytes[offset + 1] << 16) | (bytes[offset + 2] << 8) | bytes[offset + 3];
}

function paeth(a: number, b: number, c: number): number {
  const p = a + b - c;
  const pa = Math.abs(p - a);
  const pb = Math.abs(p - b);
  const pc = Math.abs(p - c);
  if (pa <= pb && pa <= pc) return a;
  if (pb <= pc) return b;
  return c;
}

/**
 * Minimal PNG decoder for the narrow case this app needs: non-interlaced,
 * 8-bit-per-channel RGB/RGBA/grayscale images (exactly what
 * expo-image-manipulator produces when asked to crop+resize a photo and
 * save as PNG). Not a general-purpose PNG library — deliberately small and
 * fully unit-tested (see scripts/test-png-decode.js) rather than pulling in
 * a native image-decoding dependency.
 */
export function decodePng(base64: string): { width: number; height: number; channels: number; data: Uint8Array } {
  const bytes = toByteArray(base64);

  for (let i = 0; i < PNG_SIGNATURE.length; i++) {
    if (bytes[i] !== PNG_SIGNATURE[i]) throw new Error("Not a PNG file");
  }

  let offset = 8;
  let ihdr: Ihdr | null = null;
  const idatChunks: Uint8Array[] = [];

  while (offset < bytes.length) {
    const length = readUint32(bytes, offset);
    const type = String.fromCharCode(bytes[offset + 4], bytes[offset + 5], bytes[offset + 6], bytes[offset + 7]);
    const dataStart = offset + 8;

    if (type === "IHDR") {
      ihdr = {
        width: readUint32(bytes, dataStart),
        height: readUint32(bytes, dataStart + 4),
        bitDepth: bytes[dataStart + 8],
        colorType: bytes[dataStart + 9],
      };
    } else if (type === "IDAT") {
      idatChunks.push(bytes.subarray(dataStart, dataStart + length));
    } else if (type === "IEND") {
      break;
    }

    offset = dataStart + length + 4; // skip CRC
  }

  if (!ihdr) throw new Error("Missing IHDR chunk");
  if (ihdr.bitDepth !== 8) throw new Error(`Unsupported PNG bit depth: ${ihdr.bitDepth}`);

  const channelsByColorType: Record<number, number> = { 0: 1, 2: 3, 4: 2, 6: 4 };
  const channels = channelsByColorType[ihdr.colorType];
  if (!channels) throw new Error(`Unsupported PNG color type: ${ihdr.colorType}`);

  const totalIdatLength = idatChunks.reduce((sum, c) => sum + c.length, 0);
  const compressed = new Uint8Array(totalIdatLength);
  let pos = 0;
  for (const chunk of idatChunks) {
    compressed.set(chunk, pos);
    pos += chunk.length;
  }

  const raw = pako.inflate(compressed);

  const { width, height } = ihdr;
  const bytesPerPixel = channels; // bitDepth 8
  const stride = width * bytesPerPixel;
  const out = new Uint8Array(width * height * bytesPerPixel);

  let rawOffset = 0;
  for (let y = 0; y < height; y++) {
    const filterType = raw[rawOffset];
    rawOffset += 1;
    const rowStart = y * stride;
    const prevRowStart = (y - 1) * stride;

    for (let x = 0; x < stride; x++) {
      const rawByte = raw[rawOffset + x];
      const a = x >= bytesPerPixel ? out[rowStart + x - bytesPerPixel] : 0;
      const b = y > 0 ? out[prevRowStart + x] : 0;
      const c = y > 0 && x >= bytesPerPixel ? out[prevRowStart + x - bytesPerPixel] : 0;

      let value: number;
      switch (filterType) {
        case 0:
          value = rawByte;
          break;
        case 1:
          value = rawByte + a;
          break;
        case 2:
          value = rawByte + b;
          break;
        case 3:
          value = rawByte + Math.floor((a + b) / 2);
          break;
        case 4:
          value = rawByte + paeth(a, b, c);
          break;
        default:
          throw new Error(`Unsupported PNG filter type: ${filterType}`);
      }
      out[rowStart + x] = value & 0xff;
    }
    rawOffset += stride;
  }

  return { width, height, channels, data: out };
}

/** Average RGB across every pixel of a (typically tiny, cropped) PNG — robust to noise and to the
 * exact output size expo-image-manipulator's resize actually produces. */
export function averagePngColor(base64: string): RGB {
  const { width, height, channels, data } = decodePng(base64);
  let r = 0;
  let g = 0;
  let b = 0;
  const pixelCount = width * height;
  if (pixelCount === 0) throw new Error("Empty image");

  for (let i = 0; i < pixelCount; i++) {
    const offset = i * channels;
    if (channels === 1 || channels === 2) {
      const gray = data[offset];
      r += gray;
      g += gray;
      b += gray;
    } else {
      r += data[offset];
      g += data[offset + 1];
      b += data[offset + 2];
    }
  }

  return { r: Math.round(r / pixelCount), g: Math.round(g / pixelCount), b: Math.round(b / pixelCount) };
}

export const __internal = { fromByteArray, toByteArray };
