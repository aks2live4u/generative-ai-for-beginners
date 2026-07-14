import React from "react";
import { StyleSheet, Text, View, useWindowDimensions } from "react-native";
import { useAppSettings } from "../hooks/useAppSettings";

interface Props {
  /** Overlay square's side length as a fraction of the shorter screen dimension. */
  sizeFraction?: number;
  statusLabel?: string;
  statusColor?: string;
  rotationHint?: string;
}

export function CubeOverlay({ sizeFraction = 0.72, statusLabel, statusColor, rotationHint }: Props) {
  const { width, height } = useWindowDimensions();
  const { colors } = useAppSettings();
  const side = Math.min(width, height) * sizeFraction;

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="none">
      <View style={styles.center}>
        <View style={[styles.square, { width: side, height: side, borderColor: statusColor ?? colors.primary }]}>
          {[1, 2].map((i) => (
            <View key={`v${i}`} style={[styles.gridLineV, { left: (side / 3) * i }]} />
          ))}
          {[1, 2].map((i) => (
            <View key={`h${i}`} style={[styles.gridLineH, { top: (side / 3) * i }]} />
          ))}
        </View>
        {statusLabel ? (
          <View style={[styles.badge, { backgroundColor: statusColor ?? colors.primary }]}>
            <Text style={styles.badgeText}>{statusLabel}</Text>
          </View>
        ) : null}
        {rotationHint ? (
          <View style={styles.hintBubble}>
            <Text style={styles.hintText}>{rotationHint}</Text>
          </View>
        ) : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  center: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  square: {
    borderWidth: 3,
    borderRadius: 16,
  },
  gridLineV: {
    position: "absolute",
    top: 0,
    bottom: 0,
    width: 1,
    backgroundColor: "rgba(255,255,255,0.6)",
  },
  gridLineH: {
    position: "absolute",
    left: 0,
    right: 0,
    height: 1,
    backgroundColor: "rgba(255,255,255,0.6)",
  },
  badge: {
    marginTop: 16,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
  },
  badgeText: {
    color: "white",
    fontWeight: "600",
  },
  hintBubble: {
    position: "absolute",
    bottom: 40,
    marginHorizontal: 24,
    backgroundColor: "rgba(0,0,0,0.65)",
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  hintText: {
    color: "white",
    textAlign: "center",
    fontSize: 15,
  },
});
