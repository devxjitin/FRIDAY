# FRIDAY: rebuild plan (Ultron → FRIDAY)

Owner: Nandivex Technologies · Plan date: 2026-11-13 · Updated: 2026-11-16 · Status: **Phases 1 and 2 done, Phase 3 done
(version 0.0.19), Phases 4 to 7 still to build**

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
3. **Distribution is an open decision (D7).** Google Play restricts the Accessibility API hard, so how FRIDAY is
   distributed decides its permissions, payments and updates.
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

**Not built yet:** Firebase sign-in and Firestore rules, onboarding, free trial, coupons, Razorpay, Play or website
release, wake word. The activation-key screen from the Ultron days still guards release builds; tester and debug builds
skip it.

---

## 3. Decisions I need from you

| # | Question | My recommendation |
|---|---|---|
| D1 | **Payments.** If FRIDAY is on Play, digital subscriptions must use Google Play Billing. In India, Razorpay can sit next to it only through Google's **User Choice Billing**. Off Play (D7, route B) there is no such rule and Razorpay alone is fine. | Decide after D7. Route A: User Choice Billing. Route B: Razorpay only. |
| D2 | Package name. | **Done:** `com.nandivex.friday`. |
| D3 | **Plan prices** (monthly / yearly, INR). | You decide. The plan supports both. |
| D4 | **Name and IP.** F.R.I.D.A.Y. is a Marvel character. Marvel names, quotes and film artwork risk a takedown. | App name "FRIDAY – Voice Assistant", personality *inspired by* the film AI, no Marvel names or logos. |
| D5 | Knowledge base, YouTube, Images page. | **Done:** all removed. |
| D6 | Firebase account and project. | **Done:** project `friday-nandivex`. |
| **D7** | **Distribution route.** On 2026-11-16 I read Google's Accessibility API policy page. It says that an app that is not an accessibility tool may not use the API "to autonomously initiate, plan, and execute actions", and that only services whose main purpose is helping people with disabilities may set `isAccessibilityTool`. A general voice assistant does not qualify. FRIDAY's tap-and-type loop is exactly that kind of autonomy. | See section 12 for the three routes. I recommend **route B first** (website and Firebase App Distribution, Razorpay only), and trying route A only if you are willing to run FRIDAY as a real accessibility tool for people with disabilities. Please read the Play page yourself before you commit: this is from my one reading of it. |

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
- Housekeeping: the comment in `accessibility_service_config.xml` still says screen tools are "switched on in Settings"
  and the file sets `isAccessibilityTool="true"`. Fix both together with D7: the flag must match what we declare to Google.

## 8. Theme and logo (done in Sprints 2.2 and 2.3)

FRIDAY HUD palette (deep navy, cyan, amber) with contrast tests, adaptive and monochrome icons, splash screen, animated
HUD ring on the voice screen. Still to do for any store listing: the 512 px icon and the 1024×500 feature graphic.

## 9. Firebase (project `friday-nandivex`)

Done in Sprint 1.1: project, Android app, `google-services.json` (gitignored), App Distribution, Crashlytics.

Still to do (Phase 4 and 5):
1. **Authentication** (Google sign-in, phone OTP optional), **Firestore**, **Cloud Functions** (Node 22, `asia-south1`),
   **App Check** (Play Integrity if on Play, otherwise device attestation or none).
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
  expired), `expiresAt`, `source` (razorpay | play | coupon | trial), `subscriptionId`.
- Razorpay `key_secret` and the webhook secret live in Functions secrets. Only the public `key_id` is in the app.
- Cached status with an offline grace period (3 days), read from Firestore's offline cache.
- Account page: plan, renewal date, **Cancel subscription** (cancel at cycle end).
- Replaces the Ultron-era `License.kt` (`ULTR-…` keys), `AccessViewModel` and `ActivationScreen`.
- You do the Razorpay merchant account, KYC and the Plans in the Razorpay dashboard. I add the plan IDs.
- **Play Billing** (route A only): the Play Billing Library plus a `verifyPlayPurchase` function writing the same doc.

