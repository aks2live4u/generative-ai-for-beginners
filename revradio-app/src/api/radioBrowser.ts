import { RawStation, Station, normalizeStation } from "@/types/station";
import { StationFilters } from "@/types/station";
import { getCached, setCached, getStale } from "./cache";

/**
 * Radio-Browser API client (radio-browser.info) — see RevRadio App Plan §2.
 *
 * The service is served from multiple load-balanced mirrors. There is no
 * single stable hostname, so on startup we resolve the current mirror list
 * from `all.api.radio-browser.info` and round-robin across them. If that
 * resolution fails (offline, DNS hiccup) we fall back to a hardcoded list
 * of known-stable mirrors so the app still functions.
 */

const DISCOVERY_URL = "https://all.api.radio-browser.info/json/servers";
const FALLBACK_MIRRORS = [
  "https://de1.api.radio-browser.info",
  "https://de2.api.radio-browser.info",
  "https://nl1.api.radio-browser.info",
  "https://at1.api.radio-browser.info",
];
const MIRROR_CACHE_KEY = "mirrors";
const MIRROR_TTL_MS = 24 * 60 * 60 * 1000; // 24h
const USER_AGENT = "RevRadio/1.0 (github.com/revradio)";

let mirrorPool: string[] = [];
let mirrorIndex = 0;
let resolvingMirrors: Promise<string[]> | null = null;

interface ServerEntry {
  name: string;
}

async function resolveMirrors(): Promise<string[]> {
  const cached = await getCached<string[]>(MIRROR_CACHE_KEY);
  if (cached && cached.length) return cached;

  try {
    const res = await fetch(DISCOVERY_URL, { headers: { "User-Agent": USER_AGENT } });
    if (!res.ok) throw new Error(`Mirror discovery failed: ${res.status}`);
    const servers: ServerEntry[] = await res.json();
    const hosts = servers.map((s) => `https://${s.name}`).filter(Boolean);
    if (!hosts.length) throw new Error("Empty mirror list");
    await setCached(MIRROR_CACHE_KEY, hosts, MIRROR_TTL_MS);
    return hosts;
  } catch {
    return FALLBACK_MIRRORS;
  }
}

async function getMirrorPool(): Promise<string[]> {
  if (mirrorPool.length) return mirrorPool;
  if (!resolvingMirrors) resolvingMirrors = resolveMirrors();
  mirrorPool = await resolvingMirrors;
  return mirrorPool;
}

function nextMirror(pool: string[]): string {
  const mirror = pool[mirrorIndex % pool.length];
  mirrorIndex += 1;
  return mirror;
}

/** Fetch JSON from the API, round-robining mirrors and retrying on failure. */
async function apiFetch<T>(path: string, params?: Record<string, string | number | boolean | undefined>): Promise<T> {
  const pool = await getMirrorPool();
  const attempts = Math.min(pool.length, 4);
  let lastError: unknown;

  for (let attempt = 0; attempt < attempts; attempt++) {
    const base = nextMirror(pool);
    const url = new URL(base + path);
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== "") url.searchParams.set(key, String(value));
      });
    }
    try {
      const res = await fetch(url.toString(), {
        headers: { "User-Agent": USER_AGENT, "Content-Type": "application/json" },
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return (await res.json()) as T;
    } catch (err) {
      lastError = err;
      // Drop this mirror to the back of the pool and try the next one.
      continue;
    }
  }
  throw lastError instanceof Error ? lastError : new Error("All Radio-Browser mirrors failed");
}

function buildSearchParams(filters: Partial<StationFilters>, limit: number, offset: number) {
  const orderMap: Record<string, string> = {
    popularity: "clickcount",
    alphabetical: "name",
    bitrate: "bitrate",
    recent: "lastchangetime",
  };
  return {
    name: filters.query || undefined,
    language: filters.languages?.[0] || undefined,
    countrycode: filters.countryCodes?.[0] || undefined,
    tagList: filters.tags?.length ? filters.tags.join(",") : undefined,
    codec: filters.codec || undefined,
    bitrateMin: filters.minBitrateKbps || undefined,
    order: orderMap[filters.sortBy || "popularity"],
    reverse: filters.sortBy !== "alphabetical",
    hidebroken: true,
    limit,
    offset,
  };
}

export interface SearchResult {
  stations: Station[];
  fromCache: boolean;
}

export async function searchStations(
  filters: Partial<StationFilters>,
  { limit = 60, offset = 0 }: { limit?: number; offset?: number } = {}
): Promise<SearchResult> {
  const cacheKey = `search:${JSON.stringify(filters)}:${limit}:${offset}`;
  try {
    const raw = await apiFetch<RawStation[]>("/json/stations/search", buildSearchParams(filters, limit, offset));
    let stations = raw.map((r) => normalizeStation(r));
    stations = applyClientSideFilters(stations, filters);
    await setCached(cacheKey, stations, 60 * 60 * 1000); // 1h
    return { stations, fromCache: false };
  } catch (err) {
    const stale = await getStale<Station[]>(cacheKey);
    if (stale) return { stations: stale, fromCache: true };
    throw err;
  }
}

