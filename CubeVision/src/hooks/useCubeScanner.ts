import { useCallback, useMemo, useRef, useState } from "react";
import { ColorProfile } from "../color/colorClassifier";
import { RGB } from "../color/labColor";
import { SCAN_STEPS } from "../cube/scanRoles";
import { inferSixthFace } from "../cube/inferSixthFace";
import { CubeColor, CubeFaces, Face, FaceColors, FACES, PartialCubeFaces } from "../cube/types";
import { validateCube, ValidationResult } from "../validation/validateCube";

export interface PendingReview {
  face: Face;
  color: CubeColor;
  colors: FaceColors;
  ambiguousIndices: number[];
  /** null for an auto-inferred face, where there's no camera sample to learn from. */
  samples: RGB[] | null;
  inferred: boolean;
}

export type ScannerPhase = "scanning" | "reviewing" | "complete";

export function useCubeScanner() {
  const [faces, setFaces] = useState<PartialCubeFaces>({});
  const [stepIndex, setStepIndex] = useState(0); // index into SCAN_STEPS for the *next* camera capture
  const [pendingReview, setPendingReview] = useState<PendingReview | null>(null);
  const [phase, setPhase] = useState<ScannerPhase>("scanning");
  const colorProfileRef = useRef(new ColorProfile());

  const currentStep = stepIndex < SCAN_STEPS.length ? SCAN_STEPS[stepIndex] : null;

  const submitCapture = useCallback(
    (samples: RGB[]) => {
      if (!currentStep) return;
      const classified = colorProfileRef.current.classifyFace(samples);
      setPendingReview({
        face: currentStep.face,
        color: currentStep.color,
        colors: classified.map((c) => c.color),
        ambiguousIndices: classified.map((c, i) => (c.ambiguous ? i : -1)).filter((i) => i >= 0),
        samples,
        inferred: false,
      });
      setPhase("reviewing");
    },
    [currentStep]
  );

  const editPendingSticker = useCallback((index: number, color: CubeColor) => {
    setPendingReview((prev) => {
      if (!prev) return prev;
      const colors = [...prev.colors];
      colors[index] = color;
      return { ...prev, colors, ambiguousIndices: prev.ambiguousIndices.filter((i) => i !== index) };
    });
  }, []);

  const confirmReview = useCallback(() => {
    if (!pendingReview) return;
    const { face, colors, samples } = pendingReview;

    if (samples) {
      colorProfileRef.current.learnFromConfirmedFace(samples, colors);
    }

    const next = { ...faces, [face]: colors };
    setFaces(next);
    setPendingReview(null);

    const scannedFaces = FACES.filter((f) => next[f]);
    if (scannedFaces.length === 6) {
      setPhase("complete");
      return;
    }

    // After the 5th real scan, try to infer the last face automatically.
    if (scannedFaces.length === 5) {
      const hiddenFace = FACES.find((f) => !next[f])!;
      const result = inferSixthFace(next, hiddenFace);
      if (result.ok) {
        // Don't commit yet — surface it for the same review/edit step as a normal scan.
        setPendingReview({
          face: hiddenFace,
          color: result.colors[4],
          colors: result.colors,
          ambiguousIndices: [],
          samples: null,
          inferred: true,
        });
        setPhase("reviewing");
        return;
      }
    }

    const nextMissingIndex = SCAN_STEPS.findIndex((s) => !next[s.face]);
    setStepIndex(nextMissingIndex === -1 ? SCAN_STEPS.length : nextMissingIndex);
    setPhase("scanning");
  }, [faces, pendingReview]);

  const rescanFace = useCallback((face: Face) => {
    setFaces((prev) => {
      const next = { ...prev };
      delete next[face];
      return next;
    });
    const idx = SCAN_STEPS.findIndex((s) => s.face === face);
    if (idx >= 0) setStepIndex(idx);
    setPendingReview(null);
    setPhase("scanning");
  }, []);

  const reset = useCallback(() => {
    setFaces({});
    setStepIndex(0);
    setPendingReview(null);
    setPhase("scanning");
    colorProfileRef.current.reset();
  }, []);

  const isComplete = FACES.every((f) => faces[f]);

  const validation: ValidationResult | null = useMemo(() => {
    if (!isComplete) return null;
    return validateCube(faces as CubeFaces);
  }, [faces, isComplete]);

  return {
    faces,
    currentStep,
    pendingReview,
    phase,
    isComplete,
    validation,
    submitCapture,
    editPendingSticker,
    confirmReview,
    rescanFace,
    reset,
  };
}
