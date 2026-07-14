export interface RGB {
  r: number;
  g: number;
  b: number;
}

export interface LabColor {
  L: number;
  a: number;
  b: number;
}

function srgbToLinear(c: number): number {
  const v = c / 255;
  return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
}

// D65 reference white.
const REF_X = 95.047;
const REF_Y = 100.0;
const REF_Z = 108.883;

function labPivot(t: number): number {
  return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16 / 116;
}

/** Converts an sRGB color (0-255 per channel) to CIE L*a*b*, which separates
 * perceived lightness from chroma — this is what makes sticker classification
 * far more robust to lighting changes than comparing raw RGB values. */
export function rgbToLab({ r, g, b }: RGB): LabColor {
  const lr = srgbToLinear(r);
  const lg = srgbToLinear(g);
  const lb = srgbToLinear(b);

  const x = (lr * 0.4124 + lg * 0.3576 + lb * 0.1805) * 100;
  const y = (lr * 0.2126 + lg * 0.7152 + lb * 0.0722) * 100;
  const z = (lr * 0.0193 + lg * 0.1192 + lb * 0.9505) * 100;

  const fx = labPivot(x / REF_X);
  const fy = labPivot(y / REF_Y);
  const fz = labPivot(z / REF_Z);

  return {
    L: 116 * fy - 16,
    a: 500 * (fx - fy),
    b: 200 * (fy - fz),
  };
}

/** CIE76 distance — a simple Euclidean distance in Lab space. Good enough
 * for classifying 6 well-separated cube colors without the cost of CIEDE2000. */
export function labDistance(a: LabColor, b: LabColor): number {
  const dL = a.L - b.L;
  const da = a.a - b.a;
  const db = a.b - b.b;
  return Math.sqrt(dL * dL + da * da + db * db);
}

/** Chroma (colorfulness) — used to tell low-saturation white/gray apart from hue-driven colors. */
export function chroma(lab: LabColor): number {
  return Math.sqrt(lab.a * lab.a + lab.b * lab.b);
}

export function hexToRgb(hex: string): RGB {
  const clean = hex.replace("#", "");
  return {
    r: parseInt(clean.slice(0, 2), 16),
    g: parseInt(clean.slice(2, 4), 16),
    b: parseInt(clean.slice(4, 6), 16),
  };
}
