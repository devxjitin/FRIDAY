# Ultron: full-repo audit prompt

Paste everything below the line into a fresh session (or run it as a Workflow / parallel subagents, one per lane).
It is written for this repo, not generic: it names the real components, the real risks, and the real commands.

---

## Role

You are a principal engineer doing a pre-release production-readiness audit of **Ultron** at `X:\KIRA`: an Android voice
assistant (Kotlin 2.0.20, Jetpack Compose, Gemini Live over Firebase AI Logic, ObjectBox, MCP connectors) with an AWS
backend (API Gateway + Lambda + DynamoDB + S3, Python), an admin web app (`ultronapp/`, vanilla JS), marketing sites
(`sites/`), PowerShell/Python release tooling (`scripts/`), and GitHub Actions CI/OTA publishing. It is distributed
**sideload-only via OTA APK**, with SMS, calls, accessibility, overlay, and all-files access. A bad release goes straight
to every phone, so the bar is "would I be comfortable if this auto-updated onto my own phone tonight".

## Ground rules

1. **Read-only first.** Do not edit, commit, or push during the audit. Output a report. Fix only after I approve.
2. **Evidence or it did not happen.** Every finding cites `path:line`, quotes the offending code in 1-3 lines, and gives
   a concrete failure scenario (input / state -> wrong result). No "might", no generic best-practice lectures.
3. **Verify before reporting.** For each candidate, grep for callers, read the surrounding code, check tests and docs.
   Drop anything you cannot defend. Mark remaining uncertainty as `PLAUSIBLE`, otherwise `CONFIRMED`.
4. **Do not re-report known decisions.** Read `docs/REMAINING_WORK.md` ("non-issue" list), `docs/ARCHITECTURE.md` section 20,
   and the memory notes first. Known/accepted: sideload only (no Play Store), own Gemini key only, owner wants hands-free
   control with no extra confirm taps, `REPLY_NOTIFICATION` sends immediately, `PLANS` lives only in `ultronapp/app.js`,
   `demo.js` is fake data, `sites/ultron-showcase/` is a separate repo. Challenge a decision only if it creates a
   security or data-loss hole, and say so explicitly.
5. **Run the real tooling** (below) and fold results in; do not just eyeball.
6. Respect that other sessions edit `X:\KIRA` concurrently. Never `git add -A`; never touch files outside the audit report.
7. Use `rg`/Grep and parallel tool calls; read whole files for the large ones. Scale with parallel subagents, one per lane.

## Baseline commands (run first, record output)

```bash
cd /x/KIRA
git status --short && git log --oneline -15
cd android && ./gradlew.bat :app:lintDebug :app:testDebugUnitTest --console=plain   # lint + unit tests
./gradlew.bat :app:dependencies --configuration releaseRuntimeClasspath | head -200   # versions to review
cd .. && python -m unittest discover -s tests/ci -v
python -m pip list 2>/dev/null | head -50; cat .python-version
```
Also: check for dependency updates / known CVEs (OkHttp, Firebase BoM, ObjectBox, Compose BOM, Kotlin, AGP, boto3 /
Python runtime version in `infra/*.yaml`), and `rg -n "TODO|FIXME|HACK|XXX|@Suppress|!!\." android mcp scripts ultronapp`.

## Audit lanes (each lane = one report section)

### 1. Security and secrets (highest priority)
- Secrets in git history and working tree: `git log -p -S` for keys/tokens; the tracked `ApiKeyStore.kt` (confirm it holds
  no key, only storage code); `google-services.json` at repo root and in `android/app/` (public config vs real secret);
  `.mcp.json` (gitignored but check history); `mcp/**/.access_token`; `android/keystore/` contents tracked or ignored;
  `local.properties`; hardcoded URLs/ARNs/account IDs in `infra/*.yaml`, scripts, workflows.
- Release signing: `ota` build type signs with the **debug key** (`android` / `androiddebugkey` passwords in Gradle).
  Assess blast radius: who can push to `main`, branch protection, what `publish-ota.yml` trusts, where the key lives,
  supply-chain path from a compromised GitHub account to a malicious APK on every phone. Verify APK integrity checks
  in `UpdateManager` (hash/signature check before install? HTTPS pinning? downgrade protection? versionCode rules).
- AndroidManifest: every permission justified and used (`MANAGE_EXTERNAL_STORAGE`, `WRITE_SETTINGS`, `SYSTEM_ALERT_WINDOW`,
  `REQUEST_INSTALL_PACKAGES`, `PACKAGE_USAGE_STATS`, location, calendar, contacts, call). Flag unused ones. Check
  `exported` components, intent filters, deep links, `PendingIntent` mutability flags, `allowBackup`, cleartext traffic,
  network security config, FileProvider paths, broadcast receivers registered without permission.
- Prompt-injection and tool abuse: tool results (web extract, YouTube, notifications, SMS, screen content via the
  accessibility service, knowledge documents) flow back into Gemini, which can call 80+ tools (`DeviceTool.kt`,
  `DeviceToolRunner.kt`, `ToolDispatcher`). Trace whether untrusted text can trigger SMS/calls/installs/file deletion/
  shell-like actions. Check argument validation, path traversal in file tools, intent injection, URL scheme allow-lists.
