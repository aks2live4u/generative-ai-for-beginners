import React, { useMemo, useState } from "react";
import { SafeAreaView, ScrollView, StyleSheet, Text, View } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { RootStackParamList } from "../navigation/types";
import { FaceGrid } from "../components/FaceGrid";
import { ColorSwatchGrid } from "../components/ColorSwatchGrid";
import { PrimaryButton } from "../components/PrimaryButton";
import { Card } from "../components/Card";
import { FACE_COLOR, FACE_LABEL } from "../cube/constants";
import { CubeFaces, Face, FACES } from "../cube/types";
import { validateCube } from "../validation/validateCube";
import { theme } from "../theme/theme";

function solvedFaces(): CubeFaces {
  return Object.fromEntries(FACES.map((f) => [f, new Array(9).fill(FACE_COLOR[f])])) as CubeFaces;
}

type Props = NativeStackScreenProps<RootStackParamList, "ManualEntry">;

export function ManualEntryScreen({ navigation }: Props) {
  const [faces, setFaces] = useState<CubeFaces>(solvedFaces);
  const [activeFace, setActiveFace] = useState<Face>("U");
  const [selected, setSelected] = useState<number | null>(null);

  const validation = useMemo(() => validateCube(faces), [faces]);

  const setSticker = (index: number, color: (typeof faces)["U"][number]) => {
    setFaces((prev) => {
      const updated = [...prev[activeFace]];
      updated[index] = color;
      return { ...prev, [activeFace]: updated };
    });
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Manual Entry</Text>
        <Text style={styles.subtitle}>Pick a face, then tap stickers to set their color.</Text>

        <View style={styles.tabRow}>
          {FACES.map((f) => (
            <PrimaryButton
              key={f}
              label={FACE_LABEL[f]}
              variant={f === activeFace ? "primary" : "secondary"}
              onPress={() => {
                setActiveFace(f);
                setSelected(null);
              }}
            />
          ))}
        </View>

        <Card style={styles.gridCard}>
          <FaceGrid colors={faces[activeFace]} size={260} onStickerPress={setSelected} />
        </Card>

        {selected !== null && (
          <ColorSwatchGrid selected={faces[activeFace][selected]} onSelect={(c) => setSticker(selected, c)} />
        )}

        {!validation.valid && (
          <Card style={styles.issueCard}>
            <Text style={styles.issueTitle}>Not solvable yet</Text>
            {validation.issues.slice(0, 3).map((issue, i) => (
              <Text key={i} style={styles.issueText}>
                • {issue.message}
              </Text>
            ))}
          </Card>
        )}

        <PrimaryButton
          label="Solve"
          disabled={!validation.valid}
          onPress={() => navigation.replace("Solve", { faces })}
          style={styles.solveButton}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.colors.background },
  content: { padding: theme.spacing(6), gap: theme.spacing(4), alignItems: "center" },
  title: { fontSize: 26, fontWeight: "800", color: theme.colors.text },
  subtitle: { fontSize: 14, color: theme.colors.textMuted, textAlign: "center" },
  tabRow: { flexDirection: "row", flexWrap: "wrap", justifyContent: "center", gap: theme.spacing(2) },
  gridCard: { alignItems: "center" },
  issueCard: { width: "100%", gap: theme.spacing(1) },
  issueTitle: { fontWeight: "700", color: theme.colors.danger },
  issueText: { color: theme.colors.text, fontSize: 13 },
  solveButton: { width: "100%", marginTop: theme.spacing(2) },
});
