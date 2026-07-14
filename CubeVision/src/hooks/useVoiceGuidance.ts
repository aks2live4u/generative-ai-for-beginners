import { useCallback, useEffect, useRef, useState } from "react";
import * as Speech from "expo-speech";
import { moveVoiceDescription } from "../solver/moveDescriptions";

export function useVoiceGuidance() {
  const [enabled, setEnabled] = useState(false);
  const enabledRef = useRef(enabled);
  enabledRef.current = enabled;

  useEffect(() => {
    return () => {
      Speech.stop();
    };
  }, []);

  const speakMove = useCallback((move: string) => {
    if (!enabledRef.current) return;
    Speech.stop();
    Speech.speak(moveVoiceDescription(move), { rate: 0.95 });
  }, []);

  const speak = useCallback((text: string) => {
    if (!enabledRef.current) return;
    Speech.stop();
    Speech.speak(text, { rate: 0.95 });
  }, []);

  const toggle = useCallback(() => {
    setEnabled((prev) => {
      if (prev) Speech.stop();
      return !prev;
    });
  }, []);

  return { enabled, toggle, speakMove, speak };
}
