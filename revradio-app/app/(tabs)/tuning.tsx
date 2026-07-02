import React, { useEffect, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View } from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useTheme, type, radius, spacing } from "@/theme";
import { ScreenHeader } from "@/components/ScreenHeader";
import { FilterChip } from "@/components/FilterChip";
import { useFilterStore } from "@/store/filterStore";
import { getPopularTags, getCountries, getLanguages, TagCount, CountryCount, LanguageCount } from "@/api/radioBrowser";
import { StationFilters } from "@/types/station";

const BITRATE_STEPS = [
  { label: "Any", value: 0 },
  { label: "128+", value: 128 },
  { label: "192+", value: 192 },
  { label: "256+", value: 256 },
  { label: "320", value: 320 },
];
const CODECS = ["Any", "MP3", "AAC", "OGG"];
const SORT_OPTIONS: { label: string; value: StationFilters["sortBy"] }[] = [
  { label: "Popularity", value: "popularity" },
  { label: "Alphabetical", value: "alphabetical" },
  { label: "Bitrate", value: "bitrate" },
  { label: "Recently Added", value: "recent" },
];

/** Tuning panel — the dedicated filter tab, styled like a car customization screen. Plan §4.3. */
export default function TuningScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { activeFilters, setFilters, resetFilters, presets, savePreset, deletePreset, applyPreset } = useFilterStore();

  const [tags, setTags] = useState<TagCount[]>([]);
  const [countries, setCountries] = useState<CountryCount[]>([]);
  const [languages, setLanguages] = useState<LanguageCount[]>([]);
  const [presetName, setPresetName] = useState("");

  useEffect(() => {
    getPopularTags(24).then(setTags).catch(() => {});
    getCountries().then((c) => setCountries(c.slice(0, 24))).catch(() => {});
    getLanguages().then((l) => setLanguages(l.slice(0, 24))).catch(() => {});
  }, []);

  function toggleInList(list: string[], value: string) {
    return list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
  }

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScreenHeader title="Tuning" subtitle="Customize your signal" />

      <ScrollView contentContainerStyle={{ paddingHorizontal: spacing.md, paddingBottom: spacing.xl }} showsVerticalScrollIndicator={false}>
        <SectionTitle label="Language" />
        <ChipRow>
          {languages.map((l) => (
            <FilterChip
              key={l.name}
              label={l.name}
              selected={activeFilters.languages.includes(l.name)}
              onPress={() => setFilters({ languages: toggleInList(activeFilters.languages, l.name) })}
            />
          ))}
        </ChipRow>

        <SectionTitle label="Country / Region" />
        <ChipRow>
          {countries.map((c) => (
            <FilterChip
              key={c.iso_3166_1}
              label={c.name}
              selected={activeFilters.countryCodes.includes(c.iso_3166_1)}
              onPress={() => setFilters({ countryCodes: toggleInList(activeFilters.countryCodes, c.iso_3166_1) })}
            />
          ))}
        </ChipRow>

        <SectionTitle label="Genre / Tag" />
        <ChipRow>
          {tags.map((t) => (
            <FilterChip
              key={t.name}
              label={t.name}
              selected={activeFilters.tags.includes(t.name)}
              onPress={() => setFilters({ tags: toggleInList(activeFilters.tags, t.name) })}
            />
          ))}
        </ChipRow>

        <SectionTitle label="Codec" />
        <ChipRow>
          {CODECS.map((c) => (
            <FilterChip
              key={c}
              label={c}
              selected={c === "Any" ? !activeFilters.codec : activeFilters.codec === c}
              onPress={() => setFilters({ codec: c === "Any" ? null : c })}
            />
          ))}
        </ChipRow>

        <SectionTitle label="Min Quality" />
        <ChipRow>
          {BITRATE_STEPS.map((b) => (
            <FilterChip
              key={b.value}
              label={b.label}
              selected={activeFilters.minBitrateKbps === b.value}
              onPress={() => setFilters({ minBitrateKbps: b.value })}
            />
          ))}
        </ChipRow>

        <SectionTitle label="Sort By" />
        <ChipRow>
          {SORT_OPTIONS.map((o) => (
            <FilterChip
              key={o.value}
              label={o.label}
              selected={activeFilters.sortBy === o.value}
              onPress={() => setFilters({ sortBy: o.value })}
            />
          ))}
        </ChipRow>

        <View style={[styles.toggleCard, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <ToggleRow
            label="Ad-free (best-effort)"
            note="Community-reported — not verified by RevRadio"
            value={activeFilters.adFreeOnly}
            onChange={(v) => setFilters({ adFreeOnly: v })}
          />
          <ToggleRow
            label="HTTPS-only / Secure streams"
            value={activeFilters.httpsOnly}
            onChange={(v) => setFilters({ httpsOnly: v })}
          />
          <ToggleRow
            label="Reliable stations only"
            note="Filters out stations with recent failed checks"
            value={activeFilters.reliableOnly}
            onChange={(v) => setFilters({ reliableOnly: v })}
            last
          />
        </View>

        <SectionTitle label="Presets" />
        <View style={styles.presetSaveRow}>
          <TextInput
            value={presetName}
            onChangeText={setPresetName}
            placeholder="e.g. My Commute Mix"
            placeholderTextColor={theme.textMuted}
            style={[styles.presetInput, { backgroundColor: theme.surface, borderColor: theme.border, color: theme.textPrimary }]}
          />
          <Pressable
            onPress={() => {
              if (presetName.trim()) {
                savePreset(presetName.trim());
                setPresetName("");
              }
            }}
            style={[styles.saveBtn, { backgroundColor: theme.accent }]}
          >
            <Ionicons name="save-outline" size={18} color="#FFFFFF" />
          </Pressable>
        </View>
        {presets.map((p) => (
          <View key={p.id} style={[styles.presetRow, { borderColor: theme.border }]}>
            <Pressable style={{ flex: 1 }} onPress={() => applyPreset(p.id)}>
              <Text style={[type.bodyStrong, { color: theme.textPrimary }]}>{p.name}</Text>
            </Pressable>
            <Pressable onPress={() => deletePreset(p.id)} hitSlop={8}>
              <Ionicons name="trash-outline" size={18} color={theme.titaniumGrey} />
            </Pressable>
          </View>
        ))}

        <View style={styles.footerRow}>
          <Pressable onPress={resetFilters} style={[styles.resetBtn, { borderColor: theme.border }]}>
            <Text style={[type.bodyStrong, { color: theme.textSecondary }]}>Reset</Text>
          </Pressable>
          <Pressable
            onPress={() => router.push({ pathname: "/results", params: { title: "Tuning Results" } })}
            style={[styles.applyBtn, { backgroundColor: theme.accent }]}
          >
            <Ionicons name="flag" size={18} color="#FFFFFF" />
            <Text style={[type.bodyStrong, { color: "#FFFFFF", marginLeft: spacing.xs }]}>Show Results</Text>
          </Pressable>
        </View>
      </ScrollView>
    </View>
  );
}

