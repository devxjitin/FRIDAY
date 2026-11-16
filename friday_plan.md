# FRIDAY: rebuild plan (Ultron → FRIDAY)

Owner: Nandivex Technologies · Plan date: 2026-11-13 · Status: **plan only, nothing implemented yet**

FRIDAY is a voice assistant for **short phone commands**: call someone, send an SMS, turn on the flashlight, set an alarm,
switch on Bluetooth. It is not meant for long multi-step tasks. Every decision below follows from three facts:

1. **Short commands, fast replies.** Anything that adds latency or long-task machinery gets removed.
2. **Play Store distribution.** Google Play policy now decides which permissions, payment flows and update paths we can use.
3. **Firebase only, no AWS.** One backend that you own, under a new Firebase account.

---

## Vision change (2026-11-16): hands-free phone control

FRIDAY is now a **hands-free phone control AI**: you speak, it operates the phone. This overrides the parts of the plan
below that talk about calls, messages, personal data or connectors.

- **No connectors.** Every phone-control tool is built in and always on. The Connectors page, the per-connector
  switches and the "Connect all essentials" button are gone.
- **Phone control permission on first open.** The app shows a disclosure screen and sends the user to turn on the
  FRIDAY accessibility service. The home screen opens only once it is on.
- **Removed:** calls (make/answer/end/reject, speaker, mute, dialer), SMS, WhatsApp, contacts, calendar, location and
  directions, notifications (read/reply/dismiss/announce, now playing), files, app usage, share text/file, weather and
  web search/web page reading. With them go the spoken-yes rule (ActionGate) and the SMS rate limit, since no tool
  reaches other people any more.
- **Kept, always on:** torch and SOS, sound and Do Not Disturb, display, media keys, Wi-Fi / Bluetooth / mobile /
  airplane / hotspot / NFC panels, alarms, timers, reminders, open apps and settings, camera, clipboard, device status,
  navigation (back, home, recents, scroll, lock, screenshot) and screen control (read, tap, type, swipe).
- **Play Store:** the permissions this removes (`SEND_SMS`, `CALL_PHONE`, `ANSWER_PHONE_CALLS`, `READ_CONTACTS`,
  calendar, `MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_*`, `PACKAGE_USAGE_STATS`, the notification listener) were the
  riskiest ones. The main remaining review item is the **Accessibility API**: Play does not allow non-accessibility
  apps to use it to act on their own, so FRIDAY must be presented as a hands-free control tool (like Voice Access), with
  the prominent disclosure, the Play Accessibility declaration and a demo video. Check the current policy text before
  Sprint 6.2.
- Sprints 3.2 and 3.3 work on calls/SMS is replaced by Sprint 3.4 "Hands-free control" (this change). The old 3.4
  items that still fit (stopwatch, play music, find my phone) move to 3.5.

## 0. Decisions I need from you before coding

| # | Question | My recommendation |
|---|---|---|
| D1 | **Payments on Play Store.** Play requires Google Play Billing for digital subscriptions. In India you can offer Razorpay only through Google's **User Choice Billing** (alternative billing) programme, which shows Play Billing next to Razorpay and still charges a reduced service fee. Razorpay-only checkout inside a Play app will be rejected. | Enrol in User Choice Billing. Razorpay is the main option, Play Billing is the required second choice. Both write the same entitlement in Firestore. |
| D2 | **Package name.** It is `com.assistant.app` today and **cannot be changed after the first Play upload.** | Change it to `com.nandivex.friday` now. |
| D3 | **Plan prices** (monthly / yearly, INR). | You decide. The plan supports monthly and yearly. |
| D4 | **Name and IP.** F.R.I.D.A.Y. is a Marvel character. Using "Marvel", "Tony Stark", "Iron Man" or film artwork in the app or on the store listing risks a takedown. | App name "FRIDAY – Voice Assistant". Personality *inspired by* the film AI, with no Marvel names, quotes or logos. |
| D5 | **Knowledge base (PDF/RAG), YouTube, Images page.** These don't fit a short-command assistant. YouTube downloading also breaks YouTube's terms, and Play rejects apps that do it. | Remove all three (see §6). Keep Memory. |
| D6 | **Which Google account / Firebase project name.** | You run `firebase login` on this PC. I create project `friday-nandivex` (or a name you choose). |

---

## 1. Personality: Ultron → F.R.I.D.A.Y.

**Files:** `data/Personalities.kt`, `data/AppSettingsStore.kt` (holds `DEFAULT_SYSTEM_PROMPT`, the "You are Ultron…" prompt), `data/KnowledgeBaseProvider.kt` (shared safety and tool policy), `data/Greetings.kt`, `ui/components/SettingsDialog.kt` (personality picker).

- Replace the four personalities (Destructive, Friendly, Professional, Funny) with **one FRIDAY persona**:
  - Calm, warm, quick and quietly witty. Speaks like a capable operations AI: "On it, Boss." / "Done. Torch is on."
  - Calls the user by the form of address they pick at setup ("Boss" by default, or their name).
  - Confirms an action in **one short sentence**. Explains only when asked.
  - Composed in bad news or emergencies. Dry humour only when the moment allows it.
  - Female voice by default (Gemini voice **Kore** or **Aoede**; test both on the phone). An Irish accent can be asked for in the prompt, but Gemini voices can't promise it.
