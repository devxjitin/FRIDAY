<div align="center">

# F.R.I.D.A.Y.

**Hands-free smartphone access for Android.**

Say what you want done. F.R.I.D.A.Y. reads the screen and taps, types, scrolls and opens apps for you, so you do not have to touch the phone.

![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Gemini](https://img.shields.io/badge/Gemini-Live-8E75B2?logo=googlegemini&logoColor=white)

</div>

Developed by **Nandivex Technologies**. Not yet released: the plan is to publish it on Google Play only (see [`friday_plan.md`](friday_plan.md)).

---

## What it does today

- **Voice control of the phone.** Tap the mic, talk, interrupt any time. Replies come by voice from Gemini Live.
- **Screen control** through Android's accessibility service: read the screen, tap, type, scroll, swipe, wait for text, and look at the screen as a picture when an app shows no readable text.
- **Phone navigation:** back, home, recents, notification shade, quick settings, lock screen, screenshot, power menu, split screen.
- **Settings and panels:** Wi-Fi, Bluetooth, mobile data, airplane mode, hotspot, NFC, volume, ringer, Do Not Disturb, brightness, screen timeout, auto-rotate, dark mode, battery saver, flashlight (with strobe and SOS).
- **Apps and links:** open an app, open a link, open the camera, list the apps that can be opened.
- **Clipboard, time and battery.**
- **Memory on the phone:** short notes about you that you can read, edit and delete. Past conversations are listed on the History page.
- One calm personality, Short or Detailed replies, 30 voices, many languages, light and dark themes.

The full list is on the **What F.R.I.D.A.Y. can do** page inside the app. It is read-only: there are no connectors and nothing to switch on or off.

## What it does not do

- It does not call, text, message or pay on its own. To do those, you ask, and F.R.I.D.A.Y. opens the app and uses it on screen the way your hands would. There is **no confirmation before a risky tap such as Send or Pay yet** (planned in Sprint 6.2).
- Android 10 and newer do not let apps flip Wi-Fi, mobile data, airplane mode, hotspot, NFC, dark mode or battery saver directly, so F.R.I.D.A.Y. opens the panel and taps the switch.
- Apps that protect their window (banking, some games) cannot be read or screenshotted.
- No wake word, home-screen widget, Quick Settings tile or typed command bar yet.
- No account, sign-in or payment. First-run setup (name, API key, agreement) is not built yet (Sprint 5.1).

## Privacy in short

- You bring your own **Gemini API key**. It is stored encrypted on the phone (Android Keystore) and used only to call Gemini.
- While a session is open, microphone audio goes to Gemini. When F.R.I.D.A.Y. carries out a request it reads the screen, and the screen text, or a screenshot when you ask it to look, goes to Gemini too. What Google does with it depends on the plan of your key.
- Memory, history, usage counters and settings stay on the phone. Android backup is off.
- Firebase is used for crash reports (Crashlytics) only: no Analytics, no sign-in, no App Check. See [`docs/DATA_FLOWS.md`](docs/DATA_FLOWS.md) for the exact list and the public [privacy policy](site/privacy.html).
- Text from the screen or clipboard is marked untrusted before the model sees it, and only your spoken request gives authority.

## Project layout

| Path | What it is |
|---|---|
| [`android/`](android) | The app (Kotlin, Jetpack Compose, ObjectBox, Firebase). Package `com.nandivex.friday`. |
| [`site/`](site) | The public website on Firebase Hosting: home, privacy, terms. It offers no download. |
| [`scripts/`](scripts), [`tests/`](tests) | CI helpers and their tests. |
| [`docs/`](docs) | CI/CD, latency measuring, data flows. |
| [`friday_plan.md`](friday_plan.md) | The plan: vision, what is built, phases and sprints. |
| [`track.md`](track.md) | A log of every change. |
| [`.github/workflows`](.github/workflows) | Checks on every pull request, tester builds on `main`. |

## Build and run

You need JDK 17 or newer and the Android SDK (platform 36). Copy `android/local.properties.example` to `android/local.properties` and set `sdk.dir`.

```bash
cd android
./gradlew installDebug                          # "FRIDAY Dev" on a connected phone
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew assembleBeta                          # tester build, "FRIDAY Tester"
./gradlew bundleRelease                         # App Bundle for Google Play (needs the release key)
```

After installing, open the app, allow the microphone, turn on FRIDAY in Android's accessibility settings, and add your Gemini API key in Settings.

| Build | Package | Use |
|---|---|---|
| `debug` | `com.nandivex.friday.debug` | Daily development. |
| `beta` | `com.nandivex.friday.tester` | Tester builds sent by Firebase App Distribution. |
| `release` | `com.nandivex.friday` | The Google Play build. |

`android/app/google-services.json` is git-ignored. Without it the app still builds, but the Firebase features are off.

## Versions and tester builds

The version name is the top `## x.y.z` heading of [`android/app/src/main/assets/changelog.md`](android/app/src/main/assets/changelog.md). Every pull request that changes the app must raise it. When a pull request is merged into `main`, CI builds the `beta` build and uploads it to Firebase App Distribution (group `testers`) with the newest changelog entry as release notes. See [`docs/CI_CD.md`](docs/CI_CD.md).

## Contact

contact@nandivex.in
