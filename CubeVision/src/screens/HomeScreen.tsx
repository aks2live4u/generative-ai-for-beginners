import React, { useEffect, useState } from "react";
import { SafeAreaView, StyleSheet, Text, View } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { RootStackParamList } from "../navigation/types";
import { PrimaryButton } from "../components/PrimaryButton";
import { theme } from "../theme/theme";
import { initSolver } from "../solver/solver";

type Props = NativeStackScreenProps<RootStackParamList, "Home">;

export function HomeScreen({ navigation }: Props) {
  const [solverReady, setSolverReady] = useState(false);

  useEffect(() => {
    // Kick off the ~4-5s pruning-table build now, so it's ready by the time
    // scanning finishes instead of stalling the Solve screen.
    initSolver().then(() => setSolverReady(true));
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.title}>CubeVision</Text>
        <Text style={styles.subtitle}>Scan your Rubik's Cube. Get the optimal solution. Fully offline.</Text>
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

      <Text style={styles.status}>{solverReady ? "Solver ready" : "Preparing solver…"}</Text>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
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
    color: theme.colors.text,
  },
  subtitle: {
    fontSize: 16,
    color: theme.colors.textMuted,
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
    color: theme.colors.textMuted,
    fontSize: 13,
  },
});
