import React from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import { useTheme, type, spacing } from "@/theme";

interface ScreenHeaderProps {
  title: string;
  subtitle?: string;
  showBack?: boolean;
  rightIcon?: keyof typeof Ionicons.glyphMap;
  onRightPress?: () => void;
}

export function ScreenHeader({ title, subtitle, showBack, rightIcon, onRightPress }: ScreenHeaderProps) {
  const theme = useTheme();
  const router = useRouter();
  return (
    <View style={styles.row}>
      <View style={styles.left}>
        {showBack ? (
          <Pressable hitSlop={12} onPress={() => router.back()} style={styles.backBtn}>
            <Ionicons name="chevron-back" size={24} color={theme.textPrimary} />
          </Pressable>
        ) : null}
        <View>
          <Text style={[type.h1, { color: theme.textPrimary }]}>{title}</Text>
          {subtitle ? <Text style={[type.caption, { color: theme.textMuted, marginTop: 2 }]}>{subtitle}</Text> : null}
        </View>
      </View>
      {rightIcon ? (
        <Pressable hitSlop={12} onPress={onRightPress}>
          <Ionicons name={rightIcon} size={24} color={theme.textPrimary} />
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: spacing.md,
    paddingTop: spacing.sm,
    paddingBottom: spacing.md,
  },
  left: { flexDirection: "row", alignItems: "center" },
  backBtn: { marginRight: spacing.sm },
});