function SectionTitle({ label }: { label: string }) {
  const theme = useTheme();
  return <Text style={[type.h3, { color: theme.textPrimary, marginTop: spacing.lg, marginBottom: spacing.sm }]}>{label}</Text>;
}

function ChipRow({ children }: { children: React.ReactNode }) {
  return <View style={{ flexDirection: "row", flexWrap: "wrap" }}>{children}</View>;
}

function ToggleRow({
  label,
  note,
  value,
  onChange,
  last,
}: {
  label: string;
  note?: string;
  value: boolean;
  onChange: (v: boolean) => void;
  last?: boolean;
}) {
  const theme = useTheme();
  return (
    <View style={[styles.toggleRow, !last && { borderBottomColor: theme.hairline, borderBottomWidth: 1 }]}>
      <View style={{ flex: 1, marginRight: spacing.sm }}>
        <Text style={[type.body, { color: theme.textPrimary }]}>{label}</Text>
        {note ? <Text style={[type.micro, { color: theme.textMuted, marginTop: 2 }]}>{note}</Text> : null}
      </View>
      <Switch
        value={value}
        onValueChange={onChange}
        trackColor={{ false: theme.surfaceRaised, true: theme.accent }}
        thumbColor="#FFFFFF"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  toggleCard: { borderRadius: radius.md, borderWidth: 1, marginTop: spacing.md, paddingHorizontal: spacing.md },
  toggleRow: { flexDirection: "row", alignItems: "center", paddingVertical: spacing.md },
  presetSaveRow: { flexDirection: "row", gap: spacing.sm },
  presetInput: { flex: 1, borderRadius: radius.md, borderWidth: 1, paddingHorizontal: spacing.sm, height: 44 },
  saveBtn: { width: 44, height: 44, borderRadius: radius.md, alignItems: "center", justifyContent: "center" },
  presetRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: spacing.sm,
    borderBottomWidth: StyleSheet.hairlineWidth,
    marginTop: spacing.xs,
  },
  footerRow: { flexDirection: "row", gap: spacing.sm, marginTop: spacing.xl },
  resetBtn: { flex: 1, borderWidth: 1, borderRadius: radius.md, alignItems: "center", justifyContent: "center", paddingVertical: spacing.sm + 2 },
  applyBtn: { flex: 2, flexDirection: "row", borderRadius: radius.md, alignItems: "center", justifyContent: "center", paddingVertical: spacing.sm + 2 },
});
