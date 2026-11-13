# Ultron production-readiness audit — 2026-11-10

## 1. Executive summary

**Verdict: DO NOT SHIP to the entire installed fleet until A-01 is addressed. This is an incomplete audit, not release certification.** Reviewed checkout `ac2b8f5`; initial working tree contained only untracked `docs/AUDIT_PROMPT.md`. No application code was edited, staged, committed or pushed. Parallel reviewers exhausted their usage allowance before completing all seven lanes. This report preserves verified evidence and explicitly records unfinished work.

Highest verified risks, in order: skipped-version knowledge data loss; replacement keys extending entitlement; unbounded image import; synchronous unbounded skill import; eager image decoding; replacement payments disappearing from revenue. There are six verified findings, not ten; padding the list with unverified claims would violate the evidence requirement.

| Lane | P0 | P1 | P2 | P3 |
|---|---:|---:|---:|---:|
| 1 Security/backend | 0 | 1 | 0 | 0 |
| 2 Android correctness/data | 1 | 0 | 0 | 0 |
| 3 Dead code | 0 | 0 | 0 | 0 |
| 4 UI/UX | 0 | 0 | 1 | 0 |
| 5 Build | 0 | 0 | 0 | 0 |
| 6 Resources | 0 | 2 | 1 | 0 |
| 7 Operability | 0 | 0 | 0 | 0 |

Accepted decisions respected: sideload-only, user-owned Gemini key, hands-free actions without extra confirmation, immediate notification reply, single PLANS source, demo data, and separate showcase repository. Firebase AI Logic and subscription/Razorpay findings from the old architecture review are not assumed to describe current code.

## 2. Baseline results

- `git status --short`: `?? docs/AUDIT_PROMPT.md` before report creation. HEAD `ac2b8f5` (1.23.4), preceding commits include #107/#106 WhatsApp updates and #104 context ring. `git log --oneline -15` executed.
- `python -m unittest discover -s tests/ci -v`: **13 tests, OK**, 0.003s.
- `python -B -m unittest discover -s mcp/tests -v`: **103 tests, OK**, 0.205s. One ResourceWarning about implicitly cleaning an HTTPError in the Resend test; not a failed test.
- Android lint/test command was launched by build reviewer. Its process completion could not be recovered after reviewer interruption. Generated debug lint report dated today: **0 errors, 49 warnings, 2 hints**. Existing test XML totals: **64 suites, 389 tests, 0 failures, 0 errors**. These artifacts are evidence, but not a recovered successful command exit.
- Dependency report `releaseRuntimeClasspath` first 200 lines completed according to reviewer. Full dependency/CVE assessment unfinished.
- `pip list`: imageio-ffmpeg 0.8.1, pillow 13.0.0, pip 26.2.2. `.python-version`: **3.14**. Lambda template runtime: **python3.14**.
- Requested TODO/FIXME/HACK/XXX/suppression/null-assertion search executed across source (generated build/node_modules excluded). Matches mostly permission/deprecation suppressions and test assertions; no defect inferred solely from a match.
- No successful fresh assembleOta/assembleRelease or APK smoke result established.

| Dependency | Observed version | Update/CVE verification |
|---|---|---|
| Kotlin | 2.3.21 | Reviewer inventory; authoritative update review incomplete |
| AGP / Gradle | 8.13.2 / 8.14.6 | Inventory; compatibility/build completion not certified |
| Compose BOM | 2026.04.01 | Transitive CVE review incomplete |
| Firebase BOM | 35.1.0 | Used for remaining Firebase services, not Gemini AI Logic |
| ObjectBox | 5.4.2 | Complete native/transitive review outstanding |
| OkHttp | 4.12.0 | Patch/certificate advisory applicability requires completion |
| boto3 | Lambda-provided | Deployed SDK version not retrieved |
| Python | 3.14 | Local/template match; deployed runtime not inspected |

No blanket “no CVEs” conclusion is supported by this partial audit.

## 3. Findings

### Lane 1 — Security and backend

**S-01 | P1 | CONFIRMED | `mcp/license-admin/lambda_function.py:250` | Reissuing a running key restarts its full purchased term | effort S**

