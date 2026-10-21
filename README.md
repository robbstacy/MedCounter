# MedCounter

Privacy-first Android app for tracking medication inventory and refills across multiple profiles. Everything stays on-device: no servers, no analytics, no third-party SDKs.

**Start here:**
- [`PROJECT_STATUS.md`](PROJECT_STATUS.md) — what the app is, what works, where everything lives
- [`ROADMAP.md`](ROADMAP.md) — remaining steps to launch on Google Play
- [`docs/play-store/`](docs/play-store/) — ready-to-paste store listing, data-safety answers, release checklist
- [`docs/privacy-policy.html`](docs/privacy-policy.html) — the app's privacy policy (served via GitHub Pages)
- [`PROJECTS.md`](PROJECTS.md) — status of related/other projects

## App source

The Android Studio project lives in [`android/`](android/). Open that folder in Android Studio (namespace `com.medcounter`, application id `com.pillstock.app`, debug suffix `.debug`).

```bash
cd android
./gradlew assembleDebug testDebugUnitTest
```

Debug builds use the standard debug keystore. Release signing is wired only through environment variables or Gradle properties (`PILLSTOCK_KEYSTORE`, `PILLSTOCK_KEYSTORE_PASSWORD`, `PILLSTOCK_KEY_ALIAS`, `PILLSTOCK_KEY_PASSWORD`) — do not commit a keystore.

The original debug keystore that signed the phone build is not in this repo. A debug APK built here will not install as an update over that copy; Android will require an uninstall first, which drops the on-device database. Room schema v5 is kept compatible so a build signed with the original debug key (or a future Play-signed release, after a data migration plan) can open `medcounter.db` unchanged.

The `.gitignore` already excludes build output, keystores, and personal data exports. Before pushing, confirm `git status` shows no `*.jks` / `*.keystore` and no `medcounter_*.csv` / `.txt`.

Keep the **upload keystore and its passwords** backed up separately and privately — never in this repo.

## Repo layout

```
android/            ← Android Studio project (Kotlin, Compose, Room)
docs/               ← GitHub Pages root (privacy policy)
docs/play-store/    ← Play Store launch materials
PROJECT_STATUS.md   ← current state of the project
ROADMAP.md          ← path to launch
```
