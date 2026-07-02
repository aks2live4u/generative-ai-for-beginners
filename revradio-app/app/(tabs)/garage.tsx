import React, { useMemo, useState } from "react";
import { Alert, FlatList, Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useTheme, type, radius, spacing } from "@/theme";
import { ScreenHeader } from "@/components/ScreenHeader";
import { StationCard } from "@/components/StationCard";
import { EmptyState } from "@/components/EmptyState";
import { useFavoritesStore } from "@/store/favoritesStore";
import { usePlayerStore } from "@/store/playerStore";
import { usePlayStation } from "@/utils/playStation";

type SubView = "favorites" | "fleets" | "history";

/** My Garage — favorites, custom "Fleet" folders, and play history. Plan §4.5. */
export default function GarageScreen() {
  const theme = useTheme();
  const playStation = usePlayStation();
  const currentStation = usePlayerStore((s) => s.currentStation);

  const garage = useFavoritesStore((s) => s.garage);
  const fleets = useFavoritesStore((s) => s.fleets);
  const history = useFavoritesStore((s) => s.history);
  const createFleet = useFavoritesStore((s) => s.createFleet);
  const deleteFleet = useFavoritesStore((s) => s.deleteFleet);
  const clearHistory = useFavoritesStore((s) => s.clearHistory);

  const [view, setView] = useState<SubView>("favorites");
  const [newFleetName, setNewFleetName] = useState("");
  const [activeFleetId, setActiveFleetId] = useState<string | null>(null);

  const favoriteStations = useMemo(() => Object.values(garage), [garage]);
  const historyStations = useMemo(
    () => history.map((h) => garage[h.stationId]).filter(Boolean),
    [history, garage]
  );
  const activeFleet = fleets.find((f) => f.id === activeFleetId);
  const activeFleetStations = useMemo(
    () => (activeFleet ? activeFleet.stationIds.map((id) => garage[id]).filter(Boolean) : []),
    [activeFleet, garage]
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScreenHeader title="My Garage" subtitle="Your saved stations & fleets" />

      <View style={styles.segmentRow}>
        {(["favorites", "fleets", "history"] as SubView[]).map((v) => (
          <Pressable
            key={v}
            onPress={() => {
              setView(v);
              setActiveFleetId(null);
            }}
            style={[
              styles.segment,
              { backgroundColor: view === v ? theme.accent : theme.surface, borderColor: theme.border },
            ]}
          >
            <Text style={[type.caption, { color: view === v ? "#FFFFFF" : theme.textSecondary }]}>
              {v === "favorites" ? "Garage" : v === "fleets" ? "Fleets" : "History"}
            </Text>
          </Pressable>
        ))}
      </View>

      {view === "favorites" && (
        <FlatList
          data={favoriteStations}
          keyExtractor={(s) => s.id}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => <StationCard station={item} onPress={playStation} isPlaying={currentStation?.id === item.id} />}
          ListEmptyComponent={
            <EmptyState icon="heart-outline" title="Your Garage is empty" message="Tap the heart on any station to park it here." />
          }
        />
      )}

      {view === "fleets" && !activeFleet && (
        <View style={styles.list}>
          <View style={styles.newFleetRow}>
            <TextInput
              value={newFleetName}
              onChangeText={setNewFleetName}
              placeholder="New fleet name…"
              placeholderTextColor={theme.textMuted}
              style={[styles.fleetInput, { backgroundColor: theme.surface, borderColor: theme.border, color: theme.textPrimary }]}
            />
            <Pressable
              onPress={() => {
                if (newFleetName.trim()) {
                  createFleet(newFleetName.trim());
                  setNewFleetName("");
                }
              }}
              style={[styles.addBtn, { backgroundColor: theme.accent }]}
            >
              <Ionicons name="add" size={20} color="#FFFFFF" />
            </Pressable>
          </View>

          {fleets.length === 0 ? (
            <EmptyState icon="albums-outline" title="No fleets yet" message="Create a collection to organize your favorite stations." />
          ) : (
            fleets.map((f) => (
              <Pressable
                key={f.id}
                onPress={() => setActiveFleetId(f.id)}
                style={[styles.fleetRow, { backgroundColor: theme.surface, borderColor: theme.border }]}
              >
                <Ionicons name="albums" size={20} color={theme.accentAlt} />
                <View style={{ flex: 1, marginLeft: spacing.sm }}>
                  <Text style={[type.bodyStrong, { color: theme.textPrimary }]}>{f.name}</Text>
                  <Text style={[type.caption, { color: theme.textMuted }]}>{f.stationIds.length} stations</Text>
                </View>
                <Pressable
                  hitSlop={8}
                  onPress={() =>
                    Alert.alert("Delete fleet", `Remove "${f.name}"?`, [
                      { text: "Cancel", style: "cancel" },
                      { text: "Delete", style: "destructive", onPress: () => deleteFleet(f.id) },
                    ])
                  }
                >
                  <Ionicons name="trash-outline" size={18} color={theme.titaniumGrey} />
                </Pressable>
              </Pressable>
            ))
          )}
        </View>
      )}

      {view === "fleets" && activeFleet && (
        <FlatList
          data={activeFleetStations}
          keyExtractor={(s) => s.id}
          contentContainerStyle={styles.list}
          ListHeaderComponent={
            <Pressable onPress={() => setActiveFleetId(null)} style={styles.backRow}>
              <Ionicons name="chevron-back" size={18} color={theme.accentAlt} />
              <Text style={[type.bodyStrong, { color: theme.accentAlt, marginLeft: 4 }]}>{activeFleet.name}</Text>
            </Pressable>
          }
          renderItem={({ item }) => <StationCard station={item} onPress={playStation} isPlaying={currentStation?.id === item.id} />}
          ListEmptyComponent={<EmptyState icon="albums-outline" title="Empty fleet" message="Add stations to this fleet from any station's options." />}
        />
      )}

      {view === "history" && (
        <FlatList
          data={historyStations}
          keyExtractor={(s, i) => `${s.id}-${i}`}
          contentContainerStyle={styles.list}
          ListHeaderComponent={
            historyStations.length > 0 ? (
              <Pressable onPress={clearHistory} style={styles.clearRow}>
                <Text style={[type.caption, { color: theme.textMuted }]}>Clear history</Text>
              </Pressable>
            ) : null
          }
          renderItem={({ item }) => <StationCard station={item} onPress={playStation} isPlaying={currentStation?.id === item.id} />}
          ListEmptyComponent={<EmptyState icon="time-outline" title="No play history yet" message="Stations you play will show up here." />}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  segmentRow: { flexDirection: "row", paddingHorizontal: spacing.md, gap: spacing.sm, marginBottom: spacing.md },
  segment: { flex: 1, paddingVertical: spacing.sm, borderRadius: radius.pill, borderWidth: 1, alignItems: "center" },
  list: { paddingHorizontal: spacing.md, paddingBottom: spacing.xl },
  newFleetRow: { flexDirection: "row", gap: spacing.sm, marginBottom: spacing.md },
  fleetInput: { flex: 1, borderRadius: radius.md, borderWidth: 1, paddingHorizontal: spacing.sm, height: 44 },
  addBtn: { width: 44, height: 44, borderRadius: radius.md, alignItems: "center", justifyContent: "center" },
  fleetRow: { flexDirection: "row", alignItems: "center", borderRadius: radius.md, borderWidth: 1, padding: spacing.sm, marginBottom: spacing.sm },
  backRow: { flexDirection: "row", alignItems: "center", marginBottom: spacing.md },
  clearRow: { alignSelf: "flex-end", marginBottom: spacing.sm },
});
