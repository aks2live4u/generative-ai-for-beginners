import React from "react";
import { StyleSheet, View } from "react-native";
import { theme } from "../theme/theme";

export function ProgressBar({ progress }: { progress: number }) {
  const pct = Math.max(0, Math.min(1, progress));
  return (
    <View style={styles.track}>
      <View style={[styles.fill, { width: `${pct * 100}%` }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  track: {
    height: 8,
    borderRadius: theme.radius.pill,
    backgroundColor: theme.colors.border,
    overflow: "hidden",
  },
  fill: {
    height: "100%",
    backgroundColor: theme.colors.primary,
    borderRadius: theme.radius.pill,
  },
});
