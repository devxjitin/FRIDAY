# Data flows

What FRIDAY reads, stores and sends, as the code stands on 2026-11-16 (version 0.0.24). This is the working source for the
public privacy policy (`site/privacy.html`) and for the Google Play **Data safety** form (Sprint 8.1). If the code changes,
change this file in the same pull request.

## Sent to Google Gemini (with the user's own API key)

The app talks to `generativelanguage.googleapis.com` directly from the phone (OkHttp). Nothing goes through Nandivex servers.

| Data | When | Notes |
|---|---|---|
| Microphone audio (16 kHz PCM) | While a voice session is open | Streamed live. Not written to a file. |
| The assistant's spoken reply and text | During a session | Comes back from Gemini. |
| Screen text and the names of buttons | When a screen tool runs (`read_screen`, `get_current_app`, `click_on_text`, ...) | Marked untrusted before the model sees it. Can contain private content: messages, names, numbers, one-time codes. |
| A screenshot (JPEG, longest side 1280 px) | Only when `look_at_screen` runs | A blank or protected window is detected and not sent. |
| Clipboard text, battery, time, Wi-Fi name, paired Bluetooth devices, list of launchable apps | Only when the matching tool is called | |
| The user's name or form of address, saved memory notes, up to 10 recent turns | At the start of a session | Memory is added to the instructions. |
| The Gemini API key | With every request, as the key | Stored encrypted (Android Keystore). |

What Google does with this depends on the plan the key belongs to (Google's Gemini API terms). FRIDAY cannot promise more.

## Kept on the phone only

| Data | Where | Removed by |
|---|---|---|
| Gemini API key | Encrypted preferences (`assistant_secure_prefs`) | Remove key in Settings, clear data, uninstall |
| Memory notes (up to 150) | Local store | Memory page, clear data, uninstall |
| Past conversations (text of each turn) | ObjectBox database | History page, clear data, uninstall |
| Usage counters (voice seconds, tool calls, tokens by day) | DataStore | Usage page, clear data, uninstall |
| Settings: voice, language, reply style, your name, theme | DataStore / preferences | Clear data, uninstall |

Android backup and device transfer are **off** (`allowBackup=false`, empty backup rules), so none of this is copied to Google
Drive or a new phone.

## Sent to Firebase (project `friday-nandivex`)

| Service | What it receives | Why |
|---|---|---|
| Crashlytics | Crash stack traces, app version, phone model, Android version, an installation identifier | Fixing crashes |
| Analytics | Basic usage events and an app-instance identifier | Basic statistics. **Not configured or limited in code today.** |
| Authentication | An anonymous user (a random id), created on first launch | **Nothing reads it yet.** It exists for a later account or billing step. |
| App Check | A Play Integrity token (release build) or a debug token (debug build) | Proves requests come from the real app. **Nothing enforces it yet.** |

Open decision (`friday_plan.md`): whether to keep Analytics and the anonymous sign-in while nothing uses them. Not collecting
data is the simpler answer for the Data safety form.

## Permissions and what they are for

| Permission or service | Used for |
|---|---|
| Accessibility service | Read the screen, tap, type, scroll, swipe, global actions, screenshots |
| `RECORD_AUDIO`, foreground service (microphone, special use) | Voice session |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Gemini and network status |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Android shows the Wi-Fi name only with location access. Foreground only. |
| `BLUETOOTH` (to Android 11), `BLUETOOTH_CONNECT` | Bluetooth status and paired devices |
| `POST_NOTIFICATIONS`, `VIBRATE` | The ongoing notice, the vibrate tool |
| `ACCESS_NOTIFICATION_POLICY`, `WRITE_SETTINGS` | Do Not Disturb; brightness, screen timeout, rotation (special access the user grants) |
| `MODIFY_AUDIO_SETTINGS` | Volume and ringer |
| Voice interaction service | Default assistant role (hold home or power) |

Removed on purpose: advertising ID and AdServices permissions (`tools:node="remove"`), install-packages, overlay, contacts,
calls, SMS, calendar, files, usage access, notification listener, exact alarms, activity recognition.
Still arriving from libraries and to review in Sprint 8.1: `WAKE_LOCK`, the Play install-referrer permission, and Google
Play services' own `READ_GSERVICES`.
