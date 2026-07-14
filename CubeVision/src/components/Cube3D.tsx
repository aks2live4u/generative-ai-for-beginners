import React, { forwardRef, useImperativeHandle, useMemo, useRef } from "react";
import { GestureResponderEvent, PanResponder, View } from "react-native";
import { Canvas, useFrame, useThree } from "@react-three/fiber";
import * as THREE from "three";
import { buildCubies, CubieDef, FACE_AXIS, isInLayer, Vec3 } from "../cube/cubieGeometry";
import { COLOR_HEX } from "../cube/constants";
import { CubeFaces, Face } from "../cube/types";
import { parseMove, turnSign } from "../utils/moveNotation";

const CUBIE_SIZE = 0.94;
const STICKER_INSET = 0.06;
const MOVE_DURATION_MS = 260;

interface CubieRuntime {
  def: CubieDef;
  position: THREE.Vector3;
  quaternion: THREE.Quaternion;
}

interface PendingAnimation {
  axis: THREE.Vector3;
  totalAngle: number;
  elapsed: number;
  affected: number[];
  startPositions: THREE.Vector3[];
  startQuaternions: THREE.Quaternion[];
  resolve: () => void;
}

export interface Cube3DHandle {
  playMove: (move: string) => Promise<void>;
  resetTo: (faces: CubeFaces) => void;
}

interface CubieMeshProps {
  index: number;
  def: CubieDef;
  groupRefs: React.MutableRefObject<(THREE.Group | null)[]>;
}

function CubieMesh({ index, def, groupRefs }: CubieMeshProps) {
  const faceMaterials = useMemo(() => {
    // three.js BoxGeometry face order: +X, -X, +Y, -Y, +Z, -Z
    const order: Face[] = ["R", "L", "U", "D", "F", "B"];
    return order.map((face) => {
      const color = def.stickers[face];
      return new THREE.MeshStandardMaterial({
        color: color ? COLOR_HEX[color] : "#1a1a1a",
        roughness: 0.4,
        metalness: 0.05,
      });
    });
  }, [def]);

  return (
    <group
      ref={(el) => {
        groupRefs.current[index] = el;
      }}
      position={def.basePosition}
    >
      <mesh material={faceMaterials}>
        <boxGeometry args={[CUBIE_SIZE, CUBIE_SIZE, CUBIE_SIZE, 1, 1, 1]} />
      </mesh>
    </group>
  );
}

function AnimationDriver({
  pendingRef,
  runtimeRef,
  groupRefs,
  cameraDistanceRef,
}: {
  pendingRef: React.MutableRefObject<PendingAnimation | null>;
  runtimeRef: React.MutableRefObject<CubieRuntime[]>;
  groupRefs: React.MutableRefObject<(THREE.Group | null)[]>;
  cameraDistanceRef: React.MutableRefObject<number>;
}) {
  const { camera } = useThree();

  useFrame((_, delta) => {
    const currentLength = camera.position.length();
    if (Math.abs(currentLength - cameraDistanceRef.current) > 0.001) {
      camera.position.setLength(cameraDistanceRef.current);
    }

    const anim = pendingRef.current;
    if (!anim) return;

    anim.elapsed += delta * 1000;
    const t = Math.min(1, anim.elapsed / MOVE_DURATION_MS);
    const eased = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    const currentAngle = anim.totalAngle * eased;
    const frameQuat = new THREE.Quaternion().setFromAxisAngle(anim.axis, currentAngle);

    anim.affected.forEach((cubieIndex, i) => {
      const group = groupRefs.current[cubieIndex];
      if (!group) return;
      const pos = anim.startPositions[i].clone().applyQuaternion(frameQuat);
      group.position.copy(pos);
      group.quaternion.copy(frameQuat).multiply(anim.startQuaternions[i]);
    });

    if (t >= 1) {
      const fullQuat = new THREE.Quaternion().setFromAxisAngle(anim.axis, anim.totalAngle);
      anim.affected.forEach((cubieIndex, i) => {
        const runtime = runtimeRef.current[cubieIndex];
        const pos = anim.startPositions[i].clone().applyQuaternion(fullQuat);
        pos.set(Math.round(pos.x), Math.round(pos.y), Math.round(pos.z));
        const quat = fullQuat.clone().multiply(anim.startQuaternions[i]).normalize();
        runtime.position.copy(pos);
        runtime.quaternion.copy(quat);
        const group = groupRefs.current[cubieIndex];
        if (group) {
          group.position.copy(pos);
          group.quaternion.copy(quat);
        }
      });
      pendingRef.current = null;
      anim.resolve();
    }
  });
  return null;
}

