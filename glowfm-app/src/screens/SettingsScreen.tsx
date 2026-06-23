import React, { useState } from 'react';
import { FlatList, Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import AsyncStorage from '@react-native-async-storage/async-storage';
import ScreenBackground from '../components/ScreenBackground';
import TopBar from '../components/TopBar';
import StationRow from '../components/StationRow';
import LanguageFilter from '../components/LanguageFilter';
import { colors, radii } from '../theme/theme';
import { useRadioStore } from '../store/useRadioStore';
import { usePlayer } from '../audio/PlayerContext';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Settings'>;

export default function SettingsScreen({ navigation }: Props) {
  const favorites = useRadioStore((s) => s.favorites);
  const currentStation = useRadioStore((s) => s.currentStation);
  const toggleFavorite = useRadioStore((s) => s.toggleFavorite);
  const language = useRadioStore((s) => s.language);
  const setLanguage = useRadioStore((s) => s.setLanguage);
  const refreshStations = useRadioStore((s) => s.refreshStations);
  const { playStation } = usePlayer();
  const [dataSaver, setDataSaver] = useState(false);

  const clearCache = async () => {
    await AsyncStorage.removeMany(['glowfm:stationCache', 'glowfm:unreliable']);
    refreshStations();
  };

  return (
    <ScreenBackground style={styles.container}>
      <TopBar
        leftIcon="⌂"
        onLeftPress={() => navigation.navigate('Home')}
        rightIcon="⚙"
        rightActive
        onRightPress={() => navigation.navigate('Settings')}
      />

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Language</Text>
        <LanguageFilter value={language} onChange={setLanguage} />
      </View>

      <View style={styles.section}>
        <View style={styles.row}>
          <Text style={styles.rowLabel}>Data-saver mode</Text>
          <Switch value={dataSaver} onValueChange={setDataSaver} />
        </View>
        <Pressable onPress={clearCache} style={styles.clearButton} accessibilityRole="button">
          <Text style={styles.clearButtonText}>Clear cache</Text>
        </Pressable>
      </View>

      <Text style={styles.sectionTitle}>Favorite Stations</Text>
      <FlatList
        data={favorites}
        keyExtractor={(item) => item.stationuuid}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <StationRow
            station={item}
            isFavorite
            isActive={currentStation?.stationuuid === item.stationuuid}
            onPress={() => playStation(item)}
            onToggleFavorite={() => toggleFavorite(item)}
          />
        )}
        ListEmptyComponent={<Text style={styles.empty}>No favorites yet — star a station from Home.</Text>}
      />
    </ScreenBackground>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  section: {
    paddingHorizontal: 20,
    marginBottom: 16,
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '700',
    paddingHorizontal: 20,
    marginBottom: 8,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 10,
  },
  rowLabel: {
    color: colors.textPrimary,
    fontSize: 14,
    fontWeight: '600',
  },
  clearButton: {
    marginTop: 4,
    paddingVertical: 10,
    borderRadius: radii.card,
    backgroundColor: colors.panel,
    borderWidth: 1,
    borderColor: colors.panelBorder,
    alignItems: 'center',
  },
  clearButtonText: {
    color: colors.neonPink,
    fontWeight: '600',
  },
  list: {
    paddingHorizontal: 20,
    flexGrow: 1,
  },
  empty: {
    color: colors.textSecondary,
    textAlign: 'center',
    marginTop: 20,
  },
});