- Remove the `set_personality` tool and the personality picker. Keep one optional setting: **Reply style: Short (default) / Detailed**.
- Rewrite `Greetings.kt` in FRIDAY's voice.
- Keep the existing TONE_RULES and the "other people" privacy rules. Keep the speech-language rules (Hinglish etc.) but make them gender-neutral or female to match the voice.
- **Creator line:** "FRIDAY is developed by Nandivex Technologies." Nothing else (see §3).

## 2. Remove Skills

Skills are long scripted multi-step flows (the WhatsApp skill alone has 16 task files). They don't fit short commands.

**Delete:** `assets/skills/` (whole folder), `data/Skill.kt`, `ui/components/SkillsPage.kt`, `ui/components/SkillIcons.kt`, `decisions/SkillMatcher.kt`, the skill tool declarations in `network/ToolDeclarations.kt`, skill dispatch in `viewmodel/ToolRunner.kt`, the skill state and toggles in `AssistantViewModel.kt` and `AppSettingsStore.kt`, the "Skills" drawer item in `AppDrawer.kt`, the skill-related parts of `RequestForm.kt`, and the matching unit tests.

**Replacement for common WhatsApp asks:** single-shot tools, not skills. `whatsapp_message(contact, text)` opens `wa.me/<number>?text=` with the message filled in. The user taps Send, or the screen-control connector taps it when that connector is on.

## 3. Developer: Nandivex Technologies only

- `AboutPage.kt`: remove the "Founder / Co-founder / Co-builder" lines and show **"Developed by Nandivex Technologies"** with the company website and support email.
- `Personalities.kt` creator line, as in §1.
- README "Author" section, Play listing developer name, privacy policy and terms all say **Nandivex Technologies**.
- Remove personal names and emails from user-facing strings (`git grep -i "chandra\|sonia\|jitin"`).
- The in-app **Developer** menu (manual key entry, latency log) is shown only in debug builds or for developer-coupon accounts (§11).

## 4. Remove the AWS cloud (MCP servers and everything else on AWS)

Today the app calls `api.devxjitin.dev` for web search, web extract, YouTube search, licences, OTA releases and feature requests. Everything on AWS goes.

| AWS piece | Replacement |
|---|---|
| `/v1/search/mcp` (web search) | **Gemini Live's built-in Google Search grounding** (`google_search` tool in the Live setup). No server, and better answers. Keep the on-phone DuckDuckGo client as a fallback, or drop it. |
| `/v1/web/mcp` (web extract) | Read pages on the phone (OkHttp + Jsoup readability). Or drop it: short commands rarely need it. |
| `/v1/youtube/mcp` (YouTube search) | Remove. "Play X on YouTube" becomes an intent: `youtube.com/results?search_query=` or `MEDIA_PLAY_FROM_SEARCH`. |
| Licence Lambdas + DynamoDB `ultron-licenses` | Firebase subscription backend (§9). |
| OTA releases (Lambda + S3) | **Now (testing):** Firebase App Distribution (§18). **After the Play launch:** Play In-App Updates API. Play forbids self-updating APKs, so `REQUEST_INSTALL_PACKAGES` must go. |
| Feature requests Lambda + Resend mail | A Firestore `feedback` collection (signed-in users can write only), or a `mailto:` link. |

**Repo deletions:** `mcp/`, `infra/`, `scripts/publish-update.ps1`, `scripts/decisions_probe.py`, `.github/workflows/publish-ota.yml`, `network/ApiClient.kt`, `McpClient.kt`, `WebSearchClient.kt`, `WebExtractClient.kt`, `YouTubeSearchClient.kt`, `AppUpdater.kt`, `LicenseClient.kt`, `FeatureRequestClient.kt`, `viewmodel/UpdateManager.kt`, `ultronapp/` (the old licence admin site), the `API_BASE_URL` / `STORE_URL` build fields, and the matching tests.

**AWS account teardown.** This happens only after you say yes, and after a backup of DynamoDB `ultron-licenses` and the S3 bucket. Remove stacks `ultron-api` and `ultron-youtube`, Lambdas `ultron-api-*`, the S3 bucket `ultron-app-updates-*`, the custom domain and API Gateway, and the CloudWatch log groups. Don't touch `future-self-booth-api` or `/age-prediction/hf-token`: they belong to other projects.

## 5. New Firebase account

You run `firebase login` on this PC. I then do the rest with the Firebase CLI:

1. `firebase projects:create friday-nandivex`. Upgrade it to the **Blaze** plan (Cloud Functions and outbound calls to Razorpay need it).
2. `firebase apps:create android --package-name com.nandivex.friday`, then `firebase apps:sdkconfig` to write `android/app/google-services.json` (it stays gitignored).
3. Turn on **Authentication** (Google sign-in plus phone OTP as an option), **Firestore**, **Cloud Functions** (Node 22, region `asia-south1`), **App Check** (Play Integrity), **Crashlytics** and **Analytics**.
4. Add the debug and release **SHA-1/SHA-256** fingerprints. Google sign-in fails without them.
5. Replace `.firebaserc` (`ultron-5350c` → new project) and `firebase.json` (drop the `ultronapp-admin` hosting site; add `functions` and the Firestore indexes).
6. New `firestore.rules`: users can read only their own `entitlements/{uid}` and `users/{uid}`. Clients can't write entitlements, coupons or trials; only Cloud Functions can.
7. Turn on **App Distribution** and create a tester group `testers` (see §18).
8. Leave the old `ultron-5350c` project alone until the new one is live, then delete it with your OK.

