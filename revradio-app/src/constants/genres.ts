/** Curated genre "showrooms" mapped to Radio-Browser tag queries. */
export interface GenreShowroom {
  id: string;
  label: string;
  tag: string;
  icon: string; // @expo/vector-icons Ionicons name
}

export const GENRE_SHOWROOMS: GenreShowroom[] = [
  { id: "rock", label: "Rock", tag: "rock", icon: "flash" },
  { id: "news", label: "News", tag: "news", icon: "newspaper" },
  { id: "talk", label: "Talk", tag: "talk", icon: "mic" },
  { id: "classical", label: "Classical", tag: "classical", icon: "musical-notes" },
  { id: "edm", label: "EDM", tag: "electronic", icon: "pulse" },
  { id: "jazz", label: "Jazz", tag: "jazz", icon: "disc" },
  { id: "pop", label: "Pop", tag: "pop", icon: "star" },
  { id: "sports", label: "Sports", tag: "sport", icon: "football" },
  { id: "hiphop", label: "Hip-Hop", tag: "hiphop", icon: "headset" },
  { id: "country", label: "Country", tag: "country", icon: "trail-sign" },
  { id: "oldies", label: "Oldies", tag: "oldies", icon: "time" },
  { id: "chill", label: "Chill", tag: "chillout", icon: "moon" },
];

export const ONBOARDING_LANGUAGES = [
  "English",
  "Spanish",
  "French",
  "German",
  "Portuguese",
  "Arabic",
  "Hindi",
  "Japanese",
  "Korean",
  "Italian",
  "Russian",
  "Mandarin",
];

export const EQ_PRESETS = [
  { id: "flat", label: "Flat", description: "No coloration — reference sound" },
  { id: "highway", label: "Highway", description: "Boosted lows/highs to cut through road noise" },
  { id: "city", label: "City", description: "Balanced mids for stop-and-go listening" },
  { id: "track-day", label: "Track Day", description: "Punchy, high-energy tuning" },
] as const;

export type EqPresetId = (typeof EQ_PRESETS)[number]["id"];
