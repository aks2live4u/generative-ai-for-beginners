import React, { useEffect, useState } from "react";
import { Alert, SafeAreaView, StyleSheet, Switch, Text, View } from "react-native";
import { Card } from "../components/Card";
import { PrimaryButton } from "../components/PrimaryButton";
import { AppSettings, clearHistory, DEFAULT_SETTINGS, loadSettings, saveSettings } from "../utils/storage";
import { theme } from "../theme/theme";

function Row({ label, value, onChange }: { label: string; value: boolean; onChange: (v: boolean) => void }) {
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Switch value={value} onValueChange={onChange} />
    </View>
  );
}

export function SettingsScreen() {
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS);

  useEffect(() => {
    loadSettings().then(setSettings);
  }, []);

  const update = (patch: Partial<AppSettings>) => {
    const next = { ...settings, ...patch };
    setSettings(next);
    saveSettings(next);
  };

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Settings</Text>
      <Card style={styles.card}>
        <Row label="Dark Mode" value={settings.darkMode} onChange={(v) => update({ darkMode: v })} />
        <Row label="Voice Guidance" value={settings.voiceGuidance} onChange={(v) => update({ voiceGuidance: v })} />
        <Row label="Haptic Feedback" value={settings.hapticFeedback} onChange={(v) => update({ hapticFeedback: v })} />
      </Card>
      <PrimaryButton
        label="Clear Solve History"
        variant="secondary"
        onPress={() => {
          clearHistory();
          Alert.alert("History cleared");
        }}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.colors.background, padding: theme.spacing(6), gap: theme.spacing(4) },
  title: { fontSize: 26, fontWeight: "800", color: theme.colors.text },
  card: { gap: theme.spacing(3) },
  row: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  rowLabel: { fontSize: 16, color: theme.colors.text },
});
