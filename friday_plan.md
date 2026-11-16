# FRIDAY: rebuild plan (Ultron → FRIDAY)

Owner: Nandivex Technologies · Plan date: 2026-11-13 · Updated: 2026-11-16 · Status: **Phases 1 and 2 done, Phase 3 done
(version 0.0.19), Phases 4 to 7 still to build · Distribution: own website (route B, decided)**

## 1. Product vision

FRIDAY is a **hands-free phone control assistant**. You speak, and it operates the phone for you: it reads the screen,
taps, types, scrolls, opens apps, and flips settings such as Wi-Fi, Bluetooth, volume and the torch. It does not call,
text or message people by itself, and it keeps no calendar, files, contacts or location. If you want one of those, you
say so and FRIDAY opens the app and uses it, the way your own hands would.

Every decision below follows from four facts:

1. **Hands-free control, not features.** FRIDAY has no connectors and nothing to switch on or off. Every phone-control
   tool is built in and always on. The sidebar page "What F.R.I.D.A.Y. can do" lists them, read-only.
2. **Phone control permission first.** On every open, until it is on, the app shows a disclosure and sends the user to
   turn on FRIDAY's accessibility service. The home screen stays closed without it.
3. **Distribution is our own website, not Google Play (D7, decided 2026-11-16).** Google Play restricts the
   Accessibility API hard, so FRIDAY is released as a signed APK from a Nandivex-owned site, with its own updater and
   Razorpay payments. Google Play stays a later option (section 12).
4. **Firebase only, no AWS.** One backend that you own, under the Firebase project `friday-nandivex`.

---

## 2. Where we are

| Done | What it did | Version |
|---|---|---|
| Sprint 1.1 | Package `com.nandivex.friday`, debug / beta / release variants, Firebase project, App Distribution | 0.0.1 to 0.0.10 |
| Sprint 1.2 | Skills and the Decisions API removed | 0.0.4 |
| Sprint 1.3 | AWS clients, YouTube, updater, licence server, Knowledge base, Images and history search removed | 0.0.7 to 0.0.8 |
| Sprint 2.1 | One FRIDAY persona, Short / Detailed reply style, Nandivex credit, default voice Kore | 0.0.11 |
| Sprint 2.2 | FRIDAY theme, logo, icons, splash, animated HUD ring | 0.0.12 to 0.0.13 |
| Sprint 2.3 | Every "Ultron" changed to FRIDAY, new README | 0.0.14 |
| Sprints 3.1 to 3.3 | Connectors, calls, SMS, WhatsApp, Wi-Fi / Bluetooth toggles, strobe and SOS torch, dark mode, battery saver | 0.0.15 to 0.0.17 |
| Sprint 3.4 | **Vision change:** connectors, calls, SMS, WhatsApp, contacts, calendar, location, notifications, files, app usage, share, weather and web search removed; phone control gate on open | 0.0.18 |
| Sprint 3.5 | Alarms, timers, reminders, show-notification and status tools removed; read-only Abilities page | 0.0.19 |
| Decision D7 | Route B chosen: release from our own website | n/a |
| Sprint 6.1 | Release build and updater: `latest.json` rules, checksum and signer checks, update dialog, `isAccessibilityTool` off | 0.0.21 |
| Sprint 6.2 | **Built, not deployed yet:** the website (`site/`), `firebase.json` hosting, `scripts/release_files.py`, `release.yml`, the rewritten `docs/CI_CD.md` | no app change |

**Not built yet:** Firebase sign-in and Firestore rules, onboarding, free trial, coupons, Razorpay, wake word. **Built but
not live:** the website and the release workflow (Sprint 6.2): they need the one-time setup in section 12. The old activation-key screen is already gone: every build opens straight to the
assistant until the free trial and accounts arrive in Phase 4.

---

## 3. Decisions I need from you

