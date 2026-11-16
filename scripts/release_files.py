"""Builds the files the website serves for a FRIDAY release, and refuses releases that must not go live.

    python scripts/release_files.py check-tag v0.0.21
    python scripts/release_files.py build --apk app-release.apk --version-code 29400000 --out site --assets release-assets --live live.json --tag v0.0.21

`build` copies the APK into the assets folder as FRIDAY.apk, which the workflow attaches to the GitHub release (Firebase
Hosting on the free plan refuses APK files, so the site only serves `latest.json` and the web pages). `latest.json` points
at that release asset and is written last. It refuses a version that is not newer than the one that is live, so an old or
broken build can never replace a newer `latest.json`. The in-app updater (UpdateRules.kt) reads `latest.json` and applies
the same host rules.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import sys

ROOT = Path(__file__).resolve().parents[1]
CHANGELOG = ROOT / "android/app/src/main/assets/changelog.md"
BASE_URL = "https://friday.nandivex.in"
# Must match UpdateRules.ALLOWED_HOSTS in the app; a test keeps the two in step.
ALLOWED_HOSTS = ("friday.nandivex.in", "friday-nandivex.web.app", "friday-nandivex.firebaseapp.com")
# The APK is a GitHub release asset. Must match UpdateRules.RELEASE_HOST and RELEASE_PATH_PREFIX in the app.
RELEASE_HOST = "github.com"
RELEASE_PATH_PREFIX = "/devxjitin/FRIDAY/releases/download/"
APK_ASSET = "FRIDAY.apk"
MAX_APK_BYTES = 150 * 1024 * 1024
MAX_NOTES = 2000
MIN_ANDROID = 26  # minSdk of the app


class ReleaseError(Exception):
    pass


def changelog_top(text):
    """(version, notes) of the newest "## x.y.z" entry, with notes as plain lines."""
    parts = re.split(r"(?m)^##\s+v?", text)
    for part in parts[1:]:
        head, _, body = part.partition("\n")
        match = re.match(r"(\d+\.\d+\.\d+)", head.strip())
        if match:
            return match.group(1), plain_notes(body)
    raise ReleaseError("changelog.md has no version heading")


def plain_notes(body):
    lines = []
    for raw in body.strip().splitlines():
        line = raw.strip()
        if not line:
            continue
        if line.startswith("###"):
            lines.append(line.lstrip("# ").strip() + ":")
        elif line.startswith("- "):
            lines.append("• " + line[2:].strip())
        else:
            lines.append(line)
    return "\n".join(lines)[:MAX_NOTES]


def check_tag(tag, text):
    """The release tag must be v<newest changelog version>, so the tag, the app and the site never disagree."""
    version, _ = changelog_top(text)
    if tag != "v" + version:
        raise ReleaseError(f"Tag {tag} does not match the newest changelog version {version}")
    return version


def sha256_file(path):
    digest = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def host_allowed(url):
    """True for an https address on one of our site hosts (where latest.json lives)."""
    match = re.match(r"^https://([^/:@?#]+)(/|$)", url)
    return bool(match) and match.group(1).lower() in ALLOWED_HOSTS


def apk_url_allowed(url):
    """True for an APK address the app will download from: our site, or a download in this repository's releases."""
    if host_allowed(url):
        return True
    match = re.match(r"^https://([^/:@?#]+)(/[^?#]*)$", url)
    return bool(match) and match.group(1).lower() == RELEASE_HOST and match.group(2).startswith(RELEASE_PATH_PREFIX)


def release_apk_url(tag):
    return f"https://{RELEASE_HOST}{RELEASE_PATH_PREFIX}{tag}/{APK_ASSET}"


def manifest(version_name, version_code, apk_url, sha256, size, notes, rollout=100):
    if not re.fullmatch(r"[0-9a-f]{64}", sha256):
        raise ReleaseError("sha256 must be 64 lower-case hex characters")
    if version_code <= 0:
        raise ReleaseError("version code must be positive")
    if size <= 0 or size > MAX_APK_BYTES:
        raise ReleaseError("APK size is outside the allowed range")
    if not 0 <= rollout <= 100:
        raise ReleaseError("rollout must be 0 to 100")
    if not apk_url_allowed(apk_url):
        raise ReleaseError("apkUrl is not an allowed address")
    return {
        "versionCode": version_code,
        "versionName": version_name,
        "apkUrl": apk_url,
        "sha256": sha256,
        "sizeBytes": size,
        "minAndroid": MIN_ANDROID,
        "rollout": rollout,
        "notes": notes[:MAX_NOTES],
    }


def require_newer(live, version_code):
    """Refuse a release that is not newer than the live one. [live] is the parsed live latest.json, or None."""
    if not live:
        return
    current = live.get("versionCode")
    if isinstance(current, int) and version_code <= current:
        raise ReleaseError(f"Version code {version_code} is not newer than the live {current}")


def load_live(path):
    if not path:
        return None
    p = Path(path)
    if not p.exists() or not p.read_text(encoding="utf-8").strip():
        return None
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None  # a missing or broken live file must not block the very first release


def build(apk, version_code, out, assets, live_path, tag, rollout):
    apk, out, assets = Path(apk), Path(out), Path(assets)
    if not apk.is_file():
        raise ReleaseError(f"APK not found: {apk}")
    version, notes = changelog_top(CHANGELOG.read_text(encoding="utf-8"))
    if not tag:
        raise ReleaseError("a tag is required: the APK address contains it")
    check_tag(tag, CHANGELOG.read_text(encoding="utf-8"))
    require_newer(load_live(live_path), version_code)
    data = manifest(version, version_code, release_apk_url(tag), sha256_file(apk), apk.stat().st_size, notes, rollout)
    out.mkdir(parents=True, exist_ok=True)
    assets.mkdir(parents=True, exist_ok=True)
    # Hosting refuses APK files on the free plan, so none may be left in the site folder.
    for old in out.glob("*.apk"):
        old.unlink()
    for old in assets.glob("*.apk"):
        old.unlink()
    shutil.copyfile(apk, assets / APK_ASSET)
    # latest.json is written last, so a half-copied release is never offered.
    (out / "latest.json").write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (out / "release-notes.txt").write_text(f"FRIDAY {version}\n\n{notes}\n", encoding="utf-8")
    return data


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    tag = sub.add_parser("check-tag")
    tag.add_argument("tag")
    b = sub.add_parser("build")
    b.add_argument("--apk", required=True)
    b.add_argument("--version-code", type=int, required=True)
    b.add_argument("--out", default="site")
    b.add_argument("--assets", default="release-assets")
    b.add_argument("--live", default="")
    b.add_argument("--tag", required=True)
    b.add_argument("--rollout", type=int, default=100)
    args = parser.parse_args(argv)
    try:
        if args.command == "check-tag":
            print(check_tag(args.tag, CHANGELOG.read_text(encoding="utf-8")))
        else:
            data = build(args.apk, args.version_code, args.out, args.assets, args.live, args.tag, args.rollout)
            print(json.dumps(data, indent=2))
    except ReleaseError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
