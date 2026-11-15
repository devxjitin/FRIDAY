import importlib.util
from pathlib import Path
import subprocess
import unittest

spec = importlib.util.spec_from_file_location("ci_changes", Path(__file__).resolve().parents[2] / "scripts/ci_changes.py")
ci = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ci)


class ChangesTests(unittest.TestCase):
    def test_docs_and_python_version_do_not_publish_apk(self):
        self.assertFalse(any(ci.classify(["README.md", "docs/a.md", "track.md", ".gitignore"]).values()))

    def test_android_assets_including_markdown_are_app_inputs(self):
        for path in ("android/app/src/main/assets/skills/a/SKILL.md", "android/app/src/main/java/Main.kt", "android/gradle.properties"):
            with self.subTest(path=path):
                result = ci.classify([path])
                self.assertTrue(result["android"] and result["publish"])

    def test_other_folders_do_not_publish(self):
        self.assertFalse(any(ci.classify(["firebase.json", "firestore.rules", "scripts/summarize_latency.py"]).values()))

    def test_publishing_logic_is_validated_and_released(self):
        for path in (".github/workflows/distribute.yml",):
            result = ci.classify([path])
            self.assertTrue(result["android"] and result["publish"])

    def test_checks_only_change_validates_all_without_publishing(self):
        result = ci.classify([".github/workflows/checks.yml"])
        self.assertTrue(result["android"])
        self.assertFalse(result["publish"])

    def test_multi_commit_push_uses_before_after(self):
        seen = []
        def diff(base, head, triple):
            seen.append((base, head, triple))
            return ["android/app/src/main/java/EarlierCommit.kt", "docs/last-commit.md"]
        self.assertTrue(ci.plan("push", {"before": "aaa", "after": "bbb"}, diff)["publish"])
        self.assertEqual([("aaa", "bbb", False)], seen)

    def test_pr_uses_merge_base_diff_and_covers_deleted_paths(self):
        def diff(base, head, triple):
            self.assertEqual(("aaa", "bbb", True), (base, head, triple))
            return ["android/app/src/main/java/Removed.kt"]
        self.assertTrue(ci.plan("pull_request", {"pull_request": {"base": {"sha": "aaa"}, "head": {"sha": "bbb"}}}, diff)["android"])

    def test_unreachable_base_and_manual_runs_validate_everything(self):
        def fail(*args):
            raise subprocess.CalledProcessError(1, "git")
        self.assertTrue(all(ci.plan("push", {"before": "aaa", "after": "bbb"}, fail).values()))
        self.assertTrue(all(ci.plan("workflow_dispatch", {}, fail).values()))


if __name__ == "__main__":
    unittest.main()
