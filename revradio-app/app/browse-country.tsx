import React, { useEffect, useState } from "react";
import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { useTheme, type, radius, spacing } from "@/theme";
import { ScreenHeader } from "@/components/ScreenHeader";
import { EmptyState } from "@/components/EmptyState";
import { getCountries, CountryCount } from "@/api/radioBrowser";

/** ISO 3166-1 alpha-2 -> flag emoji via regional indicator symbols. */
function flagEmoji(iso: string): string {
  if (!iso || iso.length !== 2) return "🌐";
  const codePoints = iso
    .toUpperCase()
    .split("")
    .map((c) => 127397 + c.charCodeAt(0));
  return String.fromCodePoint(...codePoints);
}

/**
 * "Track Map" — Browse by Country. A lightweight, dependency-free stand-in
 * for the full interactive world map view (Plan §4.1), which is scoped to
 * Phase 2 (react-native-maps) per the build phases in §9.
 */
export default function BrowseCountryScreen() {
  const theme = useTheme();
  const router = useRouter();
  const [countries, setCountries] = useState<CountryCount[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getCountries()
      .then(setCountries)
      .finally(() => setLoading(false));
  }, []);

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScreenHeader title="Track Map" subtitle="Browse stations by country" showBack />

      {loading ? (
        <ActivityIndicator color={theme.accent} style={{ marginTop: spacing.xl }} />
      ) : (
        <FlatList
          data={countries.filter((c) => c.stationcount > 0)}
          keyExtractor={(c) => c.iso_3166_1}
          numColumns={2}
          contentContainerStyle={{ paddingHorizontal: spacing.md, paddingBottom: spacing.xl }}
          columnWrapperStyle={{ gap: spacing.sm }}
          renderItem={({ item }) => (
            <Pressable
              onPress={() => router.push({ pathname: "/results", params: { countryCode: item.iso_3166_1, title: item.name } })}
              style={[styles.tile, { backgroundColor: theme.surface, borderColor: theme.border }]}
            >
              <Text style={styles.flag}>{flagEmoji(item.iso_3166_1)}</Text>
              <Text style={[type.bodyStrong, { color: theme.textPrimary, marginTop: spacing.xs }]} numberOfLines={1}>
                {item.name}
              </Text>
              <Text style={[type.micro, { color: theme.textMuted, marginTop: 2 }]}>{item.stationcount} stations</Text>
            </Pressable>
          )}
          ListEmptyComponent={<EmptyState icon="earth-outline" title="Couldn't load countries" />}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  tile: { flex: 1, borderRadius: radius.md, borderWidth: 1, padding: spacing.md, marginBottom: spacing.sm, alignItems: "center" },
  flag: { fontSize: 32 },
});
