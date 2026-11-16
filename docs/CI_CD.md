# CI/CD

FRIDAY ships through **Google Play only**. Two GitHub Actions workflows exist today; the Play upload workflow is planned
(Sprint 8.2 in `friday_plan.md`) and does not exist yet. Nothing runs on AWS.

| Workflow | When | What it does |
|---|---|---|
| `checks.yml` | Every pull request, and called by the other workflow | Tests and lint, plus the Python tests in `tests/ci` |
| `distribute.yml` | Every merge to `main` that touches `android/` | Builds the **tester** APK and sends it to Firebase App Distribution (group `testers`) |
| *(planned, Sprint 8.2)* | A version tag | Builds the signed `.aab` and uploads it to Play's internal testing track |

## Checks

`checks.yml` classifies the complete diff (`scripts/ci_changes.py`) so docs-only changes stay cheap but the required gate
(`Required checks`) always reports. Android changes run unit tests and lint in one Gradle call with a fixed version code
(`-PappVersionCode=1`), which is used only for validation and never published. A pull request that changes the app must put
a newer version at the top of `android/app/src/main/assets/changelog.md` (`scripts/check_version_bump.py`). The Python tests
(`python -m unittest discover -s tests/ci`) cover change selection and the version bump.

## Tester builds (Firebase App Distribution)

Every merge to `main` that touches the app builds `assembleBeta` (package `com.nandivex.friday.tester`, signed with the tester
key) and uploads it to App Distribution with the newest changelog entry as release notes. Testers get a notification in
the Firebase App Tester app. Login to Google is keyless (Workload Identity Federation), so no token is stored.

## Play release (planned)

The `release` build (`com.nandivex.friday`) is for Google Play only. It will be uploaded as an App Bundle
(`./gradlew bundleRelease`) signed with the **upload key**, with Play App Signing holding the app's own key. The release
signing values are read from `android/local.properties` (`RELEASE_*`) on the dev PC. Nothing publishes the release build
automatically yet.

## The website

`site/` (home, privacy, terms, 404) is static HTML on Firebase Hosting, project `friday-nandivex`. It does **not** offer a
download: FRIDAY is not distributed from the website. It exists because Play requires a public privacy policy URL. It is
deployed by hand (`firebase deploy --only hosting`, which needs `firebase login --reauth` on the dev PC); no workflow does it.
The Content Security Policy in `firebase.json` allows only the site's own files.

## Secrets and variables

GitHub repository **secrets**:

| Name | Used by | Content |
|---|---|---|
| `GOOGLE_SERVICES_JSON` | `distribute.yml`, `checks.yml` | `android/app/google-services.json`, base64 |
| `TESTER_KEYSTORE`, `TESTER_STORE_PASSWORD`, `TESTER_KEY_ALIAS`, `TESTER_KEY_PASSWORD` | `distribute.yml` | The shared tester signing key |

The `RELEASE_*` secrets and `RELEASE_CERT_SHA256` from the old website release are no longer used and can be deleted from
the repository settings.

## Keep the release key safe

The release key (`android/keystore/nivi-release.jks` on the dev PC) becomes the Play **upload key**. Keep the keystore and
its passwords outside git, with backups in two separate places. If an upload key is lost, Play support can reset it, but that
takes time.

## Local commands

```bash
# Tests, lint and a debug build
cd android && ./gradlew testDebugUnitTest lintDebug assembleDebug

# The Python tests
python -m unittest discover -s tests/ci -v

# A release App Bundle (needs the RELEASE_* values in android/local.properties)
cd android && ./gradlew bundleRelease

# Preview the website on this PC
python -m http.server 8080 --directory site
```

## Repository protection

Protect `main` with a pull request requirement, the `Required checks` status, up-to-date branches, resolved conversations,
administrator enforcement, and force pushes and deletions disabled. This is a repository setting and is independent of the
YAML: inspect the real GitHub API result before claiming it is active.
