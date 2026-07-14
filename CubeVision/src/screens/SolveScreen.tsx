import React, { useEffect, useRef, useState } from "react";
import { SafeAreaView, StyleSheet, Switch, Text, View } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { RootStackParamList } from "../navigation/types";
import { Cube3D, Cube3DHandle } from "../components/Cube3D";
import { MoveBadge } from "../components/MoveBadge";
import { ProgressBar } from "../components/ProgressBar";
import { PrimaryButton } from "../components/PrimaryButton";
import { useSolver } from "../hooks/useSolver";
import { useVoiceGuidance } from "../hooks/useVoiceGuidance";
import { invertMove } from "../utils/moveNotation";
import { appendHistory } from "../utils/storage";
import { theme } from "../theme/theme";

type Props = NativeStackScreenProps<RootStackParamList, "Solve">;

export function SolveScreen({ route }: Props) {
  const { faces } = route.params;
  const solver = useSolver();
  const voice = useVoiceGuidance();
  const cubeRef = useRef<Cube3DHandle>(null);
  const [moveIndex, setMoveIndex] = useState(0);
  const [autoPlaying, setAutoPlaying] = useState(false);
  const [animating, setAnimating] = useState(false);

  useEffect(() => {
    solver.solve(faces);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (solver.status === "solved") {
      appendHistory({
        id: `${Date.now()}`,
        timestamp: Date.now(),
        moveCount: solver.moves.length,
        durationMs: null,
      });
    }
  }, [solver.status]);

  const stepForward = async () => {
    if (animating || moveIndex >= solver.moves.length) return false;
    const move = solver.moves[moveIndex];
    setAnimating(true);
    voice.speakMove(move);
    await cubeRef.current?.playMove(move);
    setMoveIndex((i) => i + 1);
    setAnimating(false);
    return true;
  };

  const stepBackward = async () => {
    if (animating || moveIndex <= 0) return;
    const move = invertMove(solver.moves[moveIndex - 1]);
    setAnimating(true);
    await cubeRef.current?.playMove(move);
    setMoveIndex((i) => i - 1);
    setAnimating(false);
  };

  const restart = () => {
    setAutoPlaying(false);
    cubeRef.current?.resetTo(faces);
    setMoveIndex(0);
  };

  useEffect(() => {
    if (!autoPlaying) return;
    if (moveIndex >= solver.moves.length) {
      setAutoPlaying(false);
      voice.speak("Solved!");
      return;
    }
    if (animating) return;
    stepForward();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoPlaying, moveIndex, animating]);

  const isSolved = solver.status === "solved" && moveIndex >= solver.moves.length;

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.cubeArea}>
        {solver.status === "solving" && <Text style={styles.statusText}>Solving…</Text>}
        {solver.status === "error" && <Text style={styles.errorText}>{solver.error}</Text>}
        {(solver.status === "solved" || solver.status === "idle") && (
          <Cube3D ref={cubeRef} initialFaces={faces} size={320} />
        )}
      </View>

      <View style={styles.panel}>
        <View style={styles.voiceRow}>
          <Text style={styles.voiceLabel}>Voice guidance</Text>
          <Switch value={voice.enabled} onValueChange={voice.toggle} />
        </View>

        <MoveBadge move={isSolved ? "✓" : solver.moves[moveIndex]} />

        <ProgressBar progress={solver.moves.length ? moveIndex / solver.moves.length : 0} />
        <Text style={styles.progressText}>
          {isSolved ? "Solved!" : `${moveIndex} / ${solver.moves.length}`}
        </Text>

        <View style={styles.controlsRow}>
          <PrimaryButton label="⟲ Restart" variant="secondary" onPress={restart} />
          <PrimaryButton label="◀ Prev" variant="secondary" onPress={stepBackward} disabled={moveIndex === 0} />
          <PrimaryButton
            label={autoPlaying ? "⏸ Pause" : "▶ Auto Play"}
            onPress={() => setAutoPlaying((p) => !p)}
            disabled={isSolved}
          />
          <PrimaryButton label="Next ▶" variant="secondary" onPress={stepForward} disabled={isSolved} />
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.colors.background },
  cubeArea: { flex: 1, alignItems: "center", justifyContent: "center" },
  statusText: { fontSize: 16, color: theme.colors.textMuted },
  errorText: { fontSize: 16, color: theme.colors.danger, textAlign: "center", paddingHorizontal: 24 },
  panel: { padding: theme.spacing(6), gap: theme.spacing(3), alignItems: "center" },
  voiceRow: { flexDirection: "row", alignItems: "center", gap: theme.spacing(2) },
  voiceLabel: { color: theme.colors.text, fontSize: 14 },
  progressText: { color: theme.colors.textMuted, fontSize: 13 },
  controlsRow: { flexDirection: "row", flexWrap: "wrap", justifyContent: "center", gap: theme.spacing(2) },
});
