#!/usr/bin/env python3
"""Extract Gradle/Kotlin failure diagnostics for CI."""
from __future__ import annotations

import json
import os
import pathlib
import re
import urllib.error
import urllib.request

ROOT = pathlib.Path(".")
LOG = ROOT / "ci-logs" / "gradle-build.log"
OUT = ROOT / "ci-logs" / "failure-extract.txt"
SUMMARY = os.environ.get("GITHUB_STEP_SUMMARY")


def main() -> int:
    text = LOG.read_text(errors="replace") if LOG.exists() else ""
    lines = text.splitlines()

    diag: list[str] = []
    patterns = [
        re.compile(r"^\s*e:"),
        re.compile(r"^\s*w: file://"),
        re.compile(r"Unresolved reference"),
        re.compile(r"Type mismatch"),
        re.compile(r"None of the following functions"),
        re.compile(r"is not abstract"),
        re.compile(r"error: "),
        re.compile(r"error:"),
        re.compile(r"FAILED"),
        re.compile(r"Execution failed for task"),
        re.compile(r"What went wrong"),
        re.compile(r"Caused by:"),
        re.compile(r"e: file://"),
        re.compile(r"\.kt:\d+"),
        re.compile(r"\.java:\d+"),
    ]

    for idx, line in enumerate(lines):
        if any(p.search(line) for p in patterns):
            # include a little context
            lo = max(0, idx - 1)
            hi = min(len(lines), idx + 3)
            for j in range(lo, hi):
                if lines[j] not in diag:
                    diag.append(lines[j])

    if not diag:
        start = 0
        for i, line in enumerate(lines):
            if "What went wrong" in line or "FAILURE: Build failed" in line or "Execution failed for task" in line:
                start = i
        diag = lines[start : start + 100] if lines else ["(empty gradle log)"]

    # Prefer lines containing .kt: errors first in the posted body
    kt_first = [l for l in diag if ".kt:" in l or l.strip().startswith("e:")]
    rest = [l for l in diag if l not in kt_first]
    chunk_lines = (kt_first + rest)[:120]
    chunk = "\n".join(chunk_lines)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(chunk + "\n", encoding="utf-8")

    if SUMMARY:
        with open(SUMMARY, "a", encoding="utf-8") as fh:
            fh.write("### Gradle failure extract\n\n```\n")
            fh.write(chunk[:10000])
            fh.write("\n```\n")

    count = 0
    for line in chunk_lines:
        clean = "".join(ch for ch in line if ord(ch) >= 32)[:200].strip()
        if not clean:
            continue
        # Prefer annotating actual e: lines
        level = "error" if clean.startswith("e:") or "Execution failed" in clean or "error:" in clean else "warning"
        print(f"::{level} title=Gradle::{clean}")
        count += 1
        if count >= 30:
            break

    token = os.environ.get("GITHUB_TOKEN") or ""
    repo = os.environ.get("GITHUB_REPOSITORY") or ""
    sha = os.environ.get("GITHUB_SHA") or ""
    if token and repo and sha and chunk.strip() and chunk.strip() != "(empty gradle log)":
        body = {"body": "### CI Gradle failure extract\n\n```\n" + chunk[:10000] + "\n```\n"}
        data = json.dumps(body).encode("utf-8")
        url = f"https://api.github.com/repos/{repo}/commits/{sha}/comments"
        req = urllib.request.Request(
            url,
            data=data,
            method="POST",
            headers={
                "Authorization": f"Bearer {token}",
                "Accept": "application/vnd.github+json",
                "Content-Type": "application/json",
                "User-Agent": "anoyloader-ci",
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                print("commit comment status", resp.status)
        except urllib.error.HTTPError as e:
            print("commit comment HTTPError", e.code, e.read()[:300])
        except Exception as e:
            print("commit comment failed:", e)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
