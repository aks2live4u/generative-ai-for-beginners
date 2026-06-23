import React, { useEffect } from 'react';
import { StyleSheet, Text } from 'react-native';
import { colors, radii } from '../theme/theme';

type Props = {
  message: string;
  onDismiss: () => void;
};

export default function Toast({ message, onDismiss }: Props) {
  useEffect(() => {
    const timer = setTimeout(onDismiss, 3000);
    return () => clearTimeout(timer);
  }, [message]);

  return (
    <Text style={styles.toast} accessibilityRole="alert">
      {message}
    </Text>
  );
}

const styles = StyleSheet.create({
  toast: {
    position: 'absolute',
    bottom: 110,
    alignSelf: 'center',
    backgroundColor: 'rgba(20,20,40,0.95)',
    color: colors.textPrimary,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: radii.card,
    borderWidth: 1,
    borderColor: colors.panelBorder,
    fontSize: 13,
    overflow: 'hidden',
  },
});
