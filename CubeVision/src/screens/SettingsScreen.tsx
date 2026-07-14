import React from "react";
import { Alert, SafeAreaView, StyleSheet, Switch, Text, View } from "react-native";
import { Card } from "../components/Card";
import { PrimaryButton } from "../components/PrimaryButton";
import { clearHistory } from "../utils/storage";
import { theme } from "../theme/theme";
import { useAppSettings } from "../hooks/useAppSettings";

function Row({ label, value, onChange }: { label: string; value: boolean; onChange: (v: boolean) => void }) {
  const { colors } = useAppSettings();
  return (
    <View style={styles.row}>
      <Text style={[styles.rowLabel, { color: colors.text }]}>{label}</Text>
      <Switch value={value} onValueChange={onChange} />
    </View>
  );
}

export function SettingsScreen() {
  const { settings, updateSetting, colors } = useAppSettings();

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]}>
      <Text style={[styles.title, { color: colors.text }]}>Settings</Text>
      <Card style={styles.card}>
        <Row
          label="Dark Mode"
          value={settings.darkMode}
          onChange={(v) => updateSetting("darkMode", v)}
        />
        <Row
          label="Voice Guidance"
          value={settings.voiceGuidance}
          onChange={(v) => updateSetting("voiceGuidance", v)}
        />
        <Row
          label="Haptic Feedback"
          value={settings.hapticFeedback}
          onChange={(v) => updateSetting("hapticFeedback", v)}
        />
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
  container: { flex: 1, padding: theme.spacing(6), gap: theme.spacing(4) },
  title: { fontSize: 26, fontWeight: "800" },
  card: { gap: theme.spacing(3) },
  row: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  rowLabel: { fontSize: 16 },
});
