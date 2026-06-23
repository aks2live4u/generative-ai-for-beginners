import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors } from '../theme/theme';

type Props = {
  leftIcon: string;
  onLeftPress?: () => void;
  rightIcon: string;
  onRightPress?: () => void;
  rightActive?: boolean;
};

export default function TopBar({ leftIcon, onLeftPress, rightIcon, onRightPress, rightActive }: Props) {
  return (
    <View style={styles.bar}>
      <Pressable
        onPress={onLeftPress}
        style={styles.iconButton}
        accessibilityRole="button"
        accessibilityLabel="Home"
      >
        <Text style={styles.icon}>{leftIcon}</Text>
      </Pressable>
      <Text style={styles.title}>GlowFM</Text>
      <Pressable
        onPress={onRightPress}
        style={styles.iconButton}
        accessibilityRole="button"
        accessibilityLabel="Settings"
      >
        <Text style={[styles.icon, rightActive && styles.iconActive]}>{rightIcon}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 8,
  },
  iconButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  icon: {
    fontSize: 20,
    color: colors.textSecondary,
  },
  iconActive: {
    color: colors.neonCyan,
  },
  title: {
    color: colors.textPrimary,
    fontSize: 20,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
});
