import React from "react";
import { StyleSheet, View } from "react-native";
import { theme } from "../theme/theme";
import { useAppSettings } from "../hooks/useAppSettings";

export function ProgressBar({ progress }: { progress: number }) {
  const { colors } = useAppSettings();
  const pct = Math.max(0, Math.min(1, progress));
  return (
    <View style={[styles.track, { backgroundColor: colors.border }]}>
      <View style={[styles.fill, { width: `${pct * 100}%`, backgroundColor: colors.primary }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  track: {
    height: 8,
    borderRadius: theme.radius.pill,
    overflow: "hidden",
  },
  fill: {
    height: "100%",
    borderRadius: theme.radius.pill,
  },
});
