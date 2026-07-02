import React, { useEffect } from "react";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { Event, useTrackPlayerEvents } from "react-native-track-player";
import { useTheme } from "@/theme";
import { usePlayerStore } from "@/store/playerStore";

const PLAYER_EVENTS = [Event.PlaybackError, Event.PlaybackState];

function PlaybackErrorBridge() {
  const handleStreamDropped = usePlayerStore((s) => s.handleStreamDropped);
  useTrackPlayerEvents(PLAYER_EVENTS, (event) => {
    if (event.type === Event.PlaybackError) {
      handleStreamDropped();
    }
  });
  return null;
}

export default function RootLayout() {
  const theme = useTheme();

  useEffect(() => {
    usePlayerStore.getState().ensureSetup();
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <StatusBar style={theme.mode === "dark" ? "light" : "dark"} />
        <PlaybackErrorBridge />
        <Stack
          screenOptions={{
            headerShown: false,
            contentStyle: { backgroundColor: theme.background },
            animation: "fade",
          }}
        >
          <Stack.Screen name="index" />
          <Stack.Screen name="onboarding" options={{ animation: "fade" }} />
          <Stack.Screen name="(tabs)" />
          <Stack.Screen name="filters" options={{ presentation: "modal", animation: "slide_from_bottom" }} />
          <Stack.Screen name="results" options={{ animation: "slide_from_right" }} />
          <Stack.Screen name="now-playing" options={{ presentation: "fullScreenModal", animation: "slide_from_bottom" }} />
          <Stack.Screen name="settings" options={{ animation: "slide_from_right" }} />
          <Stack.Screen name="browse-country" options={{ animation: "slide_from_right" }} />
        </Stack>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
