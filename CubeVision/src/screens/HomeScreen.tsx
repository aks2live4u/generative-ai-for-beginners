import React, { useCallback, useEffect, useRef, useState } from "react";
import { SafeAreaView, StyleSheet, Text, View } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { RootStackParamList } from "../navigation/types";
import { PrimaryButton } from "../components/PrimaryButton";
import { theme } from "../theme/theme";
import { initSolver } from "../solver/solver";
import { useAppSettings } from "../hooks/useAppSettings";

type Props = NativeStackScreenProps<RootStackParamList, "Home">;

type SolverStatus = "loading" | "slow" | "ready" | "error";

export function HomeScreen({ navigation }: Props) {
  const [solverStatus, setSolverStatus] = useState<SolverStatus>("loading");
  const [solverError, setSolverError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const { colors } = useAppSettings();
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const startSolver = useCallback(() => {
    setSolverStatus("loading");
    setSolverError(null);

    // Building the solver's lookup tables genuinely takes longer than a
    // couple seconds on some phones — let the user know it's not stuck
    // rather than leaving them guessing.
    const slowTimer = setTimeout(() => {
      if (mountedRef.current) setSolverStatus((s) => (s === "loading" ? "slow" : s));
    }, 8000);

    initSolver()
      .then(() => {
        clearTimeout(slowTimer);
        if (mountedRef.current) setSolverStatus("ready");
      })
      .catch((e) => {
        clearTimeout(slowTimer);
        if (mountedRef.current) {
          setSolverStatus("error");
          setSolverError(e instanceof Error ? e.message : "The solver failed to start.");
        }
      });

    return () => clearTimeout(slowTimer);
  }, []);

  useEffect(() => {
    const cleanup = startSolver();
    return cleanup;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attempt]);

  const statusText =
    solverStatus === "ready"
      ? "Solver ready"
      : solverStatus === "slow"
      ? "Still preparing solver… this can take a minute on some phones"
      : solverStatus === "error"
      ? solverError ?? "Solver failed to start"
      : "Preparing solver…";

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

      <View style={styles.statusArea}>
        <Text style={[styles.status, { color: solverStatus === "error" ? colors.danger : colors.textMuted }]}>
          {statusText}
        </Text>
        {solverStatus === "error" && (
          <PrimaryButton
            label="Retry"
            variant="secondary"
            onPress={() => setAttempt((a) => a + 1)}
            style={styles.retryButton}
          />
        )}
      </View>
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
  statusArea: {
    alignItems: "center",
    gap: theme.spacing(2),
  },
  status: {
    textAlign: "center",
    fontSize: 13,
  },
  retryButton: {
    minHeight: 40,
    paddingVertical: 8,
  },
});
