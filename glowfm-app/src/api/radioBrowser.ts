import type { LanguageOption, Station } from '../types';

// Tags we accept as proof a station is structurally ad-free (non-commercial).
// A station must carry at least one of these to ever reach the UI.
export const NON_COMMERCIAL_TAGS = ['non-commercial', 'public radio', 'community radio', 'college radio'] as const;

const FALLBACK_MIRRORS = [
  'de1.api.radio-browser.info',
  'de2.api.radio-browser.info',
  'nl1.api.radio-browser.info',
  'at1.api.radio-browser.info',
];

let cachedMirrors: string[] | null = null;

async function discoverMirrors(): Promise<string[]> {
  if (cachedMirrors && cachedMirrors.length > 0) return cachedMirrors;
  try {
    const res = await fetch('https://all.api.radio-browser.info/json/servers');
    const list: Array<{ name: string }> = await res.json();
    const hosts = list.map((s) => s.name).filter(Boolean);
    if (hosts.length > 0) {
      cachedMirrors = hosts;
      return hosts;
    }
  } catch {
    // fall through to hardcoded mirrors
  }
  cachedMirrors = FALLBACK_MIRRORS;
  return FALLBACK_MIRRORS;
}

function pickMirror(mirrors: string[], exclude: string[] = []): string {
  const candidates = mirrors.filter((m) => !exclude.includes(m));
  const pool = candidates.length > 0 ? candidates : mirrors;
  return pool[Math.floor(Math.random() * pool.length)];
}

async function requestWithFailover<T>(path: string): Promise<T> {
  const mirrors = await discoverMirrors();
  const tried: string[] = [];
  let lastError: unknown;

  for (let attempt = 0; attempt < mirrors.length; attempt++) {
    const host = pickMirror(mirrors, tried);
    tried.push(host);
    try {
      const res = await fetch(`https://${host}${path}`, {
        headers: { 'User-Agent': 'GlowFM/1.0' },
      });
      if (!res.ok) throw new Error(`Mirror ${host} responded ${res.status}`);
      return (await res.json()) as T;
    } catch (err) {
      lastError = err;
    }
  }
  throw lastError ?? new Error('All Radio Browser mirrors failed');
}

type RawStation = {
  stationuuid: string;
  name: string;
  url: string;
  url_resolved: string;
  favicon: string;
  tags: string;
  language: string;
  codec: string;
  bitrate: number;
  lastcheckok: number;
  clickcount: number;
};

function mapStation(raw: RawStation): Station {
  return {
    stationuuid: raw.stationuuid,
    name: raw.name,
    url: raw.url,
    urlResolved: raw.url_resolved || raw.url,
    favicon: raw.favicon,
    tags: raw.tags,
    language: raw.language,
    codec: raw.codec,
    bitrate: raw.bitrate,
    lastCheckOk: raw.lastcheckok === 1,
    clickCount: raw.clickcount,
  };
}

export function isNonCommercial(station: Pick<Station, 'tags'>): boolean {
  const tags = station.tags.toLowerCase();
  return NON_COMMERCIAL_TAGS.some((tag) => tags.includes(tag));
}

function languageQueryValue(language: LanguageOption): string | undefined {
  if (language === 'all' || language === 'other') return undefined;
  return language;
}

// Radio Browser's `tagList` param is an AND filter (station must carry every tag
// listed), not OR — so each accepted tag is queried separately and merged here.
export async function searchStations(language: LanguageOption, limit = 100): Promise<Station[]> {
  const lang = languageQueryValue(language);

  const resultsPerTag = await Promise.all(
    NON_COMMERCIAL_TAGS.map(async (tag) => {
      const params = new URLSearchParams({
        tag,
        order: 'clickcount',
        reverse: 'true',
        limit: String(limit),
        hidebroken: 'true',
      });
      if (lang) params.set('language', lang);
      try {
        const raw = await requestWithFailover<RawStation[]>(`/json/stations/search?${params.toString()}`);
        return raw.map(mapStation);
      } catch {
        return [];
      }
    })
  );

  const seen = new Map<string, Station>();
  for (const station of resultsPerTag.flat()) {
    if (station.lastCheckOk && isNonCommercial(station) && !seen.has(station.stationuuid)) {
      seen.set(station.stationuuid, station);
    }
  }
  return Array.from(seen.values()).sort((a, b) => b.clickCount - a.clickCount).slice(0, limit);
}

export async function getStationByUuid(uuid: string): Promise<Station | null> {
  const raw = await requestWithFailover<RawStation[]>(`/json/stations/byuuid/${uuid}`);
  const found = raw[0];
  return found ? mapStation(found) : null;
}

export async function registerClick(uuid: string): Promise<void> {
  try {
    await requestWithFailover(`/json/url/${uuid}`);
  } catch {
    // best-effort; click registration failures shouldn't block playback
  }
}
