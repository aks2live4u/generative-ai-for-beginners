import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { StationFilters, FilterPreset, defaultFilters } from "@/types/station";

interface FilterState {
  activeFilters: StationFilters;
  presets: FilterPreset[]; // saved "Tuning" presets — §4.3 ("My Commute Mix")
  recentSearches: string[];

  setFilters: (filters: Partial<StationFilters>) => void;
  resetFilters: () => void;
  savePreset: (name: string) => FilterPreset;
  deletePreset: (presetId: string) => void;
  applyPreset: (presetId: string) => void;
  addRecentSearch: (query: string) => void;
  clearRecentSearches: () => void;
}

const MAX_RECENT_SEARCHES = 15;

export const useFilterStore = create<FilterState>()(
  persist(
    (set, get) => ({
      activeFilters: defaultFilters,
      presets: [],
      recentSearches: [],

      setFilters: (filters) => set((s) => ({ activeFilters: { ...s.activeFilters, ...filters } })),

      resetFilters: () => set({ activeFilters: defaultFilters }),

      savePreset: (name) => {
        const preset: FilterPreset = {
          id: `preset-${Date.now()}`,
          name,
          filters: get().activeFilters,
          createdAt: Date.now(),
        };
        set((s) => ({ presets: [...s.presets, preset] }));
        return preset;
      },

      deletePreset: (presetId) => set((s) => ({ presets: s.presets.filter((p) => p.id !== presetId) })),

      applyPreset: (presetId) => {
        const preset = get().presets.find((p) => p.id === presetId);
        if (preset) set({ activeFilters: preset.filters });
      },

      addRecentSearch: (query) =>
        set((s) => {
          const trimmed = query.trim();
          if (!trimmed) return {};
          const filtered = s.recentSearches.filter((q) => q.toLowerCase() !== trimmed.toLowerCase());
          return { recentSearches: [trimmed, ...filtered].slice(0, MAX_RECENT_SEARCHES) };
        }),

      clearRecentSearches: () => set({ recentSearches: [] }),
    }),
    { name: "revradio:filters", storage: createJSONStorage(() => AsyncStorage) }
  )
);
