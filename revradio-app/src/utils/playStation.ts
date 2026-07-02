import { useRouter } from "expo-router";
import { Station } from "@/types/station";
import { usePlayerStore } from "@/store/playerStore";

/** Starts playback for a station and opens the full Now Playing dashboard. */
export function usePlayStation() {
  const router = useRouter();
  const play = usePlayerStore((s) => s.play);
  return (station: Station) => {
    play(station);
    router.push("/now-playing");
  };
}
