# Changelog

The newest version goes first. The first "## x.y.z" heading below is the app's version: the build, the update
notes and the About page all read it from here. A date after the version is optional.

## 0.0.24 - 2026-11-16

### Changed
- The app is a little smaller: an unused web-page library from the removed web reader is gone. Nothing you can see changes.

## 0.0.23 - 2026-11-16

### Changed
- F.R.I.D.A.Y. will be delivered through Google Play only, so the built-in "Check for updates" and the permission to install apps are gone. Updates will come from Play.
- The phone control screen no longer shows the "Allow restricted settings" help, which is only needed for apps installed from a file.
- The Profile page shows your name and this phone. The activation key card, which did nothing, is removed.
- The About page now says plainly that what you say, and the screen text or picture F.R.I.D.A.Y. reads for a request, goes to Google Gemini with your own API key.

### Removed
- The second "knowledge" API key that the removed Knowledge base used is deleted from phones that still held one.

## 0.0.21 - 2026-11-16

### Added
- The website release can now update itself. When you open F.R.I.D.A.Y. it checks the Nandivex site (at most every 12 hours), and About has a "Check for updates" button. You choose when to download, and Android asks you to confirm the install.
- An update is only installed if it comes from the Nandivex site, matches its checksum, is a newer F.R.I.D.A.Y. and is signed with the same key.
- On Android 13 and newer, the phone control screen now explains how to get past a greyed-out accessibility switch ("Allow restricted settings").

### Changed
- F.R.I.D.A.Y. no longer says it is an accessibility tool for people with disabilities, because it is a general hands-free assistant.
- Removed the permission to draw over other apps, which nothing used.
- Removed access to all files from the app's file sharing, which nothing used.

## 0.0.20 - 2026-11-16

### Fixed
- The contact email in Help, About and bug reports is now contact@nandivex.in. It pointed to a wrong domain before, so messages never arrived.

## 0.0.19 - 2026-11-16

### Added
- A new "What F.R.I.D.A.Y. can do" page in the menu lists everything it can do on your phone, grouped by screen control, navigation, connectivity, sound, display, apps and clipboard. It only shows the list: there is nothing to switch on or off.

### Removed
- Alarms, timers, reminders and "show a notification". To set one, ask F.R.I.D.A.Y. to open the Clock app and use it for you.
- Steps, light sensor, storage and memory, and phone model details. The time and battery are still answered.
- The alarm ringing screen and service, and the permissions they needed (exact alarms, full-screen alerts, run at startup, wake lock, physical activity, data sync).

## 0.0.18 - 2026-11-16

### Changed
- F.R.I.D.A.Y. is now a hands-free phone control assistant. There are no connectors any more: every phone control is built in and always on.
- When you open the app, it asks you to turn on phone control (Android accessibility) and explains what it does. The home screen opens once it is on.
- Wi-Fi name, Bluetooth and step permissions are asked once, after phone control is on.

### Removed
- Calls, text messages, WhatsApp, contacts, calendar, location and directions, notifications, files, app usage, sharing, weather and web search. To do these, ask F.R.I.D.A.Y. to open the app and use it for you.
- The Connectors page, the "Read out notifications" setting and the spoken-yes rule that only the removed tools needed.
- The calling, texting, contacts, calendar, storage, usage and notification-listener permissions.

## 0.0.17 - 2026-11-15

### Added
- Wi-Fi: "turn Wi-Fi off". Bluetooth: "Bluetooth on", and "which Bluetooth devices are paired". Mobile data, airplane mode, hotspot and NFC: F.R.I.D.A.Y. opens the right panel.
- "Dark mode on" and "battery saver on" open the right settings switch.
- Android does not let apps flip these switches, so when Screen control is on, F.R.I.D.A.Y. taps the switch and checks the screen. When it is off, F.R.I.D.A.Y. tells you which switch to tap.
- "SOS flashlight" and "strobe the torch" blink the flashlight for up to 30 seconds. Saying "flashlight off" stops it.

## 0.0.16 - 2026-11-15

