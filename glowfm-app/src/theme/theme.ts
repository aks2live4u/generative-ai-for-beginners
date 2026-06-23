export const colors = {
  background: '#0B0B1E',
  panel: 'rgba(255,255,255,0.06)',
  panelBorder: 'rgba(255,255,255,0.12)',
  textPrimary: '#F5F4FF',
  textSecondary: '#9C9CC4',
  neonPurple: '#9D4DFF',
  neonPink: '#FF4DD8',
  neonCyan: '#4DE8FF',
};

export const gradients = {
  primary: [colors.neonPurple, colors.neonPink, colors.neonCyan] as const,
  glowSoft: ['rgba(157,77,255,0.35)', 'rgba(255,77,216,0.25)', 'rgba(77,232,255,0.2)'] as const,
};

export const radii = {
  card: 22,
  pill: 999,
};
