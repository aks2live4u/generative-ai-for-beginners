import { useCallback, useEffect, useRef, useState } from "react";
import { Alert } from "react-native";

/**
 * Voice search wrapper around @react-native-voice/voice (native device STT —
 * Plan §4.2). This native module requires a custom dev client / prebuild;
 * it is not available inside Expo Go. Calls are guarded so the rest of the
 * app keeps working if the module isn't present in the current build.
 */
export function useVoiceSearch(onResult: (text: string) => void) {
  const [isListening, setIsListening] = useState(false);
  const voiceRef = useRef<typeof import("@react-native-voice/voice").default | null>(null);

  useEffect(() => {
    try {
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      const Voice = require("@react-native-voice/voice").default;
      voiceRef.current = Voice;
      Voice.onSpeechResults = (event: { value?: string[] }) => {
        if (event.value?.[0]) onResult(event.value[0]);
      };
      Voice.onSpeechEnd = () => setIsListening(false);
      Voice.onSpeechError = () => setIsListening(false);
    } catch {
      voiceRef.current = null;
    }
    return () => {
      voiceRef.current?.destroy().then(() => voiceRef.current?.removeAllListeners());
    };
  }, [onResult]);

  const start = useCallback(async () => {
    if (!voiceRef.current) {
      Alert.alert(
        "Voice search unavailable",
        "Voice search needs a custom dev build (native STT module) — it doesn't run inside Expo Go. Run `npx expo prebuild` and build a dev client to enable it."
      );
      return;
    }
    try {
      setIsListening(true);
      await voiceRef.current.start("en-US");
    } catch {
      setIsListening(false);
    }
  }, []);

  const stop = useCallback(async () => {
    try {
      await voiceRef.current?.stop();
    } finally {
      setIsListening(false);
    }
  }, []);

  return { isListening, start, stop };
}
