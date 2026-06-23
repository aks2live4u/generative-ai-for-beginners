import React from 'react';
import { StyleSheet, View, type ViewStyle } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';

type Props = {
  children: React.ReactNode;
  style?: ViewStyle;
};

export default function ScreenBackground({ children, style }: Props) {
  return (
    <View style={[styles.container, style]}>
      <LinearGradient
        colors={['#0B0B1E', '#160F2E', '#1A0E2A', '#0B0B1E']}
        locations={[0, 0.35, 0.7, 1]}
        style={StyleSheet.absoluteFill}
      />
      <View style={[styles.glow, styles.glowTopLeft]} />
      <View style={[styles.glow, styles.glowBottomRight]} />
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  glow: {
    position: 'absolute',
    width: 280,
    height: 280,
    borderRadius: 280,
    opacity: 0.18,
  },
  glowTopLeft: {
    top: -100,
    left: -80,
    backgroundColor: '#9D4DFF',
  },
  glowBottomRight: {
    bottom: -60,
    right: -100,
    backgroundColor: '#4DE8FF',
  },
});
