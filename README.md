<div align="center">

# F.R.I.D.A.Y.

**A voice assistant for Android that does short phone commands by voice.**

Say "torch on", "call Mummy" or "set an alarm for 6": F.R.I.D.A.Y. does it and confirms in one sentence.

![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Gemini](https://img.shields.io/badge/Gemini-Live-8E75B2?logo=googlegemini&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Crashlytics-FFCA28?logo=firebase&logoColor=black)

</div>

Developed by **Nandivex Technologies**.

---

## What it does

- **Live voice conversations** with Gemini Live. Tap the mic, talk, interrupt any time.
- **One personality:** calm, warm and quick. Replies are Short by default, or Detailed in Settings.
- **Phone control by connectors:** calls and messages, Wi-Fi and Bluetooth, torch, volume and brightness, alarms, timers and reminders, apps, battery and more. Each connector is opt-in and uses the permission you grant.
- **Web answers:** FRIDAY searches the web (DuckDuckGo) and reads pages on the phone. No Google Search grounding.
- **Memory on the phone:** it remembers facts you share and forgets them when you ask. Past conversations are listed on the History page.
- **Light and dark themes** in a navy, cyan and amber look.

Removed on purpose: the knowledge base, YouTube, the images page and Skills. They do not fit short commands.

## Project layout

| Path | What it is |
|---|---|
| [`android/`](android) | The app (Kotlin, Jetpack Compose, ObjectBox, Firebase). Package `com.nandivex.friday`. |
| [`scripts/`](scripts) | CI helpers, such as the version-bump check. |
| [`tests/`](tests) | Tests for the CI scripts. |
| [`docs/`](docs) | Notes: CI/CD, latency, the ViewModel split plan, privacy plan. |
| [`track.md`](track.md) | A log of every change. |
| [`.github/workflows`](.github/workflows) | Checks on every pull request and tester builds on `main`. |

## Build and run

You need JDK 17 or newer and the Android SDK (platform 36). Copy `android/local.properties.example` to `android/local.properties` and set `sdk.dir`.

```bash
cd android
./gradlew assembleDebug        # debug build, "FRIDAY Dev"
./gradlew testBetaUnitTest     # unit tests
./gradlew assembleBeta         # tester build, "FRIDAY Tester"
```

Open the app, allow the microphone and enter your Gemini API key in Settings.

| Build type | Package | Use |
|---|---|---|
| `debug` | `com.nandivex.friday.debug` | Daily development. |
| `beta` | `com.nandivex.friday.tester` | Tester builds: shrunk, arm64 and armeabi-v7a only, about 10 MB. |
| `release` | `com.nandivex.friday` | Play Store build, signed with your own keystore. |

`android/app/google-services.json` is git-ignored. Without it the app still builds, but Firebase features are off.

## Versions and tester builds

The version name is the top `## x.y.z` heading of [`android/app/src/main/assets/changelog.md`](android/app/src/main/assets/changelog.md). Every pull request that changes the app must raise it. When a pull request is merged into `main`, CI builds the `beta` build and uploads it to Firebase App Distribution (group `testers`) with the newest changelog entry as release notes. Authentication uses Workload Identity Federation, so no service-account key is stored in GitHub.

## Privacy and security

- Memory, history and usage counters are stored on the phone.
- Your voice goes to Gemini to produce replies. Crash reports go to Firebase Crashlytics.
- API keys are encrypted at rest with the Android keystore.
- Screen reading and tapping, and notification access, are off until you turn them on and allow the Android permission.
- Web pages and tool output are treated as data, never as instructions.

## Contact

Support: contact@nandivex.in
