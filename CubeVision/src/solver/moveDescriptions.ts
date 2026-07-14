import { FACE_LABEL } from "../cube/constants";
import { parseMove } from "../utils/moveNotation";

/** Short label shown under the current move, e.g. "R'". */
export function moveNotationLabel(move: string): string {
  return move;
}

/** Spoken/voice-guidance description of a single move, e.g. "Turn the right face clockwise." */
export function moveVoiceDescription(move: string): string {
  const { face, turns } = parseMove(move);
  const label = FACE_LABEL[face];
  if (turns === 2) return `Turn the ${label.toLowerCase()} face twice.`;
  if (turns === 3) return `Turn the ${label.toLowerCase()} face counterclockwise.`;
  return `Turn the ${label.toLowerCase()} face clockwise.`;
}
