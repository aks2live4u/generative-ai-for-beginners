import React, { useEffect, useState } from "react";
import { SafeAreaView, StyleSheet, Text, View } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { RootStackParamList } from "../navigation/types";
import { PrimaryButton } from "../components/PrimaryButton";
import { theme } from "../theme/theme";
import { initSolver } from "../solver/solver";
import { useAppSettings } from "../hooks/useAppSettings";

type Props = NativeStackScreenProps<RootStackParamList, "Home">;

export function HomeScreen({ navigation }: Props) {
  const [solverReady, setSolverReady] = useState(false);
  const { colors } = useAppSettings();

  useEffect(() => {
    // Kick off the ~4-5s pruning-table build now, so it's ready by the time
    // scanning finishes instead of stalling the Solve screen.
    initSolver().then(() => setSolverReady(true));
  }, []);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={styles.hero}>
        <Text style={[styles.title, { color: colors.text }]}>CubeVision</Text>
        <Text style={[styles.subtitle, { color: colors.textMuted }]}>
          Scan your Rubik's Cube. Get the optimal solution. Fully offline.
        </Text>
      </View>

      <View style={styles.actions}>
        <PrimaryButton label="Scan Cube" onPress={() => navigation.navigate("Scan")} style={styles.scanButton} />
        <PrimaryButton
          label="Manual Entry"
          variant="secondary"
          onPress={() => navigation.navigate("ManualEntry")}
        />
        <View style={styles.row}>
          <PrimaryButton label="Settings" variant="ghost" onPress={() => navigation.navigate("Settings")} />
          <PrimaryButton label="Help" variant="ghost" onPress={() => navigation.navigate("Help")} />
        </View>
      </View>

      <Text style={[styles.status, { color: colors.textMuted }]}>
        {solverReady ? "Solver ready" : "Preparing solver…"}
      </Text>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "space-between",
    paddingHorizontal: theme.spacing(6),
    paddingVertical: theme.spacing(10),
  },
  hero: {
    marginTop: theme.spacing(16),
  },
  title: {
    fontSize: 40,
    fontWeight: "800",
  },
  subtitle: {
    fontSize: 16,
    marginTop: theme.spacing(3),
    lineHeight: 22,
  },
  actions: {
    gap: theme.spacing(4),
  },
  row: {
    flexDirection: "row",
    justifyContent: "center",
    gap: theme.spacing(4),
  },
  scanButton: {
    minHeight: 72,
  },
  status: {
    textAlign: "center",
    fontSize: 13,
  },
});
