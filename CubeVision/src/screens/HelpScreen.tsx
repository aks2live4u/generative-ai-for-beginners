import React from "react";
import { SafeAreaView, ScrollView, StyleSheet, Text } from "react-native";
import { Card } from "../components/Card";
import { theme } from "../theme/theme";
import { useAppSettings } from "../hooks/useAppSettings";

const TIPS = [
  {
    title: "Scanning",
    body: "Scan each face in good, even lighting. Hold the cube steady inside the square — CubeVision captures automatically once it stops moving.",
  },
  {
    title: "Orientation",
    body: "For each face, keep the color named in the on-screen hint at the top edge of the face you're showing the camera. This keeps every face aligned so the solver can read the cube correctly.",
  },
  {
    title: "Fixing mistakes",
    body: "After each face, tap any sticker to correct its color before continuing. If the finished scan isn't solvable, CubeVision tells you which face to rescan.",
  },
  {
    title: "Solving",
    body: "Once solved, step through moves one at a time, or use Auto Play. Turn on Voice Guidance in Settings to hear each move spoken aloud.",
  },
];

export function HelpScreen() {
  const { colors } = useAppSettings();
  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={[styles.title, { color: colors.text }]}>Help</Text>
        {TIPS.map((tip) => (
          <Card key={tip.title} style={styles.card}>
            <Text style={[styles.cardTitle, { color: colors.text }]}>{tip.title}</Text>
            <Text style={[styles.cardBody, { color: colors.textMuted }]}>{tip.body}</Text>
          </Card>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: theme.spacing(6), gap: theme.spacing(4) },
  title: { fontSize: 26, fontWeight: "800" },
  card: { gap: theme.spacing(2) },
  cardTitle: { fontSize: 16, fontWeight: "700" },
  cardBody: { fontSize: 14, lineHeight: 20 },
});
