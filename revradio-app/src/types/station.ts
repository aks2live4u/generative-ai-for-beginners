/** Shape returned by the Radio-Browser API (subset of fields RevRadio uses). */
export interface RawStation {
  stationuuid: string;
  name: string;
  url: string;
  url_resolved: string;
  homepage: string;
  favicon: string;
  tags: string;
  country: string;
  countrycode: string;
  language: string;
  languagecodes: string;
  votes: number;
  clickcount: number;
  clicktrend: number;
  codec: string;
  bitrate: number;
  hls: number;
  lastcheckok: number;
  lastchecktime: string;
}

/** Normalized station shape used throughout the RevRadio UI. */
export interface Station {
  id: string;
  name: string;
  streamUrl: string;
  homepage: string;
  favicon: string | null;
  tags: string[];
  country: string;
  countryCode: string;
  language: string;
  votes: number;
  clickCount: number;
  codec: string;
  bitrate: number;
  isSecure: boolean;
  isLikelyReliable: boolean;
  isAdFreeReported: boolean;
}

export function normalizeStation(raw: RawStation, adFreeReportedIds?: Set<string>): Station {
  return {
    id: raw.stationuuid,
    name: raw.name?.trim() || "Unnamed Station",
    streamUrl: raw.url_resolved || raw.url,
    homepage: raw.homepage,
    favicon: raw.favicon || null,
    tags: raw.tags ? raw.tags.split(",").map((t) => t.trim()).filter(Boolean) : [],
    country: raw.country || "Unknown",
    countryCode: (raw.countrycode || "").toUpperCase(),
    language: raw.language || "Unknown",
    votes: raw.votes ?? 0,
    clickCount: raw.clickcount ?? 0,
    codec: (raw.codec || "?").toUpperCase(),
    bitrate: raw.bitrate ?? 0,
    isSecure: (raw.url_resolved || raw.url || "").startsWith("https://"),
    isLikelyReliable: raw.lastcheckok === 1,
    isAdFreeReported: adFreeReportedIds?.has(raw.stationuuid) ?? false,
  };
}

export interface StationFilters {
  query: string;
  languages: string[];
  countryCodes: string[];
  tags: string[];
  codec: string | null;
  minBitrateKbps: number;
  adFreeOnly: boolean;
  httpsOnly: boolean;
  reliableOnly: boolean;
  sortBy: "popularity" | "alphabetical" | "bitrate" | "recent";
}

export const defaultFilters: StationFilters = {
  query: "",
  languages: [],
  countryCodes: [],
  tags: [],
  codec: null,
  minBitrateKbps: 0,
  adFreeOnly: false,
  httpsOnly: false,
  reliableOnly: false,
  sortBy: "popularity",
};

export interface FilterPreset {
  id: string;
  name: string;
  filters: StationFilters;
  createdAt: number;
}

export interface Fleet {
  id: string;
  name: string;
  stationIds: string[];
  createdAt: number;
}

export interface HistoryEntry {
  stationId: string;
  playedAt: number;
}