```python
keys, items = _make_keys(1, {**fields, "reissuedFrom": license_id}, str(old.get("plan", "")), int(old.get("durationDays", 0)), now, 0)
```

`_make_keys` line 173 sets `expiresAt: 0`; activation uses durationDays to start the clock. Input: a 30-day key activated 29 days ago, replaced as a lost key. Result: original revoked, replacement has 30 days from its next activation instead of one remaining day. The reviewer reproduced this with the existing in-memory test fake. Tests at `mcp/tests/test_license_admin.py:255` check replacement details but not preservation of running expiry. Smallest fix: preserve existing nonzero expiresAt and original activation semantics; only unused keys get a new activation-relative period. Preserve explicit admin extensions separately.

### Lane 2 — Android correctness and data

**A-01 | P0 | CONFIRMED | `android/app/src/main/java/com/assistant/app/data/RagDocumentStore.kt:218` | Opening the current store deletes legacy documents without migration | effort M**

```kotlin
retiredKeys.forEach { prefs.remove(it) }
```

The list at line 187 includes `documents` and `chunks`; coordinator then opens the ObjectBox source. Input: a phone with knowledge saved before ObjectBox became authoritative, updated directly to current OTA without opening Knowledge on the transitional release. Result: its only persisted documents are removed and the new source is empty. `docs/REMAINING_WORK.md` explicitly acknowledges this skipped-version loss. **This challenges a known decision because it is a data-loss hole**, not a newly discovered decision. Restore idempotent migration with verification before deleting the source; preserve legacy data until successful migration. No phone migration run was performed.

### Lane 3 — Dead code and bloat

No new symbol is proven unused to the required standard. Known wrappers and cleanup helpers are not relabeled as new defects. See section 4.

### Lane 4 — UI/UX and accessibility

**U-01 | P2 | CONFIRMED | `ultronapp/app.js:98` | Reissuing a paid key removes the payment from dashboard revenue | effort S**

```javascript
const paid = licenses.filter((l) => l.state !== "revoked");
```

Reissue revokes the original (`mcp/license-admin/lambda_function.py:252`) and creates replacement amount zero (line 250). Input: one ₹699 sale followed by lost-key replacement, without refund. Result: displayed revenue decreases by ₹699. `test_license_admin.py:235` deliberately asserts replacement amount zero but never evaluates the dashboard aggregation. Smallest fix: distinguish replacement revocation from refund and count the original payment exactly once. Do not simply assign payment to both rows.

Built-in browser inspection: localhost demo dashboard at **375×812** and **1440×900**, mobile New key dialog, desktop Keys table. Layouts rendered; mobile dialog scrolls. Captured warning/error console list was empty. Demo uses fake data and makes no production API calls. Full network/Lighthouse and real authentication tests remain outstanding.

### Lane 5 — Tech stack/build

No final verified new defect. Current workflow waits for reusable checks; old claim that publish always skips tests was dropped. Debug signing alone does not prove signing-key compromise: public passwords are not possession of the private key. Branch protections and deployment trust need live verification.

### Lane 6 — Resource and performance

**R-01 | P1 | CONFIRMED | `android/app/src/main/java/com/assistant/app/viewmodel/AssistantViewModel.kt:438` | Image size limit is applied after allocating the entire input | effort S**

```kotlin
val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
if (bytes == null || bytes.size > MAX_PICKED_IMAGE_BYTES) null else imageLibrary.add(bytes, title, description)
```

Input: picker supplies an image stream larger than available app heap. Result: readBytes exhausts memory before the 25 MiB check; OutOfMemoryError is not caught by catch(Exception). Caller is onSaveImage from the Images page; image tests cover scaling/search, not bounded input. Smallest fix: read at most limit+1 bytes, reject overflow before copying/decoding; treat provider size metadata only as a preliminary check.

**R-02 | P1 | CONFIRMED | `android/app/src/main/java/com/assistant/app/ui/components/SkillsPage.kt:297` | Skill picker performs an unbounded synchronous read on the UI callback | effort S**

