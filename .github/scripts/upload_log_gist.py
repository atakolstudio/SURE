#!/usr/bin/env python3
"""CI yardımcı betiği: build_output.log dosyasının son kısmını gizli bir Gist'e yükler.
Sadece build başarısız olduğunda, tanılama amacıyla çalıştırılır.
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
    if not token:
        print("HATA: GIST_TOKEN ortam değişkeni bulunamadı.")
        return 1

    if not os.path.exists(LOG_PATH):
        print(f"HATA: {LOG_PATH} bulunamadı.")
        return 1

    with open(LOG_PATH, "r", errors="replace") as f:
        content = f.read()

    if len(content) > MAX_CHARS:
        content = content[-MAX_CHARS:]

    payload = json.dumps({
        "description": "SURE build failure log",
        "public": False,
        "files": {"build_output.log": {"content": content}}
    }).encode("utf-8")

    req = urllib.request.Request(
        "https://api.github.com/gists",
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
            print("GIST_URL:", result["html_url"])
            print("GIST_ID:", result["id"])
            return 0
    except urllib.error.HTTPError as e:
        print("GIST OLUŞTURMA HATASI, HTTP", e.code)
        print(e.read().decode("utf-8", errors="replace"))
        return 1
    except Exception as e:
        print("BEKLENMEYEN HATA:", repr(e))
        return 1


if __name__ == "__main__":
    sys.exit(main())
