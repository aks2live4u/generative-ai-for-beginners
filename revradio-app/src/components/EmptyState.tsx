import React from "react";
import { StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useTheme, type, spacing } from "@/theme";

interface EmptyStateProps {
  icon?: keyof typeof Ionicons.glyphMap;
  title: string;
  message?: string;
}

export function EmptyState({ icon = "radio-outline", title, message }: EmptyStateProps) {
  const theme = useTheme();
  return (
    <View style={styles.wrap}>
      <Ionicons name={icon} size={40} color={theme.titaniumGrey} />
      <Text style={[type.h3, { color: theme.textPrimary, marginTop: spacing.sm }]}>{title}</Text>
      {message ? (
        <Text style={[type.body, { color: theme.textSecondary, marginTop: spacing.xs, textAlign: "center" }]}>
          {message}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { alignItems: "center", justifyContent: "center", paddingVertical: spacing.xxl, paddingHorizontal: spacing.lg },
});
