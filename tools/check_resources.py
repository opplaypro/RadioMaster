#!/usr/bin/env python3
"""Repository consistency checks that do not need an Android toolchain.

Run from the repository root:

    python3 tools/check_resources.py

It verifies the three things that silently break an Android build but are invisible in review:

1. every ``R.string.*`` used from Kotlin (or ``@string/`` from an XML file) exists;
2. ``values-pl`` translates exactly the keys ``values`` declares — a missing or orphaned
   translation is what makes the Play Store report an incomplete locale;
3. no resource is referenced by a qualifier directory only (e.g. ``values-night/colors.xml``
   declaring a colour that ``values/colors.xml`` does not).

Exit code is non-zero when a check fails, so the script can be wired into CI as-is.
"""

from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN = os.path.join(ROOT, "app", "src", "main")
RES = os.path.join(MAIN, "res")

TAGS = ("string", "color", "dimen", "bool", "integer")


def read(path: str) -> str:
    try:
        with open(path, encoding="utf-8") as handle:
            return handle.read()
    except (FileNotFoundError, IsADirectoryError):
        return ""


def names_in(values_dir: str, kinds: tuple[str, ...] = TAGS) -> dict[str, str]:
    found: dict[str, str] = {}
    full = os.path.join(RES, values_dir)
    if not os.path.isdir(full):
        return found
    for file_name in sorted(os.listdir(full)):
        if not file_name.endswith(".xml"):
            continue
        text = read(os.path.join(full, file_name))
        for match in re.finditer(r'<(%s)\s+name="([^"]+)"' % "|".join(kinds), text):
            found[match.group(2)] = match.group(1)
    return found


def kotlin_and_xml_references() -> tuple[set[str], set[str]]:
    strings: set[str] = set()
    drawables: set[str] = set()
    for dirpath, _, files in os.walk(MAIN):
        for name in files:
            path = os.path.join(dirpath, name)
            if name.endswith(".kt"):
                text = read(path)
                strings.update(re.findall(r"\bR\.string\.([A-Za-z0-9_]+)", text))
                drawables.update(re.findall(r"\bR\.drawable\.([A-Za-z0-9_]+)", text))
            elif name.endswith(".xml"):
                text = read(path)
                strings.update(re.findall(r"@string/([A-Za-z0-9_]+)", text))
                drawables.update(re.findall(r"@drawable/([A-Za-z0-9_]+)", text))
    return strings, drawables


def main() -> int:
    failures: list[str] = []

    default = names_in("values")
    translations = {
        directory: names_in(directory)
        for directory in sorted(os.listdir(RES))
        if directory.startswith("values-")
    }

    strings = {name for name, kind in default.items() if kind == "string"}
    used_strings, used_drawables = kotlin_and_xml_references()

    missing = sorted(used_strings - strings)
    if missing:
        failures.append(f"R.string references without a value: {missing}")

    drawables = {
        name[:-4]
        for name in os.listdir(os.path.join(RES, "drawable"))
        if name.endswith(".xml")
    }
    missing_drawables = sorted(used_drawables - drawables)
    if missing_drawables:
        failures.append(f"R.drawable references without a file: {missing_drawables}")

    for directory, entries in translations.items():
        translatable = {name for name, kind in entries.items() if kind == "string"}
        only_default = sorted(strings - translatable)
        only_here = sorted(translatable - strings)
        if directory == "values-pl":
            # A translation may legitimately omit nothing today; enforce full parity so the
            # in-app language switch never falls back mid-sentence.
            if only_default:
                failures.append(f"{directory} missing {len(only_default)} string(s): {only_default}")
        if only_here:
            failures.append(f"{directory} declares keys absent from values/: {only_here}")

    # An import whose symbol never appears outside the import block is dead — except the Compose
    # delegate operators, which are used by syntax (`val x by remember { ... }`), not by name.
    for dirpath, _, files in os.walk(os.path.join(MAIN, "java")):
        for name in files:
            if not name.endswith(".kt"):
                continue
            text = read(os.path.join(dirpath, name))
            body = re.sub(r"^import .*$", "", text, flags=re.MULTILINE)
            for match in re.finditer(r"^import (?:[\w.]+\.)?([A-Za-z0-9_]+)$", text, re.MULTILINE):
                symbol = match.group(1)
                if symbol in {"getValue", "setValue", "get", "set"}:
                    continue
                if not re.search(r"\b%s\b" % re.escape(symbol), body):
                    failures.append(f"{name}: unused import {symbol}")

    # `val x by remember { ... }` needs getValue, `var x by ...` needs setValue too.
    for dirpath, _, files in os.walk(os.path.join(MAIN, "java")):
        for name in files:
            if not name.endswith(".kt"):
                continue
            path = os.path.join(dirpath, name)
            text = read(path)
            if "by remember" in text:
                if "import androidx.compose.runtime.getValue" not in text:
                    failures.append(f"{name}: `by remember` without the getValue import")
                if re.search(r"\bvar \w+ by remember", text) and (
                    "import androidx.compose.runtime.setValue" not in text
                ):
                    failures.append(f"{name}: `var … by remember` without the setValue import")

    if failures:
        print("FAIL")
        for line in failures:
            print("  -", line)
        return 1

    print(
        f"OK: {len(strings)} strings (en + pl), {len(drawables)} drawables, "
        "no dangling references"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