| # | Question | My recommendation |
|---|---|---|
| D1 | **Payments.** On Play, digital subscriptions must use Google Play Billing. Off Play there is no such rule. | **Decided with D7:** Razorpay only. If FRIDAY ever goes on Play, add Play Billing then. |
| D2 | Package name. | **Done:** `com.nandivex.friday`. |
| D3 | **Plan prices** (monthly / yearly, INR). | You decide. The plan supports both. |
| D4 | **Name and IP.** F.R.I.D.A.Y. is a Marvel character. Marvel names, quotes and film artwork risk a takedown. | App name "FRIDAY – Voice Assistant", personality *inspired by* the film AI, no Marvel names or logos. |
| D5 | Knowledge base, YouTube, Images page. | **Done:** all removed. |
| D6 | Firebase account and project. | **Done:** project `friday-nandivex`. |
| **D7** | **Distribution route.** Google's Accessibility API policy (read 2026-11-16) says an app that is not an accessibility tool may not use the API "to autonomously initiate, plan, and execute actions", and only services whose main purpose is helping people with disabilities may set `isAccessibilityTool`. A general voice assistant does not qualify, and FRIDAY's tap-and-type loop is that kind of autonomy. | **Decided: route B.** Release from our own website (Firebase Hosting on a Nandivex GoDaddy domain) and Firebase App Distribution, Razorpay only. See section 12. |

---

## 4. Persona (done in Sprint 2.1)

One FRIDAY persona: calm, warm, quick, quietly witty. "On it, Boss." / "Done. Torch is on." It confirms in one short
sentence and explains only when asked. The user picks the form of address ("Boss" by default). Female default voice
(Kore). Reply style: Short (default) or Detailed. Creator line: "FRIDAY is developed by Nandivex Technologies."

The prompt now says FRIDAY operates the phone by voice and does not call, text or message people by itself, and that it
should do those by opening the app and using the screen. Keep this in step with the Abilities page.

## 5. Removed for good

- **Skills, Decisions API, Knowledge base (RAG), YouTube search and downloads, Images page, history-search tools:**
  gone since Sprints 1.2 and 1.3.
- **Connectors:** the registry, the page, the per-tool switches and "Connect all essentials". Gone in Sprint 3.4.
- **Gone in Sprint 3.4:** `make_call`, answer / end / reject call, speaker, mute, dialer, `send_sms`, SMS draft, WhatsApp
  message and call, contacts, calendar, location and directions, read / reply / dismiss notifications and now playing,
  file search and browse, app usage, share text and file, weather, web search, web page reading, the spoken-yes
  `ActionGate`, the SMS rate limit and the "Read out notifications" setting.
- **Gone in Sprint 3.5:** alarms, timers, reminders (with the ringing screen, service, boot receiver and automation
  rules), `show_notification`, steps, light sensor, storage and memory, device info.
- **Permissions gone with them:** `CALL_PHONE`, `ANSWER_PHONE_CALLS`, `SEND_SMS`, `READ_CONTACTS`, calendar,
  `MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_*`, `PACKAGE_USAGE_STATS`, the notification listener, exact alarms, full-screen
  intent, boot completed, wake lock, activity recognition, foreground data sync.

## 6. What FRIDAY can do now (58 tools, no connectors)

Everything is always on. A unit test keeps every tool in exactly one group of the Abilities page.

| Group | Tools |
|---|---|
| **See and use the screen** | `read_screen`, `look_at_screen`, `get_current_app`, `wait_for_screen_text`, `click_on_text`, `click_screen_item`, `tap_screen_position`, `long_press_position`, `type_text`, `press_enter`, `scroll_screen`, `swipe_screen` |
| **Phone navigation** | back, home, recents, previous app, notification shade, quick settings, lock screen, screenshot, power menu, split screen |
| **Connectivity** | Wi-Fi details and toggle, Bluetooth status and toggle, paired devices, network status, mobile data panel, airplane mode, hotspot, NFC, connectivity panels |
| **Sound and media** | volume (get / set), ringer (get / set), vibrate, Do Not Disturb, media keys |
| **Display and torch** | brightness (get / set), screen timeout, auto-rotate, dark mode, battery saver, flashlight, strobe and SOS |
| **Apps and links** | open app, list apps, app settings page, open settings, open camera, open link |
| **Clipboard and status** | read and write clipboard, time, battery |

Also: `save_memory` and `forget_memory` (150 short notes about the user, kept on the phone).

**Platform limits the design accounts for:**
- Android 10 and newer do not let apps switch Wi-Fi, mobile data, airplane mode, hotspot, NFC, dark mode or battery
  saver directly. FRIDAY opens the system panel or settings and then taps the switch itself through the screen tools.
  Bluetooth on Android 13 and newer shows an Android "Allow" question.
- Apps that protect their window (banking, some games) cannot be read or screenshotted. `look_at_screen` needs
  Android 11 or newer.
- Anything that used to be a tool (call, text, alarm, notes, music search, maps) is now done by driving the app on the
  screen. That is slower and depends on the app's layout, so Sprint 3.6 tests the common ones.

