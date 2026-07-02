import React, { useEffect, useRef } from "react";
import { Animated, Easing, StyleSheet, View, Text } from "react-native";
import Svg, { Circle, G } from "react-native-svg";
import { useTheme } from "@/theme";
import { type } from "@/theme/typography";

const AnimatedCircle = Animated.createAnimatedComponent(Circle);

interface TachometerDialProps {
  size?: number;
  /** 0–1 needle position. When `active` is true and no value is given, pulses to simulate a live signal. */
  value?: number;
  active?: boolean;
  label?: string;
  sublabel?: string;
  strokeWidth?: number;
}

/**
 * Large circular "tachometer" dial — doubles as the volume/signal-strength
 * indicator on Home, and an RPM-style gauge synced to playback on Now
 * Playing (Plan §3, §4.4).
 */
export function TachometerDial({
  size = 220,
  value,
  active = false,
  label,
  sublabel,
  strokeWidth = 14,
}: TachometerDialProps) {
  const theme = useTheme();
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const sweep = 0.75; // gauge covers 270° like a real tachometer
  const arcLength = circumference * sweep;

  const pulseAnim = useRef(new Animated.Value(0)).current;
  const displayValue = useRef(new Animated.Value(value ?? 0)).current;

  useEffect(() => {
    if (typeof value === "number") {
      Animated.timing(displayValue, {
        toValue: value,
        duration: 400,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: false,
      }).start();
    }
  }, [value]);

  useEffect(() => {
    if (!active || typeof value === "number") return;
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, { toValue: 1, duration: 900, easing: Easing.inOut(Easing.sin), useNativeDriver: false }),
        Animated.timing(pulseAnim, { toValue: 0.35, duration: 900, easing: Easing.inOut(Easing.sin), useNativeDriver: false }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [active, value]);

  const animatedValue = typeof value === "number" ? displayValue : pulseAnim;
  // 0 -> fully hidden (offset = arcLength), 1 -> fully revealed (offset = 0)
  const strokeDashoffset = animatedValue.interpolate({
    inputRange: [0, 1],
    outputRange: [arcLength, 0],
  });

  return (
    <View style={{ width: size, height: size, alignItems: "center", justifyContent: "center" }}>
      <Svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <G rotation={135} originX={size / 2} originY={size / 2}>
          <Circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={theme.surfaceRaised}
            strokeWidth={strokeWidth}
            strokeDasharray={`${arcLength} ${circumference}`}
            strokeLinecap="round"
            fill="none"
          />
          <AnimatedCircle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={active ? theme.accent : theme.accentAlt}
            strokeWidth={strokeWidth}
            strokeDasharray={`${arcLength} ${circumference}`}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            fill="none"
          />
        </G>
      </Svg>
      <View style={styles.centerContent} pointerEvents="none">
        {label ? <Text style={[type.gaugeL, { color: theme.textPrimary }]}>{label}</Text> : null}
        {sublabel ? <Text style={[type.caption, { color: theme.textMuted, marginTop: 4 }]}>{sublabel}</Text> : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  centerContent: {
    position: "absolute",
    alignItems: "center",
    justifyContent: "center",
  },
});
