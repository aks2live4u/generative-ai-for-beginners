import React from "react";
import { Pressable, StyleSheet, View } from "react-native";
import { COLOR_HEX, COLORS } from "../cube/constants";
import { CubeColor } from "../cube/types";
import { theme } from "../theme/theme";

interface Props {
  selected?: CubeColor;
  onSelect: (color: CubeColor) => void;
}

/** A picker of all 6 cube colors, used to correct a sticker in Face Review / Manual Entry. */
export function ColorSwatchGrid({ selected, onSelect }: Props) {
  return (
    <View style={styles.row}>
      {COLORS.map((color) => (
        <Pressable
          key={color}
          onPress={() => onSelect(color)}
          style={[
            styles.swatch,
            { backgroundColor: COLOR_HEX[color] },
            selected === color && styles.selected,
          ]}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    gap: 12,
    justifyContent: "center",
  },
  swatch: {
    width: 40,
    height: 40,
    borderRadius: theme.radius.pill,
    borderWidth: 1,
    borderColor: "rgba(0,0,0,0.12)",
  },
  selected: {
    borderWidth: 3,
    borderColor: theme.colors.primary,
  },
});
