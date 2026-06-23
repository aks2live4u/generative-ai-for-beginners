import React, { useEffect } from 'react';
import { StatusBar } from 'expo-status-bar';
import { NavigationContainer, DarkTheme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { PlayerProvider } from './src/audio/PlayerContext';
import { useRadioStore } from './src/store/useRadioStore';
import { colors } from './src/theme/theme';
import type { RootStackParamList } from './src/navigation/types';
import HomeScreen from './src/screens/HomeScreen';
import NowPlayingScreen from './src/screens/NowPlayingScreen';
import SettingsScreen from './src/screens/SettingsScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();

const navTheme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    background: colors.background,
    card: colors.background,
    text: colors.textPrimary,
    border: colors.panelBorder,
  },
};

export default function App() {
  const hydrate = useRadioStore((s) => s.hydrate);

  useEffect(() => {
    hydrate();
  }, []);

  return (
    <SafeAreaProvider>
      <PlayerProvider>
        <NavigationContainer theme={navTheme}>
          <Stack.Navigator screenOptions={{ headerShown: false }}>
            <Stack.Screen name="Home" component={HomeScreen} />
            <Stack.Screen name="NowPlaying" component={NowPlayingScreen} />
            <Stack.Screen name="Settings" component={SettingsScreen} />
          </Stack.Navigator>
        </NavigationContainer>
        <StatusBar style="light" />
      </PlayerProvider>
    </SafeAreaProvider>
  );
}
