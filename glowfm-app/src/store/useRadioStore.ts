import { create } from 'zustand';
import { searchStations } from '../api/radioBrowser';
import {
  loadFavorites,
  loadLastStation,
  loadStationCache,
  loadUnreliable,
  saveFavorites,
  saveLastStation,
  saveStationCache,
  saveUnreliable,
} from '../storage/storage';
import type { LanguageOption, Station } from '../types';

type RadioState = {
  stations: Station[];
  favorites: Station[];
  unreliableUuids: string[];
  language: LanguageOption;
  currentStation: Station | null;
  loading: boolean;
  error: string | null;
  hydrate: () => Promise<void>;
  refreshStations: () => Promise<void>;
  setLanguage: (language: LanguageOption) => void;
  setCurrentStation: (station: Station) => Promise<void>;
  toggleFavorite: (station: Station) => void;
  markUnreliable: (stationuuid: string) => void;
  nextSuggestedStation: (excludeUuid: string) => Station | null;
};

function sortByReliability(stations: Station[], unreliableUuids: string[]): Station[] {
  return [...stations].sort((a, b) => {
    const aUnreliable = unreliableUuids.includes(a.stationuuid) ? 1 : 0;
    const bUnreliable = unreliableUuids.includes(b.stationuuid) ? 1 : 0;
    if (aUnreliable !== bUnreliable) return aUnreliable - bUnreliable;
    return b.clickCount - a.clickCount;
  });
}

export const useRadioStore = create<RadioState>((set, get) => ({
  stations: [],
  favorites: [],
  unreliableUuids: [],
  language: 'all',
  currentStation: null,
  loading: false,
  error: null,

  hydrate: async () => {
    const [favorites, lastStation, cachedStations, unreliableUuids] = await Promise.all([
      loadFavorites(),
      loadLastStation(),
      loadStationCache(),
      loadUnreliable(),
    ]);
    set({
      favorites,
      currentStation: lastStation,
      stations: sortByReliability(cachedStations, unreliableUuids),
      unreliableUuids,
    });
    await get().refreshStations();
  },

  refreshStations: async () => {
    set({ loading: true, error: null });
    try {
      const stations = await searchStations(get().language);
      const sorted = sortByReliability(stations, get().unreliableUuids);
      set({ stations: sorted, loading: false });
      await saveStationCache(sorted);
    } catch (err) {
      set({ loading: false, error: err instanceof Error ? err.message : 'Failed to load stations' });
    }
  },

  setLanguage: (language) => {
    set({ language });
    get().refreshStations();
  },

  setCurrentStation: async (station) => {
    set({ currentStation: station });
    await saveLastStation(station);
  },

  toggleFavorite: (station) => {
    const { favorites } = get();
    const exists = favorites.some((s) => s.stationuuid === station.stationuuid);
    const next = exists
      ? favorites.filter((s) => s.stationuuid !== station.stationuuid)
      : [...favorites, station];
    set({ favorites: next });
    saveFavorites(next);
  },

  markUnreliable: (stationuuid) => {
    const next = Array.from(new Set([...get().unreliableUuids, stationuuid]));
    set({ unreliableUuids: next, stations: sortByReliability(get().stations, next) });
    saveUnreliable(next);
  },

  nextSuggestedStation: (excludeUuid) => {
    const { stations, unreliableUuids } = get();
    const candidates = stations.filter(
      (s) => s.stationuuid !== excludeUuid && !unreliableUuids.includes(s.stationuuid)
    );
    return candidates[0] ?? null;
  },
}));