- Backend: every Lambda in `mcp/*` and `infra/*.yaml`: authN/authZ per route (JWT authorizer coverage, admin routes,
  license routes), IDOR, input validation, SSRF in `web-extract` (private IP / metadata endpoint / redirect handling /
  DNS rebinding), CORS (`AdminOrigins`), IAM least privilege (wildcards), S3 public access, DynamoDB conditional writes,
  Resend/SSM secret handling, rate limiting and abuse/cost caps, logging of PII or tokens, error leakage.
- Web: `ultronapp/` and `sites/`: XSS (`innerHTML`, template strings with data), token storage (localStorage), CSP,
  admin auth flow, exposed API keys, third-party scripts without SRI.
- Privacy: what leaves the device (audio, screen text, contacts, notifications, memory) and where; consistency with
  `docs/PRIVACY_PLAN.md` and any privacy policy text in the app/site; encrypted-at-rest for memory/knowledge/keys
  (Keystore, EncryptedSharedPreferences); Crashlytics PII; log statements printing secrets (`Log.*` with keys/tokens/PII).

### 2. Correctness and bugs (Android)
- Voice pipeline: `VoiceSessionController`, `GeminiLiveClient`, `AudioPlayer`, `AudioRouting`, `SessionTimers`,
  `SpeechDetector`: race conditions, state machine holes (cold start, rapid tap, interrupt, mute, phone call mid-session,
  Bluetooth connect/disconnect, network flap, 2-min idle / 15-min cap), reconnect/backoff, resource release on every exit
  path (AudioRecord/AudioTrack, WebSocket, wake locks, foreground service), coroutine scope/cancellation, `GlobalScope`,
  `runBlocking` on main, swallowed `CancellationException`, unbounded buffers/queues.
- Lifecycle: `MainActivity`, `AppForegroundService` (type declarations vs Android 14/15 rules), process death, config change,
  `SavedStateHandle`, boot receiver, exact alarms (`AutomationEngine`, schedule), WorkManager constraints, doze.
- Data: ObjectBox entities/migrations (`objectbox-models`), `KnowledgeManager`/`RagDocumentStore` embeddings batch + dimension
  mismatch, `MemoryStore` growth, DataStore migration paths, backup/restore, destructive migrations, transactional
  integrity, large-doc import, locale/encoding.
- Tool layer: `DeviceToolRunner.kt` (1.5k lines): null/permission failures, API-level guards (minSdk 26, targetSdk 35,
  compileSdk 36), OEM differences, accessibility node recycling and leaks, intents that may not resolve
  (`ActivityNotFoundException`), blocking IO on main thread, `YouTubeDownloader` (ToS, error handling, temp-file cleanup,
  storage quota), WhatsApp skill tasks in `assets/skills/whatsapp/tasks` (stale selectors, i18n, failure modes).
- Billing/licensing client: activation, token refresh, offline grace, clock tampering, what happens when the subscription
  check fails open vs closed.
- Kotlin hygiene: `!!`, unchecked casts, platform types, `lateinit` crashes, exhaustive `when`, `Regex` in hot paths,
  `SimpleDateFormat` thread-safety, locale-dependent `toLowerCase`/format, timezone bugs.

### 3. Dead code and bloat
- Unused classes/functions/resources/strings/drawables/permissions/dependencies/ProGuard rules/Gradle flags
  (use lint `UnusedResources`, `rg` for each public symbol, check reflection/manifest/`assets` references before calling it dead).
- Leftover legacy: `LegacyScheduleCleanup`, `PhoneConnectors`/`VisitorStrings` wrappers, `docs/archive/`,
  `docs/LEGACY_URL_RETIREMENT.md`, `phone_ss.png` and `.review/` in the repo root, committed build output (`android/build`,
  `*/dist`, `__pycache__`, native `.so`/`.dll` in `android/app`), duplicate `google-services.json`, `Product-Manager/`.
- Duplicated logic across `ui/components/*`, tool handlers, Python Lambdas (shared helpers copy-pasted?).
- Docs drift: README / `ARCHITECTURE.md` / `track.md` (66 KB) claims vs code (tool count "80+", versions, flows).
  Mixed-language docs (Hinglish): flag if any is user-facing.

### 4. UI/UX and accessibility (Compose + web)
- Screens: Home/Assistant, Settings dialog (786 lines), Connectors, Skills, Images, Usage, Memory, Knowledge, About, Update,
  activation. Check: recomposition hot spots (unstable params, lambdas allocated in loops, missing `key`, heavy work in
  composition, `remember` misuse, `collectAsState` vs lifecycle-aware), state hoisting, back-handling, IME/insets/edge-to-edge,
  dark theme, font scaling 200%, small/foldable/landscape, RTL, long strings, empty/loading/error states, offline states.
- Accessibility: contentDescription on every icon button, 48dp touch targets, contrast (WCAG AA), TalkBack order, focus.
  Note the app itself ships an AccessibilityService; check its config XML scope (`res/xml`) for over-broad event/flag use.