## 6. Remove Decisions API, and other cuts

**Decisions API (`gemini-decision-1`):** remove the whole `decisions/` package: `DecisionsClient`, `DecisionsModels`, `ActionChecker`, `ScreenChecker`, `SkillMatcher`, `MemoryTidier`, `ChunkReranker`. Also remove the five `decisions*Enabled` settings and toggles, `docs/decisions-testset/` and the tests.
Why: every check adds a network round trip (up to 10 s timeout) in front of a short command, and the skills and reranking it served are being removed. The one useful job it did was blocking SMS or calls triggered by injected text. That moves to a **local, zero-latency rule** (§7, *Safety*).

**Other cuts (pending D5):**
- Knowledge base / RAG: `KnowledgeManager`, `RagDocumentStore`, `RagSearchEngine`, `KnowledgeBaseProvider` RAG parts, `GeminiEmbeddingClient`, ObjectBox (`objectbox-models/`, plugin). This makes the app much smaller and faster to start.
- YouTube Search + YouTube Downloads (`YouTubeDownloader.kt`, `YouTubeDownloadsMcp.kt`, NewPipe Extractor, `DownloadsPage.kt`).
- Images page (`ImagesPage.kt`, `ImageLibrary.kt`). Photos can still be opened through Files.
- Conversation-history search tools (`SEARCH_CONVERSATION_HISTORY`, `RECENT_CONVERSATION`). Keep the History page for the user to read.
- `docs/archive/`, `sites/reference-analysis/`, `Product-Manager/PM.md` (Ultron-era), old screenshots. A new PM file gets written after the rebuild.

**Kept:** Gemini Live voice, barge-in, 13 languages, Memory (150 notes), Usage page, Storage page, History, foreground service, alarms/timers/reminders engine, accessibility service.

## 7. Connectors, rebuilt and categorised

Rules stay the same: `ConnectorRegistry.kt` is the single source, and each tool belongs to exactly one connector. The categories change, and new direct tools are added (**new** = to be built).

| Category | Connector | Tools | Default |
|---|---|---|---|
| **Calls & messages** | Phone calls | `make_call` (by name or number), **new** `answer_call`, `end_call`, `reject_call`, `speakerphone`, `mute_call`, `dial_number` (opens dialer) | Off |
| | SMS | **new** `send_sms` (see Play note), `open_sms_draft` | Off |
| | WhatsApp | **new** `whatsapp_message`, `whatsapp_call` (opens the chat) | Off |
| | Contacts | `search_contact`, `contact_birthday`, `create_contact` | Off |
| **Connectivity** | Wi-Fi | `wifi_info`, **new** `wifi_toggle` (system panel on Android 10+) | On |
| | Bluetooth | `bluetooth` info, **new** `bluetooth_toggle`, `list_paired_devices` | On |
| | Mobile & network | `network`, **new** `mobile_data_panel`, `airplane_mode` (via quick settings when screen control is on), `hotspot_settings`, `nfc_settings` | On |
| **Device controls** | Torch | `flashlight` (on/off), **new** `flashlight_strobe` / SOS blink | On |
| | Sound | `volume`, `get_volume`, `get_ringer`, `set_ringer`, `vibrate`, `set_dnd` | On |
| | Display | `brightness`, `get_brightness`, `screen_timeout`, `auto_rotate`, **new** `dark_mode` (settings), `battery_saver` (panel) | On |
| | Media | `media_control`, `now_playing`, **new** `play_music` (search intent to Spotify / YT Music / default player) | On |
| **Clock** | Alarms & timers | `set/list/cancel/modify_alarm`, `set/list/cancel_timer`, **new** `stopwatch` | On |
| | Reminders | `schedule/list/cancel/modify_reminder` | On |
| **Apps** | Apps | `open_app`, `list_apps`, `open_app_settings`, `open_settings`, `open_url`, `share_text` | On |
| | Camera | `open_camera`, **new** `take_selfie` / `record_video` (intents) | On |
| | Clipboard & notes | `get_clipboard`, `set_clipboard`, **new** `quick_note` (saves to the app) | On |
| | App usage | `most_used_apps`, `app_usage` | Off |
| **Info** | Device status | `time`, `battery`, `device_info`, `storage_memory`, `steps`, `light` | On |
| | Weather | `get_weather` | On |
| | Web | Google Search grounding (built into the model), `fetch_webpage` (on phone) | On |
| **Personal** | Calendar | `upcoming_events`, `calendar_conflicts`, `create_event`, `cancel_event` | Off |
| | Location | `location`, `directions`, **new** `share_location` (send a maps link by SMS/WhatsApp) | Off |
| | Notifications | `read/reply/dismiss_notification` | Off |
| | Files | `search_files`, `open_file`, `list_folder`, `share_file` (MediaStore, see Play note) | Off |
| **Phone control (advanced)** | Navigation | `press_back`, `home`, `recents`, `previous_app`, shades, `scroll`, `lock_screen`, `screenshot`, `power_menu`, `split_screen` | Off |
| | Screen control | `read_screen`, `look_at_screen`, `current_app`, `click_text`, `click_item`, `tap_position`, `type_text`, `press_enter`, `wait_for_text`, `swipe`, `long_press` | Off |
| **Safety** | Find my phone | **new** `ring_phone` (full volume for 30 s, even on silent) | On |

