import React, { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, View } from 'react-native';
import { colors } from '../theme/theme';

const BAR_COUNT = 5;
const BAR_COLORS = [colors.neonPurple, colors.neonPink, colors.neonCyan, colors.neonPink, colors.neonPurple];

type Props = {
  active: boolean;
};

export default function Equalizer({ active }: Props) {
  const heights = useRef(Array.from({ length: BAR_COUNT }, () => new Animated.Value(0.3))).current;

  useEffect(() => {
    if (!active) {
      heights.forEach((h) => h.setValue(0.2));
      return;
    }
    const animations = heights.map((value, index) =>
      Animated.loop(
        Animated.sequence([
          Animated.timing(value, {
            toValue: 0.4 + Math.random() * 0.6,
            duration: 280 + index * 60,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: false,
          }),
          Animated.timing(value, {
            toValue: 0.2 + Math.random() * 0.4,
            duration: 280 + index * 60,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: false,
          }),
        ])
      )
    );
    animations.forEach((a) => a.start());
    return () => animations.forEach((a) => a.stop());
  }, [active]);

  return (
    <View style={styles.row} accessible={false}>
      {heights.map((value, index) => (
        <Animated.View
          key={index}
          style={[
            styles.bar,
            {
              backgroundColor: BAR_COLORS[index % BAR_COLORS.length],
              height: value.interpolate({ inputRange: [0, 1], outputRange: [4, 44] }),
            },
          ]}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'center',
    gap: 6,
    height: 44,
  },
  bar: {
    width: 6,
    borderRadius: 3,
  },
});
