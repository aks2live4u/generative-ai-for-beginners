import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Station, Fleet, HistoryEntry } from "@/types/station";

const MAX_HISTORY = 100;

interface FavoritesState {
  garage: Record<string, Station>; // favorited stations, keyed by id — "My Garage" §4.5
  fleets: Fleet[]; // custom folders/collections — "Fleet" §4.5
  history: HistoryEntry[];
  adFreeReportedIds: string[]; // community "ad-free" tag reports — §4.3
  brokenReportedIds: string[]; // locally queued broken-stream reports — §4.6

  isFavorite: (stationId: string) => boolean;
  toggleFavorite: (station: Station) => void;
  createFleet: (name: string) => Fleet;
  deleteFleet: (fleetId: string) => void;
  addToFleet: (fleetId: string, station: Station) => void;
  removeFromFleet: (fleetId: string, stationId: string) => void;
  recordPlay: (station: Station) => void;
  clearHistory: () => void;
  reportAdFree: (stationId: string) => void;
  reportBroken: (stationId: string) => void;
}

export const useFavoritesStore = create<FavoritesState>()(
  persist(
    (set, get) => ({
      garage: {},
      fleets: [],
      history: [],
      adFreeReportedIds: [],
      brokenReportedIds: [],

      isFavorite: (stationId) => !!get().garage[stationId],

      toggleFavorite: (station) =>
        set((s) => {
          const next = { ...s.garage };
          if (next[station.id]) delete next[station.id];
          else next[station.id] = station;
          return { garage: next };
        }),

      createFleet: (name) => {
        const fleet: Fleet = { id: `fleet-${Date.now()}`, name, stationIds: [], createdAt: Date.now() };
        set((s) => ({ fleets: [...s.fleets, fleet] }));
        return fleet;
      },

      deleteFleet: (fleetId) => set((s) => ({ fleets: s.fleets.filter((f) => f.id !== fleetId) })),

      addToFleet: (fleetId, station) =>
        set((s) => ({
          garage: { ...s.garage, [station.id]: station },
          fleets: s.fleets.map((f) =>
            f.id === fleetId && !f.stationIds.includes(station.id)
              ? { ...f, stationIds: [...f.stationIds, station.id] }
              : f
          ),
        })),

      removeFromFleet: (fleetId, stationId) =>
        set((s) => ({
          fleets: s.fleets.map((f) =>
            f.id === fleetId ? { ...f, stationIds: f.stationIds.filter((id) => id !== stationId) } : f
          ),
        })),

      recordPlay: (station) =>
        set((s) => {
          const filtered = s.history.filter((h) => h.stationId !== station.id);
          const entry: HistoryEntry = { stationId: station.id, playedAt: Date.now() };
          return { history: [entry, ...filtered].slice(0, MAX_HISTORY) };
        }),

      clearHistory: () => set({ history: [] }),

      reportAdFree: (stationId) =>
        set((s) => ({
          adFreeReportedIds: s.adFreeReportedIds.includes(stationId)
            ? s.adFreeReportedIds
            : [...s.adFreeReportedIds, stationId],
        })),

      reportBroken: (stationId) =>
        set((s) => ({
          brokenReportedIds: s.brokenReportedIds.includes(stationId)
            ? s.brokenReportedIds
            : [...s.brokenReportedIds, stationId],
        })),
    }),
    { name: "revradio:garage", storage: createJSONStorage(() => AsyncStorage) }
  )
);