**Coupons** (`FRIDAYFREE` = developer code):
- Live in `coupons/{code}`, written only by Cloud Functions. Redeemed through `redeemCoupon(code)`. The code is never in
  the app, because anything in the APK can be extracted.
- `FRIDAYFREE` = `{ type: "developer", grants: "developer" plan, no expiry, allowedEmails: [<Nandivex developer
  emails>], maxRedemptions: 10 }`. Anyone else gets "This code is not valid for your account."
- Later types: `percent_off` / `flat_off` (as a Razorpay offer) and `free_days`.
- One redemption per account, recorded in `coupons/{code}/redemptions/{uid}`. A developer-only admin page under Settings
  → Developer creates, disables and lists coupons.

**1-day free trial:** `startTrial()` writes `entitlements/{uid} = { plan: "trial", expiresAt: now + 24h }`. Once per
account and once per device (`trials/{deviceHash}`: hash of the device attestation plus `ANDROID_ID` and the signing
hash), so reinstalling or making a new Google account on the same phone does not restart it. A countdown shows on home;
the plan screen opens when it ends. The trial is app-managed: Play's own free trials are longer than a day.

## 12. Distribution routes (decision D7)

Google's Accessibility API policy (read on 2026-11-16; check the live page again before any submission):
- An app that is **not** an accessibility tool must not use the API to autonomously initiate, plan and execute actions.
  Fixed, human-defined scripts are not covered by that rule. FRIDAY's model decides what to tap next, so it is covered.
- `isAccessibilityTool="true"` is for services whose **main purpose** is helping people with disabilities. A general
  voice assistant that also helps some of them does not qualify.
- A tool that does qualify may use autonomy for that core purpose. It must describe the feature, the disabilities
  served and the target users, and send a video. Non-tools need a prominent disclosure and a declaration video.
- Misdeclaring is grounds for suspension of the developer account.

| Route | What it means | Cost and risk |
|---|---|---|
| **A. Google Play as an accessibility tool** | Position FRIDAY honestly as hands-free voice control for people who cannot use a touchscreen (like Voice Access). The store listing, onboarding and declaration all say so. | Highest review risk. The disability use must be the true core purpose, not a label. If rejected, resubmitting costs weeks. Needs Play Billing next to Razorpay (D1). |
| **B. Own website and Firebase App Distribution (recommended first)** | Signed APK on the Nandivex site and App Distribution. Updates go through our own updater. Razorpay only, no Play fee. Sideloading needs "install unknown apps" and a clear explanation. | No store review of the Accessibility API. Smaller reach, and no Play trust badge. Can add other stores later (Samsung Galaxy Store, Amazon Appstore), which have their own rules to read. |
| **C. Play with a reduced FRIDAY** | A Play build without autonomous screen control: only fixed-script tools (torch, volume, brightness, panels) and the system's own gestures. | Safe on Play, but it is not the product vision. Treat it as a fallback only. |

Whatever the route, the in-app disclosure (section 7) stays, and the sideload build keeps its own updater, so the plan's
old "no self-update on Play" step only applies to routes A and C.

## 13. Permissions today (manifest of version 0.0.19)

