import React, { useRef, useState } from "react";
import { StyleSheet, View } from "react-native";
import { CameraView } from "expo-camera";
import { CubeOverlay } from "./CubeOverlay";
import { useStableFrame } from "./useStableFrame";
import { samplePointsToPixelRects } from "./gridGeometry";
import { samplePixelColors } from "./pixelSampler";
import { RGB } from "../color/labColor";
import { useAppSettings } from "../hooks/useAppSettings";

const OVERLAY_SIZE_FRACTION = 0.72;

export interface FaceCaptureResult {
  samples: RGB[];
  photoUri: string;
}

interface Props {
  statusLabel: string;
  rotationHint?: string;
  /** Set to false while the parent is busy classifying/reviewing, to pause auto-capture. */
  active: boolean;
  onCaptured: (result: FaceCaptureResult) => void;
}

export function FaceScannerCamera({ statusLabel, rotationHint, active, onCaptured }: Props) {
  const cameraRef = useRef<CameraView>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { colors, triggerHaptic } = useAppSettings();
  const { status, start, stop } = useStableFrame(cameraRef, OVERLAY_SIZE_FRACTION);

  React.useEffect(() => {
    if (!active) {
      stop();
      return;
    }

    const retryAfterError = (message: string) => {
      triggerHaptic("warning");
      setError(message);
      setBusy(false);
      // Give the user a moment to see the message, then keep scanning instead of getting stuck.
      setTimeout(() => {
        setError(null);
        if (active) start(runCapture, retryAfterError);
      }, 2200);
    };

    const runCapture = async () => {
      setBusy(true);
      setError(null);
      try {
        const photo = await cameraRef.current?.takePictureAsync({ quality: 0.85, skipProcessing: false });
        if (!photo?.uri || !photo.width || !photo.height) throw new Error("Camera did not return a photo");
        const rects = samplePointsToPixelRects(photo.width, photo.height, OVERLAY_SIZE_FRACTION);
        const samples = await samplePixelColors(photo.uri, photo.height, rects);
        triggerHaptic("success");
        onCaptured({ samples, photoUri: photo.uri });
      } catch (e) {
        retryAfterError(e instanceof Error ? e.message : "Couldn't read colors from that photo — trying again.");
      } finally {
        setBusy(false);
      }
    };

    start(runCapture, retryAfterError);
    return () => stop();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active]);

  const statusColor =
    status === "stable" || busy ? colors.success : status === "watching" ? colors.primary : colors.textMuted;

  const label = error ? "Retrying…" : busy ? "Capturing…" : status === "stable" ? "Hold still…" : statusLabel;

  return (
    <View style={styles.container}>
      <CameraView ref={cameraRef} style={StyleSheet.absoluteFill} facing="back" />
      <CubeOverlay
        sizeFraction={OVERLAY_SIZE_FRACTION}
        statusLabel={label}
        statusColor={error ? colors.danger : statusColor}
        rotationHint={error ?? rotationHint}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "black" },
});
