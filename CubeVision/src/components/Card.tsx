import React, { PropsWithChildren } from "react";
import { StyleSheet, View, ViewStyle } from "react-native";
import { theme } from "../theme/theme";
import { useAppSettings } from "../hooks/useAppSettings";

export function Card({ children, style }: PropsWithChildren<{ style?: ViewStyle }>) {
  const { colors } = useAppSettings();
  return <View style={[styles.card, { backgroundColor: colors.card }, style]}>{children}</View>;
}

const styles = StyleSheet.create({
  card: {
    borderRadius: theme.radius.lg,
    padding: theme.spacing(4),
    shadowColor: "#000",
    shadowOpacity: 0.06,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
    elevation: 2,
  },
});
