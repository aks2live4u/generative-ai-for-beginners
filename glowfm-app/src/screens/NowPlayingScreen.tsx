import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import ScreenBackground from '../components/ScreenBackground';
import TopBar from '../components/TopBar';
import FrequencyDial from '../components/FrequencyDial';
import Equalizer from '../components/Equalizer';
import PlaybackBar from '../components/PlaybackBar';
import Toast from '../components/Toast';
import { colors, radii } from '../theme/theme';
import { useRadioStore } from '../store/useRadioStore';
import { usePlayer } from '../audio/PlayerContext';
import { pseudoFrequency } from '../utils/pseudoFrequency';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'NowPlaying'>;

const SIGNAL_LABEL: Record<string, string> = {
  excellent: 'Excellent',
  good: 'Good',
  weak: 'Weak',
};

export default function NowPlayingScreen({ navigation }: Props) {
  const currentStation = useRadioStore((s) => s.currentStation);
  const stations = useRadioStore((s) => s.stations);
  const favorites = useRadioStore((s) => s.favorites);
  const { playbackState, signal, toast, dismissToast, playStation, togglePlayPause } = usePlayer();

  const list = favorites.length > 0 ? favorites : stations;

  const handleNeighbour = (direction: 1 | -1) => {
    if (!currentStation || list.length === 0) return;
    const index = list.findIndex((s) => s.stationuuid === currentStation.stationuuid);
    const nextIndex = (index + direction + list.length) % list.length;
    playStation(list[nextIndex]);
  };

  const genre = currentStation?.tags.split(',')[0]?.trim() || 'Radio';

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
          frequency={currentStation ? pseudoFrequency(currentStation.stationuuid) : 87.5}
          stationName={currentStation?.name ?? 'No station selected'}
          signal={signal}
        />
        <Equalizer active={playbackState === 'playing'} />
        <Text style={styles.signalText}>Signal: {SIGNAL_LABEL[signal]}</Text>
      </View>

      <View style={styles.nowPlayingCard}>
        <Text style={styles.label}>Now Playing</Text>
        <Text style={styles.stationName} numberOfLines={1}>
          {currentStation?.name ?? 'No station selected'}
        </Text>
        {/* TODO: parse ICY metadata (StreamTitle) for live artist/track info */}
        <Text style={styles.track}>Live Broadcast</Text>
        <View style={styles.genreBadge}>
          <Text style={styles.genreText}>{genre}</Text>
        </View>
      </View>

      <View style={styles.spacer} />

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
    gap: 12,
  },
  signalText: {
    color: colors.textSecondary,
    fontSize: 12,
    fontWeight: '600',
  },
  nowPlayingCard: {
    marginHorizontal: 20,
    marginTop: 16,
    padding: 18,
    borderRadius: radii.card,
    backgroundColor: colors.panel,
    borderWidth: 1,
    borderColor: colors.panelBorder,
    alignItems: 'center',
  },
  label: {
    color: colors.textSecondary,
    fontSize: 12,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  stationName: {
    color: colors.textPrimary,
    fontSize: 20,
    fontWeight: '700',
    marginTop: 6,
  },
  track: {
    color: colors.textSecondary,
    fontSize: 14,
    marginTop: 4,
  },
  genreBadge: {
    marginTop: 10,
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: radii.pill,
    backgroundColor: 'rgba(157,77,255,0.2)',
    borderWidth: 1,
    borderColor: 'rgba(157,77,255,0.4)',
  },
  genreText: {
    color: colors.neonPurple,
    fontSize: 12,
    fontWeight: '600',
  },
  spacer: {
    flex: 1,
  },
});
