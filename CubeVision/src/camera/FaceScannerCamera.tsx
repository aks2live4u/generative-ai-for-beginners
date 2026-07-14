import React, { useRef, useState } from "react";
import { StyleSheet, View } from "react-native";
import { CameraView } from "expo-camera";
import { CubeOverlay } from "./CubeOverlay";
import { useStableFrame } from "./useStableFrame";
import { samplePointsToPixelRects } from "./gridGeometry";
import { samplePixelColors } from "./pixelSampler";
import { RGB } from "../color/labColor";
import { theme } from "../theme/theme";

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
  const { status, start, stop } = useStableFrame(cameraRef, OVERLAY_SIZE_FRACTION);

  React.useEffect(() => {
    if (!active) {
      stop();
      return;
    }
    start(async () => {
      setBusy(true);
      try {
        const photo = await cameraRef.current?.takePictureAsync({ quality: 0.85, skipProcessing: false });
        if (!photo?.uri || !photo.width || !photo.height) return;
        const rects = samplePointsToPixelRects(photo.width, photo.height, OVERLAY_SIZE_FRACTION);
        const samples = await samplePixelColors(photo.uri, photo.height, rects);
        onCaptured({ samples, photoUri: photo.uri });
      } finally {
        setBusy(false);
      }
    });
    return () => stop();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active]);

  const statusColor =
    status === "stable" || busy
      ? theme.colors.success
      : status === "watching"
      ? theme.colors.primary
      : theme.colors.textMuted;

  const label = busy ? "Capturing…" : status === "stable" ? "Hold still…" : statusLabel;

  return (
    <View style={styles.container}>
      <CameraView ref={cameraRef} style={StyleSheet.absoluteFill} facing="back" />
      <CubeOverlay
        sizeFraction={OVERLAY_SIZE_FRACTION}
        statusLabel={label}
        statusColor={statusColor}
        rotationHint={rotationHint}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "black" },
});
