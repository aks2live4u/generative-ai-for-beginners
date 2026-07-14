#!/usr/bin/env node
/**
 * Standalone correctness test for src/camera/pngDecode.ts, run with:
 *   node scripts/test-png-decode.js
 *
 * Transpiles the actual TS source on the fly (via the `typescript` package,
 * already a devDependency) rather than duplicating its logic here, so this
 * test can't silently drift out of sync with the real decoder. It hand-builds
 * PNGs exercising every PNG filter type (None/Sub/Up/Average/Paeth) plus
 * RGB/RGBA/grayscale color types, since that's the part of the pipeline that
 * has no native equivalent to cross-check against in this repo.
 */
const fs = require("fs");
const path = require("path");
const Module = require("module");
const zlib = require("zlib");
const ts = require("typescript");

function loadTs(relativePath) {
  const fullPath = path.join(__dirname, "..", relativePath);
  const source = fs.readFileSync(fullPath, "utf8");
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2019, esModuleInterop: true },
  });
  const mod = new Module(fullPath, module);
  mod.filename = fullPath;
  mod.paths = Module._nodeModulePaths(path.dirname(fullPath));
  mod._compile(outputText, fullPath);
  return mod.exports;
}

const { decodePng, averagePngColor } = loadTs("src/camera/pngDecode.ts");

function crcTable() {
  const table = [];
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c >>> 0;
  }
  return table;
}
const CRC_TABLE = crcTable();
function crc32Buf(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const typeBuf = Buffer.from(type, "ascii");
  const lenBuf = Buffer.alloc(4);
  lenBuf.writeUInt32BE(data.length, 0);
  const crcBuf = Buffer.alloc(4);
  crcBuf.writeUInt32BE(crc32Buf(Buffer.concat([typeBuf, data])), 0);
  return Buffer.concat([lenBuf, typeBuf, data, crcBuf]);
}

function buildPng(width, height, colorType, rows) {
  const sig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const ihdrData = Buffer.alloc(13);
  ihdrData.writeUInt32BE(width, 0);
  ihdrData.writeUInt32BE(height, 4);
  ihdrData[8] = 8;
  ihdrData[9] = colorType;
  const ihdr = chunk("IHDR", ihdrData);
  const raw = Buffer.concat(rows.map((r) => Buffer.from(r)));
  const idat = chunk("IDAT", zlib.deflateSync(raw));
  const iend = chunk("IEND", Buffer.alloc(0));
  return Buffer.concat([sig, ihdr, idat, iend]);
}

let failures = 0;
function check(name, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a === e) {
    console.log(`OK   ${name}`);
  } else {
    failures++;
    console.log(`FAIL ${name}: expected ${e}, got ${a}`);
  }
}

{
  const png = buildPng(2, 1, 2, [[0, 10, 20, 30, 200, 210, 220]]);
  const d = decodePng(png.toString("base64"));
  check("filter-none pixel0", [d.data[0], d.data[1], d.data[2]], [10, 20, 30]);
  check("filter-none pixel1", [d.data[3], d.data[4], d.data[5]], [200, 210, 220]);
}
{
  const png = buildPng(2, 1, 2, [[1, 10, 20, 30, 5, 5, 5]]);
  const d = decodePng(png.toString("base64"));
  check("filter-sub pixel1", [d.data[3], d.data[4], d.data[5]], [15, 25, 35]);
}
{
  const png = buildPng(1, 2, 2, [[0, 50, 60, 70], [2, 10, 10, 10]]);
  const d = decodePng(png.toString("base64"));
  check("filter-up row1", [d.data[3], d.data[4], d.data[5]], [60, 70, 80]);
}
{
  const png = buildPng(2, 2, 2, [
    [0, 100, 100, 100, 20, 20, 20],
    [3, 10, 10, 10, 5, 5, 5],
  ]);
  const d = decodePng(png.toString("base64"));
  check("filter-avg row1 pixel0", [d.data[6], d.data[7], d.data[8]], [60, 60, 60]);
  check("filter-avg row1 pixel1", [d.data[9], d.data[10], d.data[11]], [45, 45, 45]);
}
{
  const png = buildPng(2, 2, 2, [
    [0, 30, 30, 30, 90, 90, 90],
    [4, 0, 0, 0, 0, 0, 0],
  ]);
  const d = decodePng(png.toString("base64"));
  check("filter-paeth row1 pixel0", [d.data[6], d.data[7], d.data[8]], [30, 30, 30]);
  check("filter-paeth row1 pixel1", [d.data[9], d.data[10], d.data[11]], [90, 90, 90]);
}
{
  const png = buildPng(2, 1, 6, [[0, 100, 150, 200, 255, 50, 60, 70, 128]]);
  check("rgba average", averagePngColor(png.toString("base64")), { r: 75, g: 105, b: 135 });
}
{
  const png = buildPng(2, 1, 0, [[0, 40, 200]]);
  check("grayscale average", averagePngColor(png.toString("base64")), { r: 120, g: 120, b: 120 });
}
{
  const color = [180, 90, 30];
  const rows = [
    [0, ...color, ...color, ...color],
    [1, ...color, 0, 0, 0, 0, 0, 0],
    [2, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  ];
  const png = buildPng(3, 3, 2, rows);
  check("uniform 3x3 average", averagePngColor(png.toString("base64")), { r: color[0], g: color[1], b: color[2] });
}

console.log(failures === 0 ? "\nALL TESTS PASSED" : `\n${failures} TEST(S) FAILED`);
process.exit(failures === 0 ? 0 : 1);
