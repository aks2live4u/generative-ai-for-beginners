import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Svg, { Circle, Defs, LinearGradient, Stop, Line } from 'react-native-svg';
import { colors } from '../theme/theme';
import type { SignalStrength } from '../types';

const SIZE = 220;
const STROKE = 14;
const RADIUS = (SIZE - STROKE) / 2;
const MIN_FREQ = 87.5;
const MAX_FREQ = 108.0;
const START_ANGLE = -210;
const SWEEP_ANGLE = 240;

function angleForFrequency(freq: number): number {
  const ratio = (freq - MIN_FREQ) / (MAX_FREQ - MIN_FREQ);
  return START_ANGLE + ratio * SWEEP_ANGLE;
}

function polarPoint(angleDeg: number, radius: number) {
  const angleRad = (angleDeg * Math.PI) / 180;
  return {
    x: SIZE / 2 + radius * Math.cos(angleRad),
    y: SIZE / 2 + radius * Math.sin(angleRad),
  };
}

type Props = {
  frequency: number;
  stationName: string;
  signal: SignalStrength;
};

export default function FrequencyDial({ frequency, stationName, signal }: Props) {
  const needleAngle = angleForFrequency(frequency);
  const needleTip = polarPoint(needleAngle, RADIUS - STROKE);
  const center = SIZE / 2;

  return (
    <View
      style={styles.wrapper}
      accessible
      accessibilityRole="image"
      accessibilityLabel={`Tuned to ${frequency.toFixed(1)} megahertz, ${stationName}, signal ${signal}`}
    >
      <Svg width={SIZE} height={SIZE}>
        <Defs>
          <LinearGradient id="dialGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <Stop offset="0%" stopColor={colors.neonPurple} />
            <Stop offset="50%" stopColor={colors.neonPink} />
            <Stop offset="100%" stopColor={colors.neonCyan} />
          </LinearGradient>
        </Defs>
        <Circle
          cx={center}
          cy={center}
          r={RADIUS}
          stroke="rgba(255,255,255,0.08)"
          strokeWidth={STROKE}
          fill="none"
        />
        <Circle
          cx={center}
          cy={center}
          r={RADIUS}
          stroke="url(#dialGradient)"
          strokeWidth={STROKE}
          strokeLinecap="round"
          strokeDasharray={`${(SWEEP_ANGLE / 360) * 2 * Math.PI * RADIUS} ${2 * Math.PI * RADIUS}`}
          strokeDashoffset={-((START_ANGLE + 360) / 360) * 2 * Math.PI * RADIUS}
          fill="none"
          transform={`rotate(0 ${center} ${center})`}
        />
        <Line
          x1={center}
          y1={center}
          x2={needleTip.x}
          y2={needleTip.y}
          stroke={colors.neonCyan}
          strokeWidth={4}
          strokeLinecap="round"
        />
        <Circle cx={center} cy={center} r={6} fill={colors.neonCyan} />
      </Svg>
      <View style={styles.labelOverlay} pointerEvents="none">
        <Text style={styles.frequencyText}>{frequency.toFixed(1)}</Text>
        <Text style={styles.unitText}>MHz</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    width: SIZE,
    height: SIZE,
    alignItems: 'center',
    justifyContent: 'center',
  },
  labelOverlay: {
    position: 'absolute',
    alignItems: 'center',
  },
  frequencyText: {
    color: colors.textPrimary,
    fontSize: 38,
    fontWeight: '800',
  },
  unitText: {
    color: colors.textSecondary,
    fontSize: 14,
    fontWeight: '600',
    marginTop: -2,
  },
});
