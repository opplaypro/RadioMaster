#!/usr/bin/env python3
"""Generate `app/src/main/assets/world_land.json` — the offline land-mask used by the
Globe screen (Radio-Garden style map).

Source data: world-atlas (https://github.com/topojson/world-atlas, ISC) which ships
Natural Earth "land" polygons (public domain) as a quantized TopoJSON.

The script decodes the topology, projects it to an equirectangular grid, simplifies the
rings and writes them out as integer polylines. The Android side only has to scale the
grid to the viewport, so no geographic code is needed at runtime.

Usage:
    python3 tools/generate_world_map.py /path/to/land-110m.json
"""

from __future__ import annotations

import json
import os
import sys

# Grid resolution: every coordinate is stored as an integer in [0, GRID].
GRID = 10000
# Drop consecutive points closer than this (in grid units) — 0.08 % of the map width.
MIN_STEP = 8
# Discard rings that end up smaller than this (micro islands).
MIN_RING_POINTS = 5
# Discard rings whose absolute area is below this fraction of the whole map.
MIN_REL_AREA = 0.000004

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_SRC = "/tmp/wa/package/land-110m.json"
OUT_JSON = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "world_land.json")


def decode_arcs(topo: dict) -> list[list[tuple[float, float]]]:
    """Turn quantized, delta-encoded arcs into (lon, lat) point lists."""
    scale_x, scale_y = topo["transform"]["scale"]
    trans_x, trans_y = topo["transform"]["translate"]
    arcs: list[list[tuple[float, float]]] = []
    for raw_arc in topo["arcs"]:
        x = y = 0
        points: list[tuple[float, float]] = []
        for dx, dy in raw_arc:
            x += dx
            y += dy
            points.append((x * scale_x + trans_x, y * scale_y + trans_y))
        arcs.append(points)
    return arcs


def ring_points(arcs, arc_indices) -> list[tuple[float, float]]:
    """Materialize one ring from (possibly reversed) arc indices."""
    points: list[tuple[float, float]] = []
    for index in arc_indices:
        part = arcs[index] if index >= 0 else list(reversed(arcs[~index]))
        points.extend(part[1:] if points else part)
    return points


def project(lon: float, lat: float) -> tuple[float, float]:
    return (lon + 180.0) / 360.0 * GRID, (90.0 - lat) / 180.0 * GRID


def split_at_antimeridian(points: list[tuple[float, float]]) -> list[list[tuple[float, float]]]:
    """Cut a ring wherever it jumps across the map width (180th meridian).

    TopoJSON rings of countries that straddle the antimeridian (Russia, Antarctica,
    Fiji, ...) contain one edge that connects x≈GRID with x≈0. Filling such a ring
    paints a horizontal band over the whole map, so the ring is split into parts and
    every part is closed along the cut instead.
    """
    jump = GRID / 2.0
    parts: list[list[tuple[float, float]]] = []
    current: list[tuple[float, float]] = []
    for index, point in enumerate(points):
        if current and abs(point[0] - current[-1][0]) > jump:
            current.append(point)
            parts.append(current)
            current = [point]
        else:
            current.append(point)
    if current:
        if parts and abs(current[0][0] - parts[-1][-1][0]) > jump:
            # Ring closes across the meridian: finish the first part, start a new one.
            parts[-1].append(current[0])
        else:
            if len(current) > 1 and abs(current[-1][0] - current[0][0]) <= jump:
                current.append(current[0])
            parts.append(current)
    return [p for p in parts if len(p) >= 3]


def is_thin_sliver(points: list[tuple[float, float]]) -> bool:
    """True for rings that are only a horizontal line across the map (bad source data)."""
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    width = max(xs) - min(xs)
    height = max(ys) - min(ys)
    return width > GRID * 0.8 and height < GRID * 0.05


def simplify(points: list[tuple[float, float]]) -> list[tuple[float, float]]:
    out: list[tuple[float, float]] = []
    for x, y in points:
        if not out:
            out.append((x, y))
            continue
        prev_x, prev_y = out[-1]
        if abs(x - prev_x) >= MIN_STEP or abs(y - prev_y) >= MIN_STEP:
            out.append((x, y))
    if len(out) > 1 and out[0] == out[-1]:
        out.pop()
    return out


def ring_area(ring: list[tuple[float, float]]) -> float:
    total = 0.0
    n = len(ring)
    for i in range(n):
        x1, y1 = ring[i]
        x2, y2 = ring[(i + 1) % n]
        total += x1 * y2 - x2 * y1
    return abs(total) / 2.0


def build(src: str) -> list[list[int]]:
    with open(src) as handle:
        topo = json.load(handle)
    arcs = decode_arcs(topo)
    polygons: list[list[int]] = []
    for geometry in topo["objects"]["land"]["geometries"]:
        polygons_per_geom = (
            geometry["arcs"] if geometry["type"] == "MultiPolygon" else [geometry["arcs"]]
        )
        for polygon in polygons_per_geom:
            for ring in polygon:
                projected = [project(lon, lat) for lon, lat in ring_points(arcs, ring)]
                for part in split_at_antimeridian(projected):
                    reduced = simplify(part)
                    if len(reduced) < MIN_RING_POINTS or is_thin_sliver(reduced):
                        continue
                    if ring_area(reduced) < GRID * GRID * MIN_REL_AREA:
                        continue
                    polygons.append(
                        [int(round(value)) for point in reduced for value in point]
                    )
    return polygons


def main() -> int:
    src = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SRC
    if not os.path.exists(src):
        print(f"missing source file: {src}", file=sys.stderr)
        return 1
    polygons = build(src)
    os.makedirs(os.path.dirname(OUT_JSON), exist_ok=True)
    with open(OUT_JSON, "w") as handle:
        json.dump({"grid": GRID, "polygons": polygons}, handle, separators=(",", ":"))
    points = sum(len(p) // 2 for p in polygons)
    print(
        f"wrote {os.path.relpath(OUT_JSON, REPO_ROOT)}: "
        f"{len(polygons)} polygons, {points} points, {os.path.getsize(OUT_JSON)} bytes"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
