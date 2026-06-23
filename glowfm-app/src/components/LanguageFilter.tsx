import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';
import { colors, radii } from '../theme/theme';
import type { LanguageOption } from '../types';

const OPTIONS: { value: LanguageOption; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'english', label: 'English' },
  { value: 'spanish', label: 'Spanish' },
  { value: 'french', label: 'French' },
  { value: 'german', label: 'German' },
  { value: 'other', label: 'Other' },
];

type Props = {
  value: LanguageOption;
  onChange: (value: LanguageOption) => void;
};

export default function LanguageFilter({ value, onChange }: Props) {
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.scroll} contentContainerStyle={styles.content}>
      {OPTIONS.map((option) => {
        const selected = option.value === value;
        return (
          <Pressable
            key={option.value}
            onPress={() => onChange(option.value)}
            style={[styles.pill, selected && styles.pillSelected]}
            accessibilityRole="button"
            accessibilityLabel={`Filter by ${option.label}`}
          >
            <Text style={[styles.label, selected && styles.labelSelected]}>{option.label}</Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scroll: {
    flexGrow: 0,
  },
  content: {
    paddingHorizontal: 20,
    gap: 8,
  },
  pill: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: radii.pill,
    backgroundColor: colors.panel,
    borderWidth: 1,
    borderColor: colors.panelBorder,
  },
  pillSelected: {
    backgroundColor: 'rgba(157,77,255,0.25)',
    borderColor: colors.neonPurple,
  },
  label: {
    color: colors.textSecondary,
    fontSize: 13,
    fontWeight: '600',
  },
  labelSelected: {
    color: colors.textPrimary,
  },
});