**Platform limits the plan accounts for:**
- Android 10+ apps can't switch Wi-Fi, mobile data or airplane mode directly. FRIDAY opens the system **Settings panel**, and taps the toggle itself when Screen control is on. Bluetooth on Android 13+ also needs the system dialog or a panel.
- `answer_call` / `end_call` need `ANSWER_PHONE_CALLS` (allowed on Play). Call-log features ("redial last caller") need the restricted `READ_CALL_LOG`, so they are **not** in scope.

**Safety (replaces ActionChecker):** while text from outside (an SMS, notification, web page or screen read) is in the current turn, `send_sms`, `make_call`, `whatsapp_message` and `share_*` need a **spoken "yes"** before they run. Keep the SMS rate limit (10 per hour). Zero extra network calls.

**UI:** the Connectors page groups cards by the categories above, with a short line under each heading. Each connector page lists its tools with a one-line explanation. One **Connect all essentials** button.

## 8. New theme and logo

**Theme** (`ui/theme/Color.kt`, `Theme.kt`): replace Ultron crimson with a FRIDAY HUD palette, keeping the existing light/dark structure and contrast checks.

| Token | Dark | Light |
|---|---|---|
| background | `#0A0F1A` deep navy | `#F6F8FB` |
| surface | `#111827` | `#EBEFF5` |
| accent (text, outlines) | `#3DD9EB` cyan | `#0E7C8C` |
| accentFill (white text on it) | `#0E7C8C` | `#0E7C8C` |
| accentAlt | `#F5A524` amber (HUD warning, highlights) | `#B45309` |
| error | `#F87171` | `#B42318` |

Every text pair is re-checked for ≥ 4.5:1 contrast. The voice ribbon and mic button get a cyan glow and pulse that react to voice level.

**Logo, designed with canvas:**
- Concept: a **circular HUD ring** (three broken arc segments, like a targeting reticle) around a **stylised "F"** whose middle bar is a short audio waveform. Cyan on deep navy, with an amber dot at one ring gap.
- I draw it as an HTML `<canvas>` design you can preview and tweak (colours, ring gaps, F weight). The final shape is then exported as:
  - an Android adaptive icon (`ic_launcher_foreground.xml` vector, `ic_launcher_background.xml`, `ic_launcher_monochrome.xml` for themed icons),
  - a splash icon (`ic_splash.xml`),
  - the Play Store 512×512 PNG and a 1024×500 feature graphic.
- The same ring is reused as an animated Compose `Canvas` on the home screen and the voice screen.

## 9. Razorpay subscriptions (Play Store)

**Architecture, all on Firebase:**

```
App (Firebase Auth) ──callable──▶ Cloud Function createSubscription(planId)
                                     └─▶ Razorpay Subscriptions API → subscription_id
App opens Razorpay Checkout (Android SDK) with subscription_id
App ──callable──▶ verifyPayment(payment_id, subscription_id, signature)  → HMAC check → entitlements/{uid}
Razorpay ──webhook──▶ Cloud Function razorpayWebhook (signature check)
                        subscription.activated / charged / halted / cancelled / completed → entitlements/{uid}
App listens to entitlements/{uid} (read-only for the user)
```

- **Entitlement doc** `entitlements/{uid}`: `plan` (trial | monthly | yearly | developer), `status` (active | halted | cancelled | expired), `expiresAt`, `source` (razorpay | play | coupon | trial), `subscriptionId`.
- **Secrets:** the Razorpay `key_secret` and webhook secret live in Cloud Functions secrets (`firebase functions:secrets:set`). Only the public `key_id` goes in the app.
- **Play Billing** (needed under D1): the Play Billing Library plus a `verifyPlayPurchase` function (Google Play Developer API) writes the same entitlement doc. The app shows both options on the subscription screen, as User Choice Billing requires.
- **Offline:** keep the current idea of a cached status with an offline grace period (`License.kt`, 7 days → 3 days), now read from Firestore's offline cache.
- **Manage / cancel:** the Account page shows the plan, renewal date and a **Cancel subscription** button (Cloud Function → Razorpay cancel at cycle end).
- **Replaces:** `License.kt` key format (`ULTR-…`), `AccessViewModel` activation-key flow, `ActivationScreen`, `ultronapp/` admin site.
- Razorpay merchant account, KYC and creating the monthly/yearly **Plans** in the Razorpay dashboard are steps **you** do (payment provider sign-up). I add the plan IDs to config.

## 10. First-install setup screen

A new onboarding flow (`ui/onboarding/`). It is shown until finished, and each step can be revisited later in Settings.

