import React from "react";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import { usePlaybackState, State } from "react-native-track-player";
import { useTheme, type, radius, spacing } from "@/theme";
import { usePlayerStore } from "@/store/playerStore";

/** Persistent now-playing strip shown above the tab bar while a station is active. */
export function MiniPlayer() {
  const theme = useTheme();
  const router = useRouter();
  const currentStation = usePlayerStore((s) => s.currentStation);
  const togglePlayPause = usePlayerStore((s) => s.togglePlayPause);
  const isLoading = usePlayerStore((s) => s.isLoading);
  const playback = usePlaybackState();

  if (!currentStation) return null;
  const isPlaying = playback.state === State.Playing;

  return (
    <Pressable
      onPress={() => router.push("/now-playing")}
      style={[styles.bar, { backgroundColor: theme.surfaceRaised, borderTopColor: theme.border }]}
    >
      <View style={[styles.logoWrap, { backgroundColor: theme.surface }]}>
        {currentStation.favicon ? (
          <Image source={{ uri: currentStation.favicon }} style={styles.logo} resizeMode="contain" />
        ) : (
          <Ionicons name="radio" size={18} color={theme.titaniumGrey} />
        )}
      </View>
      <View style={styles.info}>
        <Text style={[type.bodyStrong, { color: theme.textPrimary }]} numberOfLines={1}>
          {currentStation.name}
        </Text>
        <Text style={[type.micro, { color: theme.accentAlt }]}>{isLoading ? "TUNING…" : isPlaying ? "ON AIR" : "PAUSED"}</Text>
      </View>
      <Pressable
        hitSlop={12}
        onPress={(e) => {
          e.stopPropagation();
          togglePlayPause();
        }}
        style={[styles.playButton, { backgroundColor: theme.accent }]}
      >
        <Ionicons name={isPlaying ? "pause" : "play"} size={18} color="#FFFFFF" />
      </Pressable>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  logoWrap: {
    width: 36,
    height: 36,
    borderRadius: radius.sm,
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
    marginRight: spacing.sm,
  },
  logo: { width: 26, height: 26 },
  info: { flex: 1 },
  playButton: {
    width: 34,
    height: 34,
    borderRadius: radius.pill,
    alignItems: "center",
    justifyContent: "center",
  },
});
