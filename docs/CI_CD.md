# CI/CD

FRIDAY is built and delivered by three GitHub Actions workflows. Nothing runs on AWS any more.

| Workflow | When | What it does |
|---|---|---|
| `checks.yml` | Every pull request, and called by the other two | Tests and lint, plus the Python tests in `tests/ci` |
| `distribute.yml` | Every merge to `main` that touches `android/` | Builds the **tester** APK and sends it to Firebase App Distribution (group `testers`) |
| `release.yml` | Pushing a tag like `v0.0.21` | Builds the signed **release** APK and publishes it to the website |

## Checks

`checks.yml` classifies the complete diff (`scripts/ci_changes.py`) so docs-only changes stay cheap but the required gate
(`Required checks`) always reports. Android changes run unit tests and lint in one Gradle call with a fixed version code
(`-PappVersionCode=1`), which is used only for validation and never published. A pull request that changes the app must put
a newer version at the top of `android/app/src/main/assets/changelog.md` (`scripts/check_version_bump.py`). The Python tests
(`python -m unittest discover -s tests/ci`) cover change selection, the version bump and the release files.

## Tester builds (Firebase App Distribution)

Every merge to `main` that touches the app builds `assembleBeta` (package `com.nandivex.friday.tester`, signed with the tester
key) and uploads it to App Distribution with the newest changelog entry as release notes. Testers get a notification in
the Firebase App Tester app. Login to Google is keyless (Workload Identity Federation), so no token is stored.

## Website releases

The release APK (`com.nandivex.friday`) is published on **https://friday.nandivex.in** (Firebase Hosting, project
`friday-nandivex`). Its backup address is `https://friday-nandivex.web.app`.

To release:

1. Put the new version at the top of `changelog.md` and merge to `main` (the tester build goes out as usual).
2. Tag that commit and push the tag. The tag must be `v` plus the newest changelog version:

   ```bash
   git tag v0.0.21
   git push origin v0.0.21
   ```

3. `release.yml` then:
   - checks that the tag matches the changelog (`scripts/release_files.py check-tag`) and runs `checks.yml`;
   - builds `assembleRelease` with a version code of minutes since 1970, so each release is higher than the one before;
   - checks the APK is signed (and, if the repository variable `RELEASE_CERT_SHA256` is set, with that exact key);
   - reads the **live** `latest.json`. If it cannot be read, or it is newer or equal, the release stops. A 404 on both
     addresses means this is the first release;
   - builds the release files (`scripts/release_files.py build`): `release-assets/FRIDAY.apk` and `site/latest.json`, whose
     `apkUrl` is `https://github.com/devxjitin/FRIDAY/releases/download/<tag>/FRIDAY.apk`;
   - publishes the APK in a GitHub release (a re-run replaces the asset), then downloads it back and checks the SHA-256;
   - only then deploys `site/` to Firebase Hosting (so `latest.json` never points at a file that is not there), and checks
     the live site serves the new version.

The APK is a GitHub release asset, not a Hosting file, because Firebase's free Spark plan refuses executable files
("Executable files are forbidden on the Spark billing plan"). Every version stays available in the releases.

`latest.json` holds `versionCode`, `versionName`, `apkUrl`, `sha256`, `sizeBytes`, `minAndroid`, `rollout` and `notes`. The
app (`UpdateRules.kt`) offers an update only for a newer version code, a host in its allow-list, a matching SHA-256 and a
minimum Android it meets. To hold a release back, deploy `latest.json` with `rollout` below 100; the app puts each phone in
a fixed bucket from 0 to 99 and offers the update only when the bucket is lower than `rollout`.

### What the site serves

- `site/` is the whole website (home, download, install guide, privacy, terms). It is static HTML, one stylesheet, one script
  and no outside resources; the Content Security Policy in `firebase.json` blocks everything else.
- `latest.json` and `release-notes.txt` are written into `site/` by the workflow and are gitignored. No APK may be in `site/`.
- `/FRIDAY.apk` on the site is a redirect (`firebase.json`) to the newest release asset on GitHub, so the download button and
  old links keep working.
- Cache rules (`firebase.json`): `latest.json` is never cached.
- The app downloads from github.com only for this repository's releases, and accepts GitHub's redirect to
  `release-assets.githubusercontent.com` / `objects.githubusercontent.com` (`UpdateRules.kt`).

## Secrets and variables

GitHub repository **secrets**:

| Name | Used by | Content |
|---|---|---|
| `GOOGLE_SERVICES_JSON` | both | `android/app/google-services.json`, base64 |
| `TESTER_KEYSTORE`, `TESTER_STORE_PASSWORD`, `TESTER_KEY_ALIAS`, `TESTER_KEY_PASSWORD` | `distribute.yml` | The shared tester signing key |
| `RELEASE_KEYSTORE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | `release.yml` | The release signing key (`android/keystore/nivi-release.jks` on the dev PC), keystore as base64 |

Repository **variable** (optional but recommended): `RELEASE_CERT_SHA256`, the SHA-256 of the release certificate, so a build
signed with any other key is refused.

The Google service account `firebase-adminsdk-fbsvc@friday-nandivex.iam.gserviceaccount.com` needs the **Firebase Hosting
Admin** role for `release.yml` to deploy, and the Firebase Hosting API must be enabled on the project.

## Keep the release key safe

Android installs an update only if it is signed with the same key as the installed app. If the release key is lost, every
user has to uninstall and reinstall. Keep the keystore and its passwords outside git, with backups in two separate places.

## Local commands

```bash
# Tests, lint and a debug build
cd android && ./gradlew testDebugUnitTest lintDebug assembleDebug

# The Python tests, including the release files
python -m unittest discover -s tests/ci -v

# Preview the website on this PC
python -m http.server 8080 --directory site
```

## Repository protection

Protect `main` with a pull request requirement, the `Required checks` status, up-to-date branches, resolved conversations,
administrator enforcement, and force pushes and deletions disabled. This is a repository setting and is independent of the
YAML: inspect the real GitHub API result before claiming it is active.
