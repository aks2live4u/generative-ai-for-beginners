import React, { useEffect, useState } from "react";
import { ActivityIndicator, FlatList, StyleSheet, Text, View } from "react-native";
import { useLocalSearchParams } from "expo-router";
import { useTheme, type, spacing } from "@/theme";
import { ScreenHeader } from "@/components/ScreenHeader";
import { StationCard } from "@/components/StationCard";
import { EmptyState } from "@/components/EmptyState";
import { searchStations } from "@/api/radioBrowser";
import { Station, defaultFilters } from "@/types/station";
import { useFilterStore } from "@/store/filterStore";
import { usePlayerStore } from "@/store/playerStore";
import { usePlayStation } from "@/utils/playStation";

/** Station list (search/filter/browse results). Plan §6.6. */
export default function ResultsScreen() {
  const theme = useTheme();
  const params = useLocalSearchParams<{ tag?: string; countryCode?: string; title?: string }>();
  const activeFilters = useFilterStore((s) => s.activeFilters);
  const playStation = usePlayStation();
  const currentStation = usePlayerStore((s) => s.currentStation);

  const [stations, setStations] = useState<Station[]>([]);
  const [loading, setLoading] = useState(true);
  const [errored, setErrored] = useState(false);
  const [fromCache, setFromCache] = useState(false);

  useEffect(() => {
    const filters = params.tag
      ? { ...defaultFilters, tags: [params.tag] }
      : params.countryCode
      ? { ...defaultFilters, countryCodes: [params.countryCode] }
      : activeFilters;

    setLoading(true);
    setErrored(false);
    searchStations(filters, { limit: 60 })
      .then(({ stations: results, fromCache: stale }) => {
        setStations(results);
        setFromCache(stale);
      })
      .catch(() => setErrored(true))
      .finally(() => setLoading(false));
  }, [params.tag, params.countryCode, activeFilters]);

  const title = params.title || (params.tag ? params.tag : params.countryCode ? params.countryCode : "Results");

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScreenHeader title={String(title)} subtitle={`${stations.length} station${stations.length === 1 ? "" : "s"}`} showBack />

      {fromCache && (
        <Text style={[type.micro, { color: theme.accentAlt, paddingHorizontal: spacing.md, marginBottom: spacing.sm }]}>
          OFFLINE — SHOWING CACHED RESULTS
        </Text>
      )}

      {loading ? (
        <ActivityIndicator color={theme.accent} style={{ marginTop: spacing.xl }} />
      ) : errored ? (
        <EmptyState icon="cloud-offline-outline" title="Couldn't load stations" message="Check your connection and try again." />
      ) : (
        <FlatList
          data={stations}
          keyExtractor={(s) => s.id}
          contentContainerStyle={{ paddingHorizontal: spacing.md, paddingBottom: spacing.xl }}
          renderItem={({ item }) => <StationCard station={item} onPress={playStation} isPlaying={currentStation?.id === item.id} />}
          ListEmptyComponent={<EmptyState icon="alert-circle-outline" title="No stations match" message="Try loosening your filters." />}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
});
