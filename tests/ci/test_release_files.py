import importlib.util
import json
from pathlib import Path
import re
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("release_files", ROOT / "scripts/release_files.py")
rf = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rf)

SHA = "a" * 64
CHANGELOG = """# Changelog

Intro text.

## 0.0.21 - 2026-11-16

### Added
- First thing.
- Second thing.

### Removed
- Old thing.

## 0.0.20 - 2026-11-16

### Fixed
- Older.
"""


class ChangelogTests(unittest.TestCase):
    def test_top_entry_is_the_newest_version_with_plain_notes(self):
        version, notes = rf.changelog_top(CHANGELOG)
        self.assertEqual("0.0.21", version)
        self.assertEqual("Added:\n• First thing.\n• Second thing.\nRemoved:\n• Old thing.", notes)
        self.assertNotIn("Older", notes)

    def test_a_changelog_without_a_version_is_refused(self):
        with self.assertRaises(rf.ReleaseError):
            rf.changelog_top("# Changelog\n\nNothing here")

    def test_the_real_changelog_has_a_version(self):
        version, _ = rf.changelog_top(rf.CHANGELOG.read_text(encoding="utf-8"))
        self.assertRegex(version, r"^\d+\.\d+\.\d+$")

    def test_notes_are_cut_to_the_size_the_app_accepts(self):
        self.assertEqual(rf.MAX_NOTES, len(rf.plain_notes("- " + "x" * 5000)))

    def test_the_tag_must_match_the_newest_version(self):
        self.assertEqual("0.0.21", rf.check_tag("v0.0.21", CHANGELOG))
        for bad in ("v0.0.20", "0.0.21", "v0.0.22", ""):
            with self.subTest(tag=bad), self.assertRaises(rf.ReleaseError):
                rf.check_tag(bad, CHANGELOG)


class ManifestTests(unittest.TestCase):
    def test_a_good_manifest_points_at_the_versioned_apk_on_our_host(self):
        m = rf.manifest("0.0.21", 100, "FRIDAY-100.apk", SHA, 1000, "notes")
        self.assertEqual("https://friday.nandivex.in/FRIDAY-100.apk", m["apkUrl"])
        self.assertEqual(100, m["rollout"])
        self.assertEqual(26, m["minAndroid"])

    def test_bad_values_are_refused(self):
        cases = [
            dict(sha256="abc"), dict(sha256="A" * 64), dict(version_code=0), dict(size=0),
            dict(size=rf.MAX_APK_BYTES + 1), dict(rollout=101), dict(rollout=-1),
        ]
        for case in cases:
            args = dict(version_name="0.0.21", version_code=100, apk_name="FRIDAY-100.apk", sha256=SHA, size=1000, notes="n", rollout=100)
            args.update(case)
            with self.subTest(case=case), self.assertRaises(rf.ReleaseError):
                rf.manifest(**args)

    def test_only_our_https_hosts_are_allowed(self):
        self.assertTrue(rf.host_allowed("https://friday.nandivex.in/FRIDAY.apk"))
        self.assertTrue(rf.host_allowed("https://friday-nandivex.web.app/FRIDAY.apk"))
        for bad in ("http://friday.nandivex.in/x", "https://evil.example/x", "https://friday.nandivex.in.evil.example/x",
                    "https://friday.nandivex.in@evil.example/x", "https://friday.nandivex.in:8443/x"):
            with self.subTest(url=bad):
                self.assertFalse(rf.host_allowed(bad))