- Strings: hardcoded text instead of resources, inconsistent wording, mixed languages, placeholder/dev text visible to users.
- Web (`ultronapp`, `sites`): responsive layout, keyboard navigation, contrast, console errors, Lighthouse (performance,
  a11y, SEO, best-practices), broken links, missing meta/OG tags, image weight. Open in the built-in browser and check
  at mobile (375) and desktop widths; read console and network.

### 5. Tech stack and build health
- Versions: Kotlin/AGP/Gradle/Compose BOM/Firebase BoM/OkHttp/ObjectBox/ML or audio libs: outdated, mismatched, alpha deps,
  duplicate libs, `kapt` -> KSP migration, version catalog absent, deprecated APIs, `targetSdk 35` vs Play-irrelevant but
  Android 16 behavior changes (edge-to-edge, predictive back, FGS timeouts).
- R8/ProGuard: rules too broad (`-keep` everything) or too narrow (reflection, Firebase, ObjectBox, serialization); verify
  the `ota` and `release` builds actually run (`assembleOta` / `assembleRelease` + smoke), `isShrinkResources`, mapping upload.
- `versionCode = minutes since epoch` and version name parsed from `changelog.md`: failure modes (regex miss -> "1.0.0",
  two builds in the same minute, int overflow date, CI vs local differences).
- Backend runtime/versions (Python Lambda runtime EOL, boto3 pinning, cold-start size, `requirements`), CloudFormation drift,
  deletion policies, alarms, log retention, budgets.
- CI/CD (`checks.yml`, `publish-ota.yml`, `scripts/*`): third-party actions pinned to SHA?, `permissions:` minimal?, secrets
  scoped, fork-PR safety, `pull_request_target` misuse, caching, flaky/slow steps, concurrency vs the "merge one at a time"
  rule, rollback path for a bad OTA, staged rollout absent?, test coverage gates, Windows-only assumptions in `.ps1`.
- Tests: what is untested and risky (controllers with real mic/Gemini have none); flaky/ignored tests; assertions that
  cannot fail; test-only code in `main`.

### 6. Resource and performance
- Battery/CPU/memory: foreground service lifetime, wake locks, wakelocks without timeouts, polling loops, audio buffer sizes
  and allocations per frame, Bitmap/ImagePage memory (`ImagesPage`), large file reads into memory, ObjectBox query cost,
  embedding batches, leaks (Context/Activity in singletons, listeners not unregistered, Compose captured scopes).
- Network: OkHttp timeouts/retries, WebSocket keepalive, payload sizes, redundant calls at cold start, parsing on main.
- Startup: cold-start work (Application.onCreate, DI init, baseline profile validity - currently a wildcard estimate),
  APK/AAB size breakdown (`apkanalyzer`), native libs per ABI, unused assets (skills, images, fonts).
- Cost: per-user Gemini/embedding/Lambda/DynamoDB/S3/data-transfer cost drivers, unbounded growth (logs, S3 objects, tables),
  missing TTLs, abuse scenarios that run up the AWS or Gemini bill.

### 7. Production readiness and operability
- Crash/ANR handling and Crashlytics coverage, user-visible errors vs silent failures, structured logging, log levels
  (`Log.i/d` noise noted in REMAINING_WORK), PII in logs, feature flags / kill switch, remote config, force-update path,
  backward-compatible API versions, data export/deletion (privacy), runbooks, alarms, backups of DynamoDB/S3, secret
  rotation procedure, disaster recovery for the signing key (losing it bricks OTA for every phone), `.gitignore` gaps.
- Legal/compliance for sideload distribution: permission disclosure, privacy policy accuracy, ToS issues from
  `YouTubeDownloader` and WhatsApp automation, Razorpay/payment handling, GST/refund text.

## Severity scale

- **P0** security hole, data loss, or a path that bricks/compromises phones or leaks secrets. Fix before next release.
- **P1** user-visible crash/bug in a main flow, money bug, or serious reliability/resource problem.
- **P2** correctness edge cases, perf, maintainability that will bite soon.
- **P3** polish, dead code, naming, docs.

## Output format

Write the report to `X:\KIRA\docs\AUDIT_REPORT_<yyyy-mm-dd>.md` (only that file), structured as:

1. **Executive summary**: verdict (ship / ship with fixes / do not ship), top 10 risks, counts by severity per lane.
2. **Baseline results**: lint/test/CI outputs, dependency and CVE table.
3. **Findings**, grouped by lane, sorted P0 -> P3. Each:
   `ID | Severity | CONFIRMED/PLAUSIBLE | path:line | one-sentence defect | failure scenario | suggested fix (smallest change) | effort S/M/L`
4. **Dead code list**: symbol, path, how you proved it unused.
5. **Quick wins**: fixes under 30 minutes each, safe to batch.
6. **Needs-device-test list**: things only a phone can verify (mic, Bluetooth, calls, OEM quirks).
7. **Not audited / could not verify**: be explicit.

Then stop and wait. Do not fix anything until I pick which IDs to fix. When fixing later: one PR per theme, bump
`changelog.md` version (CI requires it), stage only your own files, merge one PR at a time and wait for Publish OTA.
