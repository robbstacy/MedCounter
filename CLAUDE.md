# CLAUDE.md

When I correct you or you catch yourself making a mistake, before continuing, add the lesson as a one-line rule under #LESSONS so it never happens again.

# LESSONS
- Never commit real medication data (the `medcounter_*` exports or anything naming family members' prescriptions) to this repo — use demo data for screenshots, examples, and tests.
- Never commit keystores (`*.jks`, `*.keystore`) or their passwords; they are covered by .gitignore but check `git status` before pushing.
- Any feature suggestion must respect the app's core promise: 100% local, no network, no accounts, no third-party SDKs. Reject cloud-sync-style ideas unless Robb explicitly changes that policy.

# PROJECT CONTEXT
- Med Counter is a local-only Android medication inventory/refill tracker; see PROJECT_STATUS.md for the full picture and ROADMAP.md for the launch plan.
- The Android Studio source lives in `android/` once pushed (README has onboarding instructions); until then it exists only on Robb's dev machine.
- `docs/` is the GitHub Pages root; the privacy policy URL there is referenced by the Play Store listing — don't move or rename `docs/privacy-policy.html` without updating Play Console.
- Google Play developer account: "Mojito4paz" (identity verified 2026-06-15). New-personal-account rule applies: 12+ testers for 14 days on a closed track before production.
- Robb's other projects (Replit apps, 3D printing, workstation build) are catalogued in PROJECTS.md.
