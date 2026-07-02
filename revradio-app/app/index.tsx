import React, { useEffect, useRef, useState } from "react";
import { Animated, Easing, StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { useTheme, type, spacing } from "@/theme";
import { TachometerDial } from "@/components/TachometerDial";
import { useSettingsStore } from "@/store/settingsStore";

/** Splash / Launch — "engine start" animation before routing to onboarding or Home. Plan §6.1. */
export default function SplashScreen() {
  const theme = useTheme();
  const router = useRouter();
  const hasCompletedOnboarding = useSettingsStore((s) => s.hasCompletedOnboarding);
  const [rpm, setRpm] = useState(0);
  const fadeAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(fadeAnim, { toValue: 1, duration: 400, useNativeDriver: true }).start();

    // "Rev" the tachometer needle up on launch, then settle and navigate.
    const revUp = setTimeout(() => setRpm(1), 150);
    const revSettle = setTimeout(() => setRpm(0.35), 750);
    const navigate = setTimeout(() => {
      router.replace(hasCompletedOnboarding ? "/(tabs)" : "/onboarding");
    }, 1400);

    return () => {
      clearTimeout(revUp);
      clearTimeout(revSettle);
      clearTimeout(navigate);
    };
  }, [hasCompletedOnboarding]);

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <Animated.View style={{ opacity: fadeAnim, alignItems: "center" }}>
        <TachometerDial size={200} value={rpm} label="REV" sublabel="RADIO" />
        <Text style={[type.h2, { color: theme.textPrimary, marginTop: spacing.lg, letterSpacing: 2 }]}>REVRADIO</Text>
        <Text style={[type.caption, { color: theme.textMuted, marginTop: spacing.xs }]}>TUNE IN. REV UP.</Text>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: "center", justifyContent: "center" },
});
