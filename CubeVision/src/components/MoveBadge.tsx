import React from "react";
import { StyleSheet, Text, View } from "react-native";
import { theme } from "../theme/theme";

export function MoveBadge({ move }: { move: string | undefined }) {
  return (
    <View style={styles.badge}>
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
    backgroundColor: theme.colors.primary,
    alignItems: "center",
  },
  text: {
    color: "white",
    fontSize: 32,
    fontWeight: "700",
  },
});
