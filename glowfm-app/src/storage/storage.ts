import AsyncStorage from '@react-native-async-storage/async-storage';
import type { Station } from '../types';

const KEYS = {
  favorites: 'glowfm:favorites',
  lastStation: 'glowfm:lastStation',
  stationCache: 'glowfm:stationCache',
  unreliable: 'glowfm:unreliable',
};

export async function loadFavorites(): Promise<Station[]> {
  const raw = await AsyncStorage.getItem(KEYS.favorites);
  return raw ? JSON.parse(raw) : [];
}

export async function saveFavorites(favorites: Station[]): Promise<void> {
  await AsyncStorage.setItem(KEYS.favorites, JSON.stringify(favorites));
}

export async function loadLastStation(): Promise<Station | null> {
  const raw = await AsyncStorage.getItem(KEYS.lastStation);
  return raw ? JSON.parse(raw) : null;
}

export async function saveLastStation(station: Station): Promise<void> {
  await AsyncStorage.setItem(KEYS.lastStation, JSON.stringify(station));
}

export async function loadStationCache(): Promise<Station[]> {
  const raw = await AsyncStorage.getItem(KEYS.stationCache);
  return raw ? JSON.parse(raw) : [];
}

export async function saveStationCache(stations: Station[]): Promise<void> {
  await AsyncStorage.setItem(KEYS.stationCache, JSON.stringify(stations));
}

export async function loadUnreliable(): Promise<string[]> {
  const raw = await AsyncStorage.getItem(KEYS.unreliable);
  return raw ? JSON.parse(raw) : [];
}

export async function saveUnreliable(uuids: string[]): Promise<void> {
  await AsyncStorage.setItem(KEYS.unreliable, JSON.stringify(uuids));
}
