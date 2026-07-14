import { useCallback, useRef, useState } from "react";
import { CameraView } from "expo-camera";
import { faceGridSamplePoints } from "./gridGeometry";
import { samplePixelColors } from "./pixelSampler";
import { RGB } from "../color/labColor";

const CHECK_INTERVAL_MS = 350;
const STABLE_FRAMES_REQUIRED = 3;
/** Average per-channel change (0-255 scale) below which two frames count as "the same". */
const STABLE_DIFF_THRESHOLD = 6;
/** Stop silently retrying and surface an error after this many consecutive sampling failures. */
const MAX_CONSECUTIVE_FAILURES = 6;

function averageDiff(a: RGB[], b: RGB[]): number {
  let total = 0;
  for (let i = 0; i < a.length; i++) {
    total += Math.abs(a[i].r - b[i].r) + Math.abs(a[i].g - b[i].g) + Math.abs(a[i].b - b[i].b);
  }
  return total / (a.length * 3);
}

export type ScanStatus = "idle" | "watching" | "stable" | "error";

/**
 * Polls the camera at a coarse grid to detect when the cube face has held
 * still long enough to auto-capture — no shutter button needed. Runs
 * entirely on cheap low-res stills via `takePictureAsync`, since Expo's
 * managed camera API doesn't expose a raw continuous frame stream.
 *
 * Sampling failures are retried silently for a few frames (the camera can
 * genuinely not be ready yet right after mount), but after
 * MAX_CONSECUTIVE_FAILURES in a row this stops and calls `onError` instead
 * of retrying forever with no feedback.
 */
export function useStableFrame(cameraRef: React.RefObject<CameraView>, overlaySizeFraction: number) {
  const [status, setStatus] = useState<ScanStatus>("idle");
  const lastSampleRef = useRef<RGB[] | null>(null);
  const stableCountRef = useRef(0);
  const failureCountRef = useRef(0);
  const runningRef = useRef(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const stop = useCallback(() => {
    runningRef.current = false;
    if (timerRef.current) clearTimeout(timerRef.current);
    setStatus("idle");
    stableCountRef.current = 0;
    failureCountRef.current = 0;
    lastSampleRef.current = null;
  }, []);

  const start = useCallback(
    (onStable: () => void, onError?: (message: string) => void) => {
      runningRef.current = true;
      failureCountRef.current = 0;
      setStatus("watching");

      const tick = async () => {
        if (!runningRef.current || !cameraRef.current) return;
        try {
          const photo = await cameraRef.current.takePictureAsync({
            quality: 0.2,
            skipProcessing: true,
            base64: false,
          });
          if (!photo?.uri || !photo.width || !photo.height) throw new Error("no photo");

          const side = Math.min(photo.width, photo.height) * overlaySizeFraction;
          const originX = (photo.width - side) / 2;
          const originY = (photo.height - side) / 2;
          const rects = faceGridSamplePoints(0.35).map((p) => ({
            x: originX + p.x * side - (p.size * side) / 2,
            y: originY + p.y * side - (p.size * side) / 2,
            width: p.size * side,
            height: p.size * side,
          }));

          const samples = await samplePixelColors(photo.uri, photo.height, rects);
          failureCountRef.current = 0;

          if (lastSampleRef.current && averageDiff(lastSampleRef.current, samples) < STABLE_DIFF_THRESHOLD) {
            stableCountRef.current += 1;
          } else {
            stableCountRef.current = 0;
          }
          lastSampleRef.current = samples;

          if (stableCountRef.current >= STABLE_FRAMES_REQUIRED) {
            runningRef.current = false;
            setStatus("stable");
            onStable();
            return;
          }
          setStatus("watching");
        } catch (e) {
          failureCountRef.current += 1;
          if (failureCountRef.current >= MAX_CONSECUTIVE_FAILURES) {
            runningRef.current = false;
            setStatus("error");
            onError?.(e instanceof Error ? e.message : "Couldn't read colors from the camera.");
            return;
          }
        }
        if (runningRef.current) timerRef.current = setTimeout(tick, CHECK_INTERVAL_MS);
      };

      timerRef.current = setTimeout(tick, CHECK_INTERVAL_MS);
    },
    [cameraRef, overlaySizeFraction]
  );

  return { status, start, stop };
}
