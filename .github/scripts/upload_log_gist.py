#!/usr/bin/env python3
"""CI yardimci betigi: build_output.log dosyasinin son kismini, ilgili commit'e
yorum olarak ekler. Sadece build basarisiz oldugunda, tanilama amaciyla calisir.
Gist yerine commit comment kullanilir cunku kullanilan token'da 'gist' yetkisi yoktur;
commit comment icin standart 'repo' yetkisi yeterlidir.
"""
import json
import os
import sys
import urllib.request
import urllib.error

LOG_PATH = "build_output.log"
MAX_CHARS = 55000


def main() -> int:
    token = os.environ.get("GIST_TOKEN")
    repo = os.environ.get("GITHUB_REPOSITORY")
    sha = os.environ.get("GITHUB_SHA")

    if not token or not repo or not sha:
        print("HATA: Gerekli ortam degiskenleri eksik.")
        return 1

    if not os.path.exists(LOG_PATH):
        print(f"HATA: {LOG_PATH} bulunamadi.")
        return 1

    with open(LOG_PATH, "r", errors="replace") as f:
        content = f.read()

    if len(content) > MAX_CHARS:
        content = content[-MAX_CHARS:]

    body = "## Otomatik CI Hata Raporu\n\n```\n" + content + "\n```"
    payload = json.dumps({"body": body}).encode("utf-8")

    url = f"https://api.github.com/repos/{repo}/commits/{sha}/comments"
    req = urllib.request.Request(
        url,
        data=payload,
        method="POST",
        headers={
            "Authorization": f"token {token}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
            "User-Agent": "sure-ci-log-uploader"
        }
    )

    try:
        with urllib.request.urlopen(req) as resp:
            result = json.loads(resp.read().decode("utf-8"))
            print("COMMENT_URL:", result["html_url"])
            return 0
    except urllib.error.HTTPError as e:
        print("YORUM HATASI, HTTP", e.code)
        print(e.read().decode("utf-8", errors="replace"))
        return 1
    except Exception as e:
        print("BEKLENMEYEN HATA:", repr(e))
        return 1


if __name__ == "__main__":
    sys.exit(main())
