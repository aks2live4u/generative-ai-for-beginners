import React from "react";
import { StyleSheet, Text, View } from "react-native";
import { theme } from "../theme/theme";
import { useAppSettings } from "../hooks/useAppSettings";

export function MoveBadge({ move }: { move: string | undefined }) {
  const { colors } = useAppSettings();
  return (
    <View style={[styles.badge, { backgroundColor: colors.primary }]}>
      <Text style={styles.text}>{move ?? "—"}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    minWidth: 96,
    paddingVertical: 14,
    paddingHorizontal: 20,
    borderRadius: theme.radius.md,
    alignItems: "center",
  },
  text: {
    color: "white",
    fontSize: 32,
    fontWeight: "700",
  },
});