```kotlin
val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
```

Input: OpenDocument returns a slow cloud-provider stream, or a very large text/octet-stream file. Result: UI callback blocks for the stream's read duration; large input is fully allocated before parser/body limits. The complete SkillEditor contains no coroutine/IO dispatch for query/read/parse. Smallest fix: bounded IO-dispatched read with visible loading/error state and cancellation. Parser tests do not exercise provider latency or byte limits.

**R-03 | P2 | CONFIRMED | `android/app/src/main/java/com/assistant/app/ui/components/ImagesPage.kt:155` | Images page decodes and retains every thumbnail eagerly | effort M**

```kotlin
images.chunked(2).forEach { row ->
```

PageColumn (`PageParts.kt:47`) is an ordinary vertically scrolling Column. Each PictureCard calls rememberPicture with maxEdge 480. ImageLibrary permits 200 entries. Input: 200 square 1600px images. All 200 cards compose and decode approximately 800px thumbnails (power-of-two sampling), retaining roughly 488 MiB of ARGB pixels before other allocations, instead of only visible rows. Smallest fix: keyed LazyVerticalGrid with visible-item decoding and bounded image cache. Allocation estimate is code-derived; physical-device heap/crash threshold was not measured.

### Lane 7 — Production readiness and operability

Reviewed template retention/encryption/log policies and privacy plan. License table has Retain, PITR and SSE; logs specify 14/30-day retention. Privacy plan explicitly says cloud redaction is not implemented and enumerates data currently sent to Gemini. No deployed alarms, budgets, restore procedure or operational access was verified. Missing verification is not counted as a confirmed production defect.

## 4. Dead code list

No proven dead-code removal candidates. `LegacyScheduleCleanup` is called from Application.onCreate and must not be removed just because its feature is gone. PhoneConnectors/VisitorStrings are already documented cleanup candidates. Tracked-artifact search did not establish committed build output/native binaries/duplicate google-services config among the queried patterns; Product-Manager/PM.md is tracked documentation, not APK bloat. Archive documents are not shipped code. Complete lint UnusedResources triage and reflection/asset reference checks remain outstanding.

## 5. Quick wins

- R-01: enforce image read cap before allocation.
- R-02: bound skill bytes and move provider IO off main.
- S-01: preserve expiry on reissue, with running/expired/lifetime cases.
- U-01: retain revenue for replaced keys, with aggregation regression test.

These are candidate small fixes, not guarantees of under-30-minute completion. Keep entitlement/revenue changes together for review; do not batch them with Android resource fixes. No fixes made.

## 6. Needs-device-test list

- Direct upgrade from a pre-ObjectBox release with saved knowledge (preserve a test backup).
- Cold/warm connection, mute during CONNECTING, reconnect while muted, home/background during warm activation.
- Rapid taps, interruption, phone call, Bluetooth attach/detach, audio focus, Wi-Fi flap, idle and maximum-session timers.
- Permission denial/revocation, notifications disabled when alarms/timers fire, OEM battery restrictions and process death.
- 200-image library, oversized/slow provider imports, low storage during image save.
- TalkBack labels/order, 200% fonts, landscape/foldable/RTL, IME, dark/light themes.
- Real signed OTA install, signer continuity, older/equal version rejection and rollback recovery.

## 7. Not audited / could not verify

The requested full audit is unfinished. Reviewer interruption prevented final independent verification of voice mute/warm-session candidates; those are device-test items, not findings. No full secret-history clearance, live GitHub protections/collaborator inventory, AWS drift/alarms/budgets, deployed Lambda versions, Firebase restrictions, signing-key custody/recovery or production credentials inspection is certified. No real-phone test, successful fresh release/OTA assembly, APK size analysis, full dependency advisory closure, Lighthouse run, production sign-in/API network trace, or legal/payment compliance assessment was completed. The separate showcase repository was excluded as instructed. UI source sampling is not exhaustive screen coverage. A release verdict cannot be upgraded from these partial checks alone.

Only this report was intentionally written. Build/test tooling generates normal ignored outputs. Stop here; fixes require the owner's selected IDs and approval.