| Permission or service | Why | Play note |
|---|---|---|
| `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, foreground service (`microphone`, `specialUse`) | Voice session | Foreground service types need a use description in Play Console |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Gemini and network status | Normal |
| `POST_NOTIFICATIONS`, `VIBRATE` | Service notification, vibrate tool | Normal |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Android only shows the Wi-Fi name with location access | Foreground only. Disclose in Data safety |
| `BLUETOOTH` (to Android 11), `BLUETOOTH_CONNECT` | Bluetooth status and paired devices | Normal |
| `ACCESS_NOTIFICATION_POLICY` | Do Not Disturb | Special access, user-granted |
| `WRITE_SETTINGS` | Brightness, screen timeout, auto-rotate | Special access, user-granted |
| `SYSTEM_ALERT_WINDOW` | Declared for starting screens from the background, but no code uses it (checked 2026-11-16) | Drop it in Sprint 6.1: Play asks why every sensitive permission is there |
| **Accessibility service** | The whole product | See section 12 |
| Voice interaction and recognition services | Default assistant role (hold home or power) | Normal |

Not requested, and to keep it that way: `ACCESS_BACKGROUND_LOCATION`, `QUERY_ALL_PACKAGES` (the `<queries>` block lists
launcher apps instead), `REQUEST_INSTALL_PACKAGES` (only needed if route B ships its own updater: then declare it).

## 14. Rebrand and housekeeping

Done in Sprint 2.3 (README, strings, channels, help page). Still open:
- `git grep -i ultron` should only hit `track.md` history and `docs/ultron-changelog.md`. Check `docs/`, and delete the
  empty folders left under `mcp/` on the dev PC (git does not track them).
- Update `docs/PRIVACY_PLAN.md` and the privacy policy: the app no longer touches contacts, calls, files or location
  beyond the Wi-Fi name, and screen text goes to Gemini.
- Change `docs/Idea.md` and `README.md` to describe a hands-free phone control assistant.
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
| **6. Release** | Policy-compliant release on the route you choose | 6.1 – 6.3 | To do | **D7**, D1 |
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

### Phase 6: Release (route chosen in D7)

**Sprint 6.1: Release readiness (all routes)**
- Build:
  - Re-read Google's Accessibility API policy and settle D7. Make `isAccessibilityTool`, the declaration, the store text,
    the in-app disclosure and the privacy policy all say the same true thing.
  - Drop `SYSTEM_ALERT_WINDOW` (nothing uses it). Privacy policy and terms pages on the Nandivex site.
  - Own signing key for release (`RELEASE_*` in `local.properties`, kept out of git).
  - Updater: route B ships its own update check against the Nandivex site; routes A and C use Play In-App Updates.
- Demo: a release build installs cleanly, shows the disclosure before the Android settings screen, and passes lint with
  no policy warnings.
- Done when: the release manifest matches section 13 and nothing is declared that the app does not use.

**Sprint 6.2: Publish**
- Route B: signed APK on the Nandivex site, download page with a sideloading guide, App Distribution for testers,
  Razorpay live mode.
- Route A or C: register the Play Console **organization** account (you, with the D-U-N-S number), Play App Signing,
  store listing and Data safety form, the Accessibility declaration with video, User Choice Billing (D1) with a
  `verifyPlayPurchase` function, then internal and closed testing.
- Demo: testers install FRIDAY from the chosen channel and subscribe.
- Done when: the chosen channel accepts the build, or we move to another route and resubmit.

**Sprint 6.3: Production and AWS teardown**
- Build: staged rollout (10% → 50% → 100% on Play, or a phased link on the site), Crashlytics dashboards and alerts, and
  with your OK a backup and then deletion of the AWS resources and the old `ultron-5350c` project. A notice goes to
  existing Ultron users.
- Done when: crash-free users are above 99% at 100% rollout and the AWS bill is ₹0.

### Phase 7: Extras (you pick)

**Sprint 7.1: Quick launch**: Quick Settings tile, home-screen widget and the default-assistant role. Demo: long-press
home and FRIDAY is listening in under a second.

**Sprint 7.2: Shortcuts and typed commands**: user-defined fixed routines ("Good night" = Do Not Disturb, low brightness,
Wi-Fi off), run locally without model planning, and a typed command bar on home.

## 17. Risks

| Risk | Mitigation |
|---|---|
| **Play rejects the Accessibility use, or suspends the account for a misdeclaration** | D7: ship on route B first, and only declare what is true. Never set `isAccessibilityTool` unless FRIDAY really is one. |
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
| `release` | `com.nandivex.friday` | release key | Website or Play Store, depending on D7 |
