import React from "react";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { Station } from "@/types/station";
import { useTheme, type, radius, spacing } from "@/theme";
import { useFavoritesStore } from "@/store/favoritesStore";

interface StationCardProps {
  station: Station;
  onPress: (station: Station) => void;
  isPlaying?: boolean;
}

/** Station "trim card" — flag/logo, horsepower (signal/bitrate) bar, favorite toggle. Plan §3. */
export function StationCard({ station, onPress, isPlaying }: StationCardProps) {
  const theme = useTheme();
  const isFavorite = useFavoritesStore((s) => s.isFavorite(station.id));
  const toggleFavorite = useFavoritesStore((s) => s.toggleFavorite);

  const horsepowerPct = Math.max(0.08, Math.min(1, station.bitrate / 320));

  return (
    <Pressable
      onPress={() => onPress(station)}
      style={({ pressed }) => [
        styles.card,
        {
          backgroundColor: theme.surface,
          borderColor: isPlaying ? theme.accent : theme.border,
          opacity: pressed ? 0.85 : 1,
        },
      ]}
    >
      <View style={[styles.logoWrap, { backgroundColor: theme.surfaceRaised }]}>
        {station.favicon ? (
          <Image source={{ uri: station.favicon }} style={styles.logo} resizeMode="contain" />
        ) : (
          <Ionicons name="radio" size={26} color={theme.titaniumGrey} />
        )}
      </View>

      <View style={styles.info}>
        <Text style={[type.bodyStrong, { color: theme.textPrimary }]} numberOfLines={1}>
          {station.name}
        </Text>
        <Text style={[type.caption, { color: theme.textSecondary, marginTop: 2 }]} numberOfLines={1}>
          {station.countryCode ? `${station.countryCode} · ` : ""}
          {station.tags.slice(0, 2).join(", ") || station.language}
        </Text>

        <View style={styles.hpRow}>
          <View style={[styles.hpTrack, { backgroundColor: theme.surfaceRaised }]}>
            <View
              style={[
                styles.hpFill,
                { width: `${horsepowerPct * 100}%`, backgroundColor: isPlaying ? theme.accent : theme.accentAlt },
              ]}
            />
          </View>
          <Text style={[type.micro, { color: theme.textMuted, marginLeft: spacing.xs }]}>
            {station.bitrate ? `${station.bitrate}kbps` : station.codec}
          </Text>
          {station.isSecure ? <Ionicons name="lock-closed" size={11} color={theme.textMuted} style={{ marginLeft: 6 }} /> : null}
          {station.isAdFreeReported ? (
            <Text style={[type.micro, { color: theme.success, marginLeft: 6 }]}>AD-FREE</Text>
          ) : null}
        </View>
      </View>

      <View style={styles.actions}>
        {isPlaying ? <Ionicons name="volume-high" size={18} color={theme.accent} style={{ marginBottom: 8 }} /> : null}
        <Pressable hitSlop={10} onPress={() => toggleFavorite(station)}>
          <Ionicons
            name={isFavorite ? "heart" : "heart-outline"}
            size={22}
            color={isFavorite ? theme.accent : theme.titaniumGrey}
          />
        </Pressable>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: radius.md,
    borderWidth: 1,
    padding: spacing.sm,
    marginBottom: spacing.sm,
  },
  logoWrap: {
    width: 52,
    height: 52,
    borderRadius: radius.sm,
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
    marginRight: spacing.sm,
  },
  logo: { width: 36, height: 36 },
  info: { flex: 1 },
  hpRow: { flexDirection: "row", alignItems: "center", marginTop: 8 },
  hpTrack: { flex: 1, height: 4, borderRadius: 2, overflow: "hidden", maxWidth: 90 },
  hpFill: { height: 4, borderRadius: 2 },
  actions: { alignItems: "center", marginLeft: spacing.sm },
});
