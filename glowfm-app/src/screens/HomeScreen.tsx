import React, { useCallback, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import ScreenBackground from '../components/ScreenBackground';
import TopBar from '../components/TopBar';
import FrequencyDial from '../components/FrequencyDial';
import Equalizer from '../components/Equalizer';
import StationRow from '../components/StationRow';
import PlaybackBar from '../components/PlaybackBar';
import Toast from '../components/Toast';
import { colors } from '../theme/theme';
import { useRadioStore } from '../store/useRadioStore';
import { usePlayer } from '../audio/PlayerContext';
import { pseudoFrequency } from '../utils/pseudoFrequency';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Home'>;

export default function HomeScreen({ navigation }: Props) {
  const favorites = useRadioStore((s) => s.favorites);
  const stations = useRadioStore((s) => s.stations);
  const currentStation = useRadioStore((s) => s.currentStation);
  const toggleFavorite = useRadioStore((s) => s.toggleFavorite);
  const refreshStations = useRadioStore((s) => s.refreshStations);
  const error = useRadioStore((s) => s.error);
  const { playbackState, signal, toast, dismissToast, playStation, togglePlayPause } = usePlayer();

  const [refreshing, setRefreshing] = useState(false);
  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await refreshStations();
    setRefreshing(false);
  }, [refreshStations]);

  const list = favorites.length > 0 ? favorites : stations.slice(0, 10);
  const listTitle = favorites.length > 0 ? 'Favorite Stations' : 'Top Stations';
  const dialStation = currentStation ?? list[0] ?? null;

  const handleSelectStation = async (station: typeof stations[number]) => {
    await playStation(station);
    navigation.navigate('NowPlaying');
  };

  const handleNeighbour = (direction: 1 | -1) => {
    if (!currentStation || list.length === 0) return;
    const index = list.findIndex((s) => s.stationuuid === currentStation.stationuuid);
    const nextIndex = (index + direction + list.length) % list.length;
    handleSelectStation(list[nextIndex]);
  };

  return (
    <ScreenBackground style={styles.container}>
      <TopBar
        leftIcon="⌂"
        onLeftPress={() => navigation.navigate('Home')}
        rightIcon="⚙"
        onRightPress={() => navigation.navigate('Settings')}
      />

      <View style={styles.dialSection}>
        <FrequencyDial
          frequency={dialStation ? pseudoFrequency(dialStation.stationuuid) : 87.5}
          stationName={dialStation?.name ?? 'No station selected'}
          signal={signal}
        />
        <Equalizer active={playbackState === 'playing'} />
      </View>

      <Text style={styles.sectionTitle}>{listTitle}</Text>
      <FlatList
        data={list}
        keyExtractor={(item) => item.stationuuid}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <StationRow
            station={item}
            isFavorite={favorites.some((f) => f.stationuuid === item.stationuuid)}
            isActive={currentStation?.stationuuid === item.stationuuid}
            onPress={() => handleSelectStation(item)}
            onToggleFavorite={() => toggleFavorite(item)}
          />
        )}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.neonCyan} />
        }
        ListEmptyComponent={
          <Text style={styles.empty}>
            {error ? `Couldn't load stations: ${error}` : 'No stations yet — pull to refresh.'}
          </Text>
        }
      />

      <PlaybackBar
        isPlaying={playbackState === 'playing'}
        onPlayPause={togglePlayPause}
        onPrevious={() => handleNeighbour(-1)}
        onNext={() => handleNeighbour(1)}
      />

      {toast && <Toast message={toast.message} onDismiss={dismissToast} />}
    </ScreenBackground>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  dialSection: {
    alignItems: 'center',
    paddingVertical: 12,
    gap: 16,
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '700',
    paddingHorizontal: 20,
    marginBottom: 8,
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
