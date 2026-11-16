# FRIDAY: product plan

Owner: Nandivex Technologies · Package `com.nandivex.friday` · Plan date 2026-11-13 · **Rewritten 2026-11-16** ·
App version on `main`: **0.0.23**; Sprint 4.2 (0.0.24) is in review on its own branch

**Status in one line:** the app itself (voice, phone control tools, theme, persona) is built and works in tester builds.
On 2026-11-16 the distribution decision changed from "our own website" to **Google Play only**. The website download,
the in-app updater and the Razorpay plan were built or planned for the old route and now have to be removed or
replaced. The setup screen, the Play Store work and the payment model are not started.

Rules for this file: every line is true on the date above. "Done" means merged to `main` with tests, lint and build
passing. Nothing here is a promise about Google's review: Google decides, and the plan says where that risk is.

---

## 1. Vision: Hands-Free Smartphone Access

FRIDAY lets a person use their Android phone **without touching it**. They speak; FRIDAY reads the screen and taps,
types, scrolls, opens apps and changes settings for them, and says what it did.

Who it is for, first: people who cannot easily use a touchscreen (motor disabilities, tremor, limited hand use,
paralysis, a temporary injury). This is not marketing wording: it is the condition for being allowed on Google Play
(section 2), so features, store listing and support must actually serve these users first.

What FRIDAY does not do by itself: it does not call, text, message, pay or post on its own initiative. When the user
asks for one of those, FRIDAY opens the app and does it on screen, the way the user's hands would, and asks before
the final irreversible tap (Sprint 6.2).

## 2. Hard constraints (decided)

1. **Google Play is the only channel.** No website APK, no GitHub APK, no other store. (Decided 2026-11-16, replaces
   the old D7 "route B" decision.)
2. **Google Play's Accessibility API policy** (read from the live policy page on 2026-11-16, page answer/10964491):
   - Any use of the Accessibility API that lets an app "autonomously initiate, plan, and execute actions" is
     prohibited. Fixed, human-defined scripts ("if X then do Y") are allowed.
   - Only apps with `isAccessibilityTool="true"` are exempt, and only services **whose primary purpose is helping
     people with disabilities** may set it. Voice-based input is one of the listed kinds of accessibility tool.
   - The page says a **general voice-activated assistant does not qualify**, even if it helps some users with motor
     impairments. "Assistants" and "automation tools" are named as not accessibility tools.
   - Every app using the API fills the Permission Declaration in Play Console. Accessibility tools explain how they
     serve an accessibility need. (Non-tools also need a disclosure video; tools are exempt from the in-app disclosure,
     but FRIDAY keeps its disclosure anyway, in setup.)
   - **What this means:** FRIDAY's model decides what to tap next, so FRIDAY can be on Play **only as a genuine
     accessibility tool**. Phase 6 is the work for that. If Google does not accept FRIDAY as an accessibility tool,
     the only Play-safe version is one without model-driven screen control, which is not this product. There is no
     third option on Play.
3. **Play's other rules that change our code** (checked 2026-11-16):
   - A Play app may not update itself outside Play: the website updater and `REQUEST_INSTALL_PACKAGES` must go.
     Play's own In-App Updates API is allowed.
   - New apps and updates must target **API 36** (in force since 2026-08-31). FRIDAY targets 35 today.
   - Paid digital features sold inside the app go through **Google Play Billing**. Razorpay is out for in-app
     subscriptions.
   - Play subscription free trials are **at least 3 days**. A 1-day trial is not possible with Play Billing.
   - Uploads are App Bundles (`.aab`) with Play App Signing.
4. **Firebase only, no AWS.** Project `friday-nandivex`.
5. **The user brings their own Gemini API key.** The key never leaves the phone except to call Gemini.

---

## 3. What is built (true on 2026-11-16)

### 3.1 Done and merged