**Safety:**
- Text from other apps (a screen, the clipboard) is marked untrusted before the model sees it, and the prompt says only
  the owner's spoken request gives authority.
- The old spoken-yes rule is gone because no tool sends anything to a person any more. But FRIDAY can still press
  Send inside WhatsApp or approve a payment screen. Sprint 3.6 adds a confirmation before irreversible screen actions.

## 7. First open and the phone control gate (done in Sprint 3.4)

`MainActivity` routes today: access check (release builds only) → **phone control gate** → home.

- The gate (`PhoneControlScreen`) says what the service does (reads the screen and taps, types and scrolls for you),
  what is shared (screen text goes to Google Gemini only to carry out the request), and that it can be turned off in
  Settings. "Agree and turn on" opens Android's accessibility settings. It re-checks every time the app resumes.
- After it is on, the app asks once for the runtime permissions the tools use: location (Wi-Fi name) and nearby devices
  (Bluetooth, Android 12 and newer). Microphone and notifications are asked on first launch.
- **Sideloaded installs on Android 13 and newer:** Android treats an app that came from a browser or file manager (not
  from a store) as "restricted" and greys out its accessibility switch until the user opens the app's info page and
  chooses "Allow restricted settings" (menu with three dots). The gate must show this step with pictures, and we must
  test it on a real phone with the website APK and with the App Distribution build (Sprint 4.1 and 6.1).
- Housekeeping, now that D7 is decided: the comment in `accessibility_service_config.xml` still says screen tools are
  "switched on in Settings", and the file sets `isAccessibilityTool="true"`. FRIDAY is not an accessibility tool, so set
  it to `false` and rewrite the comment (Sprint 6.1).

## 8. Theme and logo (done in Sprints 2.2 and 2.3)

FRIDAY HUD palette (deep navy, cyan, amber) with contrast tests, adaptive and monochrome icons, splash screen, animated
HUD ring on the voice screen. Still to do for any store listing: the 512 px icon and the 1024×500 feature graphic.

## 9. Firebase (project `friday-nandivex`)

Done in Sprint 1.1: project, Android app, `google-services.json` (gitignored), App Distribution, Crashlytics.

Still to do (Phase 4 and 5):
1. **Authentication** (Google sign-in, phone OTP optional), **Firestore**, **Cloud Functions** (Node 22, `asia-south1`),
   **Firebase Hosting** (the download site, section 12), and **App Check**. App Check's Play Integrity needs a
   Play-installed app to give a full verdict, so for sideloaded builds we rely on Auth, security rules and rate limits,
   and decide in Sprint 4.2 whether App Check can still help.
2. Debug and release **SHA-1 / SHA-256** fingerprints. Google sign-in fails without them.
3. `firestore.rules`: users read only their own `entitlements/{uid}` and `users/{uid}`. Clients cannot write
   entitlements, coupons or trials; only Cloud Functions can.
4. Old project `ultron-5350c` and the AWS account stay until the new one is live, then are deleted **with your OK**
   after a backup (DynamoDB `ultron-licenses`, the `ultron-app-updates-*` bucket). Do not touch `future-self-booth-api`
   or `/age-prediction/hf-token`: they belong to other projects.

## 10. Onboarding (Sprint 4.1)

A first-install flow (`ui/onboarding/`), shown until finished. Each step can be revisited in Settings.

1. **Welcome:** animated FRIDAY logo, "Your phone, on voice command", language.
2. **Your name** and how FRIDAY should address you.
3. **Sign in:** Google sign-in, so the trial and subscription follow the user, not the phone.
4. **Gemini API key:** paste field, "Get a free key" button, and a live check (one tiny Gemini call) before Next.
5. **Voice:** pick a voice with a 2-second sample.
6. **Phone control:** the disclosure from section 7, plus microphone and notifications. This step is required; the app
   cannot work without it. It replaces the old "essential permissions" step.
7. **Plan:** 1-day free trial, subscribe, or enter a coupon.
8. **Done:** tips such as *"Say: turn on the torch, or open YouTube and search for lo-fi"*.

Routing becomes: onboarding not done → onboarding; done but no active entitlement → plan screen; phone control off →
gate; otherwise → home.

## 11. Subscriptions, coupons and trial (Phase 4 and 5)

**Razorpay architecture, all on Firebase:**

