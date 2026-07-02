import React from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { GENRE_SHOWROOMS, GenreShowroom as GenreShowroomType } from "@/constants/genres";
import { useTheme, type, radius, spacing } from "@/theme";

interface GenreShowroomProps {
  onSelect: (genre: GenreShowroomType) => void;
}

/** Horizontal-scroll "genre garage" — stations grouped into visual showrooms. Plan §4.1. */
export function GenreShowroom({ onSelect }: GenreShowroomProps) {
  const theme = useTheme();
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.row}>
      {GENRE_SHOWROOMS.map((genre) => (
        <Pressable
          key={genre.id}
          onPress={() => onSelect(genre)}
          style={({ pressed }) => [
            styles.tile,
            { backgroundColor: theme.surface, borderColor: theme.border, opacity: pressed ? 0.8 : 1 },
          ]}
        >
          <View style={[styles.iconWrap, { backgroundColor: theme.surfaceRaised }]}>
            <Ionicons name={genre.icon as any} size={20} color={theme.accentAlt} />
          </View>
          <Text style={[type.caption, { color: theme.textPrimary, marginTop: spacing.xs }]}>{genre.label}</Text>
        </Pressable>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  row: { paddingVertical: spacing.xs, gap: spacing.sm },
  tile: {
    width: 84,
    borderRadius: radius.md,
    borderWidth: 1,
    paddingVertical: spacing.sm,
    alignItems: "center",
    marginRight: spacing.sm,
  },
  iconWrap: { width: 40, height: 40, borderRadius: radius.pill, alignItems: "center", justifyContent: "center" },
});
