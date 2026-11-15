import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OLD_NAME = "ultr" + "on"  # split so this file itself does not match


class OldNameTests(unittest.TestCase):
    def test_the_old_app_name_only_remains_in_the_change_log(self):
        out = subprocess.run(
            ["git", "grep", "-i", "-l", OLD_NAME, "--", ".", ":!track.md", ":!friday_plan.md"],
            cwd=ROOT, capture_output=True, text=True,
        ).stdout.split()
        self.assertEqual([], out)


if __name__ == "__main__":
    unittest.main()