```
App (Firebase Auth) ──callable──▶ createSubscription(planId) ─▶ Razorpay Subscriptions API → subscription_id
App opens Razorpay Checkout with subscription_id
App ──callable──▶ verifyPayment(payment_id, subscription_id, signature) → HMAC check → entitlements/{uid}
Razorpay ──webhook──▶ razorpayWebhook (signature check)
     subscription.activated / charged / halted / cancelled / completed → entitlements/{uid}
App listens to entitlements/{uid} (read-only for the user)
```

- `entitlements/{uid}`: `plan` (trial | monthly | yearly | developer), `status` (active | halted | cancelled |
  expired), `expiresAt`, `source` (razorpay | coupon | trial), `subscriptionId`.
- Razorpay `key_secret` and the webhook secret live in Functions secrets. Only the public `key_id` is in the app.
  Razorpay is the only payment method (D1), so there is no Play Billing and no User Choice Billing.
- Cached status with an offline grace period (3 days), read from Firestore's offline cache.
- Account page: plan, renewal date, **Cancel subscription** (cancel at cycle end). With no Play, we also own refunds, GST invoices and support for payments.
- Replaces the Ultron-era `License.kt` (`ULTR-…` keys), `AccessViewModel` and `ActivationScreen`.
- You do the Razorpay merchant account, KYC and the Plans in the Razorpay dashboard. I add the plan IDs.

**Coupons** (`FRIDAYFREE` = developer code):
- Live in `coupons/{code}`, written only by Cloud Functions. Redeemed through `redeemCoupon(code)`. The code is never in
  the app, because anything in the APK can be extracted.
- `FRIDAYFREE` = `{ type: "developer", grants: "developer" plan, no expiry, allowedEmails: [<Nandivex developer
  emails>], maxRedemptions: 10 }`. Anyone else gets "This code is not valid for your account."
- Later types: `percent_off` / `flat_off` (as a Razorpay offer) and `free_days`.
- One redemption per account, recorded in `coupons/{code}/redemptions/{uid}`. A developer-only admin page under Settings
  → Developer creates, disables and lists coupons.

**1-day free trial:** `startTrial()` writes `entitlements/{uid} = { plan: "trial", expiresAt: now + 24h }`. Once per
account and once per device (`trials/{deviceHash}`: hash of `ANDROID_ID` and the app signing certificate, and
the device attestation where the phone gives one), so reinstalling or making a new Google account on the same phone does not restart it. A countdown shows on home;
the plan screen opens when it ends. The trial is app-managed through Firebase.

## 12. Distribution: our own website (route B, decided)

**Why:** Google's Accessibility API policy (read on 2026-11-16; check the live page again before any Play submission)
- An app that is **not** an accessibility tool must not use the API to autonomously initiate, plan and execute actions.
  Fixed, human-defined scripts are not covered. FRIDAY's model decides what to tap next, so it is covered.
- `isAccessibilityTool="true"` is for services whose **main purpose** is helping people with disabilities. FRIDAY is not
  one, so we do not claim it.
- Misdeclaring is grounds for suspension of a developer account. Staying off Play avoids that risk entirely.

**What we build**
- **The site:** a Firebase Hosting site in `friday-nandivex` (free, automatic HTTPS), served on a **Nandivex domain
  bought at GoDaddy**: `nandivex.in`. I propose the subdomain `friday.nandivex.in`, so the main `nandivex.in` site is not
  touched. The contact address is `contact@nandivex.in`; the app used the wrong domain (`support@nandivex.com`) until
  it was corrected (see section 14).
  Pages: landing, **Download**, sideloading guide with pictures (including "Allow restricted settings"), privacy
  policy, terms, support contact.
- **The domain steps:** I add the custom domain in Firebase Hosting, which gives the DNS records to create. **You** add
  those records (a TXT to verify and the A records Firebase shows) in GoDaddy's DNS page, because that is your account.
  Firebase then issues the HTTPS certificate by itself, which can take up to a day. Keep the Firebase `*.web.app`
  address as a backup link until the domain works.
- **The release files:** `FRIDAY.apk` and `latest.json` (`versionCode`, `versionName`, `apkUrl`, `sha256`, `notes`,
  `minAndroid`). `latest.json` is written last, so a half-uploaded release is never offered.
- **The updater (in the app):** checks `latest.json` on open, shows "Update available" with the notes, downloads the APK
  into the app's cache (the `FileProvider` for `updates/` already exists), checks the SHA-256, and hands it to Android's
  installer. This needs `REQUEST_INSTALL_PACKAGES`, which is fine off Play. Never auto-installs without a tap.