| Sprint | What | Version | PR | Phone check |
|---|---|---|---|---|
| 1.1 | Package `com.nandivex.friday`; `debug` / `beta` / `release` builds; Firebase `friday-nandivex`; App Distribution; Crashlytics | 0.0.1–0.0.10 | #1–#3, #5, #6, #10 | not recorded |
| 1.2 | Skills and Decisions API removed | 0.0.4 | #4 | not recorded |
| 1.3 | AWS clients, YouTube, old updater, licence server, Knowledge base, Images, history search removed | 0.0.7–0.0.8 | #7–#9 | not recorded |
| 2.1 | One FRIDAY persona, Short / Detailed replies, Nandivex credit, default voice Kore | 0.0.11 | #11 | not recorded |
| 2.2 | Theme, logo, icons, splash, animated HUD ring | 0.0.12–0.0.13 | #12, #13 | not recorded |
| 2.3 | "Ultron" renamed to FRIDAY everywhere in the app | 0.0.14 | #14 | not recorded |
| 3.1–3.3 | Connectors, calls, SMS, WhatsApp, toggles. **Superseded by 3.4** (only the connectivity and torch tools stayed) | 0.0.15–0.0.17 | #15–#17 | n/a |
| 3.4 | Hands-free control: connectors and personal-data tools removed; phone control gate on open | 0.0.18 | #18 | **pending** |
| 3.5 | Alarms, timers, reminders, status tools removed; read-only "What F.R.I.D.A.Y. can do" page | 0.0.19 | #19 | **pending** |
| fix | Contact email `contact@nandivex.in` | 0.0.20 | #22 | **pending** |
| 6.1 (old) | Release build, website updater, `isAccessibilityTool=false`, restricted-settings help | 0.0.21 | #23 | **must be undone** (section 4) |
| 6.2 (old) | Website `site/` with download page, `release.yml`, `scripts/release_files.py` | n/a | #24 | undone in 4.1 |
| **4.1** | Play-only cleanup: updater, install permission, download pages and release workflow removed; dead licence code removed; `targetSdk` 36; ad-ID permissions removed | 0.0.23 | #26 | passed on Android 16 (UI and Gemini connect); spoken commands to be tried by you |

PR #25 (0.0.22, APK from GitHub releases) was closed without merging.

The first `release.yml` run (tag `v0.0.21`) failed at the Hosting deploy, and `gh release list` is empty, so **no APK
was ever published to the public**. Nobody has a sideloaded release build to migrate.

### 3.2 What the app does today

