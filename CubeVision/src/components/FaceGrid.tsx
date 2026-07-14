import React from "react";
import { Pressable, StyleSheet, View } from "react-native";
import { COLOR_HEX } from "../cube/constants";
import { FaceColors } from "../cube/types";
import { theme } from "../theme/theme";
import { useAppSettings } from "../hooks/useAppSettings";

interface Props {
  colors: FaceColors;
  size?: number;
  /** Sticker indices to highlight (e.g. low-confidence detections). */
  flaggedIndices?: number[];
  onStickerPress?: (index: number) => void;
}

export function FaceGrid({ colors, size = 132, flaggedIndices = [], onStickerPress }: Props) {
  const { colors: themeColors } = useAppSettings();
  const cell = (size - 8) / 3;
  return (
    <View style={[styles.grid, { width: size, height: size }]}>
      {colors.map((color, i) => {
        const flagged = flaggedIndices.includes(i);
        const Wrapper = onStickerPress ? Pressable : View;
        return (
          <Wrapper
            key={i}
            onPress={onStickerPress ? () => onStickerPress(i) : undefined}
            style={[
              styles.cell,
              {
                width: cell,
                height: cell,
                backgroundColor: COLOR_HEX[color],
                borderColor: flagged ? themeColors.warning : "rgba(0,0,0,0.12)",
                borderWidth: flagged ? 2.5 : 1,
              },
            ]}
          />
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    borderRadius: theme.radius.sm,
    overflow: "hidden",
    gap: 2,
  },
  cell: {
    borderRadius: 3,
  },
});