- **The release key:** the existing release key `android/keystore/nivi-release.jks` (from the Ultron days, already set in
  `local.properties`) signs FRIDAY. It is kept outside git and must be backed up in **two** places. Android only accepts
  an update signed with the same key, so losing it strands every user. The tester and debug keys stay separate.
- **The release workflow:** `.github/workflows/release.yml` builds the release APK when a tag `v<version>` is pushed, runs
  the tests, publishes the files to Hosting, then keeps the APK in a GitHub release. Hosting serves only the current
  APK; **every** version stays in GitHub Releases (this replaces the old "keep three" idea). It replaces the old AWS OTA
  workflow, and `docs/CI_CD.md` now describes it.
- **Testers keep using Firebase App Distribution** for pre-release builds (section 18).

**One-time setup before the first release** (nothing here is automatic):
1. `firebase login --reauth` on the dev PC (the saved login has expired), then `firebase deploy --only hosting` once to
   publish the pages, and check them on `https://friday-nandivex.web.app`. This is the first public publish, so it needs
   your OK.
2. In the Firebase console, Hosting → **Add custom domain** → `friday.nandivex.in`. Firebase shows a TXT record and A
   records; add them in GoDaddy's DNS page for `nandivex.in`. The certificate follows by itself (up to a day).
3. In GitHub, add the secrets and the variable listed in `docs/CI_CD.md` (the release keystore as base64 and its
   passwords, plus `RELEASE_CERT_SHA256`), and give the service account the **Firebase Hosting Admin** role.
4. Have a lawyer read `site/privacy.html` and `site/terms.html`. They describe how version 0.0.21 behaves and are a
   plain-language starting point, not legal advice.
5. Tag a release: `git tag v0.0.21 && git push origin v0.0.21`.

**Costs of this route:** no Play trust badge or discovery, users must allow "install unknown apps" once, the sideload
restriction on accessibility (section 7) needs a clear guide, and we handle payments support ourselves. Security
comes from signed builds, a checksum shown on the download page, and HTTPS only.

**Later options.** Other stores (Samsung Galaxy Store, Amazon Appstore, Xiaomi GetApps) have their own rules to read
before we submit. Google Play stays possible in one of two forms, each needing its own decision:
- **As an accessibility tool**, only if FRIDAY truly becomes hands-free control for people who cannot use a touchscreen,
  with that as the real core purpose. Needs Play Billing next to Razorpay.
- **A reduced build** without autonomous screen control (fixed-script tools only). Safe on Play, but not the vision.

## 13. Permissions today (manifest of version 0.0.19)

