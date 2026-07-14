import React, { useEffect, useState } from "react";
import { SafeAreaView, ScrollView, StyleSheet, Text, View } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useCameraPermissions } from "expo-camera";
import { RootStackParamList } from "../navigation/types";
import { FaceScannerCamera } from "../camera/FaceScannerCamera";
import { useCubeScanner } from "../hooks/useCubeScanner";
import { PrimaryButton } from "../components/PrimaryButton";
import { Card } from "../components/Card";
import { FaceGrid } from "../components/FaceGrid";
import { ColorSwatchGrid } from "../components/ColorSwatchGrid";
import { FACE_LABEL } from "../cube/constants";
import { SCAN_STEPS } from "../cube/scanRoles";
import { theme } from "../theme/theme";
import { CubeFaces } from "../cube/types";
import { useAppSettings } from "../hooks/useAppSettings";

type Props = NativeStackScreenProps<RootStackParamList, "Scan">;

export function ScanScreen({ navigation }: Props) {
  const [permission, requestPermission] = useCameraPermissions();
  const scanner = useCubeScanner();
  const [selected, setSelected] = useState<number | null>(null);
  const { colors, triggerHaptic } = useAppSettings();

  useEffect(() => {
    if (permission && !permission.granted) requestPermission();
  }, [permission]);

  useEffect(() => {
    if (scanner.phase === "complete" && scanner.validation?.valid) {
      navigation.replace("Solve", { faces: scanner.faces as CubeFaces });
    }
  }, [scanner.phase, scanner.validation]);

  useEffect(() => {
    setSelected(null);
  }, [scanner.pendingReview?.face]);

  const confirmAndAdvance = () => {
    triggerHaptic("success");
    scanner.confirmReview();
  };

  if (!permission) return <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]} />;

  if (!permission.granted) {
    return (
      <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]}>
        <View style={styles.centered}>
          <Text style={[styles.message, { color: colors.text }]}>
            CubeVision needs camera access to scan your cube.
          </Text>
          <PrimaryButton label="Grant Camera Access" onPress={requestPermission} />
        </View>
      </SafeAreaView>
    );
  }

  if (scanner.phase === "complete" && scanner.validation && !scanner.validation.valid) {
    return (
      <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]}>
        <ScrollView contentContainerStyle={styles.centered}>
          <Text style={[styles.title, { color: colors.text }]}>This cube state isn't solvable</Text>
          {scanner.validation.issues.map((issue, i) => (
            <Card key={i} style={styles.issueCard}>
              <Text style={[styles.issueText, { color: colors.text }]}>{issue.message}</Text>
              {issue.suggestFaces && issue.suggestFaces.length > 0 && (
                <View style={styles.issueActions}>
                  {issue.suggestFaces.map((face) => (
                    <PrimaryButton
                      key={face}
                      label={`Rescan ${FACE_LABEL[face]}`}
                      variant="secondary"
                      onPress={() => scanner.rescanFace(face)}
                    />
                  ))}
                </View>
              )}
            </Card>
          ))}
        </ScrollView>
      </SafeAreaView>
    );
  }

  if (scanner.phase === "reviewing" && scanner.pendingReview) {
    const review = scanner.pendingReview;
    return (
      <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]}>
        <View style={styles.centered}>
          <Text style={[styles.title, { color: colors.text }]}>
            {review.inferred ? "Inferred from the other 5 faces" : `${FACE_LABEL[review.face]} face`}
          </Text>
          <Text style={[styles.subtitle, { color: colors.textMuted }]}>
            Tap any sticker to correct it, then continue.
          </Text>
          <FaceGrid
            colors={review.colors}
            size={220}
            flaggedIndices={review.ambiguousIndices}
            onStickerPress={setSelected}
          />
          {selected !== null && (
            <View style={styles.swatchWrap}>
              <ColorSwatchGrid
                selected={review.colors[selected]}
                onSelect={(c) => scanner.editPendingSticker(selected, c)}
              />
            </View>
          )}
          <PrimaryButton label="Save & Continue" onPress={confirmAndAdvance} style={styles.continueButton} />
        </View>
      </SafeAreaView>
    );
  }

  const step = scanner.currentStep;
  const stepNumber = step ? SCAN_STEPS.findIndex((s) => s.face === step.face) + 1 : 6;

  return (
    <FaceScannerCamera
      active={scanner.phase === "scanning"}
      statusLabel={step ? `Scanning ${step.color} (${stepNumber}/6)` : "Scanning…"}
      rotationHint={step ? `Show the ${step.color} face with ${step.topNeighborColor} at the top edge` : undefined}
      onCaptured={({ samples }) => scanner.submitCapture(samples)}
    />
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  centered: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: theme.spacing(6),
    gap: theme.spacing(4),
  },
  message: { fontSize: 16, textAlign: "center" },
  title: { fontSize: 22, fontWeight: "700", textAlign: "center" },
  subtitle: { fontSize: 14, textAlign: "center" },
  swatchWrap: { marginTop: theme.spacing(2) },
  continueButton: { marginTop: theme.spacing(4), width: "100%" },
  issueCard: { marginBottom: theme.spacing(3), gap: theme.spacing(2) },
  issueText: { fontSize: 15 },
  issueActions: { flexDirection: "row", flexWrap: "wrap", gap: theme.spacing(2) },
});
