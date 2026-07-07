# MedCounter — Roadmap to Google Play Launch

_Ordered checklist. Items marked ⚠️ are blockers; everything else can proceed in parallel._

## Phase 0 — Protect the work (do first, ~30 minutes)

- [ ] ⚠️ **Push the app source code to this repo.** Follow the step-by-step instructions in `README.md`. Until this is done the app exists only on one machine.
- [ ] Verify the privacy policy is publicly reachable. Expected URL: `https://robbstacy.github.io/MedCounter/privacy-policy.html`
      If it 404s: GitHub repo → Settings → Pages → Source: "Deploy from a branch" → branch `main`, folder `/docs` → Save, then wait ~2 minutes and retest. The Play data-safety form requires a working public privacy-policy URL.

## Phase 1 — Start the 14-day tester clock (this week)

Google Play requires personal developer accounts created after Nov 2023 to run a **closed test with 12+ testers for 14 continuous days** before production access can be requested. This is the longest lead-time item, so start it before polishing anything.

- [ ] In Play Console: Create app → "Med Counter", App, Free, English (US)
- [ ] Build a signed release **AAB** (Android Studio: Build → Generate Signed Bundle). Enroll in **Play App Signing** (default) — Google holds the release key; keep your upload keystore + passwords backed up somewhere safe (password manager, printed copy — NOT in the repo).
- [ ] Create a **Closed testing** track → upload the AAB → add 12+ testers by email (family, friends; a Google Group makes management easier)
- [ ] Send testers the opt-in link; confirm at least 12 have opted in and installed. The 14-day clock only counts while 12+ testers are enrolled.
- [ ] Complete the required declarations as prompted (see Phase 2 — most are also required for closed testing)

## Phase 2 — Store presence & compliance (during the 14 days)

All draft content is ready in `docs/play-store/` — mostly copy/paste.

- [ ] Store listing: app name, short + full description (`docs/play-store/listing.md`), app icon 512×512, feature graphic 1024×500, 2–8 phone screenshots
- [ ] **Data safety form** — answers in `docs/play-store/data-safety.md` (easy: nothing is collected or shared)
- [ ] Privacy policy URL (from Phase 0)
- [ ] **Content rating questionnaire** — utility/productivity app, no user-generated content → Everyone
- [ ] **Target audience:** 18+ (avoids child-directed requirements; the app manages prescriptions)
- [ ] **Health apps declaration** — Play Console flags medication-related apps. Med Counter is an organizer/reminder, not a medical device: it gives no dosing advice and makes no health claims. Consider adding a short in-app disclaimer ("This app does not provide medical advice…") if not already present.
- [ ] Check target API level meets Play's current requirement for new apps (Play Console will warn if not; as of 2026 that means a recent Android 15+ target — update `targetSdkVersion` if needed)

## Phase 3 — Launch (after the 14-day test)

- [ ] Fix anything testers surfaced; upload new builds to the same closed track as needed
- [ ] Apply for production access in Play Console (becomes available after the 14-day/12-tester bar is met)
- [ ] Promote the tested build to Production → submit for review
- [ ] After approval: verify the live listing, install from the Play Store on a real device, and confirm export + notifications still work on the store-signed build

## Phase 4 — Post-launch backlog (ideas, in rough priority order)

- **Backup/restore** — import from the exported CSVs, or a one-file DB backup/restore. Top user-safety item: today, losing the phone loses the data. Fits the local-only privacy model.
- Refill-reminder notification tuning (e.g., notify at reorder-by date minus pharmacy lead time)
- Dose-taken tracking UI improvements (usage_logs exists but appears lightly used)
- Widgets / at-a-glance "what's due today"
- Tablet layout, dark theme polish, accessibility pass
- F-Droid or GitHub Releases APK as a second distribution channel (privacy-conscious users overlap heavily with F-Droid's audience)

## Reference

- Play Console: https://play.google.com/console (account "Mojito4paz", verified 2026-06-15)
- Closed-testing requirement docs: https://support.google.com/googleplay/android-developer/answer/14151465
- Privacy policy source: `docs/privacy-policy.html`