| Permission or service | Why | Play note |
|---|---|---|
| `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, foreground service (`microphone`, `specialUse`) | Voice session | Normal. (A Play listing would need a use description for the foreground service types.) |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Gemini and network status | Normal |
| `POST_NOTIFICATIONS`, `VIBRATE` | Service notification, vibrate tool | Normal |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Android only shows the Wi-Fi name with location access | Foreground only. Disclose in Data safety |
| `BLUETOOTH` (to Android 11), `BLUETOOTH_CONNECT` | Bluetooth status and paired devices | Normal |
| `ACCESS_NOTIFICATION_POLICY` | Do Not Disturb | Special access, user-granted |
| `WRITE_SETTINGS` | Brightness, screen timeout, auto-rotate | Special access, user-granted |
| `SYSTEM_ALERT_WINDOW` | Declared for starting screens from the background, but no code uses it (checked 2026-11-16) | Drop it in Sprint 6.1: it makes Android show a scary warning for nothing |
| **Accessibility service** | The whole product | Not on Play: no Play review. Disclosed in the app (section 7) and on the website |
| Voice interaction and recognition services | Default assistant role (hold home or power) | Normal |

Not requested, and to keep it that way: `ACCESS_BACKGROUND_LOCATION`, `QUERY_ALL_PACKAGES` (the `<queries>` block lists
launcher apps instead). **To add in Sprint 6.1:** `REQUEST_INSTALL_PACKAGES`, for the updater (release build only).

## 14. Rebrand and housekeeping

Done in Sprint 2.3 (README, strings, channels, help page). Still open:
- `git grep -i ultron` should only hit `track.md` history and `docs/ultron-changelog.md`. Check `docs/`, and delete the
  empty folders left under `mcp/` on the dev PC (git does not track them).
- **Contact email:** `contact@nandivex.in`. The app, README and an old changelog line said `support@nandivex.com`, the
  wrong domain; fixed in the contact-email change. The website's support page and the privacy policy use it too.
- Update `docs/PRIVACY_PLAN.md` and the privacy policy: the app no longer touches contacts, calls, files or location
  beyond the Wi-Fi name, and screen text goes to Gemini.
- Change `docs/Idea.md` and `README.md` to describe a hands-free phone control assistant.
- `sites/ultron-showcase/` is the old Ultron showcase. It is not in git (`sites/` is gitignored), so it exists only on the
  dev PC; delete the folder by hand when you no longer need it.
- `track.md` keeps logging every change.

## 15. Other features worth building

| Feature | Why | Where |
|---|---|---|
| **Wake word "Hey FRIDAY"** (on-device, opt-in, foreground service) | For a hands-free product this is core, not an extra. It also means a microphone foreground service that Play checks closely. | Pulled forward to Sprint 4.3 |
| **Default assistant role** (long-press power or home opens FRIDAY), already partly in `DefaultAssistantSection.kt` | Fastest way to start | Sprint 7.1 |
| **Quick Settings tile and a "Talk to FRIDAY" widget** | One tap from anywhere | Sprint 7.1 |
| **Command shortcuts:** "Good night" = Do Not Disturb on, brightness low, Wi-Fi off, run locally in fixed order | Predictable, and fixed scripts are the kind of automation the Accessibility policy allows | Sprint 7.2 |
| **Typed command bar** on home for quiet places | Already half there | Sprint 7.2 |

Dropped with the vision change: **Emergency SOS by SMS** (no SMS or contacts any more) and the "Good night" alarm. The
torch SOS blink stays.

## 16. Phases and sprints

### Sprint rules (every sprint)

- **Length:** about 1 week, shorter where the work is small.
- **Ends in a working app:** every sprint ends in a build testers can install. Unfinished work moves on; it is never
  merged half-built.
- **Flow:** one branch per sprint (`sprint/<n.n>-<name>`), one PR, merged only when all of these pass:
  - `./gradlew testDebugUnitTest lintDebug assembleDebug`
  - new or updated unit tests for the sprint's logic
  - a user-facing entry in `changelog.md` and a line in `track.md`
  - **your phone test** of the sprint's *Demo* steps
- **Delivery:** every merge to `main` sends a tester build through Firebase App Distribution (section 18).

### Overview

| Phase | Goal | Sprints | Status | Needs from you |
|---|---|---|---|---|
| **1. Foundation** | New package, new Firebase, tester delivery, dead weight removed | 1.1 – 1.3 | **Done** | none |
| **2. Identity** | Looks, sounds and introduces itself as FRIDAY by Nandivex | 2.1 – 2.3 | **Done** | none |
| **3. Hands-free control** | The tool set, the gate and the Abilities page | 3.1 – 3.5 done, 3.6 next | **Almost done** | phone tests |
| **4. Accounts and setup** | Onboarding, sign-in, free trial, wake word | 4.1 – 4.3 | To do | none |
| **5. Subscriptions** | Coupons, Razorpay, account page | 5.1 – 5.3 | To do | D3, Razorpay account and KYC |
| **6. Release** | Signed APK, updater and download site on our own website | 6.1 – 6.3 | To do | GoDaddy DNS records, Razorpay live keys |
| **7. Extras** | Faster ways to reach FRIDAY | 7.1 – 7.2 | To do | pick which ones |

---

### Phase 3: Hands-free control

**Sprints 3.1 to 3.5: done** (see section 2). Sprints 3.1 and 3.2 built connectors and calling that Sprint 3.4 then
removed; the plan keeps them in the history only.

**Sprint 3.6: Screen control you can trust** *(new)*
- Build:
  - A confirmation before irreversible screen actions: pressing Send in a chat, confirming a payment, deleting, or
    posting. FRIDAY asks "Send this to Rahul?" aloud and waits for a spoken yes. Detected from the label of the button
    it is about to tap and from the app (messaging, payment, banking apps), not from a tool name.
  - A test run of the everyday jobs that used to be tools, done through the screen: call a contact, text and WhatsApp,
    set an alarm in the Clock app, play a song, open a map route, take a note. Fix the weak spots in the prompt and the
    screen tools, and record which ones are reliable.
  - Unit tests for the confirmation rule (labels, apps, Hinglish yes and no).
- Demo: "Message Rahul on WhatsApp that I'm late" opens WhatsApp, types it, and asks before pressing Send. "Set an alarm
  for 6:30" works through the Clock app on your phone.
- Done when: the confirmation tests pass and the list of reliable jobs is written into the Abilities page intro.

### Phase 4: Accounts and setup

**Sprint 4.1: First-install setup and sign-in**
- Build: onboarding steps 1 to 6 and 8 (section 10), the `users/{uid}` profile, and new `firestore.rules` with emulator
  rules tests.
- Demo: a fresh install walks through setup in under 2 minutes. A bad Gemini key is caught before Next. Reinstalling and
  signing in brings your name and preferences back.
- Done when: the rules tests prove users can read only their own docs and cannot write entitlements.

**Sprint 4.2: 1-day free trial and access gate**
- Build: `startTrial` with the per-account and per-device check, the `entitlements/{uid}` listener, the gate in
  `MainActivity` (setup → plan → phone control → home), the trial countdown, and a plan screen with **Start free
  trial** only. The release activation-key gate is removed.
- Demo: start the trial and FRIDAY works for 24 hours (test with a 5-minute trial). After it ends the plan screen
  opens. Reinstalling or a second Google account on the same phone does not give a new trial.
- Done when: function tests cover the repeat-trial cases and the offline grace works (3 days).

**Sprint 4.3: "Hey FRIDAY" wake word** *(pulled forward)*
- Build: on-device wake word (opt-in), a foreground microphone service with a battery note in Settings, and a clear
  indicator when it is listening.
- Demo: with the screen off, "Hey FRIDAY, torch on" works from across the room.
- Done when: battery drain is measured over 8 hours and stated in Settings.

### Phase 5: Subscriptions

**Sprint 5.1: Coupons and `FRIDAYFREE`**
- Build: the `coupons` collection and `redeemCoupon` function, a coupon field on the plan screen and in onboarding,
  `FRIDAYFREE` with the developer email allowlist, and the developer-only coupon admin page.
- Demo: you enter `FRIDAYFREE` and get the **Developer** plan with no expiry. Another account gets "not valid for your
  account". The same code twice is refused.
- Done when: function tests cover the allowlist, redemption cap, once per account and disabled codes.

**Sprint 5.2: Razorpay subscriptions (test mode)**
- Build: `createSubscription` and `verifyPayment`, Razorpay Android Checkout, monthly and yearly plans (D3), secrets in
  Functions config.
- Demo: in Razorpay **test mode**, subscribe monthly with a test card or UPI. The entitlement turns active within
  seconds and FRIDAY unlocks.
- Done when: signature verification has unit tests and no secret is in the app or the repo.

**Sprint 5.3: Webhooks, renewals and the account page**
- Build: `razorpayWebhook` (activated / charged / halted / cancelled / completed), the account page with plan, renewal
  date and **Cancel subscription**, and percent- and flat-off coupons through Razorpay offers.
- Demo: a renewal in test mode extends the expiry. A failed charge marks it halted and the app asks to update payment.
  Cancel keeps access until the cycle ends.
- Done when: webhook tests cover every event and duplicate deliveries.

### Phase 6: Release from our own website (route B)

The site and the updater do not depend on accounts or payments. If you want testers on a public link sooner, Sprints 6.1
and 6.2 can run before Phase 4 or 5.

**Sprint 6.1: Release build and updater**
- Build:
  - Release signing with the existing key (`RELEASE_*` in `local.properties`, backed up in two places) and a `release`
    build that works without the debug or tester shortcuts. **Built.**
  - `isAccessibilityTool="false"`, a corrected service comment, drop `SYSTEM_ALERT_WINDOW`, add
    `REQUEST_INSTALL_PACKAGES` (release only). **Built.**
  - The in-app updater: reads `latest.json`, shows notes, downloads, checks SHA-256 and the signer, opens the installer.
    Unit tests for the manifest, host allow-list, version, Android minimum and rollout rules. **Built** (release build
    only; an About-page "Check for updates" button).
  - The "Allow restricted settings" help in the phone control gate. **Built as text steps**; pictures come with the website
    guide in Sprint 6.2.
- Demo: install the release APK from a file, hit the restricted-settings step and get past it with the in-app guide,
  turn on phone control, then install a newer build through the updater and keep all data.
- Done when: the release manifest matches section 13 (**checked**), and an update installs over the previous release on
  Android 10, 13 and 15 (**not tested yet: needs the site from Sprint 6.2 and your phones**).

**Sprint 6.2: The website and the release workflow**
- **Built:** the Firebase Hosting site (landing, Download, install guide, privacy, terms, 404; one stylesheet and one
  script, no outside resources, a strict Content Security Policy), `release.yml`
  (tag → tests → release APK → upload to Hosting → `latest.json` last), a rewritten `docs/CI_CD.md`, and removal of
  `sites/ultron-showcase/`.
- Demo: you push a version tag, and within minutes the site offers the new APK with its checksum, and an installed
  phone shows "Update available".
- Done when: a broken or older build can never replace a newer `latest.json` (**tested in `tests/ci`**), every APK is kept
  in GitHub Releases, and the setup in section 12 is done (**not yet**: it needs your Firebase login, GoDaddy and GitHub).

**Sprint 6.3: Go live and AWS teardown**
- Build: Razorpay **live** mode, Crashlytics dashboards and alerts, a phased rollout (`latest.json` can hold a
  percentage until it is 100), and, with your OK, a backup and then deletion of the AWS resources and the old
  `ultron-5350c` project. A notice goes to existing Ultron users.
- Done when: crash-free users are above 99% at full rollout and the AWS bill is ₹0.

### Phase 7: Extras (you pick)

**Sprint 7.1: Quick launch**: Quick Settings tile, home-screen widget and the default-assistant role. Demo: long-press
home and FRIDAY is listening in under a second.

**Sprint 7.2: Shortcuts and typed commands**: user-defined fixed routines ("Good night" = Do Not Disturb, low brightness,
Wi-Fi off), run locally without model planning, and a typed command bar on home.

## 17. Risks

| Risk | Mitigation |
|---|---|
| Play bans accessibility-driven assistants | Not on Play (D7). Never set `isAccessibilityTool` unless FRIDAY truly is one. |
| Sideloading puts users off, and Android 13+ greys out the accessibility switch until "Allow restricted settings" | A picture guide in the gate and on the site, tested on real phones in Sprints 4.1 and 6.1. |
| Losing the release signing key strands every user | Two offline backups, checked before the first public release. |
| The domain or its DNS lapses, and downloads and updates stop | Auto-renew the domain at GoDaddy, keep the `*.web.app` address as a backup, and let the app fall back to it for `latest.json`. |
| A bad update reaches everyone | `latest.json` is written last, with a rollout percentage and a checksum. |
| Prompt injection from text on screen makes FRIDAY press Send or pay | Untrusted-content marking now; confirmation before irreversible actions in Sprint 3.6. |
| App layouts change and the screen tools break | The Sprint 3.6 test list, and `look_at_screen` as a fallback. Report the reliable jobs honestly. |
| Marvel IP complaint | D4: no Marvel names or artwork, "inspired" persona. |
| Trial abuse | One trial per account **and** per device, enforced server-side. |
| `FRIDAYFREE` leaks | Email allowlist plus a redemption cap, server-side only. |
| Sideloaded updates are not trusted by users | Signed builds, a checksum on the download page, and Play later if D7 allows it. |
| Existing Ultron sideload users | They cannot upgrade to the new package. Announce it before the AWS teardown. |
| Gemini key limits or cost | The user brings their own key (onboarding step 4). Show a clear error when it runs out. |

## 18. Updates during development and testing

**Developer phones: adb or Gradle**
- `./gradlew installDebug` (or `adb install -r app-debug.apk`) over USB or wireless debugging.
- The debug build uses `applicationIdSuffix = ".debug"` and the name "FRIDAY Dev", so it sits next to a tester or release
  build on one phone.

**Testers: Firebase App Distribution** (free, in `friday-nandivex`)
- Testers are invited by email into the group `testers` and install **Firebase App Tester** once. Every new build then
  arrives as a notification with one-tap install.
- Builds are signed with one shared **tester signing key** (kept outside git, stored as a GitHub secret), so each build
  installs over the last and keeps the tester's data.
- **Manual upload:** `firebase appdistribution:distribute app/build/outputs/apk/<variant>/app-<variant>.apk --app
  <FIREBASE_APP_ID> --groups testers --release-notes "<notes>"`
- **Automatic:** `.github/workflows/distribute.yml` builds the tester variant on every merge to `main` that touches
  `android/**`, runs the unit tests and uploads with the top entry of `changelog.md` as release notes.
- **In-app notice:** the App Distribution SDK shows an "update available" prompt. It goes only in the tester build,
  never in a Play build.

**Build variants**

| Variant | Package | Signed with | Delivered by |
|---|---|---|---|
| `debug` | `com.nandivex.friday.debug` | debug key | adb / Gradle |
| `beta` | `com.nandivex.friday.tester` | tester key | Firebase App Distribution |
| `release` | `com.nandivex.friday` | release key | Our website (D7, route B) |
