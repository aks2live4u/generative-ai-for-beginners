import React, { useState } from "react";
import { Alert, Pressable, ScrollView, Share, StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { usePlaybackState, State } from "react-native-track-player";
import { useTheme, type, radius, spacing } from "@/theme";
import { TachometerDial } from "@/components/TachometerDial";
import { EmptyState } from "@/components/EmptyState";
import { EQ_PRESETS } from "@/constants/genres";
import { usePlayerStore } from "@/store/playerStore";
import { useFavoritesStore } from "@/store/favoritesStore";
import { useSettingsStore } from "@/store/settingsStore";
import { voteForStation } from "@/api/radioBrowser";
import { formatCountdown } from "@/utils/format";

const SLEEP_OPTIONS = [15, 30, 45, 60];

/** Now Playing — full-screen "dashboard" view with RPM-style gauge and HUD strip. Plan §3, §4.4. */
export default function NowPlayingScreen() {
  const theme = useTheme();
  const router = useRouter();
  const station = usePlayerStore((s) => s.currentStation);
  const togglePlayPause = usePlayerStore((s) => s.togglePlayPause);
  const stop = usePlayerStore((s) => s.stop);
  const sleepTimerEndsAt = usePlayerStore((s) => s.sleepTimerEndsAt);
  const setSleepTimer = usePlayerStore((s) => s.setSleepTimer);
  const clearSleepTimer = usePlayerStore((s) => s.clearSleepTimer);
  const errorMessage = usePlayerStore((s) => s.errorMessage);
  const isLoading = usePlayerStore((s) => s.isLoading);

  const isFavorite = useFavoritesStore((s) => (station ? s.isFavorite(station.id) : false));
  const toggleFavorite = useFavoritesStore((s) => s.toggleFavorite);
  const reportAdFree = useFavoritesStore((s) => s.reportAdFree);
  const reportBroken = useFavoritesStore((s) => s.reportBroken);

  const equalizerPreset = useSettingsStore((s) => s.equalizerPreset);
  const setEqualizerPreset = useSettingsStore((s) => s.setEqualizerPreset);

  const playback = usePlaybackState();
  const [voted, setVoted] = useState(false);
  const isPlaying = playback.state === State.Playing;
  const isBuffering = playback.state === State.Buffering || playback.state === State.Connecting || isLoading;

  if (!station) {
    return (
      <View style={[styles.container, { backgroundColor: theme.background, justifyContent: "center" }]}>
        <EmptyState icon="radio-outline" title="Nothing playing" message="Pick a station from Home, Search, or your Garage." />
        <Pressable onPress={() => router.back()} style={[styles.closeFallback, { backgroundColor: theme.accent }]}>
          <Text style={[type.bodyStrong, { color: "#FFFFFF" }]}>Back</Text>
        </Pressable>
      </View>
    );
  }

  async function handleShare() {
    try {
      await Share.share({
        message: `Tuning into ${station!.name} on RevRadio — revradio://station/${station!.id}`,
      });
    } catch {
      // user cancelled — no-op
    }
  }

  async function handleVote() {
    const ok = await voteForStation(station!.id);
    if (ok) setVoted(true);
  }

  function handleReportBroken() {
    reportBroken(station!.id);
    Alert.alert("Reported", "Thanks — this helps deprioritize dead streams for other drivers.");
  }

  const countdown = formatCountdown(sleepTimerEndsAt);

  return (
    <ScrollView style={{ backgroundColor: theme.background }} contentContainerStyle={styles.container}>
      <View style={styles.topRow}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="chevron-down" size={26} color={theme.textPrimary} />
        </Pressable>
        <Text style={[type.micro, { color: theme.textMuted }]}>NOW PLAYING</Text>
        <Pressable onPress={handleShare} hitSlop={12}>
          <Ionicons name="share-outline" size={22} color={theme.textPrimary} />
        </Pressable>
      </View>

      <View style={styles.dialWrap}>
        <TachometerDial
          size={260}
          active={isPlaying}
          label={isBuffering ? "…" : isPlaying ? "ON AIR" : "PAUSED"}
          sublabel={station.codec + (station.bitrate ? ` · ${station.bitrate}kbps` : "")}
        />
      </View>

      {/* HUD strip */}
      <View style={[styles.hud, { backgroundColor: theme.surface, borderColor: theme.border }]}>
        <Text style={[type.h1, { color: theme.textPrimary, textAlign: "center" }]} numberOfLines={2}>
          {station.name}
        </Text>
        <Text style={[type.body, { color: theme.textSecondary, textAlign: "center", marginTop: 4 }]}>
          {station.country} · {station.language}
        </Text>
        {errorMessage ? (
          <Text style={[type.caption, { color: theme.danger, textAlign: "center", marginTop: spacing.sm }]}>{errorMessage}</Text>
        ) : null}
        <View style={styles.tagRow}>
          {station.tags.slice(0, 4).map((t) => (
            <View key={t} style={[styles.tagChip, { backgroundColor: theme.surfaceRaised }]}>
              <Text style={[type.micro, { color: theme.textSecondary }]}>{t}</Text>
            </View>
          ))}
        </View>
      </View>

      <View style={styles.controlsRow}>
        <ControlButton icon={isFavorite ? "heart" : "heart-outline"} active={isFavorite} onPress={() => toggleFavorite(station)} />
        <Pressable onPress={togglePlayPause} style={[styles.playBtn, { backgroundColor: theme.accent }]}>
          <Ionicons name={isPlaying ? "pause" : "play"} size={32} color="#FFFFFF" />
        </Pressable>
        <ControlButton icon="thumbs-up-outline" active={voted} onPress={handleVote} />
      </View>

      <Pressable onPress={stop} style={styles.stopRow}>
        <Ionicons name="stop-circle-outline" size={18} color={theme.textMuted} />
        <Text style={[type.caption, { color: theme.textMuted, marginLeft: 6 }]}>Stop playback</Text>
      </Pressable>

      {/* Pit Stop Timer */}
      <SectionCard title="Pit Stop Timer" theme={theme}>
        <View style={styles.chipRow}>
          {SLEEP_OPTIONS.map((m) => (
            <Pressable key={m} onPress={() => setSleepTimer(m)} style={[styles.smallChip, { backgroundColor: theme.surfaceRaised }]}>
              <Text style={[type.caption, { color: theme.textPrimary }]}>{m}m</Text>
            </Pressable>
          ))}
          {countdown ? (
            <Pressable onPress={clearSleepTimer} style={[styles.smallChip, { backgroundColor: theme.accent }]}>
              <Text style={[type.caption, { color: "#FFFFFF" }]}>Cancel ({countdown})</Text>
            </Pressable>
          ) : null}
        </View>
      </SectionCard>

      {/* Equalizer */}
      <SectionCard title="Equalizer" theme={theme}>
        <View style={styles.chipRow}>
          {EQ_PRESETS.map((preset) => (
            <Pressable
              key={preset.id}
              onPress={() => setEqualizerPreset(preset.id)}
              style={[
                styles.smallChip,
                { backgroundColor: equalizerPreset === preset.id ? theme.accent : theme.surfaceRaised },
              ]}
            >
              <Text style={[type.caption, { color: equalizerPreset === preset.id ? "#FFFFFF" : theme.textPrimary }]}>{preset.label}</Text>
            </Pressable>
          ))}
        </View>
      </SectionCard>

      {/* Ad-free & broken report */}
      <View style={styles.footerActions}>
        <Pressable onPress={() => reportAdFree(station.id)} style={styles.footerAction}>
          <Ionicons name="checkmark-done-outline" size={16} color={theme.success} />
          <Text style={[type.caption, { color: theme.success, marginLeft: 6 }]}>Tag as ad-free</Text>
        </Pressable>
        <Pressable onPress={handleReportBroken} style={styles.footerAction}>
          <Ionicons name="warning-outline" size={16} color={theme.danger} />
          <Text style={[type.caption, { color: theme.danger, marginLeft: 6 }]}>Report broken stream</Text>
        </Pressable>
      </View>
    </ScrollView>
  );
}

function ControlButton({ icon, active, onPress }: { icon: keyof typeof Ionicons.glyphMap; active?: boolean; onPress: () => void }) {
  const theme = useTheme();
  return (
    <Pressable onPress={onPress} style={[styles.controlBtn, { backgroundColor: theme.surface, borderColor: theme.border }]}>
      <Ionicons name={icon} size={22} color={active ? theme.accent : theme.titaniumGrey} />
    </Pressable>
  );
}

function SectionCard({ title, theme, children }: { title: string; theme: ReturnType<typeof useTheme>; children: React.ReactNode }) {
  return (
    <View style={[styles.card, { backgroundColor: theme.surface, borderColor: theme.border }]}>
      <Text style={[type.h3, { color: theme.textPrimary, marginBottom: spacing.sm }]}>{title}</Text>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { paddingHorizontal: spacing.lg, paddingTop: 56, paddingBottom: spacing.xxl },
  topRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  dialWrap: { alignItems: "center", marginTop: spacing.lg },
  hud: { borderRadius: radius.lg, borderWidth: 1, padding: spacing.lg, marginTop: spacing.lg },
  tagRow: { flexDirection: "row", flexWrap: "wrap", justifyContent: "center", marginTop: spacing.sm, gap: 6 },
  tagChip: { paddingHorizontal: spacing.sm, paddingVertical: 4, borderRadius: radius.pill },
  controlsRow: { flexDirection: "row", alignItems: "center", justifyContent: "center", marginTop: spacing.xl, gap: spacing.lg },
  controlBtn: { width: 52, height: 52, borderRadius: radius.pill, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  playBtn: { width: 76, height: 76, borderRadius: radius.pill, alignItems: "center", justifyContent: "center" },
  stopRow: { flexDirection: "row", alignItems: "center", justifyContent: "center", marginTop: spacing.md },
  card: { borderRadius: radius.md, borderWidth: 1, padding: spacing.md, marginTop: spacing.lg },
  chipRow: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  smallChip: { paddingHorizontal: spacing.sm, paddingVertical: spacing.xs + 2, borderRadius: radius.pill },
  footerActions: { marginTop: spacing.lg, gap: spacing.sm },
  footerAction: { flexDirection: "row", alignItems: "center", justifyContent: "center", paddingVertical: spacing.xs },
  closeFallback: { alignSelf: "center", marginTop: spacing.lg, paddingHorizontal: spacing.lg, paddingVertical: spacing.sm, borderRadius: radius.pill },
});
