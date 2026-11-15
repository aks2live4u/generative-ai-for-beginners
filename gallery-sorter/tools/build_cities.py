#!/usr/bin/env python3
"""Builds app/src/main/assets/cities.bin from GeoNames cities15000 (CC BY 4.0).

Stdlib only. Downloads to a temp dir outside the repo. Deterministic output:
cities sorted by (lat, geonameid).

Format (big-endian, Java DataOutputStream):
  int count; per city: float lat, float lon, writeUTF name, writeUTF country code.
"""
import io
import os
import struct
import sys
import urllib.request
import zipfile

URL = "https://download.geonames.org/export/dump/cities15000.zip"
CACHE_DIR = os.environ.get("GEONAMES_DIR", "/tmp/claude-0/geonames")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "cities.bin")


def write_utf(buf, s):
    """Java modified UTF-8 with 2-byte length prefix."""
    out = bytearray()
    for ch in s:
        c = ord(ch)
        if c > 0xFFFF:  # surrogate pair, each encoded as 3 bytes
            c -= 0x10000
            units = [0xD800 + (c >> 10), 0xDC00 + (c & 0x3FF)]
        else:
            units = [c]
        for u in units:
            if 1 <= u <= 0x7F:
                out.append(u)
            elif u <= 0x7FF:
                out += bytes([0xC0 | (u >> 6), 0x80 | (u & 0x3F)])
            else:
                out += bytes([0xE0 | (u >> 12), 0x80 | ((u >> 6) & 0x3F), 0x80 | (u & 0x3F)])
    if len(out) > 65535:
        raise ValueError("string too long")
    buf.write(struct.pack(">H", len(out)))
    buf.write(out)


def main():
    os.makedirs(CACHE_DIR, exist_ok=True)
    zpath = os.path.join(CACHE_DIR, "cities15000.zip")
    if not os.path.exists(zpath):
        print("downloading", URL, file=sys.stderr)
        urllib.request.urlretrieve(URL, zpath)
    with zipfile.ZipFile(zpath) as z:
        text = z.read("cities15000.txt").decode("utf-8")
    rows = []
    for line in text.splitlines():
        f = line.split("\t")
        if len(f) < 9:
            continue
        rows.append((float(f[4]), int(f[0]), float(f[5]), f[1], f[8]))
    rows.sort(key=lambda r: (r[0], r[1]))
    buf = io.BytesIO()
    buf.write(struct.pack(">i", len(rows)))
    for lat, _id, lon, name, cc in rows:
        buf.write(struct.pack(">ff", lat, lon))
        write_utf(buf, name)
        write_utf(buf, cc)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "wb") as fh:
        fh.write(buf.getvalue())
    print(f"wrote {len(rows)} cities, {len(buf.getvalue())} bytes -> {os.path.normpath(OUT)}", file=sys.stderr)


if __name__ == "__main__":
    main()