/** Filters Radio-Browser doesn't support server-side (HTTPS-only, ad-free, reliability). */
function applyClientSideFilters(stations: Station[], filters: Partial<StationFilters>): Station[] {
  let result = stations;
  if (filters.httpsOnly) result = result.filter((s) => s.isSecure);
  if (filters.reliableOnly) result = result.filter((s) => s.isLikelyReliable);
  if (filters.adFreeOnly) result = result.filter((s) => s.isAdFreeReported);
  return result;
}

export async function getTrendingStations(limit = 30): Promise<Station[]> {
  const cacheKey = `trending:${limit}`;
  try {
    const raw = await apiFetch<RawStation[]>(`/json/stations/topclick/${limit}`, { hidebroken: true });
    const stations = raw.map((r) => normalizeStation(r));
    await setCached(cacheKey, stations, 30 * 60 * 1000);
    return stations;
  } catch (err) {
    const stale = await getStale<Station[]>(cacheKey);
    if (stale) return stale;
    throw err;
  }
}

export async function getStationsByTag(tag: string, limit = 40): Promise<Station[]> {
  const cacheKey = `tag:${tag}:${limit}`;
  try {
    const raw = await apiFetch<RawStation[]>(`/json/stations/bytag/${encodeURIComponent(tag)}`, {
      limit,
      hidebroken: true,
      order: "clickcount",
      reverse: true,
    });
    const stations = raw.map((r) => normalizeStation(r));
    await setCached(cacheKey, stations, 60 * 60 * 1000);
    return stations;
  } catch (err) {
    const stale = await getStale<Station[]>(cacheKey);
    if (stale) return stale;
    throw err;
  }
}

export async function getStationsByCountry(countryCode: string, limit = 60): Promise<Station[]> {
  const cacheKey = `country:${countryCode}:${limit}`;
  try {
    const raw = await apiFetch<RawStation[]>(`/json/stations/bycountrycodeexact/${countryCode}`, {
      limit,
      hidebroken: true,
      order: "clickcount",
      reverse: true,
    });
    const stations = raw.map((r) => normalizeStation(r));
    await setCached(cacheKey, stations, 60 * 60 * 1000);
    return stations;
  } catch (err) {
    const stale = await getStale<Station[]>(cacheKey);
    if (stale) return stale;
    throw err;
  }
}

/** Picks a station at random from the API's full pool (offset randomized). "Random Drive" — §4.1. */
export async function getRandomStation(): Promise<Station | null> {
  try {
    const raw = await apiFetch<RawStation[]>("/json/stations/search", {
      limit: 1,
      offset: Math.floor(Math.random() * 20000),
      hidebroken: true,
      order: "random",
    });
    return raw.length ? normalizeStation(raw[0]) : null;
  } catch {
    return null;
  }
}

export interface TagCount {
  name: string;
  stationcount: number;
}
export async function getPopularTags(limit = 50): Promise<TagCount[]> {
  const cacheKey = `tags:${limit}`;
  const cached = await getCached<TagCount[]>(cacheKey);
  if (cached) return cached;
  const raw = await apiFetch<TagCount[]>("/json/tags", { limit, order: "stationcount", reverse: true });
  await setCached(cacheKey, raw, 24 * 60 * 60 * 1000);
  return raw;
}

export interface CountryCount {
  name: string;
  iso_3166_1: string;
  stationcount: number;
}
export async function getCountries(): Promise<CountryCount[]> {
  const cacheKey = "countries";
  const cached = await getCached<CountryCount[]>(cacheKey);
  if (cached) return cached;
  const raw = await apiFetch<CountryCount[]>("/json/countries", { order: "stationcount", reverse: true });
  await setCached(cacheKey, raw, 24 * 60 * 60 * 1000);
  return raw;
}

export interface LanguageCount {
  name: string;
  stationcount: number;
}
export async function getLanguages(): Promise<LanguageCount[]> {
  const cacheKey = "languages";
  const cached = await getCached<LanguageCount[]>(cacheKey);
  if (cached) return cached;
  const raw = await apiFetch<LanguageCount[]>("/json/languages", { order: "stationcount", reverse: true });
  await setCached(cacheKey, raw, 24 * 60 * 60 * 1000);
  return raw;
}

/**
 * Registers a "click" for the station (increments its popularity count on
 * Radio-Browser) and resolves the canonical stream URL. Should be called
 * once per playback start, per API fair-use guidelines.
 */
export async function registerStationClick(stationId: string): Promise<{ ok: boolean; url?: string }> {
  try {
    const result = await apiFetch<{ ok: string; message: string; url?: string }>(`/json/url/${stationId}`);
    return { ok: true, url: result.url };
  } catch {
    return { ok: false };
  }
}

/**
 * Radio-Browser only exposes a positive "vote" endpoint — there is no
 * negative/"report broken" API call. A vote is the closest signal the
 * community directory supports for "this station is good"; genuine broken
 * reports are tracked locally (see favoritesStore) for a future backend
 * sync per Plan §8 Phase 3.
 */
export async function voteForStation(stationId: string): Promise<boolean> {
  try {
    const result = await apiFetch<{ ok: boolean }>(`/json/vote/${stationId}`);
    return !!result.ok;
  } catch {
    return false;
  }
}
