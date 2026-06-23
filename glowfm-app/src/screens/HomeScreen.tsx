import React from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
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
  const { playbackState, signal, toast, dismissToast, playStation, togglePlayPause } = usePlayer();

  const list = favorites.length > 0 ? favorites : stations.slice(0, 10);
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
    <View style={styles.container}>
      <TopBar
        leftIcon="⌂"
        onLeftPress={() => navigation.navigate('Home')}
        rightIcon="☆"
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

      <Text style={styles.sectionTitle}>Favorite Stations</Text>
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
        ListEmptyComponent={<Text style={styles.empty}>No stations yet — pull to refresh.</Text>}
      />

      <PlaybackBar
        isPlaying={playbackState === 'playing'}
        onPlayPause={togglePlayPause}
        onPrevious={() => handleNeighbour(-1)}
        onNext={() => handleNeighbour(1)}
      />

      {toast && <Toast message={toast.message} onDismiss={dismissToast} />}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
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
