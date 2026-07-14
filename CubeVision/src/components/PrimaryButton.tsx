import React from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, ViewStyle } from "react-native";
import { theme } from "../theme/theme";

interface Props {
  label: string;
  onPress: () => void;
  disabled?: boolean;
  loading?: boolean;
  variant?: "primary" | "secondary" | "ghost";
  style?: ViewStyle;
}

export function PrimaryButton({ label, onPress, disabled, loading, variant = "primary", style }: Props) {
  const isPrimary = variant === "primary";
  const isSecondary = variant === "secondary";

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        styles.base,
        isPrimary && styles.primary,
        isSecondary && styles.secondary,
        variant === "ghost" && styles.ghost,
        (disabled || loading) && styles.disabled,
        pressed && !disabled && styles.pressed,
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={isPrimary ? "white" : theme.colors.primary} />
      ) : (
        <Text
          style={[
            styles.label,
            isPrimary && styles.labelPrimary,
            (isSecondary || variant === "ghost") && styles.labelSecondary,
          ]}
        >
          {label}
        </Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    borderRadius: theme.radius.pill,
    paddingVertical: 16,
    paddingHorizontal: 24,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 56,
  },
  primary: {
    backgroundColor: theme.colors.primary,
  },
  secondary: {
    backgroundColor: theme.colors.surface,
    borderWidth: 1,
    borderColor: theme.colors.border,
  },
  ghost: {
    backgroundColor: "transparent",
  },
  disabled: {
    opacity: 0.5,
  },
  pressed: {
    opacity: 0.85,
  },
  label: {
    fontSize: 17,
    fontWeight: "600",
  },
  labelPrimary: {
    color: "white",
  },
  labelSecondary: {
    color: theme.colors.primary,
  },
});
