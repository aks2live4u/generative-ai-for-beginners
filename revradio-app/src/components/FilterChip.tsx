import React from "react";
import { Pressable, StyleSheet, Text } from "react-native";
import { useTheme, type, radius, spacing } from "@/theme";

interface FilterChipProps {
  label: string;
  selected: boolean;
  onPress: () => void;
}

/** Toggle chip used across the Tuning filter panel (language, tag, country...). */
export function FilterChip({ label, selected, onPress }: FilterChipProps) {
  const theme = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.chip,
        {
          backgroundColor: selected ? theme.accent : theme.surface,
          borderColor: selected ? theme.accent : theme.border,
        },
      ]}
    >
      <Text style={[type.caption, { color: selected ? "#FFFFFF" : theme.textSecondary }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  chip: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs + 2,
    borderRadius: radius.pill,
    borderWidth: 1,
    marginRight: spacing.xs,
    marginBottom: spacing.xs,
  },
});