export const Cube3D = forwardRef<Cube3DHandle, { initialFaces: CubeFaces; size?: number }>(
  function Cube3D({ initialFaces, size = 320 }, ref) {
    const cubieDefsRef = useRef<CubieDef[]>(buildCubies(initialFaces));
    const runtimeRef = useRef<CubieRuntime[]>(
      cubieDefsRef.current.map((def) => ({
        def,
        position: new THREE.Vector3(...def.basePosition),
        quaternion: new THREE.Quaternion(),
      }))
    );
    const groupRefs = useRef<(THREE.Group | null)[]>([]);
    const pendingRef = useRef<PendingAnimation | null>(null);
    const orbitRef = useRef<THREE.Group>(null);
    const cameraDistanceRef = useRef(Math.hypot(4.5, 4.5, 7.5));

    useImperativeHandle(ref, () => ({
      playMove(move: string) {
        return new Promise<void>((resolve) => {
          if (pendingRef.current) {
            resolve();
            return;
          }
          const { face, turns } = parseMove(move);
          const axisArr = FACE_AXIS[face];
          const axis = new THREE.Vector3(...axisArr);
          const angle = (-Math.PI / 2) * turnSign(turns); // clockwise (turns=1) = -90deg about outward normal

          const affected: number[] = [];
          const startPositions: THREE.Vector3[] = [];
          const startQuaternions: THREE.Quaternion[] = [];
          runtimeRef.current.forEach((c, i) => {
            const pos: Vec3 = [c.position.x, c.position.y, c.position.z];
            if (isInLayer(face, pos)) {
              affected.push(i);
              startPositions.push(c.position.clone());
              startQuaternions.push(c.quaternion.clone());
            }
          });

          pendingRef.current = {
            axis,
            totalAngle: angle,
            elapsed: 0,
            affected,
            startPositions,
            startQuaternions,
            resolve,
          };
        });
      },
      resetTo(faces: CubeFaces) {
        const defs = buildCubies(faces);
        cubieDefsRef.current = defs;
        runtimeRef.current = defs.map((def) => ({
          def,
          position: new THREE.Vector3(...def.basePosition),
          quaternion: new THREE.Quaternion(),
        }));
        groupRefs.current.forEach((group, i) => {
          if (!group) return;
          group.position.copy(runtimeRef.current[i].position);
          group.quaternion.identity();
        });
      },
    }));

    const lastPinchDistRef = useRef<number | undefined>(undefined);
    const lastPanRef = useRef<{ x: number; y: number } | undefined>(undefined);

    const panResponder = useRef(
      PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponder: () => true,
        onPanResponderMove: (evt: GestureResponderEvent) => {
          const touches = evt.nativeEvent.touches;
          const group = orbitRef.current;
          if (touches.length >= 2) {
            lastPanRef.current = undefined;
            const [a, b] = touches;
            const dist = Math.hypot(a.pageX - b.pageX, a.pageY - b.pageY);
            const last = lastPinchDistRef.current ?? dist;
            const delta = dist - last;
            cameraDistanceRef.current = Math.max(4, Math.min(14, cameraDistanceRef.current - delta * 0.02));
            lastPinchDistRef.current = dist;
          } else if (group && touches.length === 1) {
            lastPinchDistRef.current = undefined;
            const touch = touches[0];
            const last = lastPanRef.current ?? { x: touch.pageX, y: touch.pageY };
            const dx = touch.pageX - last.x;
            const dy = touch.pageY - last.y;
            group.rotation.y += dx * 0.008;
            group.rotation.x += dy * 0.008;
            group.rotation.x = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, group.rotation.x));
            lastPanRef.current = { x: touch.pageX, y: touch.pageY };
          }
        },
        onPanResponderRelease: () => {
          lastPinchDistRef.current = undefined;
          lastPanRef.current = undefined;
        },
      })
    ).current;

    return (
      <View style={{ width: size, height: size }} {...panResponder.panHandlers}>
        <Canvas camera={{ position: [4.5, 4.5, 7.5], fov: 32 }}>
          <ambientLight intensity={0.65} />
          <directionalLight position={[5, 8, 6]} intensity={0.8} />
          <directionalLight position={[-5, -4, -6]} intensity={0.25} />
          <group ref={orbitRef} rotation={[-0.5, 0.7, 0]}>
            {cubieDefsRef.current.map((def, i) => (
              <CubieMesh key={def.id} index={i} def={def} groupRefs={groupRefs} />
            ))}
          </group>
          <AnimationDriver
            pendingRef={pendingRef}
            runtimeRef={runtimeRef}
            groupRefs={groupRefs}
            cameraDistanceRef={cameraDistanceRef}
          />
        </Canvas>
      </View>
    );
  }
);
