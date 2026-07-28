#!/usr/bin/env python3
import glob, json, os
import xml.etree.ElementTree as ET
import urllib.request, urllib.error

def collect_failures():
    failures = []
    for path in glob.glob("app/build/test-results/testDebugUnitTest/*.xml"):
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
                    text = (el.text or el.get("message") or "").strip()
                    failures.append(f"### {name}\n```\n{text[:1500]}\n```")
    return failures

def main():
    token = os.environ.get("GIST_TOKEN")
    repo = os.environ.get("GITHUB_REPOSITORY")
    sha = os.environ.get("GITHUB_SHA")
    failures = collect_failures()
    if not failures:
        log_path = "test_output.log"
        if os.path.exists(log_path):
            with open(log_path, "r", errors="replace") as f:
                content = f.read()
            failures = [f"### Ham Gradle Ciktisi\n```\n{content[-55000:]}\n```"]
        else:
            print("Basarisiz test/log bulunamadi.")
            return 0
    body = "## Basarisiz Testler / Hata\n\n" + "\n\n".join(failures[:15])
    payload = json.dumps({"body": body[:60000]}).encode("utf-8")
    req = urllib.request.Request(
        f"https://api.github.com/repos/{repo}/commits/{sha}/comments",
        data=payload, method="POST",
        headers={"Authorization": f"token {token}", "Accept": "application/vnd.github+json",
                 "Content-Type": "application/json", "User-Agent": "sure-ci-reporter"}
    )
    try:
        with urllib.request.urlopen(req) as resp:
            print("OK:", json.loads(resp.read())["html_url"])
    except urllib.error.HTTPError as e:
        print("HATA:", e.code, e.read().decode("utf-8", errors="replace"))
    return 0

if __name__ == "__main__":
    exit(main())