### Added
- Phone calls: "answer the call", "end the call", "reject the call", "speaker on", "mute", and "open the dialer with 98765 43210".
- Text messages: "message Rahul on my way" sends an SMS straight away, up to 10 an hour. If SMS is not allowed on the phone, the text opens in your messages app instead. "Draft a text to Rahul" opens it without sending.
- WhatsApp: "WhatsApp Rahul I'm late" opens the chat with the message typed in, and "WhatsApp call Rahul" opens the chat to call. WhatsApp needs a tap on Send or the call icon, or F.R.I.D.A.Y. taps it when Screen control is on.
- New Text messages and WhatsApp connectors, both off until you add them.

### Safety
- If F.R.I.D.A.Y. has just read a notification, a web page, the screen or the clipboard, it will not call, text, WhatsApp, reply or share until you say yes aloud. For example: "Send OK to Rahul?" Any answer other than yes cancels it.

## 0.0.15 - 2026-11-15

### Changed
- The Connectors page is regrouped into Calls & messages, Connectivity, Device controls, Clock, Apps, Info, Personal and Phone control, with a short line under each heading.
- Connectors are smaller and clearer: Wi-Fi, Bluetooth and Mobile & network; Torch, Sound, Display and Media; Apps, Camera and Clipboard & notes; Web search and web pages are now one Web connector.
- A new "Connect all essentials" button adds every everyday connector in one tap, and asks once for the Android permissions they need.
- Every command works as before. If you had a connector on, its tools stay on.

## 0.0.14 - 2026-11-15

### Changed
- Every screen, notification and help text now says F.R.I.D.A.Y. instead of the old name. The notification channel, the ongoing "Live with F.R.I.D.A.Y." notification and the update notice are renamed too.
- The home ring is gone: the voice wave alone is shown, as before.
- Android's assistant settings now list the app under its new name.

## 0.0.13 - 2026-11-15

### Fixed
- The home ring is smaller and centred on the voice wave, so it no longer cuts through the greeting or the status line. Its amber dot is brighter.

## 0.0.12 - 2026-11-15

### Changed
- New look: a deep navy and cyan theme with an amber highlight replaces the red one, in both light and dark mode.
- New F.R.I.D.A.Y. logo: a ring of three broken arcs around an F whose middle bar is a voice waveform. It is the app icon (including the Android 13 themed icon), the splash screen and the icon in About.
- The home screen shows a slowly turning ring that brightens and speeds up with your voice.
- The name is written F.R.I.D.A.Y. on screen.

## 0.0.11 - 2026-11-15

### Changed
- FRIDAY now has one personality: calm, warm, quick and quietly witty. It confirms commands in one short sentence and calls you "Boss" until you set a name in Settings.
- Settings has a new Reply style choice: Short (default) or Detailed. The four old personalities and the "change personality" voice command are gone.
- The default voice is now Kore, a female voice. Change it any time in Settings.
- Asked who it is, FRIDAY says it is your phone assistant developed by Nandivex Technologies.
- About shows "Developed by Nandivex Technologies" and the support email; personal names are removed.
- New greetings on the home screen.

## 0.0.10 - 2026-11-15

### Added
- Test builds only: a long press on the version number in About closes the app on purpose, so crash reports can be checked.

## 0.0.9 - 2026-11-15

### Fixed
- The History page stayed empty and conversations were not saved on phones that had an earlier test build installed. FRIDAY now starts a fresh history store when the old one cannot be opened.

## 0.0.8 - 2026-11-15

### Removed
- The Knowledge base (documents and PDFs), the Images page and the show-picture command. FRIDAY no longer reads your own documents; photos can still be opened through Files.
- The tools that searched past conversations. The History page stays and still shows what you and FRIDAY said before.
- The separate Knowledge API key and the search-sections setting in Settings. The app is smaller and starts faster without them.
- Old cloud servers, their tests and website are gone from the project.

## 0.0.7 - 2026-11-15

### Changed
- Web search and web pages now work straight from your phone, with no server in between.
- The app has no activation key at all: every build opens straight to FRIDAY. Accounts and the free trial come in a later version.
- Feature requests and bug reports now open your email app with the message filled in. Support email is support@nandivex.com.

### Removed
- The YouTube connectors (search, video details, captions, playlists and downloads) and the Downloads page.
- The built-in update check and installer. Testers get new builds through Firebase App Distribution.

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

