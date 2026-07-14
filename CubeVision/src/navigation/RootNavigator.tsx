import React from "react";
import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { RootStackParamList } from "./types";
import { HomeScreen } from "../screens/HomeScreen";
import { ScanScreen } from "../screens/ScanScreen";
import { ManualEntryScreen } from "../screens/ManualEntryScreen";
import { SolveScreen } from "../screens/SolveScreen";
import { SettingsScreen } from "../screens/SettingsScreen";
import { HelpScreen } from "../screens/HelpScreen";

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShadowVisible: false }}>
        <Stack.Screen name="Home" component={HomeScreen} options={{ headerShown: false }} />
        <Stack.Screen name="Scan" component={ScanScreen} options={{ title: "Scan Cube" }} />
        <Stack.Screen name="ManualEntry" component={ManualEntryScreen} options={{ title: "Manual Entry" }} />
        <Stack.Screen name="Solve" component={SolveScreen} options={{ title: "Solution", headerBackVisible: false }} />
        <Stack.Screen name="Settings" component={SettingsScreen} />
        <Stack.Screen name="Help" component={HelpScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
