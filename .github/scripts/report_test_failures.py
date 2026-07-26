#!/usr/bin/env python3
"""Basarisiz JUnit testlerinin ozetini bir commit yorumuna yazar (tanilama amacli)."""
import glob
import json
import os
import urllib.request
import urllib.error
import xml.etree.ElementTree as ET

RESULTS_GLOB = "app/build/test-results/testDebugUnitTest/*.xml"


def collect_failures():
    failures = []
    for path in glob.glob(RESULTS_GLOB):
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        root = tree.getroot()
        for testcase in root.findall("testcase"):
            for tag in ("failure", "error"):
                el = testcase.find(tag)
                if el is not None:
                    name = f"{testcase.get('classname')}.{testcase.get('name')}"
                    message = (el.get("message") or "").strip()
                    text = (el.text or "").strip()
                    snippet = text[:1500] if text else message[:1500]
                    failures.append(f"### {name}\n```\n{snippet}\n```")
    return failures


def main():
    token = os.environ.get("GIST_TOKEN")
    repo = os.environ.get("GITHUB_REPOSITORY")
    sha = os.environ.get("GITHUB_SHA")
    if not token or not repo or not sha:
        print("Ortam degiskenleri eksik.")
        return 1

    failures = collect_failures()
    if not failures:
        print("Basarisiz test bulunamadi (XML bulunamamis olabilir).")
        return 0

    body = "## Basarisiz Birim Testleri\n\n" + "\n\n".join(failures[:15])
    if len(body) > 60000:
        body = body[:60000] + "\n... (kesildi)"

    payload = json.dumps({"body": body}).encode("utf-8")
    req = urllib.request.Request(
        f"https://api.github.com/repos/{repo}/commits/{sha}/comments",
        data=payload,
        method="POST",
        headers={
            "Authorization": f"token {token}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
            "User-Agent": "sure-ci-test-reporter"
        }
    )
    try:
        with urllib.request.urlopen(req) as resp:
            result = json.loads(resp.read().decode("utf-8"))
            print("COMMENT_URL:", result["html_url"])
    except urllib.error.HTTPError as e:
        print("YORUM HATASI:", e.code, e.read().decode("utf-8", errors="replace"))
    return 0


if __name__ == "__main__":
    exit(main())