class NewerOnlyTests(unittest.TestCase):
    def test_the_first_release_is_allowed(self):
        rf.require_newer(None, 5)

    def test_an_older_or_equal_version_never_replaces_a_newer_live_one(self):
        for code in (99, 100):
            with self.subTest(code=code), self.assertRaises(rf.ReleaseError):
                rf.require_newer({"versionCode": 100}, code)
        rf.require_newer({"versionCode": 100}, 101)

    def test_a_missing_or_broken_live_file_does_not_block_a_release(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertIsNone(rf.load_live(""))
            self.assertIsNone(rf.load_live(str(Path(tmp) / "absent.json")))
            broken = Path(tmp) / "live.json"
            broken.write_text("<html>404</html>", encoding="utf-8")
            self.assertIsNone(rf.load_live(str(broken)))


class BuildTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.dir = Path(self.tmp.name)
        self.apk = self.dir / "app-release.apk"
        self.apk.write_bytes(b"fake apk bytes")
        self.log = self.dir / "changelog.md"
        self.log.write_text(CHANGELOG, encoding="utf-8")
        self.original = rf.CHANGELOG
        rf.CHANGELOG = self.log
        self.addCleanup(lambda: setattr(rf, "CHANGELOG", self.original))

    def test_build_writes_the_apk_twice_and_latest_json_with_the_checksum(self):
        out = self.dir / "site"
        data = rf.build(self.apk, 200, out, "", "v0.0.21", 100)
        self.assertEqual(rf.sha256_file(self.apk), data["sha256"])
        self.assertEqual(b"fake apk bytes", (out / "FRIDAY.apk").read_bytes())
        self.assertEqual(b"fake apk bytes", (out / "FRIDAY-200.apk").read_bytes())
        written = json.loads((out / "latest.json").read_text(encoding="utf-8"))
        self.assertEqual(data, written)
        self.assertEqual("0.0.21", written["versionName"])
        self.assertTrue((out / "release-notes.txt").read_text(encoding="utf-8").startswith("FRIDAY 0.0.21"))

    def test_older_apks_are_removed_so_only_the_current_one_is_deployed(self):
        out = self.dir / "site"
        out.mkdir()
        (out / "FRIDAY-100.apk").write_bytes(b"old")
        rf.build(self.apk, 200, out, "", "", 100)
        self.assertFalse((out / "FRIDAY-100.apk").exists())

    def test_build_refuses_a_wrong_tag_and_an_older_version_and_writes_nothing(self):
        out = self.dir / "site"
        live = self.dir / "live.json"
        live.write_text(json.dumps({"versionCode": 300}), encoding="utf-8")
        with self.assertRaises(rf.ReleaseError):
            rf.build(self.apk, 200, out, str(live), "v0.0.21", 100)
        with self.assertRaises(rf.ReleaseError):
            rf.build(self.apk, 400, out, "", "v9.9.9", 100)
        self.assertFalse((out / "latest.json").exists())


class AppAndScriptAgreeTests(unittest.TestCase):
    """The app's updater and this script must agree, or a release would be rejected on every phone."""

    kotlin = (ROOT / "android/app/src/main/java/com/nandivex/friday/data/UpdateManifest.kt").read_text(encoding="utf-8")

    def test_allowed_hosts_match_the_app(self):
        block = re.search(r"ALLOWED_HOSTS: Set<String> = setOf\(([^)]*)\)", self.kotlin).group(1)
        self.assertEqual(set(re.findall(r'"([^"]+)"', block)), set(rf.ALLOWED_HOSTS))

    def test_size_limit_matches_the_app(self):
        match = re.search(r"MAX_APK_BYTES: Long = (\d+)L \* 1024 \* 1024", self.kotlin)
        self.assertEqual(rf.MAX_APK_BYTES, int(match.group(1)) * 1024 * 1024)

    def test_minimum_android_matches_the_app(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertEqual(rf.MIN_ANDROID, int(re.search(r"minSdk = (\d+)", gradle).group(1)))

    def test_the_app_checks_the_site_this_script_writes_for(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn(f"{rf.BASE_URL}/latest.json", gradle)

    def test_the_hosting_config_serves_the_files_the_script_writes(self):
        hosting = json.loads((ROOT / "firebase.json").read_text(encoding="utf-8"))["hosting"]
        sources = {h["source"] for h in hosting["headers"]}
        self.assertTrue({"/latest.json", "/FRIDAY.apk", "/FRIDAY-*.apk"} <= sources)
        self.assertEqual("site", hosting["public"])


if __name__ == "__main__":
    unittest.main()
