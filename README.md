# MedCounter

Privacy-first Android app for tracking medication inventory and refills across multiple profiles. Everything stays on-device: no servers, no analytics, no third-party SDKs.

**Start here:**
- [`PROJECT_STATUS.md`](PROJECT_STATUS.md) — what the app is, what works, where everything lives
- [`ROADMAP.md`](ROADMAP.md) — remaining steps to launch on Google Play
- [`docs/play-store/`](docs/play-store/) — ready-to-paste store listing, data-safety answers, release checklist
- [`docs/privacy-policy.html`](docs/privacy-policy.html) — the app's privacy policy (served via GitHub Pages)
- [`PROJECTS.md`](PROJECTS.md) — status of related/other projects

## ⚠️ Add the app source code to this repo (top priority)

The Android Studio project currently lives only on the development machine. To get it into this repo, run these commands **on that machine**, from the project's root folder (the folder containing `settings.gradle` or `settings.gradle.kts`):

```bash
# 1. Clone this repo next to your project (one-time)
cd ~
git clone https://github.com/robbstacy/MedCounter.git MedCounter-repo

# 2. Copy the Android project into an android/ source folder
#    (adjust "~/AndroidStudioProjects/MedCounter" to your actual project path)
cp -r ~/AndroidStudioProjects/MedCounter MedCounter-repo/android

# 3. Remove build junk and any embedded git folder from the copy
cd MedCounter-repo/android
rm -rf .git .gradle .idea build app/build local.properties

# 4. Commit and push
cd ..
git add android
git commit -m "Add MedCounter Android app source"
git push origin main
```

On Windows, do the same via Android Studio's terminal, or copy the project folder into the cloned repo with File Explorer and delete `.gradle`, `.idea`, `build`, `app\build`, and `local.properties` before committing.

The `.gitignore` in this repo already excludes build output, keystores, and personal data exports — but double-check before pushing that:
- no `*.jks` / `*.keystore` file is staged (`git status` should not show any), and
- no personal data export (`medcounter_*.csv` / `.txt`) is staged.

Keep your **upload keystore and its passwords** backed up separately and privately (password manager, USB drive, printed copy) — never in this repo. If you lose it before enrolling in Play App Signing, you lose the ability to update the app.

## Repo layout

```
android/            ← Android Studio project (once pushed — see above)
docs/               ← GitHub Pages root (privacy policy)
docs/play-store/    ← Play Store launch materials
PROJECT_STATUS.md   ← current state of the project
ROADMAP.md          ← path to launch
```
