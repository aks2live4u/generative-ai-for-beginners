import React, { useEffect, useState } from "react";
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View, Pressable } from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useTheme, type, radius, spacing } from "@/theme";
import { ScreenHeader } from "@/components/ScreenHeader";
import { TachometerDial } from "@/components/TachometerDial";
import { GenreShowroom } from "@/components/GenreShowroom";
import { StationCard } from "@/components/StationCard";
import { EmptyState } from "@/components/EmptyState";
import { getTrendingStations, getRandomStation, searchStations } from "@/api/radioBrowser";
import { Station } from "@/types/station";
import { usePlayerStore } from "@/store/playerStore";
import { useSettingsStore } from "@/store/settingsStore";
import { usePlayStation } from "@/utils/playStation";

/** Home Dashboard — tachometer, featured stations, quick genre shortcuts. Plan §6.2. */
export default function HomeScreen() {
  const theme = useTheme();
  const router = useRouter();
  const playStation = usePlayStation();
  const currentStation = usePlayerStore((s) => s.currentStation);
  const preferredLanguages = useSettingsStore((s) => s.preferredLanguages);
  const preferredGenreTags = useSettingsStore((s) => s.preferredGenreTags);

  const [trending, setTrending] = useState<Station[]>([]);
  const [forYou, setForYou] = useState<Station[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [randomizing, setRandomizing] = useState(false);
  const [error, setError] = useState(false);

  async function load() {
    setError(false);
    try {
      const [trendingStations] = await Promise.all([getTrendingStations(15)]);
      setTrending(trendingStations);

      if (preferredLanguages.length || preferredGenreTags.length) {
        const { stations } = await searchStations(
          { languages: preferredLanguages.slice(0, 1), tags: preferredGenreTags.slice(0, 3) },
          { limit: 15 }
        );
        setForYou(stations);
      }
    } catch {
      setError(true);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleRandomDrive() {
    setRandomizing(true);
    const station = await getRandomStation();
    setRandomizing(false);
    if (station) playStation(station);
  }

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => {
              setRefreshing(true);
              load();
            }}
            tintColor={theme.accent}
          />
        }
      >
        <ScreenHeader title="Dashboard" subtitle="Tune in. Rev up." rightIcon="settings-outline" onRightPress={() => router.push("/settings")} />

        <View style={styles.dialWrap}>
          <TachometerDial
            size={190}
            active={!!currentStation}
            label={currentStation ? "LIVE" : "IDLE"}
            sublabel={currentStation ? currentStation.name : "Pick a station to rev up"}
          />
        </View>

        <View style={styles.quickRow}>
          <Pressable
            onPress={handleRandomDrive}
            style={[styles.quickAction, { backgroundColor: theme.accent }]}
            disabled={randomizing}
          >
            {randomizing ? (
              <ActivityIndicator color="#FFFFFF" />
            ) : (
              <Ionicons name="shuffle" size={20} color="#FFFFFF" />
            )}
            <Text style={[type.bodyStrong, { color: "#FFFFFF", marginLeft: spacing.xs }]}>Random Drive</Text>
          </Pressable>
          <Pressable
            onPress={() => router.push("/browse-country")}
            style={[styles.quickAction, { backgroundColor: theme.surface, borderColor: theme.border, borderWidth: 1 }]}
          >
            <Ionicons name="map" size={20} color={theme.accentAlt} />
            <Text style={[type.bodyStrong, { color: theme.textPrimary, marginLeft: spacing.xs }]}>Track Map</Text>
          </Pressable>
        </View>

        <SectionLabel title="Genre Garage" />
        <View style={{ paddingHorizontal: spacing.md }}>
          <GenreShowroom onSelect={(genre) => router.push({ pathname: "/results", params: { tag: genre.tag, title: genre.label } })} />
        </View>

        <SectionLabel title="Trending Now" />
        <View style={{ paddingHorizontal: spacing.md }}>
          {loading ? (
            <ActivityIndicator color={theme.accent} style={{ marginVertical: spacing.lg }} />
          ) : error && !trending.length ? (
            <EmptyState icon="cloud-offline-outline" title="Can't reach the pit lane" message="Check your connection and pull to refresh." />
          ) : (
            trending.map((s) => <StationCard key={s.id} station={s} onPress={playStation} isPlaying={currentStation?.id === s.id} />)
          )}
        </View>

        {forYou.length > 0 && (
          <>
            <SectionLabel title="For You" />
            <View style={{ paddingHorizontal: spacing.md, paddingBottom: spacing.xl }}>
              {forYou.map((s) => (
                <StationCard key={s.id} station={s} onPress={playStation} isPlaying={currentStation?.id === s.id} />
              ))}
            </View>
          </>
        )}

        <View style={{ height: spacing.xl }} />
      </ScrollView>
    </View>
  );
}

function SectionLabel({ title }: { title: string }) {
  const theme = useTheme();
  return (
    <Text style={[type.h3, { color: theme.textPrimary, marginTop: spacing.lg, marginBottom: spacing.sm, paddingHorizontal: spacing.md }]}>
      {title}
    </Text>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  dialWrap: { alignItems: "center", marginVertical: spacing.sm },
  quickRow: { flexDirection: "row", paddingHorizontal: spacing.md, gap: spacing.sm },
  quickAction: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: spacing.sm + 2,
    borderRadius: radius.md,
  },
});
