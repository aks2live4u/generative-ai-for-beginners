import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { colors, gradients, radii } from '../theme/theme';

type Props = {
  isPlaying: boolean;
  onPlayPause: () => void;
  onPrevious: () => void;
  onNext: () => void;
};

export default function PlaybackBar({ isPlaying, onPlayPause, onPrevious, onNext }: Props) {
  return (
    <View style={styles.bar}>
      <Pressable onPress={onPrevious} style={styles.sideButton} accessibilityRole="button" accessibilityLabel="Previous station">
        <Text style={styles.sideIcon}>⏮</Text>
      </Pressable>
      <Pressable
        onPress={onPlayPause}
        accessibilityRole="button"
        accessibilityLabel={isPlaying ? 'Pause' : 'Play'}
      >
        <LinearGradient colors={gradients.primary} style={styles.playButton}>
          <Text style={styles.playIcon}>{isPlaying ? '⏸' : '▶'}</Text>
        </LinearGradient>
      </Pressable>
      <Pressable onPress={onNext} style={styles.sideButton} accessibilityRole="button" accessibilityLabel="Next station">
        <Text style={styles.sideIcon}>⏭</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 28,
    paddingVertical: 20,
  },
  sideButton: {
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radii.pill,
    backgroundColor: colors.panel,
    borderWidth: 1,
    borderColor: colors.panelBorder,
  },
  sideIcon: {
    fontSize: 18,
    color: colors.textPrimary,
  },
  playButton: {
    width: 68,
    height: 68,
    borderRadius: radii.pill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  playIcon: {
    fontSize: 26,
    color: '#0B0B1E',
  },
});
