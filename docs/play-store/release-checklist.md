# Release Checklist — first Google Play submission

Work top to bottom. `ROADMAP.md` has the phased plan; this is the flat pre-flight list.

## Build

- [ ] `versionCode 1`, `versionName "1.0"` (or current values) set in the app module's Gradle file
- [ ] `targetSdkVersion` meets Play's current requirement for new apps (Play Console shows an error at upload if not)
- [ ] Release build minified/shrunk if using R8/ProGuard — verify export, notifications, and DB access still work in the **release** build, not just debug
- [ ] Generate signed **AAB**: Android Studio → Build → Generate Signed Bundle → create upload keystore if you don't have one
- [ ] **Back up the upload keystore + passwords** (password manager / USB / printed — never in the repo)
- [ ] Install the release build on a real device (`bundletool` or internal testing track) and smoke-test:
  - [ ] add profile, add medication, restock, log usage
  - [ ] days-left / run-out / reorder-by math correct
  - [ ] reminders fire (test with a near-future time; check exact-alarm/notification permissions on Android 13+)
  - [ ] export produces the summary + 4 CSVs and the share sheet opens
  - [ ] privacy policy link opens in browser

## Play Console — one-time account/app setup

- [x] Developer account created and identity verified (done 2026-06-15, account "Mojito4paz")
- [ ] Create app: "Med Counter", App (not game), Free, default language en-US
- [ ] Enroll in Play App Signing (default when uploading first AAB)

## Play Console — App content (all required before any release)

- [ ] Privacy policy URL (live — see ROADMAP Phase 0)
- [ ] Data safety form (`data-safety.md`)
- [ ] Content rating questionnaire (`data-safety.md` → Related forms)
- [ ] Target audience: 18+
- [ ] Health apps declaration
- [ ] Ads declaration: none
- [ ] App access: "All functionality is available without special access" (no login)

## Store listing

- [ ] Name, short description, full description (`listing.md`)
- [ ] Icon 512×512, feature graphic 1024×500, 2–8 screenshots with demo data (`listing.md` → Graphics)
- [ ] Category: Medical; contact email set

## Testing gate (required for new personal accounts)

- [ ] Closed testing track created, AAB uploaded
- [ ] 12+ testers added and opted in
- [ ] 14 continuous days elapsed with 12+ testers enrolled
- [ ] Apply for production access

## Launch

- [ ] Promote build to Production, submit for review
- [ ] After approval: install from the Play Store, re-run the smoke test above
- [ ] Tag the release in git: `git tag v1.0 && git push --tags`
