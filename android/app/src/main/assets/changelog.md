# Changelog

The newest version goes first. The first "## x.y.z" heading below is the app's version: the build, the update
notes and the About page all read it from here. A date after the version is optional.

## 0.0.6 - 2026-11-14

### Changed
- Tester and debug builds no longer ask for an activation key: they open straight to FRIDAY. Accounts and the free trial arrive in a later version.

## 0.0.5 - 2026-11-14

### Fixed
- Build tooling only: tester builds are uploaded with a keyless login that does not expire. No change to how the app works.

## 0.0.4 - 2026-11-14

### Removed
- Skills are gone: the Skills page, the skill tool and the "Request a skill" form. Common asks will be handled by direct commands instead.
- The Gemini Decisions checks are gone, along with their five switches in Settings (smarter document search, memory tidying, screen action checks, screen check after each step and skill matching). Voice commands no longer wait for an extra check before they run.

### Changed
- Asking what FRIDAY can do now answers from the abilities it has, without loading a skill first.

## 0.0.3 - 2026-11-14

### Fixed
- Build tooling only: tester builds are now uploaded with a Firebase token. No change to how the app works.

## 0.0.2 - 2026-11-13

### Fixed
- Build tooling only: the tester build can now be built by the automatic checks. No change to how the app works.

## 0.0.1 - 2026-11-13

### Changed
- FRIDAY starts here. This is the first tester build, with the new package name (com.nandivex.friday). Testers receive builds through Firebase App Distribution: install "FRIDAY Tester" from the invite email. It can sit on the same phone next to the debug build ("FRIDAY Dev").

