import React, { useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useTheme, type, radius, spacing } from "@/theme";
import { FilterChip } from "@/components/FilterChip";
import { ONBOARDING_LANGUAGES, GENRE_SHOWROOMS } from "@/constants/genres";
import { useSettingsStore } from "@/store/settingsStore";

const STEPS = ["welcome", "languages", "genres"] as const;
type Step = (typeof STEPS)[number];

/** 3-screen onboarding: welcome, favorite languages, favorite genres. Plan §6.10. */
export default function OnboardingScreen() {
  const theme = useTheme();
  const router = useRouter();
  const completeOnboarding = useSettingsStore((s) => s.completeOnboarding);

  const [stepIndex, setStepIndex] = useState(0);
  const [languages, setLanguages] = useState<string[]>([]);
  const [genres, setGenres] = useState<string[]>([]);

  const step: Step = STEPS[stepIndex];

  function toggle(list: string[], setList: (v: string[]) => void, value: string) {
    setList(list.includes(value) ? list.filter((v) => v !== value) : [...list, value]);
  }

  function next() {
    if (stepIndex < STEPS.length - 1) {
      setStepIndex(stepIndex + 1);
    } else {
      completeOnboarding(languages, genres);
      router.replace("/(tabs)");
    }
  }

  function skip() {
    completeOnboarding([], []);
    router.replace("/(tabs)");
  }

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={styles.progressRow}>
        {STEPS.map((s, i) => (
          <View
            key={s}
            style={[styles.progressDot, { backgroundColor: i <= stepIndex ? theme.accent : theme.surfaceRaised }]}
          />
        ))}
      </View>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        {step === "welcome" && (
          <View style={styles.centerBlock}>
            <View style={[styles.badge, { backgroundColor: theme.surface, borderColor: theme.border }]}>
              <Ionicons name="speedometer" size={40} color={theme.accent} />
            </View>
            <Text style={[type.h1, { color: theme.textPrimary, marginTop: spacing.lg, textAlign: "center" }]}>
              Welcome to the Cockpit
            </Text>
            <Text style={[type.body, { color: theme.textSecondary, marginTop: spacing.sm, textAlign: "center" }]}>
              50,000+ live stations from every corner of the world. Let's tune your dashboard to your taste — takes
              10 seconds.
            </Text>
          </View>
        )}

        {step === "languages" && (
          <View>
            <Text style={[type.h1, { color: theme.textPrimary }]}>Pick your languages</Text>
            <Text style={[type.body, { color: theme.textSecondary, marginTop: spacing.xs, marginBottom: spacing.lg }]}>
              We'll surface stations broadcasting in these languages first.
            </Text>
            <View style={styles.chipWrap}>
              {ONBOARDING_LANGUAGES.map((lang) => (
                <FilterChip
                  key={lang}
                  label={lang}
                  selected={languages.includes(lang)}
                  onPress={() => toggle(languages, setLanguages, lang)}
                />
              ))}
            </View>
          </View>
        )}

        {step === "genres" && (
          <View>
            <Text style={[type.h1, { color: theme.textPrimary }]}>Pick your genres</Text>
            <Text style={[type.body, { color: theme.textSecondary, marginTop: spacing.xs, marginBottom: spacing.lg }]}>
              Choose a few showrooms to feature on your Home dashboard.
            </Text>
            <View style={styles.chipWrap}>
              {GENRE_SHOWROOMS.map((genre) => (
                <FilterChip
                  key={genre.id}
                  label={genre.label}
                  selected={genres.includes(genre.tag)}
                  onPress={() => toggle(genres, setGenres, genre.tag)}
                />
              ))}
            </View>
          </View>
        )}
      </ScrollView>

      <View style={styles.footer}>
        <Pressable onPress={skip} hitSlop={12}>
          <Text style={[type.body, { color: theme.textMuted }]}>Skip</Text>
        </Pressable>
        <Pressable onPress={next} style={[styles.nextBtn, { backgroundColor: theme.accent }]}>
          <Text style={[type.bodyStrong, { color: "#FFFFFF" }]}>
            {stepIndex === STEPS.length - 1 ? "Start the Engine" : "Next"}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingTop: 64, paddingHorizontal: spacing.lg },
  progressRow: { flexDirection: "row", gap: 8, marginBottom: spacing.xl },
  progressDot: { flex: 1, height: 4, borderRadius: 2 },
  content: { flexGrow: 1 },
  centerBlock: { alignItems: "center", paddingTop: spacing.xxl },
  badge: {
    width: 88,
    height: 88,
    borderRadius: radius.pill,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
  },
  chipWrap: { flexDirection: "row", flexWrap: "wrap" },
  footer: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: spacing.lg,
  },
  nextBtn: { paddingHorizontal: spacing.lg, paddingVertical: spacing.sm + 2, borderRadius: radius.pill },
});
