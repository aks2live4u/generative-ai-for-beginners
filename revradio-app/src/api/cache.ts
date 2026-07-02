import AsyncStorage from "@react-native-async-storage/async-storage";

const PREFIX = "revradio:cache:";

interface CacheEnvelope<T> {
  storedAt: number;
  ttlMs: number;
  data: T;
}

/**
 * Simple TTL-based cache over AsyncStorage. Used to avoid hammering the
 * free Radio-Browser API and to keep browsing instant/offline-capable
 * (see RevRadio App Plan §7 — Reliability & Performance).
 */
export async function getCached<T>(key: string): Promise<T | null> {
  try {
    const raw = await AsyncStorage.getItem(PREFIX + key);
    if (!raw) return null;
    const envelope: CacheEnvelope<T> = JSON.parse(raw);
    if (Date.now() - envelope.storedAt > envelope.ttlMs) return null;
    return envelope.data;
  } catch {
    return null;
  }
}

export async function setCached<T>(key: string, data: T, ttlMs: number): Promise<void> {
  const envelope: CacheEnvelope<T> = { storedAt: Date.now(), ttlMs, data };
  try {
    await AsyncStorage.setItem(PREFIX + key, JSON.stringify(envelope));
  } catch {
    // Storage full or unavailable — fail silently, cache is best-effort.
  }
}

/** Returns cached data immediately if present (even if stale) while a fresh fetch runs. */
export async function getStale<T>(key: string): Promise<T | null> {
  try {
    const raw = await AsyncStorage.getItem(PREFIX + key);
    if (!raw) return null;
    const envelope: CacheEnvelope<T> = JSON.parse(raw);
    return envelope.data;
  } catch {
    return null;
  }
}

export async function clearCache(): Promise<void> {
  const keys = await AsyncStorage.getAllKeys();
  const cacheKeys = keys.filter((k) => k.startsWith(PREFIX));
  if (cacheKeys.length) await AsyncStorage.multiRemove(cacheKeys);
}
