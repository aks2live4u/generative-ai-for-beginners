// Internet radio streams have no real FM frequency. We derive a stable,
// decorative one from the station's UUID so the dial has something to show
// and the same station always lands on the same reading.
export function pseudoFrequency(stationuuid: string): number {
  let hash = 0;
  for (let i = 0; i < stationuuid.length; i++) {
    hash = (hash * 31 + stationuuid.charCodeAt(i)) >>> 0;
  }
  const range = 108.0 - 87.5;
  const value = 87.5 + (hash % 1000) / 1000 * range;
  return Math.round(value * 10) / 10;
}
