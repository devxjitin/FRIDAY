# FRIDAY: product plan

Owner: Nandivex Technologies · Package `com.nandivex.friday` · Plan date 2026-11-13 · **Rewritten 2026-11-16** ·
App version on `main`: **0.0.26** · Plan updated after your app review of 2026-11-16 (section 12)

**Status in one line:** voice control, the phone tools, the Play-only cleanup and first-run setup are built and merged
(0.0.26). Your review of 2026-11-16 found that the app does not look or read like a professional product (colours,
logos, font, wording, sidebar, Help, Storage, Usage) and asked for accounts with email verification, age, and plans with
a trial chosen in setup. Those are now Phases 6 and 7. Accessibility-tool readiness, the Play launch and payments are
not started.

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
the final irreversible tap (Sprint 8.2).

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
     accessibility tool**. Phase 8 is the work for that. If Google does not accept FRIDAY as an accessibility tool,
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
5. **Gemini access (D9, decided 2026-11-16):** the user brings their **own Gemini API key with billing enabled**
   (Google's paid tier). It never leaves the phone except to call Gemini. Nandivex does not pay for or resell Gemini.

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
| R2 | `KnowledgeBaseProvider.kt` (prompt) | The prompt says "Never ask if they are sure". That contradicts asking before Send / pay / delete. | 8.2 |
| R3 **(fixed in 4.1)** | `ApiKeyStore` embedding key, `AssistantOptions.DATASET_MODEL` | Leftovers of the removed Knowledge base. (`VisitorStrings.kt` is still used for the status line, so it stays.) | 4.1 |
| R4 **(fixed in 4.1)** | `build.gradle.kts` | `targetSdk = 35`; Play needs 36. | 4.1 |
| R5 **(fixed in 4.2)** | `README.md` | Still describes calls, alarms, web search and connectors, all removed. Says "Play Store build" next to a website updater. | 4.2 |
| R6 **(fixed in 4.2)** | `docs/Idea.md`, `docs/PRIVACY_PLAN.md`, `docs/VIEWMODEL_SPLIT_PLAN.md` | Old white-label kiosk plan, a privacy plan for removed tools, a finished refactor plan. | 4.2 |
| R7 **(fixed in 4.1)** | Merged manifest | The advertising ID (`AD_ID`) and `ACCESS_ADSERVICES_*` arrived from Firebase libraries although FRIDAY shows no ads; removed with `tools:node="remove"`. `WAKE_LOCK`, the install-referrer and `READ_GSERVICES` permissions went away with Analytics in 4.3; re-check the merged manifest in 9.1. | 4.1 |
| R8 **(fixed in 5.1)** | API key and name | Entered only in Settings; a new user is not guided. No agreement is ever accepted. | 5.1 |
| R9 (decision made: D9) | Setup key step, agreement | Google's Gemini API terms (read 2026-11-16) say **"Do not submit sensitive, confidential, or personal information to the Unpaid Services"**, and FRIDAY sends screen content. The setup still says "Get a free key". With a free key FRIDAY is used against Google's terms. | D9, then 6.4 / 7.3 |
| R10 | Whole app | Google's Gemini API terms say users **"must be 18 years of age or older"** and an app may not be **"directed towards or is likely to be accessed by individuals under the age of 18"**. FRIDAY has no age check today. | 7.1 |

---

## 4. Undo list: what the Play-only decision removes

| Item | Action |
|---|---|
| In-app updater (`UpdateManifest`, `UpdateInstaller`, `UpdateController`, `UpdateDialog`, About "Check for updates", `SELF_UPDATE`, `UPDATE_URLS`) and their tests | Delete. Optionally replace with Play In-App Updates (Sprint 9.2). |
| `REQUEST_INSTALL_PACKAGES` (`src/release/AndroidManifest.xml`), `updates/` FileProvider path | Delete. |
| Restricted-settings help in the phone control gate | Delete: Play installs are not restricted. |
| `.github/workflows/release.yml`, `scripts/release_files.py`, `tests/ci/test_release_files.py`, release secrets in `docs/CI_CD.md` | Replace with a workflow that builds a signed `.aab` and uploads to Play's internal track (Sprint 9.2). |
| `site/download.html`, `site/download.js`, `site/install-guide.html`, APK redirect in `firebase.json` | Delete. Keep `index`, `privacy`, `terms`, `404`: Play requires a public privacy policy URL. |
| Razorpay design (old sections 11, Phase 5), coupons `FRIDAYFREE`, `trials/{deviceHash}` | Drop. Replaced by Phase 7 (Play Billing). |
| `isAccessibilityTool="false"` | Becomes `true` in Sprint 8.1, only together with the product work that makes it honest. |
| PR #25 | Close without merging. |
| Changelog entries 0.0.21 / 0.0.22 that mention the website | Leave history as is; the next entry says updates now come from Play. |

---

## 5. Decisions needed from you

| # | Question | Recommendation |
|---|---|---|
| D1 | **Play developer account:** personal or organisation (Nandivex)? | Organisation. It needs a D-U-N-S number but skips the "12 testers for 14 days" closed test that new personal accounts must pass before production. |
| D2 | **Free or paid?** | **Decided 2026-11-16: paid plans with a trial**, chosen in setup and upgradable later (your review, item 8). |
| D3 | **Prices** and which plans (monthly, yearly, tiers). | You decide. |
| D4 | **Name.** | **Decided 2026-11-16 by you: keep F.R.I.D.A.Y.** The risk stays: F.R.I.D.A.Y. is a Marvel character, and Play acts on intellectual-property complaints, which can mean a forced rename or a takedown after launch. What we do to keep the risk low: no Marvel quotes, art, "Iron Man" or "Stark" references anywhere; our own logo; a descriptive store title such as "F.R.I.D.A.Y. – Hands-Free Phone Access"; a trademark search before launch (9.1); and a fallback name kept ready so a rename is quick if a complaint comes. |
| D5 | **Persona.** A witty "Boss" assistant reads as a general assistant, which Play says does not qualify. | Keep the voice calm and short; make chat and memory secondary features of an access tool, not the headline. |
| D6 | **Setup details.** | **Superseded 2026-11-16 by your review:** name, age and a verified email, in an account (Phase 7). |
| D7 | **Firebase Analytics, anonymous sign-in, App Check** while nothing uses them. | **Decided 2026-11-16: removed** (4.3). Accounts (7.1) bring Firebase Authentication back for a real reason; Analytics stays out unless you ask for it. |
| D8 | **How the email is verified.** | **Google sign-in** (Google has already verified the email; one tap; works on Firebase's free plan), with **email link sign-in** as the fallback for people without a Google account on the phone. A typed 6-digit code needs our own mail sender and Cloud Functions; skip it. |
| D9 | **Whose Gemini key?** (R9) | **Decided 2026-11-16: the user's own key, with billing enabled (Google's paid tier).** Setup says plainly that a free key is not allowed for FRIDAY because Google's terms forbid personal data on the unpaid tier, and links to Google's page for enabling billing. Nandivex pays nothing to Google per user, and the subscription (D3) pays only for FRIDAY itself. The app cannot see from the key alone whether billing is on, so this is a stated rule the user agrees to, not a check. |
| D10 | **Trial.** Play subscription trials are at least 3 days and are part of a Play subscription purchase. | Either a Play free trial (3 days or more, inside the subscription) or an app-managed free period with no purchase (our server records it once per account). Decide with D3. |
| D11 | **Age.** (R10) | Ask the **year of birth** in setup; under 18 cannot continue (Gemini terms). Use only an age band (18–24, 25–39, 40–59, 60+) to set the tone and pace of replies, never to change what FRIDAY will do. |

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
| 2. Identity | Look, voice, name | 2.1–2.3 | **Done** (the look is redone in Phase 6) | – |
| 3. Hands-free control | Tools, gate, Abilities page | 3.1–3.5 | **Done** (phone checks pending) | phone checks |
| 4. Play-only cleanup | Remove website route and dead code, target 36, true docs, unused Firebase | 4.1–4.3 | **Done** (PRs #26–#28) | – |
| 5. Setup | Setup screen: details, API key, agreement | 5.1 | **Done** (PR #29) | lawyer's read of the agreement, TalkBack check |
| 6. Brand and UI overhaul | Purple brand, new logos, system font, human wording, new sidebar, Help, Storage, Usage | 6.1–6.5 | To do — **next** | the Nandivex logo file, your approval of the design (6.1) |
| 7. Accounts, profile and plans | Verified email, age, profile, plans and trial in setup, upgrade, account deletion | 7.1–7.4 | To do | D1, D3, D8, D10, D11, Play account |
| 8. Accessibility tool readiness | Make FRIDAY genuinely an access tool, safe and reliable | 8.1–8.3 | To do | D5, testers with disabilities |
| 9. Play launch | Console, policy forms, testing tracks, release | 9.1–9.3 | To do | D1, legal review |
| 10. Extras | Faster ways to start, routines | 10.1–10.2 | Later | pick |

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

**Sprint 4.2: Documentation that is true** — **done and merged 2026-11-16 (0.0.24, PR #27)**
- Build: new README (hands-free access, Play only, how to build); delete `docs/Idea.md` and
  `docs/VIEWMODEL_SPLIT_PLAN.md`; rewrite `docs/PRIVACY_PLAN.md` for the current data flows (voice and screen content
  to Gemini, name and memory on the phone, crash reports to Crashlytics); `docs/CI_CD.md` without the website release.
- Built: new README that lists only what exists and what does not; `docs/DATA_FLOWS.md` (what goes to Gemini, what stays on the phone, what Firebase receives, every permission) replaces `PRIVACY_PLAN.md`; `Idea.md` and `VIEWMODEL_SPLIT_PLAN.md` deleted; `LATENCY.md` corrected; the unused jsoup library removed.
- Done when: every feature named in the docs exists in the app.
- Found while checking: the app created a Firebase anonymous user on every launch and included Firebase Analytics, and nothing read either. Removed in Sprint 4.3.

**Sprint 4.3: Remove unused Firebase parts** — **done and merged 2026-11-16 (0.0.25, PR #28)**; a Crashlytics test report reaching the console is still unchecked
- Built: Firebase Analytics, Authentication (anonymous sign-in) and App Check removed from the app, with `FridayApp` and the three `AppCheckInstaller` files; only Crashlytics stays. Privacy page, README and `docs/DATA_FLOWS.md` updated to say so.
- Not done: a Crashlytics test crash on a phone to prove reports still arrive (the tester build has the long-press test crash in About).
- Done when: a test crash from a tester build shows up in the Firebase console.

### Phase 5: Setup

**Sprint 5.1: First-run setup** (section 6) — **done 2026-11-16, version 0.0.26, PR #29; you walked through it on your phone**
- Built: `ui/SetupScreens.kt`, `viewmodel/SetupViewModel.kt`, `data/SetupLogic.kt` (routing rules, terms version, name rule), `data/Agreement.kt` (the in-app agreement text) and `network/GeminiKeyCheck.kt` (asks Google for the first page of models with the key in a header; the key is saved only after Google accepts it). Routing in `MainActivity`: setup steps, then the phone control gate, then a one-time "all set" screen, then home. The voice session (`AccessGate`) stays closed until setup has nothing left to ask. 20 new unit tests (`SetupLogicTest`, `GeminiKeyCheckTest`).
- Differences from the section 6 sketch, so the plan stays true: one name field ("What should FRIDAY call you?", with a "Use Boss" shortcut) instead of name plus a separate form of address; **no voice sample** (a sample would need its own Gemini call, so the voice step says you will hear it in your first conversation); the agreement text lives in the app and does **not** link to the website because the site is not deployed yet.
- Checked: tests, lint and build pass; on an Android 16 phone with an existing key the app opens on the agreement, the Accept button stays disabled until the box is ticked, Decline is there.
- **Phone check:** you completed the setup on a fresh install (Android 16). Still **not** checked: TalkBack reading every step, and the agreement text read by a lawyer (9.1). What you saw go wrong, if anything, is not recorded here: tell me and it goes into the next sprint.
- Done when: no path reaches home without an accepted agreement, a saved name and a checked key (**proved in `SetupLogicTest` for every combination**, and walked through on the phone).

### Phase 6: Brand and UI overhaul (your review, items 1–6 and 9–13)

Design is a matter of taste, so this phase starts with pictures you approve, not code.

**Sprint 6.1: Design direction (approval before code)**
- Build: one page of mock-ups for you to approve: the purple palette in light and dark (with contrast numbers), two or
  three FRIDAY logo concepts in purple, where the Nandivex logo sits (welcome, About, splash), the system font, and the
  new home, sidebar, Profile, Help, Storage and Usage screens.
- Needs from you: the **Nandivex Technologies logo as an SVG or a large PNG** (the name is decided: F.R.I.D.A.Y.). I cannot create the company's logo; I can only place the one you give me.
- Done when: you have picked one direction in writing. Nothing in 6.2–6.5 starts before that.

**Sprint 6.2: Brand in the app** (items 1–4)
- Build: the purple palette as the app's palette (light and dark), with `PaletteContrastTest` updated so every text
  colour stays at 4.5:1 or better; the new FRIDAY logo as adaptive, monochrome and splash icons (vector) and the 512 px
  store icon; the Nandivex logo on the welcome screen, About and the splash; the **system font** (`FontFamily.Default`)
  everywhere, with the bundled Inter font and its licence file removed; the HUD ring and voice ribbon recoloured.
- Done when: no teal or amber is left in `Color.kt`, the contrast test passes, and the icons look right on your phone's
  home screen in light and dark.

**Sprint 6.3: Navigation and sidebar** (items 6, 12, 13)
- Build: a new sidebar: a header with the user's name, email and plan (filled from 7.1); then **Home**, **What FRIDAY
  can do**, **History**, **Memory**; then **Account** (Profile, Plan); then **App** (Settings, Usage, Storage); then
  **Help & support** and **About**. Material 3 navigation items with a clear selected state, 48 dp touch targets, one
  icon style, and the same page header on every page.
- Done when: every page is reachable in two taps, TalkBack reads every item, and you approve it on the phone.

**Sprint 6.4: Pages and wording** (items 5, 9, 10, 11)
- **Help & support**, laid out like other apps: frequently asked questions (expandable, searchable), Contact support
  (email with version and phone filled in), Report a problem, Suggest a feature, Privacy policy, Terms. **"Request a
  phone control" is removed.**
- **Storage:** the total used by FRIDAY at the top, a bar split by category (conversation history, memories, temporary
  files, settings), and for each category its size, what it is, and its own Clear button with a confirmation.
- **Usage:** today / 7 days / 30 days: voice minutes, sessions, commands, actions done on the screen, actions that
  failed, the apps FRIDAY opened most, the tools used most, and the busiest times of day. **All counted on the phone;
  nothing is sent anywhere.** New counters start from the version that adds them, so earlier days show less.
- **Wording:** every user-facing text rewritten in plain, short, human language (screens, buttons, errors, toasts,
  agreement, Help, Abilities page, store text), with one style guide in `docs/WRITING.md`, and the R9 fix to the key
  step and agreement: a key with billing enabled, why a free key is not allowed, and a link to Google's billing page. You read and approve the agreement and Help text.
- Done when: you approve each page on the phone, and the Usage numbers match a session you counted by hand.

**Sprint 6.5: UI polish pass**
- Build: consistent spacing, cards, buttons, empty states, loading states and motion on every page; large text and
  dark mode checked on every screen; an Accessibility Scanner run with no errors.
- Done when: you call it professional, screen by screen. This sprint repeats until you do.

### Phase 7: Accounts, profile and plans (your review, items 7 and 8)

Accounts change the privacy story: email, age and plan become personal data that leaves the phone, the Data safety form
grows, and Play then **requires a way to delete the account inside the app and a web page to request deletion**.

**Sprint 7.1: Account and verified email** (D8, D11)
- Build: Firebase Authentication again (Google sign-in, email link as fallback); a `users/{uid}` document in Firestore
  with name, year of birth, email and its verified flag, written only by its own signed-in user; real
  `firestore.rules` with emulator tests; the **18+ check** from the year of birth, with a clear message for under-18s.
  Setup becomes: Welcome → Agreement → Sign in and verify email → About you (name, year of birth, language) → Plan →
  Gemini key with billing enabled (D9) → Voice → Permissions → Phone control → All set.
- Profile page: name, email with a "Verified" badge, age band, plan, member since, edit, sign out, **Delete account**
  (removes the Firestore document and the sign-in), plus the deletion request page on the website.
- Done when: rules tests prove a user can read and write only their own document, delete removes everything on the
  server, and an under-18 year of birth cannot get past setup.

**Sprint 7.2: Age-aware replies** (D11)
- Build: the age band goes into the session instructions to set tone, speed and wording (for example slower, fuller
  confirmations for 60+). It never changes which actions FRIDAY will or will not do. Unit tests for the band rules.
- Done when: you hear the difference between two bands on the phone and agree it is respectful.

**Sprint 7.3: Plans and trial in setup** (D3, D10)
- Build: Play Billing Library; the plans you create in Play Console; the Plan step in setup (trial and paid plans, with
  price, period, renewal and how to cancel shown before purchase); purchase verification on the server (Cloud Functions
  + Play Developer API + Real-time developer notifications, Firebase **Blaze** plan); an `entitlements/{uid}` document
  only the server writes; access gated by the entitlement, with an offline grace period; developer access through Play
  licence testers, not a code in the app. The user's own Gemini key (D9) stays on the phone; plans pay for FRIDAY
  only.
- Done when: test purchases, trial start and end, renewals, cancellations and refunds from Play's test tools change
  access correctly, and no secret is in the app.

**Sprint 7.4: Upgrade and manage**
- Build: a Plan page (current plan, renewal date, upgrade or change plan through Play's subscription replacement, a
  "Manage in Google Play" link for cancelling and payment methods), and clear messages when a payment fails or a plan
  ends.
- Done when: an upgrade in test mode changes the plan without a double charge, and a cancelled plan keeps access until
  its end date.

### Phase 8: Accessibility tool readiness

This phase is what decides whether Play accepts FRIDAY. It is product work, not a flag change.

**Sprint 8.1: Access-first product pass**
- Build: `isAccessibilityTool="true"` with an honest service description; spoken feedback after every action and on
  every failure; "what's on my screen" read-out; a mode that keeps listening between commands so the user never has to
  touch the mic button; FRIDAY's own UI checked with Accessibility Scanner and TalkBack (labels, touch targets,
  contrast); store and in-app wording that describes access for people who cannot use the touchscreen (D5).
- Phone check: one full day using the phone without touching it after setup, written up with what failed.
- Done when: the scanner shows no errors on FRIDAY's screens, and the Play accessibility declaration text is drafted
  from what the app really does.

**Sprint 8.2: Safe screen actions** (was 3.6)
- Build: confirmation before irreversible taps (Send, Pay, Delete, Post, Buy, Confirm), detected from the button label
  and the app (messaging, payments, banking, shopping), asked aloud ("Send this to Rahul?"), answered by voice; remove
  the "never ask if they are sure" prompt line (R2); unit tests for labels, apps, Hindi / Hinglish / English yes and no.
- Phone check: "Message Rahul on WhatsApp that I'm late" types it and asks before Send.
- Done when: the tests pass and no tested Send / Pay button is pressed without a spoken yes.

**Sprint 8.3: Everyday jobs, measured**
- Build: a written test list of common jobs done through the screen (call a contact, WhatsApp text, set an alarm in
  Clock, play music, open a route in Maps, take a note, read the latest message), tried on at least three phone brands;
  prompt and tool fixes for the weak ones; the jobs that work reliably listed on the Abilities page, the others not
  claimed.
- Done when: the Abilities page claims only what passed.

### Phase 9: Play launch

**Sprint 9.1: Play Console readiness**
- You: create the Play developer account (D1), pay the fee, verify identity.
- Build / write: store listing (name per D4, short and full description, the 512 px icon from 6.2, 1024×500 feature graphic,
  phone screenshots); privacy policy and terms on `friday.nandivex.in` (or `friday-nandivex.web.app`) rewritten for the
  Play app; **Data safety** form (voice audio and screen content sent to Gemini to perform requests, name and memory on
  device, crash data to Crashlytics, and from Phase 7 the account data: email, year of birth, plan); content rating; target audience;
  **Accessibility API declaration**; **App access instructions** so reviewers can test (a test account with an active plan and a billing-enabled Gemini key for reviewers,
  because without them reviewers cannot use the app and the review can fail); the account deletion web page; a lawyer's read of the terms and privacy policy.
- Done when: every Play Console section shows complete.

**Sprint 9.2: Testing tracks and release pipeline**
- Build: a workflow that builds the signed AAB and uploads to the **internal** track; Play App Signing enrolled with
  the existing release key as the upload key (`android/keystore/nivi-release.jks`, backed up in two places); optional
  Play In-App Updates prompt; Crashlytics alerts.
- Then: internal test → closed test (with real users who need hands-free access) → submit for production review.
- Done when: a build reaches testers from Play and installs over the previous one.

**Sprint 9.3: Production and teardown**
- Staged rollout (for example 10% → 50% → 100%) watching crash-free users; then, with your OK and after a backup,
  delete the AWS resources and the old Firebase project `ultron-5350c` (do not touch `future-self-booth-api` or
  `/age-prediction/hf-token`).
- Done when: the app is live on Play and the AWS bill is ₹0.

### Phase 10: Extras (after launch, you pick)

- **10.1 Quick start:** Quick Settings tile, home-screen widget, wake word "Hey FRIDAY" (on-device, opt-in; engine and its
  licence to be chosen; battery drain measured before it ships).
- **10.2 Routines:** user-defined fixed sequences ("Good night" = Do Not Disturb, low brightness, Wi-Fi off) run without
  the model; a typed command bar.

---

## 8. Waiting on you

1. D1, D3, D5, D8, D10, D11 (section 5).
2. The **Nandivex Technologies logo** file (SVG, or a PNG at least 1024 px wide).
3. Your approval of the design direction (6.1) before any UI code.
4. Play developer account (and a D-U-N-S number if organisation).
5. Final Terms of Use and Privacy Policy text, read by a lawyer (accounts, age and payments change them).
6. Back up the release key `android/keystore/nivi-release.jks` and its passwords in two separate places.
7. Phone checks still open: 3.4, 3.5, the 0.0.20 email fix, TalkBack on setup, a Crashlytics test crash (4.3).
8. Testers who rely on hands-free access, for Sprints 8.1–8.3 and the closed test.
9. Your OK before the AWS / `ultron-5350c` teardown.

No longer needed (website route): GoDaddy DNS records for the download site, Razorpay account and KYC, GitHub
release-key secrets for `release.yml`. The domain is still useful for the privacy policy page.

## 9. Risks

| Risk | What we do |
|---|---|
| **Google does not accept FRIDAY as an accessibility tool** (the biggest risk; Google decides) | Phase 8 makes access the real primary purpose; honest declaration; closed test with real users. If refused, we learn the reason before any public launch; the fallback is a reduced build without model-driven screen control, which is a different product and needs a new decision. |
| Play's policy text changes | Re-read the Accessibility API page before 9.1 and before every major update. |
| Marvel IP complaint (name kept by your decision, D4) | No Marvel material, own logo, trademark search before launch, a fallback name ready. |
| Screen text tricks FRIDAY into pressing Send or Pay | Untrusted-text marking (done) and spoken confirmation (8.2). |
| App layouts change and screen jobs break | 8.3 test list; claim only what works. |
| Reviewers cannot test without an account, a plan and a Gemini key | A reviewer test account with an active plan, and a separate billing-enabled reviewer key with a spending cap (9.1). |
| Gemini key limits, cost or regional availability | Clear errors from the setup check and during use; the user owns the key. |
| Losing the signing key | Play App Signing holds the app key; the upload key can be reset through Play support, but keep two backups anyway. |
| Free-tier Gemini keys break Google's terms for personal data (R9) | D9: billing-enabled key required and explained in setup and the agreement (6.4). The app cannot verify billing from the key, so users may still paste a free key; the agreement makes that their responsibility. |
| Users balk at paying Google and Nandivex | Setup states both costs before the plan step; Google's own pricing page linked; plan prices (D3) set with that in mind. |
| Under-18 users (R10) | Year-of-birth check in setup (7.1); store listing and target audience set to adults (9.1). |
| Accounts add personal data and Play's deletion rule | In-app and web account deletion (7.1); Data safety and privacy policy updated in the same change. |
| "Modern" and "professional" are a matter of taste | Mock-ups approved before code (6.1); 6.5 repeats until you approve each screen. |

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

## 12. Your app review of 2026-11-16 and where each point goes

| # | What you found | Where it is fixed |
|---|---|---|
| 1 | The colour theme is not good; it should be purple | 6.1 (pick), 6.2 (build) |
| 2 | There is no Nandivex Technologies logo | 6.2, with the logo file from you |
| 3 | The FRIDAY logo is poor; it should be purple | 6.1 (concepts), 6.2 (icons) |
| 4 | The font is not good; use the system font | 6.2 |
| 5 | The wording reads as AI-generated | 6.4 (rewrite and style guide) |
| 6 | Sidebar content and structure are poor | 6.3 |
| 7 | Profile needs user details such as age, so FRIDAY can talk according to age | 7.1 (year of birth, 18+ check), 7.2 (age-aware replies) |
| 8 | Email with verification in setup, plan choice with a trial in setup, upgrade later | 7.1 (email), 7.3 (plans and trial), 7.4 (upgrade) |
| 9 | Help & support is not professional; remove "Request a phone control" | 6.4 |
| 10 | The Storage page has no structure | 6.4 |
| 11 | The Usage page has too little analytics | 6.4 (counted on the phone only) |
| 12 | The sidebar buttons are not professional | 6.3 |
| 13 | Overall the UI does not look modern | 6.1–6.5 |

Two of these collide with Google's own rules: age (Gemini is for 18+ only, R10, D11) and free Gemini keys with personal
screen data (R9; decided in D9: the user's own key with billing enabled).
