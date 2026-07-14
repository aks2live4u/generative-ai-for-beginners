import { Asset } from "expo-asset";
import { ExpoWebGLRenderingContext, GLView } from "expo-gl";
import { RGB } from "../color/labColor";

let glContextPromise: Promise<ExpoWebGLRenderingContext> | null = null;

/**
 * A headless (never-rendered) GL context used purely as a pixel-reading
 * surface: we upload each captured photo as a texture and use
 * `readPixels` to pull out the exact color under each sticker's sample
 * point. This avoids needing a native image-decoding module — expo-gl
 * already knows how to decode a local photo URI into a texture.
 *
 * NOTE: this is the one piece of CubeVision that most needs verification on
 * a real device/camera — see the README's "Known limitations" section.
 */
async function getOffscreenGL(): Promise<ExpoWebGLRenderingContext> {
  if (!glContextPromise) {
    glContextPromise = GLView.createContextAsync();
  }
  return glContextPromise;
}

export interface PixelRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export async function samplePixelColors(
  photoUri: string,
  photoHeight: number,
  rects: PixelRect[]
): Promise<RGB[]> {
  const gl = await getOffscreenGL();

  const asset = Asset.fromURI(photoUri);
  await asset.downloadAsync();

  const texture = gl.createTexture();
  gl.bindTexture(gl.TEXTURE_2D, texture);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  // expo-gl decodes local photo URIs directly when given an asset-like object.
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, asset as unknown as TexImageSource);

  const framebuffer = gl.createFramebuffer();
  gl.bindFramebuffer(gl.FRAMEBUFFER, framebuffer);
  gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, texture, 0);

  const pixel = new Uint8Array(4);
  const results: RGB[] = rects.map((rect) => {
    const cx = Math.round(rect.x + rect.width / 2);
    // GL reads from the bottom-left origin; photo rects are top-left origin.
    const cy = Math.round(photoHeight - (rect.y + rect.height / 2));
    gl.readPixels(cx, cy, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixel);
    return { r: pixel[0], g: pixel[1], b: pixel[2] };
  });

  gl.bindFramebuffer(gl.FRAMEBUFFER, null);
  gl.deleteFramebuffer(framebuffer);
  gl.deleteTexture(texture);

  return results;
}
