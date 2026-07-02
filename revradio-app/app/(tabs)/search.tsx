import React, { useEffect, useState } from "react";
import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useTheme, type, radius, spacing } from "@/theme";
import { ScreenHeader } from "@/components/ScreenHeader";
import { StationCard } from "@/components/StationCard";
import { EmptyState } from "@/components/EmptyState";
import { searchStations } from "@/api/radioBrowser";
import { Station } from "@/types/station";
import { useFilterStore } from "@/store/filterStore";
import { usePlayerStore } from "@/store/playerStore";
import { usePlayStation } from "@/utils/playStation";
import { useVoiceSearch } from "@/services/voiceSearch";

/** Search — real-time search by name/country/city/tag, voice search, recent searches. Plan §4.2. */
export default function SearchScreen() {
  const theme = useTheme();
  const playStation = usePlayStation();
  const currentStation = usePlayerStore((s) => s.currentStation);
  const recentSearches = useFilterStore((s) => s.recentSearches);
  const addRecentSearch = useFilterStore((s) => s.addRecentSearch);
  const clearRecentSearches = useFilterStore((s) => s.clearRecentSearches);

  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Station[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const { isListening, start } = useVoiceSearch((text) => setQuery(text));

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      setSearched(false);
      return;
    }
    setLoading(true);
    const handle = setTimeout(async () => {
      try {
        const { stations } = await searchStations({ query }, { limit: 40 });
        setResults(stations);
      } finally {
        setLoading(false);
        setSearched(true);
      }
    }, 400);
    return () => clearTimeout(handle);
  }, [query]);

  function handleSubmit() {
    if (query.trim()) addRecentSearch(query.trim());
  }

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScreenHeader title="Search" subtitle="Find any station on Earth" />

      <View style={styles.searchBarWrap}>
        <View style={[styles.searchBar, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <Ionicons name="search" size={18} color={theme.titaniumGrey} />
          <TextInput
            value={query}
            onChangeText={setQuery}
            onSubmitEditing={handleSubmit}
            placeholder="Station, country, city, or tag…"
            placeholderTextColor={theme.textMuted}
            style={[styles.input, { color: theme.textPrimary }]}
            returnKeyType="search"
          />
          {query.length > 0 && (
            <Pressable onPress={() => setQuery("")} hitSlop={8}>
              <Ionicons name="close-circle" size={18} color={theme.titaniumGrey} />
            </Pressable>
          )}
        </View>
        <Pressable
          onPress={start}
          style={[styles.micBtn, { backgroundColor: isListening ? theme.accent : theme.surface, borderColor: theme.border }]}
        >
          <Ionicons name={isListening ? "mic" : "mic-outline"} size={20} color={isListening ? "#FFFFFF" : theme.accentAlt} />
        </Pressable>
      </View>

      {!query.trim() ? (
        <View style={{ paddingHorizontal: spacing.md }}>
          {recentSearches.length > 0 && (
            <View style={styles.recentHeader}>
              <Text style={[type.h3, { color: theme.textPrimary }]}>Recent Searches</Text>
              <Pressable onPress={clearRecentSearches}>
                <Text style={[type.caption, { color: theme.textMuted }]}>Clear</Text>
              </Pressable>
            </View>
          )}
          {recentSearches.map((term) => (
            <Pressable key={term} onPress={() => setQuery(term)} style={styles.recentRow}>
              <Ionicons name="time-outline" size={16} color={theme.titaniumGrey} />
              <Text style={[type.body, { color: theme.textSecondary, marginLeft: spacing.sm }]}>{term}</Text>
            </Pressable>
          ))}
          {recentSearches.length === 0 && (
            <EmptyState icon="search-outline" title="Search the world" message="Try a station name, city, country, or genre tag." />
          )}
        </View>
      ) : loading ? (
        <ActivityIndicator color={theme.accent} style={{ marginTop: spacing.xl }} />
      ) : (
        <FlatList
          data={results}
          keyExtractor={(item) => item.id}
          contentContainerStyle={{ paddingHorizontal: spacing.md, paddingBottom: spacing.xl }}
          renderItem={({ item }) => (
            <StationCard
              station={item}
              onPress={(s) => {
                handleSubmit();
                playStation(s);
              }}
              isPlaying={currentStation?.id === item.id}
            />
          )}
          ListEmptyComponent={
            searched ? <EmptyState icon="alert-circle-outline" title="No stations found" message="Try a different search term." /> : null
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  searchBarWrap: { flexDirection: "row", alignItems: "center", paddingHorizontal: spacing.md, marginBottom: spacing.md, gap: spacing.sm },
  searchBar: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    borderRadius: radius.md,
    borderWidth: 1,
    paddingHorizontal: spacing.sm,
    height: 44,
  },
  input: { flex: 1, marginLeft: spacing.xs, fontSize: 15, height: 44 },
  micBtn: { width: 44, height: 44, borderRadius: radius.md, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  recentHeader: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: spacing.sm },
  recentRow: { flexDirection: "row", alignItems: "center", paddingVertical: spacing.sm },
});