1. **Welcome:** animated FRIDAY logo, one line ("Your phone, on voice command"), language choice.
2. **Your name:** name, plus how FRIDAY should address you ("Boss", name, Sir/Ma'am).
3. **Sign in:** Google sign-in (Firebase Auth). Needed so the trial and subscription follow the user, not the phone.
4. **Gemini API key:** paste field, an "Get a free key" button that opens Google AI Studio, and a **live check** (one tiny Gemini call) before Next is enabled. The key is stored encrypted (`ApiKeyStore`, unchanged).
5. **Voice:** pick a voice with a 2-second sample button; female default.
6. **Essential permissions:** microphone and notifications, explained in one line each. Everything else is asked only when a connector is turned on.
7. **Plan:** start the **1-day free trial**, subscribe (monthly / yearly), or enter a **coupon** (§11).
8. **Done:** "Say *Hey FRIDAY, turn on the torch*" style tips, then home.

`MainActivity` routes: no onboarding done → onboarding; done but no active entitlement → plan screen; otherwise → home.

## 11. Coupons (developer code `FRIDAYFREE`)

- Coupons live in Firestore `coupons/{code}` and are writable only by Cloud Functions or the admin. Redeeming goes through a callable function `redeemCoupon(code)`.
- **The code is never stored in the app.** A code hard-coded in the APK can be pulled out by anyone who decompiles it.
- `FRIDAYFREE` = `{ type: "developer", grants: "developer" plan, no expiry, allowedEmails: [<Nandivex developer emails>], maxRedemptions: 10 }`. If anyone else enters it, they get "This code is not valid for your account."
- General coupon types for later: `percent_off` / `flat_off` (applied as a Razorpay **offer_id** on checkout) and `free_days` (adds days to the entitlement).
- A coupon field is shown on the subscription screen and in onboarding step 7. Each redemption is recorded in `coupons/{code}/redemptions/{uid}`, so each account can use a code only once.
- Admin: a small callable function, plus a page under Settings → Developer (visible to developer accounts only) to create, disable and list coupons. This replaces the old `ultronapp` admin site.

## 12. 1-day free trial

- Callable function `startTrial()` → `entitlements/{uid} = { plan: "trial", expiresAt: now + 24h }`.
- **Once per account and once per device.** The function stores a hash of the Play Integrity device verdict and a `ANDROID_ID`+app-signing hash in `trials/{deviceHash}`, so uninstalling and reinstalling, or making a new Google account on the same phone, does not restart the trial.
- The app shows a countdown on the home screen during the trial and opens the plan screen when it ends.
- Note: Google Play Billing's own free trials have a minimum length longer than one day, so the 1-day trial is **app-managed** through Firebase, not a Play/Razorpay trial.

## 13. Google Play readiness (needed for any store release)

| Item | Action |
|---|---|
| `REQUEST_INSTALL_PACKAGES` | Remove (no self-update on Play). Use Play In-App Updates. |
| `MANAGE_EXTERNAL_STORAGE` | Remove. Play allows it only for file managers and similar. Files connector uses MediaStore and the `READ_MEDIA_*` permissions. |
| `SEND_SMS` | A restricted Play permission, allowed only for default SMS apps or approved exceptions. **Plan A:** file the Permissions Declaration (assistant / device automation use). **Plan B** if rejected: `send_sms` opens the SMS app with the message filled in and Screen control taps Send. |
| `QUERY_ALL_PACKAGES` (open any app by name) | Declaration form, or `<queries>` for launcher intents (preferred). |
| `USE_EXACT_ALARM` | Allowed for apps whose core function includes alarms. Declare it, with `SCHEDULE_EXACT_ALARM` as the fallback. |
| Accessibility service | Prominent in-app disclosure plus the Play Accessibility declaration and a demo video. Keep it off by default. |
| `ACCESS_BACKGROUND_LOCATION` | Not used, keep it that way. |
| Release signing | Play App Signing with a new upload key (`RELEASE_*` in `local.properties`). Debug-key OTA ends. |
| Store assets | Privacy policy and terms URL (Nandivex site), Data safety form, content rating, screenshots, the 512 px icon and feature graphic (§8). |
| `targetSdk` | Raise from 35 to the level Play requires at upload time (36). |

## 14. Rebrand sweep

- About 550 "Ultron" mentions in 113 files: app name, `strings.xml`, notification channels, `Downloads/Ultron`, prompts, help page, changelog, README, tests. The changelog restarted at **FRIDAY 0.0.1** in Sprint 1.1 (old history is in `docs/ultron-changelog.md`).
- Package / namespace `com.assistant.app` → `com.nandivex.friday` (D2), moved with an IDE-style refactor of the source folders.
- New README describing FRIDAY, the Firebase setup and the Play build. `track.md` keeps logging every change.

## 15. Other features I recommend

| Feature | Why |
|---|---|
| **Wake word "Hey FRIDAY"** (on-device, e.g. Porcupine or openWakeWord, opt-in, foreground service) | Hands-free short commands are the whole point. Battery cost is noted in Settings. |
| **Default assistant role** (`ROLE_ASSISTANT`, long-press power/home opens FRIDAY), already partly in `DefaultAssistantSection.kt` | Fastest way to launch. |
| **Quick Settings tile + home-screen widget** with a "Talk to FRIDAY" button | One tap from anywhere. |
| **Command shortcuts:** "Good night" = DND on + alarm 7:00 + brightness low (user-defined, run locally, no AI planning) | A light replacement for skills that stays short and predictable. |
| **Emergency SOS:** "FRIDAY, emergency" sends your location by SMS to saved contacts and calls one | High value, uses tools already in the plan. |
| **Typed command bar** on home for quiet places | Already half there with text input. |

## 16. Phases and sprints

### Sprint rules (every sprint)

- **Length:** about 1 week, shorter where the work is small.
- **Ends in a working app:** the end of every sprint is a build testers can install and use. A sprint never leaves the app broken or half-done.
- **Flow:** one branch per sprint (`sprint/<n.n>-<name>`), then one PR. Merge only when all of these pass:
  - `./gradlew testDebugUnitTest lintDebug assembleDebug`
  - new or updated unit tests for the sprint's logic
  - a user-facing entry in `changelog.md` and a line in `track.md`
  - **your phone test** of the sprint's *Demo* steps
- **Delivery:** from Sprint 1.1 on, every merge to `main` sends a tester build through **Firebase App Distribution** (§18).
- **Not done** means not merged. Unfinished work moves to the next sprint; it is never merged half-built.

### Overview

| Phase | Goal | Sprints | Needs from you |
|---|---|---|---|
| **1. Foundation** | New package, new Firebase, tester delivery, dead weight removed | 1.1 – 1.3 | D2, D5, D6, `firebase login` |
| **2. Identity** | It looks, sounds and introduces itself as FRIDAY by Nandivex | 2.1 – 2.3 | D4, logo approval |
| **3. Connectors** | All direct phone controls, clearly grouped | 3.1 – 3.4 | phone tests |
| **4. Accounts & setup** | First-install setup, sign-in, 1-day trial | 4.1 – 4.2 | none |
| **5. Subscriptions** | Coupons, Razorpay subscriptions, account management | 5.1 – 5.3 | D3, Razorpay account + KYC |
| **6. Play launch** | Policy-compliant build live on Google Play | 6.1 – 6.3 | D1, Play organization account |
| **7. Extras** | Faster ways to reach FRIDAY | 7.1 – 7.3 | pick which ones |

---

### Phase 1: Foundation

**Sprint 1.1: New package, new Firebase, tester delivery**
- Build:
  - Rename `com.assistant.app` → `com.nandivex.friday` (D2).
  - Add the three build variants `debug` / `beta` (the tester build; Android forbids build type names starting with "test") / `release` (§18).
  - Create the new Firebase project and wire `google-services.json` (§5, steps 1–4 and 7).
  - Add App Distribution and the `distribute.yml` workflow, and remove `publish-ota.yml`.
- Demo: a tester gets an email invite, installs "FRIDAY Dev / Tester" from App Tester and talks to it exactly as Ultron works today. Crashlytics shows the test crash in the new project.
- Done when: CI uploads a build on merge, and the debug and tester builds sit side by side on one phone.

**Sprint 1.2: Remove Skills and the Decisions API**
- Build: §2 (skills out), §6 (the `decisions/` package and its five toggles out), and the related tests.
- Demo: Settings has no Skills page or Decisions toggles. "Turn on the torch" answers noticeably faster than before.
- Done when: `git grep -i "skill\|decision"` shows no app code left, and all tests pass.

**Sprint 1.3: AWS-free app**
- Build:
  - Web search through Gemini Live's Google Search grounding.
  - On-phone web reader.
  - Remove `ApiClient`, MCP clients, the YouTube, Images and Knowledge base features (D5), `AppUpdater` / `UpdateManager`, and `FeatureRequestClient`. Feedback goes to Firestore instead.
  - Delete `mcp/`, `infra/`, `ultronapp/` and the scripts (§4).
  - The licence gate is **temporarily off** in tester builds until Phase 4.
- Demo: in airplane mode with Wi-Fi on and AWS unreachable, "Who won yesterday's match?" gets a grounded answer with sources. The feedback form lands in Firestore.
- Done when: `git grep -i "devxjitin.dev\|amazonaws\|McpClient"` is empty, and the APK is noticeably smaller (target: under 25 MB arm64).

### Phase 2: Identity

**Sprint 2.1: F.R.I.D.A.Y. personality and Nandivex credit**
- Build: §1 (one persona, Short/Detailed reply style, new greetings, default voice) and §3 (About page and creator line).
- Demo: "Who are you?" gets "I'm FRIDAY, your phone assistant, developed by Nandivex Technologies." Commands are confirmed in one short sentence, addressing you as "Boss".
- Done when: there are no personality picker, `set_personality` tool or personal names left in the user-facing strings.

**Sprint 2.2: New theme and logo**
- Build:
  - A canvas logo design, previewed for you; iterate until you approve it.
  - Adaptive icon, monochrome icon and splash screen.
  - New palette in `Color.kt` (§8), with contrast tests.
  - Animated HUD ring on the home and voice screens.
- Demo: the launcher shows the new icon (including the Android 13 themed icon). Light and dark modes are both readable, and no red is left.
- Done when: the contrast unit test covers every text/surface pair and passes.

**Sprint 2.3: Rebrand sweep**
- Build: every "Ultron" changed to FRIDAY (§14): `strings.xml`, notification channels, folder names, help page, changelog (already restarted at **0.0.1** in Sprint 1.1), README.
- Demo: walking through every screen and notification (alarm ring, foreground service, reminders) shows FRIDAY only.
- Done when: `git grep -i ultron` returns nothing except `track.md` history.

### Phase 3: Connectors

**Sprint 3.1: New connector structure**
- Build:
  - New categories in `ConnectorRegistry` (§7), with existing tools moved to their correct connectors.
  - Grouped Connectors page, a tool list with one-line help on each connector page, and a "Connect all essentials" button.
  - A unit test checks that every tool belongs to exactly one connector.
- Demo: the Connectors page shows *Calls & messages, Connectivity, Device controls, Clock, Apps, Info, Personal, Phone control*. Every existing command still works.
- Done when: the registry test passes, and no tool is declared to the model unless its connector is on.

**Sprint 3.2: Calls & messages**
- Build:
  - `answer_call`, `end_call`, `reject_call`, `speakerphone`, `mute_call` and `dial_number`.
  - `send_sms` (direct, with the draft fallback ready for §13 Plan B) and `open_sms_draft`.
  - `whatsapp_message` and `whatsapp_call`.
  - The **local safety rule**: when the turn contains outside text, FRIDAY asks for a spoken "yes" first.
  - The SMS rate limit.
- Demo:
  - "Call Mummy", then "speaker on", then "end call".
  - "Message Rahul *on my way*".
  - "Read my last notification and reply OK" asks "Send OK to Rahul?" before sending.
- Done when: the safety-rule unit tests cover SMS, call and share coming from notification text, screen text and web text.

**Sprint 3.3: Connectivity & device controls**
- Build:
  - `wifi_toggle`, `bluetooth_toggle`, `list_paired_devices`, `mobile_data_panel`, `airplane_mode`, `hotspot_settings` and `nfc_settings` (system panels, plus a self-tap when Screen control is on).
  - `flashlight_strobe` / SOS, `dark_mode` and `battery_saver`.
- Demo:
  - "Turn off Wi-Fi" opens the panel, or toggles it directly when Screen control is on.
  - "Bluetooth on".
  - "SOS flashlight".
- Done when: each tool returns a clear spoken result on Android 10, 13 and 15 (your phones plus an emulator).

**Sprint 3.4: Clock, apps, media and safety**
- Build: `stopwatch`, `play_music`, `take_selfie` / `record_video`, `quick_note`, `ring_phone` (find my phone) and `share_location`.
- Demo:
  - "Play Arijit Singh songs" opens your music app and starts playing.
  - "Start stopwatch".
  - "Note: buy milk".
  - "Where's my phone" rings at full volume while the phone is on silent.
- Done when: every new tool has a unit test for argument parsing and a line in its connector page.

### Phase 4: Accounts & setup

**Sprint 4.1: First-install setup + sign-in**
- Build:
  - Onboarding steps 1–6 and 8 (§10): welcome, name and form of address, Google sign-in, Gemini key with live check, voice sample, essential permissions, tips.
  - `users/{uid}` profile in Firestore.
  - New `firestore.rules` with rules tests (emulator).
- Demo: a fresh install walks through setup in under 2 minutes. A bad Gemini key is caught before Next. Reinstalling and signing in restores your name and preferences.
- Done when: Firestore rules tests prove users can read only their own docs and can't write entitlements.

**Sprint 4.2: 1-day free trial + access gate**
- Build:
  - `startTrial` Cloud Function with the per-account and per-device check (§12).
  - `entitlements/{uid}` listener.
  - Access gate in `MainActivity` (setup → plan → home).
  - Trial countdown on home, and a plan screen with **Start free trial** only.
  - The temporary gate bypass from Sprint 1.3 is removed.
- Demo: start the trial → FRIDAY works for 24 h (test with a 5-minute trial setting). After expiry it opens the plan screen. Reinstalling or using a second Google account on the same phone does not give a new trial.
- Done when: function unit tests cover the repeat-trial cases, and the offline grace works (3 days).

### Phase 5: Subscriptions

**Sprint 5.1: Coupons and `FRIDAYFREE`**
- Build: the `coupons` collection, `redeemCoupon` function (§11), coupon field on the plan screen and in onboarding, `FRIDAYFREE` with the developer email allowlist, and a developer-only coupon admin page.
- Demo: you enter `FRIDAYFREE` and get the **Developer** plan with no expiry. A non-developer account entering it gets "not valid for your account". Using the same code twice is refused.
- Done when: function tests cover the allowlist, redemption cap, once per account, and disabled codes.

**Sprint 5.2: Razorpay subscriptions (test mode)**
- Build:
  - `createSubscription` and `verifyPayment` functions.
  - Razorpay Android Checkout.
  - Monthly and yearly plans (D3) on the plan screen.
  - Secrets in Functions config.
- Demo: in Razorpay **test mode**, subscribe monthly with a test card or UPI. The entitlement turns active within seconds and FRIDAY unlocks.
- Done when: the signature verification has unit tests, and no secret is in the app or the repo.

**Sprint 5.3: Webhooks, renewals and account page**
- Build:
  - `razorpayWebhook` (activated / charged / halted / cancelled / completed).
  - Account page with plan, renewal date and **Cancel subscription**.
  - Percent- and flat-off coupons through Razorpay offers.
- Demo: a renewal triggered in Razorpay test mode extends the expiry. A failed charge marks it halted and the app asks you to update payment. Cancel keeps access until the cycle ends.
- Done when: webhook tests cover every event and duplicate deliveries.

### Phase 6: Play launch

**Sprint 6.1: Play policy compliance**
- Build:
  - Remove `REQUEST_INSTALL_PACKAGES` and `MANAGE_EXTERNAL_STORAGE`; Files moves to MediaStore.
  - `<queries>` instead of `QUERY_ALL_PACKAGES`.
  - Prominent disclosures for accessibility, SMS and location.
  - Play In-App Updates in the release variant.
  - Privacy policy and terms pages on the Nandivex site.
  - `targetSdk` 36.
- Demo: the release build passes the Play pre-launch checklist locally. Every sensitive connector shows its disclosure before the Android prompt.
- Done when: lint finds no policy warnings, and the release manifest has none of the removed permissions.

**Sprint 6.2: Play account, Play Billing and closed testing**
- Build:
  - Register the Play Console **organization** account for Nandivex (you, with the D-U-N-S number).
  - Play App Signing and upload key.
  - User Choice Billing enrolment (D1): Play Billing next to Razorpay, plus a `verifyPlayPurchase` function.
  - Store listing (icon, feature graphic, screenshots, Data safety form, content rating).
  - Upload to **internal testing**, then **closed testing**.
  - Permission declarations for SMS, accessibility and exact alarm.
- Demo: testers install FRIDAY from the Play Store. Both Razorpay and Play Billing unlock the same plan.
- Done when: Google approves the internal and closed tracks with the declarations accepted, or we switch to §13 Plan B and resubmit.

**Sprint 6.3: Production release + AWS teardown**
- Build:
  - Production staged rollout (10% → 50% → 100%).
  - Crashlytics dashboards and alerts.
  - Back up, then delete the AWS resources and the old `ultron-5350c` Firebase project, **only with your OK** (§4).
  - Notice to existing Ultron users.
- Demo: FRIDAY is public on Google Play. A new user can go from install to setup to trial to subscribe.
- Done when: crash-free users are above 99% at the 100% rollout, and the AWS bill is ₹0.

### Phase 7: Extras (you pick)

**Sprint 7.1: Quick launch**
- Build: Quick Settings tile, home-screen widget, and the default-assistant role (long-press power/home).
- Demo: long-press home → FRIDAY is listening in under 1 second.

**Sprint 7.2: Command shortcuts + Emergency SOS**
- Build: user-defined routines ("Good night" = DND + alarm + brightness, run locally) and "FRIDAY, emergency" (sends your location by SMS to saved contacts, then calls one).
- Demo: "Good night" runs all three actions. The SOS demo is run on a test contact.

**Sprint 7.3: "Hey FRIDAY" wake word**
- Build: on-device wake word (opt-in), with a battery-use note in Settings.
- Demo: with the screen off, "Hey FRIDAY, torch on" works from across the room.
- Done when: the battery drain is measured over 8 h and stated in Settings.

## 17. Risks

| Risk | Mitigation |
|---|---|
| Play rejects `SEND_SMS` or the accessibility use | Plan B in §13. Accessibility is optional and disclosed. |
| Payment-policy rejection for Razorpay | User Choice Billing enrolment (D1) **before** submitting. |
| Marvel IP complaint | D4: no Marvel names or artwork, "inspired" persona. |
| Trial abuse | One trial per account **and** device, enforced server-side. |
| `FRIDAYFREE` leaks | Email allowlist plus a redemption cap, server-side only. |
| Prompt injection triggers a call or SMS | Spoken confirmation when outside text is in the turn, plus the SMS rate limit. |
| Existing Ultron sideload users | They can't upgrade to the new package; they install FRIDAY from Play. Announce it before AWS teardown. |

## 18. Updates during development and testing (no Play account yet)

There is no Play Console account yet, so every pre-launch build is delivered without the Play Store.

**Developer phones: adb / Gradle**
- `./gradlew installDebug` (or `adb install -r app-debug.apk`) over USB or wireless debugging (`adb pair` / `adb connect`).
- The debug build gets `applicationIdSuffix = ".debug"` and the name **"FRIDAY Dev"**, so it can sit next to a tester or Play build on the same phone without signature conflicts.

**Testers: Firebase App Distribution** (free, in the new Firebase project)
- Testers are invited by email into the group `testers`. They install the **Firebase App Tester** app once, then get a notification and one-tap install for every new build. Android asks them once to allow installs from App Tester.
- Builds are signed with one shared **tester signing key** (kept outside git, stored as a GitHub secret). That way each new build installs over the last one and keeps the tester's data.
- **Manual upload from the PC:**
  `firebase appdistribution:distribute app/build/outputs/apk/<variant>/app-<variant>.apk --app <FIREBASE_APP_ID> --groups testers --release-notes "<notes>"`
- **Automatic:** a new GitHub Actions workflow `.github/workflows/distribute.yml` replaces `publish-ota.yml`. On every merge to `main` touching `android/**`, it builds the tester variant, runs the unit tests and uploads to App Distribution with the top entry of `changelog.md` as release notes. It authenticates with a Firebase service-account key stored as a GitHub secret.
- **In-app notice (optional):** the App Distribution SDK (`firebase-appdistribution`) shows testers an "update available" prompt inside FRIDAY. It goes **only in the tester build**, never the Play build: Play rejects apps that carry that SDK's install code.

**Build variants**

| Variant | Package | Signed with | Delivered by |
|---|---|---|---|
| `debug` | `com.nandivex.friday.debug` | debug key | adb / Gradle |
| `beta` | `com.nandivex.friday.tester` | tester key | Firebase App Distribution |
| `release` | `com.nandivex.friday` | Play upload key | Play Store (after launch) |

**After the Play launch:** testers move to the Play internal testing track. App Distribution can stay for quick dev builds, and production users update through Play (auto-update plus In-App Updates).
