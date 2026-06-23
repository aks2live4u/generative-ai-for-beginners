import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { colors, gradients, radii } from '../theme/theme';
import type { Station } from '../types';
import { pseudoFrequency } from '../utils/pseudoFrequency';

type Props = {
  station: Station;
  isFavorite: boolean;
  isActive: boolean;
  onPress: () => void;
  onToggleFavorite: () => void;
};

export default function StationRow({ station, isFavorite, isActive, onPress, onToggleFavorite }: Props) {
  return (
    <Pressable
      onPress={onPress}
      style={[styles.row, isActive && styles.rowActive]}
      accessibilityRole="button"
      accessibilityLabel={`${station.name}, ${pseudoFrequency(station.stationuuid).toFixed(1)} megahertz, non-commercial station`}
    >
      <LinearGradient colors={gradients.primary} style={styles.artwork} />
      <View style={styles.info}>
        <Text style={styles.name} numberOfLines={1}>
          {station.name}
        </Text>
        <View style={styles.tagRow}>
          <Text style={styles.freq}>{pseudoFrequency(station.stationuuid).toFixed(1)} MHz</Text>
          <View style={styles.badge}>
            <Text style={styles.badgeText}>Non-Comm</Text>
          </View>
        </View>
      </View>
      <Pressable
        onPress={onToggleFavorite}
        style={styles.favButton}
        accessibilityRole="button"
        accessibilityLabel={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
      >
        <Text style={[styles.favIcon, isFavorite && styles.favIconActive]}>{isFavorite ? '★' : '☆'}</Text>
      </Pressable>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderRadius: radii.card,
    backgroundColor: colors.panel,
    borderWidth: 1,
    borderColor: colors.panelBorder,
    marginBottom: 10,
  },
  rowActive: {
    borderColor: colors.neonCyan,
  },
  artwork: {
    width: 44,
    height: 44,
    borderRadius: 12,
  },
  info: {
    flex: 1,
    marginLeft: 12,
  },
  name: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '600',
  },
  tagRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
    gap: 8,
  },
  freq: {
    color: colors.textSecondary,
    fontSize: 12,
  },
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: radii.pill,
    backgroundColor: 'rgba(77,232,255,0.15)',
    borderWidth: 1,
    borderColor: 'rgba(77,232,255,0.4)',
  },
  badgeText: {
    color: colors.neonCyan,
    fontSize: 10,
    fontWeight: '700',
  },
  favButton: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  favIcon: {
    fontSize: 20,
    color: colors.textSecondary,
  },
  favIconActive: {
    color: colors.neonPink,
  },
});
