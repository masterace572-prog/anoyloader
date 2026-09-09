#!/usr/bin/env python3
"""Extract Gradle failure block, emit annotations/summary, post commit comment."""
from __future__ import annotations

import json
import os
import pathlib
import urllib.error
import urllib.request

ROOT = pathlib.Path(".")
LOG = ROOT / "ci-logs" / "gradle-build.log"
OUT = ROOT / "ci-logs" / "failure-extract.txt"
SUMMARY = os.environ.get("GITHUB_STEP_SUMMARY")


def main() -> int:
    text = LOG.read_text(errors="replace") if LOG.exists() else ""
    lines = text.splitlines()
    start = 0
    for i, line in enumerate(lines):
        if "What went wrong" in line or "FAILURE: Build failed" in line:
            start = i
            break
    # Also try last "Execution failed for task"
    for i, line in enumerate(lines):
        if "Execution failed for task" in line:
            start = i
    chunk_lines = lines[start : start + 80] if lines else ["(empty gradle log)"]
    chunk = "\n".join(chunk_lines)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(chunk + "\n", encoding="utf-8")

    if SUMMARY:
        with open(SUMMARY, "a", encoding="utf-8") as fh:
            fh.write("### Gradle failure extract\n\n```\n")
            fh.write(chunk[:8000])
            fh.write("\n```\n")

    # GitHub error annotations (first 20 non-empty lines)
    count = 0
    for line in chunk_lines:
        clean = "".join(ch for ch in line if ord(ch) >= 32)[:180].strip()
        if not clean:
            continue
        print(f"::error title=Gradle::{clean}")
        count += 1
        if count >= 20:
            break

    token = os.environ.get("GITHUB_TOKEN") or ""
    repo = os.environ.get("GITHUB_REPOSITORY") or ""
    sha = os.environ.get("GITHUB_SHA") or ""
    if token and repo and sha and chunk.strip() and chunk.strip() != "(empty gradle log)":
        body = {
            "body": "### CI Gradle failure extract\n\n```\n" + chunk[:6000] + "\n```\n"
        }
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
