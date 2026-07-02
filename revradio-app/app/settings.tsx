import React from "react";
import { Linking, Pressable, ScrollView, StyleSheet, Switch, Text, View } from "react-native";
import Constants from "expo-constants";
import { useTheme, type, radius, spacing } from "@/theme";
import { ScreenHeader } from "@/components/ScreenHeader";
import { FilterChip } from "@/components/FilterChip";
import { useSettingsStore, ThemeMode, AudioQuality } from "@/store/settingsStore";

const SLEEP_DEFAULTS = [15, 30, 45, 60];
const QUALITY_OPTIONS: { label: string; value: AudioQuality }[] = [
  { label: "Auto", value: "auto" },
  { label: "Data Saver", value: "data-saver" },
  { label: "High Quality", value: "high" },
];

/** Settings — theme, sleep timer defaults, audio quality, about. Plan §6.9. */
export default function SettingsScreen() {
  const theme = useTheme();
  const {
    themeMode,
    setThemeMode,
    engineSoundOnLaunch,
    toggleEngineSound,
    defaultSleepTimerMinutes,
    setDefaultSleepTimer,
    audioQuality,
    setAudioQuality,
    cellularWarningEnabled,
    toggleCellularWarning,
    resetOnboarding,
  } = useSettingsStore();

  return (
    <ScrollView style={{ backgroundColor: theme.background }} contentContainerStyle={styles.container}>
      <ScreenHeader title="Settings" showBack />

      <Section title="Appearance" theme={theme}>
        <Text style={[type.body, { color: theme.textSecondary, marginBottom: spacing.sm }]}>
          Cockpits are dark by default — Daylight is the optional light theme.
        </Text>
        <View style={styles.chipRow}>
          {(["cockpit", "daylight"] as ThemeMode[]).map((mode) => (
            <FilterChip key={mode} label={mode === "cockpit" ? "Cockpit (Dark)" : "Daylight"} selected={themeMode === mode} onPress={() => setThemeMode(mode)} />
          ))}
        </View>
      </Section>

      <Section title="Sleep Timer Default" theme={theme}>
        <View style={styles.chipRow}>
          {SLEEP_DEFAULTS.map((m) => (
            <FilterChip key={m} label={`${m} min`} selected={defaultSleepTimerMinutes === m} onPress={() => setDefaultSleepTimer(m)} />
          ))}
        </View>
      </Section>

      <Section title="Audio Quality" theme={theme}>
        <Text style={[type.body, { color: theme.textSecondary, marginBottom: spacing.sm }]}>
          Data Saver caps stream bitrate on cellular connections.
        </Text>
        <View style={styles.chipRow}>
          {QUALITY_OPTIONS.map((o) => (
            <FilterChip key={o.value} label={o.label} selected={audioQuality === o.value} onPress={() => setAudioQuality(o.value)} />
          ))}
        </View>
      </Section>

      <Section title="Preferences" theme={theme}>
        <ToggleRow label="Engine-rev sound on launch" value={engineSoundOnLaunch} onChange={toggleEngineSound} theme={theme} />
        <ToggleRow
          label="Warn before high-bitrate cellular playback"
          value={cellularWarningEnabled}
          onChange={toggleCellularWarning}
          theme={theme}
          last
        />
      </Section>

      <Section title="About" theme={theme}>
        <InfoRow label="Version" value={Constants.expoConfig?.version ?? "1.0.0"} theme={theme} />
        <InfoRow label="Data source" value="Radio-Browser (radio-browser.info)" theme={theme} />
        <Pressable onPress={() => Linking.openURL("https://www.radio-browser.info")}>
          <Text style={[type.caption, { color: theme.accentAlt, marginTop: spacing.xs }]}>Visit Radio-Browser →</Text>
        </Pressable>
        <Pressable onPress={resetOnboarding} style={{ marginTop: spacing.md }}>
          <Text style={[type.caption, { color: theme.textMuted }]}>Replay onboarding</Text>
        </Pressable>
      </Section>
    </ScrollView>
  );
}

function Section({ title, theme, children }: { title: string; theme: ReturnType<typeof useTheme>; children: React.ReactNode }) {
  return (
    <View style={[styles.card, { backgroundColor: theme.surface, borderColor: theme.border }]}>
      <Text style={[type.h3, { color: theme.textPrimary, marginBottom: spacing.sm }]}>{title}</Text>
      {children}
    </View>
  );
}

function ToggleRow({
  label,
  value,
  onChange,
  theme,
  last,
}: {
  label: string;
  value: boolean;
  onChange: () => void;
  theme: ReturnType<typeof useTheme>;
  last?: boolean;
}) {
  return (
    <View style={[styles.toggleRow, !last && { borderBottomColor: theme.hairline, borderBottomWidth: 1 }]}>
      <Text style={[type.body, { color: theme.textPrimary, flex: 1, marginRight: spacing.sm }]}>{label}</Text>
      <Switch value={value} onValueChange={onChange} trackColor={{ false: theme.surfaceRaised, true: theme.accent }} thumbColor="#FFFFFF" />
    </View>
  );
}

function InfoRow({ label, value, theme }: { label: string; value: string; theme: ReturnType<typeof useTheme> }) {
  return (
    <View style={styles.infoRow}>
      <Text style={[type.caption, { color: theme.textMuted }]}>{label}</Text>
      <Text style={[type.caption, { color: theme.textSecondary }]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { paddingHorizontal: spacing.md, paddingBottom: spacing.xxl },
  card: { borderRadius: radius.md, borderWidth: 1, padding: spacing.md, marginBottom: spacing.md },
  chipRow: { flexDirection: "row", flexWrap: "wrap" },
  toggleRow: { flexDirection: "row", alignItems: "center", paddingVertical: spacing.sm },
  infoRow: { flexDirection: "row", justifyContent: "space-between", paddingVertical: 4 },
});
