import React from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { Tabs } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme, type, spacing } from "@/theme";
import { MiniPlayer } from "@/components/MiniPlayer";

const TAB_ICONS: Record<string, { active: keyof typeof Ionicons.glyphMap; inactive: keyof typeof Ionicons.glyphMap }> = {
  index: { active: "speedometer", inactive: "speedometer-outline" },
  search: { active: "radio", inactive: "radio-outline" },
  garage: { active: "car-sport", inactive: "car-sport-outline" },
  tuning: { active: "construct", inactive: "construct-outline" },
};

const TAB_LABELS: Record<string, string> = {
  index: "Home",
  search: "Search",
  garage: "Garage",
  tuning: "Tuning",
};

/** Bottom tab bar styled as a car center console. Plan §3 — Navigation. */
function CenterConsoleTabBar({ state, navigation }: any) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <View style={{ backgroundColor: theme.tabBarBackground, borderTopColor: theme.border, borderTopWidth: 1 }}>
      <MiniPlayer />
      <View style={[styles.row, { paddingBottom: Math.max(insets.bottom, spacing.sm) }]}>
        {state.routes.map((route: any, index: number) => {
          const focused = state.index === index;
          const icons = TAB_ICONS[route.name] ?? TAB_ICONS.index;
          const label = TAB_LABELS[route.name] ?? route.name;

          return (
            <Pressable
              key={route.key}
              onPress={() => {
                const event = navigation.emit({ type: "tabPress", target: route.key, canPreventDefault: true });
                if (!focused && !event.defaultPrevented) navigation.navigate(route.name);
              }}
              style={styles.tabButton}
            >
              <Ionicons
                name={focused ? icons.active : icons.inactive}
                size={22}
                color={focused ? theme.tabBarActive : theme.tabBarInactive}
              />
              <Text
                style={[
                  type.micro,
                  { color: focused ? theme.tabBarActive : theme.tabBarInactive, marginTop: 4 },
                ]}
              >
                {label.toUpperCase()}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

export default function TabsLayout() {
  return (
    <Tabs tabBar={(props) => <CenterConsoleTabBar {...props} />} screenOptions={{ headerShown: false }}>
      <Tabs.Screen name="index" />
      <Tabs.Screen name="search" />
      <Tabs.Screen name="garage" />
      <Tabs.Screen name="tuning" />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: "row", paddingTop: spacing.xs },
  tabButton: { flex: 1, alignItems: "center", justifyContent: "center", paddingVertical: 4 },
});