- **Voice:** Gemini Live (user's own key), barge-in, 30 voices, language choice, mic sensitivity, Short / Detailed
  replies, history page, usage page, on-device memory (`save_memory` / `forget_memory`).
- **58 phone tools, always on**, grouped on the Abilities page: see and use the screen (read, look, tap, type, scroll,
  swipe, wait for text), navigation (back, home, recents, shade, quick settings, lock, screenshot, power menu, split
  screen), connectivity panels and toggles, sound and media, display and torch (incl. strobe / SOS), apps and links,
  clipboard, time and battery.
- **Platform limits:** Android 10+ does not let apps flip Wi-Fi, mobile data, airplane mode, hotspot, NFC, dark mode
  or battery saver directly; FRIDAY opens the panel and taps the switch. Apps that protect their window (banking,
  some games) cannot be read. `look_at_screen` needs Android 11+.
- **Safety today:** text read from the screen or clipboard is marked untrusted before the model sees it. There is
  **no** confirmation before irreversible taps yet.
- **Start:** tap the mic, or hold home / power when FRIDAY is the default assistant. **No** wake word, tile, widget or
  typed command bar exists.
- **Tests:** 236 Android unit tests, plus Python tests for the CI scripts.

### 3.3 Problems found in the code review on 2026-11-16

These are real and must be fixed; none is visible in the plan before this rewrite.

| # | Where | Problem | Fix in |
|---|---|---|---|
| R1 **(fixed in 4.1)** | `ProfilePage.kt`, `AccessViewModel.kt`, `License.kt`, `ActivationScreen` | The Ultron activation-key system is dead but still on screen: Profile shows a fake key "FRIDAY-OPEN", plan "Open", and **Refresh / Remove key buttons that do nothing**. | 4.1 |
| R2 | `KnowledgeBaseProvider.kt` (prompt) | The prompt says "Never ask if they are sure". That contradicts asking before Send / pay / delete. | 6.2 |
| R3 **(fixed in 4.1)** | `ApiKeyStore` embedding key, `AssistantOptions.DATASET_MODEL` | Leftovers of the removed Knowledge base. (`VisitorStrings.kt` is still used for the status line, so it stays.) | 4.1 |
| R4 **(fixed in 4.1)** | `build.gradle.kts` | `targetSdk = 35`; Play needs 36. | 4.1 |
| R5 **(fixed in 4.2)** | `README.md` | Still describes calls, alarms, web search and connectors, all removed. Says "Play Store build" next to a website updater. | 4.2 |
| R6 **(fixed in 4.2)** | `docs/Idea.md`, `docs/PRIVACY_PLAN.md`, `docs/VIEWMODEL_SPLIT_PLAN.md` | Old white-label kiosk plan, a privacy plan for removed tools, a finished refactor plan. | 4.2 |
| R7 **(fixed in 4.1)** | Merged manifest | The advertising ID (`AD_ID`) and `ACCESS_ADSERVICES_*` arrived from Firebase libraries although FRIDAY shows no ads; removed with `tools:node="remove"`. `WAKE_LOCK` and the Play install-referrer permission still come from libraries and go into the Data safety review (8.1). | 4.1 |
| R8 | API key and name | Entered only in Settings; a new user is not guided. No agreement is ever accepted. | 5.1 |

---

## 4. Undo list: what the Play-only decision removes

| Item | Action |
|---|---|
| In-app updater (`UpdateManifest`, `UpdateInstaller`, `UpdateController`, `UpdateDialog`, About "Check for updates", `SELF_UPDATE`, `UPDATE_URLS`) and their tests | Delete. Optionally replace with Play In-App Updates (Sprint 8.2). |
| `REQUEST_INSTALL_PACKAGES` (`src/release/AndroidManifest.xml`), `updates/` FileProvider path | Delete. |
| Restricted-settings help in the phone control gate | Delete: Play installs are not restricted. |
| `.github/workflows/release.yml`, `scripts/release_files.py`, `tests/ci/test_release_files.py`, release secrets in `docs/CI_CD.md` | Replace with a workflow that builds a signed `.aab` and uploads to Play's internal track (Sprint 8.2). |
| `site/download.html`, `site/download.js`, `site/install-guide.html`, APK redirect in `firebase.json` | Delete. Keep `index`, `privacy`, `terms`, `404`: Play requires a public privacy policy URL. |
| Razorpay design (old sections 11, Phase 5), coupons `FRIDAYFREE`, `trials/{deviceHash}` | Drop. Replaced by Phase 7 (Play Billing) if FRIDAY is paid at all. |
| `isAccessibilityTool="false"` | Becomes `true` in Sprint 6.1, only together with the product work that makes it honest. |
| PR #25 | Close without merging. |
| Changelog entries 0.0.21 / 0.0.22 that mention the website | Leave history as is; the next entry says updates now come from Play. |

---

## 5. Decisions needed from you

| # | Question | Recommendation |
|---|---|---|
| D1 | **Play developer account:** personal or organisation (Nandivex)? | Organisation. It needs a D-U-N-S number but skips the "12 testers for 14 days" closed test that new personal accounts must pass before production. |
| D2 | **Free or paid?** The user already pays Google for Gemini through their own key. | Decide before Phase 7. Options: (a) free; (b) Play subscription with a 3-day+ trial. Accessibility users are often price-sensitive; (a) is simplest and has the lowest review risk. |
| D3 | **Prices** (only if D2 = paid). | You decide. |
| D4 | **Name.** F.R.I.D.A.Y. is a Marvel character. Play enforces intellectual-property rules at review and on complaints. | Use a listing name that does not depend on Marvel (for example "FRIDAY – Hands-Free Phone Access"), no Marvel quotes, art or "Iron Man" references. A trademark search before launch. A full rename is the safest option. |
| D5 | **Persona.** A witty "Boss" assistant reads as a general assistant, which Play says does not qualify. | Keep the voice calm and short; make chat and memory secondary features of an access tool, not the headline. |
| D6 | **Setup details:** what besides the name? | Only what the app uses: name, how to be addressed, language. No email, phone or address while there is no account, so the Data safety form stays small. |
| D7 | **Firebase Analytics and anonymous sign-in** while nothing uses them. | Remove both now (and App Check until a backend needs it). Crashlytics stays. Add them back only when a feature needs them. |

---

## 6. Setup screen (your requirement, Sprint 5.1)

Shown on first open and until finished; the home screen does not open before it is done. Every value can be changed
later in Settings.

1. **Welcome:** what FRIDAY is ("Use your phone hands-free, by voice"), Nandivex credit.
2. **Agreement:** short summary plus links to the full Terms of Use and Privacy Policy (the same text as the website),
   and the phone control disclosure: what the accessibility service reads (screen text, screenshots when asked), that
   screen content and voice go to Google Gemini only to carry out the request, that it can be turned off in Android
   Settings. A checkbox "I have read and agree" and an **Accept** button; **Decline** closes the app. The app stores
   the accepted terms version and the time on the phone. When the terms version changes, the screen comes back once.
3. **About you:** name (required), how FRIDAY should address you (name, "Boss", or custom), language.
4. **Gemini API key:** paste field, a "Get a free key" button that opens Google AI Studio, a plain note that Google's
   own Gemini API terms and limits apply, and a **live check** (one tiny Gemini request) before Next. A wrong or
   exhausted key shows the real reason. The key is stored encrypted with the Android Keystore (as today).
5. **Voice:** pick a voice, hear a short sample.
6. **Permissions:** microphone and notifications, each with one line on why.
7. **Phone control:** turn on the FRIDAY accessibility service (the existing gate screen, without the sideload help).
8. **Done:** three example commands.

Existing tester installs: they see only the missing steps (agreement, and name or key if empty).

Accessibility note: a user who cannot type may need a helper for setup (the API key is a long string). The setup
must work with TalkBack and with large text, and must say on step 4 that someone can help.

---

## 7. Phases and sprints from here

Sprint rules (unchanged): about one week each; one branch and PR per sprint (`sprint/<n.n>-<name>`); merged only when
`./gradlew testDebugUnitTest lintDebug assembleDebug` passes, new tests exist for the sprint's logic, `changelog.md`
and `track.md` are updated, and **you** have done the sprint's phone check. Every merge to `main` still sends a tester
build through Firebase App Distribution.

| Phase | Goal | Sprints | Status | Needs from you |
|---|---|---|---|---|
| 1. Foundation | Package, Firebase, tester delivery, dead weight out | 1.1–1.3 | **Done** | – |
| 2. Identity | Look, voice, name | 2.1–2.3 | **Done** | – |
| 3. Hands-free control | Tools, gate, Abilities page | 3.1–3.5 | **Done** (phone checks pending) | phone checks |
| 4. Play-only cleanup | Remove website route and dead code, target 36, true docs | 4.1–4.2 | 4.1 **done**, 4.2 in review | – |
| 5. Setup | Your setup screen: details, API key, agreement | 5.1 | To do | D6, final Terms/Privacy text |
| 6. Accessibility tool readiness | Make FRIDAY genuinely an access tool, safe and reliable | 6.1–6.3 | To do | D4, D5, testers with disabilities |
| 7. Monetisation | Play Billing (only if paid) | 7.1–7.2 | Blocked on D2 | D2, D3 |
| 8. Play launch | Console, policy forms, testing tracks, release | 8.1–8.3 | To do | D1, account, legal review |
| 9. Extras | Faster ways to start, routines | 9.1–9.2 | Later | pick |

### Phase 4: Play-only cleanup

**Sprint 4.1: Remove the website route and dead code** — **done and merged 2026-11-16 (0.0.23, PR #26)**
- Build: everything in section 4 except Play upload; R1 (Profile page shows only name, device and app version; delete
  `License.kt`, `AccessViewModel`, `ActivationScreen`, `AccessState`, `AccessGate` or reduce it to what the session
  code needs); R3; R4 (`targetSdk = 36`, fix what Android 16 changes); R7; a `bundleRelease` build signed with the
  upload key.
- Built: all of the above; the website no longer offers a download (the home page says FRIDAY is not available yet); `bundleRelease` builds an `app-release.aab` (unsigned on a machine without the release key); the merged release manifest has no install permission and targets 36. Tests, lint and the beta/debug builds pass. Not yet done: a phone check on Android 16, and a signed bundle.
- Phone check: a fresh install opens to the gate, the Profile page has no key card, all voice commands from 3.4/3.5
  still work on Android 16.
- Done when: `git grep -i "latest.json\|REQUEST_INSTALL\|ULTR-"` finds nothing in `android/`, and the AAB builds.

**Sprint 4.2: Documentation that is true** — **built 2026-11-16, version 0.0.24, in review**
- Build: new README (hands-free access, Play only, how to build); delete `docs/Idea.md` and
  `docs/VIEWMODEL_SPLIT_PLAN.md`; rewrite `docs/PRIVACY_PLAN.md` for the current data flows (voice and screen content
  to Gemini, name and memory on the phone, crash reports to Crashlytics); `docs/CI_CD.md` without the website release.
- Built: new README that lists only what exists and what does not; `docs/DATA_FLOWS.md` (what goes to Gemini, what stays on the phone, what Firebase receives, every permission) replaces `PRIVACY_PLAN.md`; `Idea.md` and `VIEWMODEL_SPLIT_PLAN.md` deleted; `LATENCY.md` corrected; the unused jsoup library removed.
- Done when: every feature named in the docs exists in the app.
- **Found while checking (needs your decision, D7 below):** the app still creates a Firebase **anonymous user** on every launch and includes **Firebase Analytics**, and nothing reads either. The privacy page already says so, but they add data for the Play Data safety form for no benefit.

### Phase 5: Setup

**Sprint 5.1: First-run setup** (section 6)
- Build: `ui/setup/` with the eight steps, a `SetupState` stored in DataStore (terms version, accepted-at time, done
  flag), routing in `MainActivity`: setup not done → setup; phone control off → gate; else home. Unit tests for the
  routing, the terms-version re-prompt, name validation and the key check result mapping.
- Phone check: a fresh install finishes setup in about two minutes; a wrong key is caught on step 4; Decline closes the
  app; after a terms version bump the agreement shows once more; TalkBack can read every step.
- Done when: no path reaches home without an accepted agreement, a saved name and a checked key.

### Phase 6: Accessibility tool readiness

This phase is what decides whether Play accepts FRIDAY. It is product work, not a flag change.

**Sprint 6.1: Access-first product pass**
- Build: `isAccessibilityTool="true"` with an honest service description; spoken feedback after every action and on
  every failure; "what's on my screen" read-out; a mode that keeps listening between commands so the user never has to
  touch the mic button; FRIDAY's own UI checked with Accessibility Scanner and TalkBack (labels, touch targets,
  contrast); store and in-app wording that describes access for people who cannot use the touchscreen (D5).
- Phone check: one full day using the phone without touching it after setup, written up with what failed.
- Done when: the scanner shows no errors on FRIDAY's screens, and the Play accessibility declaration text is drafted
  from what the app really does.

**Sprint 6.2: Safe screen actions** (was 3.6)
- Build: confirmation before irreversible taps (Send, Pay, Delete, Post, Buy, Confirm), detected from the button label
  and the app (messaging, payments, banking, shopping), asked aloud ("Send this to Rahul?"), answered by voice; remove
  the "never ask if they are sure" prompt line (R2); unit tests for labels, apps, Hindi / Hinglish / English yes and no.
- Phone check: "Message Rahul on WhatsApp that I'm late" types it and asks before Send.
- Done when: the tests pass and no tested Send / Pay button is pressed without a spoken yes.

**Sprint 6.3: Everyday jobs, measured**
- Build: a written test list of common jobs done through the screen (call a contact, WhatsApp text, set an alarm in
  Clock, play music, open a route in Maps, take a note, read the latest message), tried on at least three phone brands;
  prompt and tool fixes for the weak ones; the jobs that work reliably listed on the Abilities page, the others not
  claimed.
- Done when: the Abilities page claims only what passed.

### Phase 7: Monetisation (only if D2 = paid)

**Sprint 7.1: Play Billing subscription**
- Build: Play Billing Library, monthly and/or yearly plans with a free trial of 3 days or more, a plan screen after
  setup, an offline grace period. Purchase verification on a server (Cloud Functions + Play Developer API + Real-time
  developer notifications), which needs the Firebase **Blaze** (pay-as-you-go) plan. Developer and tester access
  through Play **licence testers**, not a coupon code in the app.
- Done when: test purchases, renewals, cancellations and refunds from Play's test tools change access correctly.

**Sprint 7.2: Account page**: plan, renewal date, a "Manage subscription" link to Play. Play handles cancel and refunds.

Note: if FRIDAY stays free, Phase 7 and Google sign-in are not needed at all; the app keeps working without an account.

### Phase 8: Play launch

**Sprint 8.1: Play Console readiness**
- You: create the Play developer account (D1), pay the fee, verify identity.
- Build / write: store listing (name per D4, short and full description, 512 px icon, 1024×500 feature graphic,
  phone screenshots); privacy policy and terms on `friday.nandivex.in` (or `friday-nandivex.web.app`) rewritten for the
  Play app; **Data safety** form (voice audio and screen content sent to Gemini to perform requests, name and memory on
  device, crash data to Crashlytics, the Firebase-library permissions from R7); content rating; target audience;
  **Accessibility API declaration**; **App access instructions** so reviewers can test (they need a working Gemini key:
  decide whether to give them a limited key or a test mode, because without one they cannot use the app and the review
  can fail); a lawyer's read of the terms and privacy policy.
- Done when: every Play Console section shows complete.

**Sprint 8.2: Testing tracks and release pipeline**
- Build: a workflow that builds the signed AAB and uploads to the **internal** track; Play App Signing enrolled with
  the existing release key as the upload key (`android/keystore/nivi-release.jks`, backed up in two places); optional
  Play In-App Updates prompt; Crashlytics alerts.
- Then: internal test → closed test (with real users who need hands-free access) → submit for production review.
- Done when: a build reaches testers from Play and installs over the previous one.

**Sprint 8.3: Production and teardown**
- Staged rollout (for example 10% → 50% → 100%) watching crash-free users; then, with your OK and after a backup,
  delete the AWS resources and the old Firebase project `ultron-5350c` (do not touch `future-self-booth-api` or
  `/age-prediction/hf-token`).
- Done when: the app is live on Play and the AWS bill is ₹0.

### Phase 9: Extras (after launch, you pick)

- **9.1 Quick start:** Quick Settings tile, home-screen widget, wake word "Hey FRIDAY" (on-device, opt-in; engine and its
  licence to be chosen; battery drain measured before it ships).
- **9.2 Routines:** user-defined fixed sequences ("Good night" = Do Not Disturb, low brightness, Wi-Fi off) run without
  the model; a typed command bar.

---

## 8. Waiting on you

1. Close PR #25 (or tell me to).
2. D1–D6 (section 5).
3. Play developer account (and D-U-N-S if organisation).
4. Final Terms of Use and Privacy Policy text, read by a lawyer.
5. Back up the release key `android/keystore/nivi-release.jks` and its passwords in two separate places.
6. Phone checks still open: 3.4, 3.5, the 0.0.20 email fix.
7. Testers who rely on hands-free access, for Sprints 6.1–6.3 and the closed test.
8. Your OK before the AWS / `ultron-5350c` teardown.

No longer needed (website route): GoDaddy DNS records for the download site, Razorpay account and KYC, GitHub
release-key secrets for `release.yml`. The domain is still useful for the privacy policy page.

## 9. Risks

| Risk | What we do |
|---|---|
| **Google does not accept FRIDAY as an accessibility tool** (the biggest risk; Google decides) | Phase 6 makes access the real primary purpose; honest declaration; closed test with real users. If refused, we learn the reason before any public launch; the fallback is a reduced build without model-driven screen control, which is a different product and needs a new decision. |
| Play's policy text changes | Re-read the Accessibility API page before 8.1 and before every major update. |
| Marvel IP complaint | D4. |
| Screen text tricks FRIDAY into pressing Send or Pay | Untrusted-text marking (done) and spoken confirmation (6.2). |
| App layouts change and screen jobs break | 6.3 test list; claim only what works. |
| Reviewers cannot test without a Gemini key | App access instructions and a reviewer key or test mode (8.1). |
| Gemini key limits, cost or regional availability | Clear errors from the setup check and during use; the user owns the key. |
| Losing the signing key | Play App Signing holds the app key; the upload key can be reset through Play support, but keep two backups anyway. |
| Free-tier Gemini data use | The setup screen and privacy policy point to Google's Gemini API terms; we do not claim more privacy than Google gives. |

## 10. Delivery during development (unchanged)

| Build | Package | Signed with | Delivered by |
|---|---|---|---|
| `debug` ("FRIDAY Dev") | `com.nandivex.friday.debug` | debug key | `./gradlew installDebug` / adb |
| `beta` ("FRIDAY Tester") | `com.nandivex.friday.tester` | tester key | Firebase App Distribution, group `testers`, on every merge to `main` (`distribute.yml`) |
| `release` | `com.nandivex.friday` | upload key → Play App Signing | **Google Play only** (internal → closed → production) |

The App Distribution SDK stays in the tester build only, never in the Play build.

## 11. Removed for good (history)

Skills, Decisions API, Knowledge base, YouTube, Images, history search (1.2–1.3); connectors, calls, SMS, WhatsApp,
contacts, calendar, location, notifications, files, app usage, sharing, weather, web search (3.4); alarms, timers,
reminders, show-notification, sensor and device-info tools (3.5); the website download, self-updater and Razorpay
plan (2026-11-16, Play-only decision). Permissions removed with them are listed in the changelog entries 0.0.18,
0.0.19 and 0.0.21.
