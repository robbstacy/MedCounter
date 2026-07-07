# MedCounter — Project Status

_Last updated: 2026-07-07. Written so that Robb, a future developer, or a future AI session can pick this project up cold._

## What it is

**Med Counter** is a privacy-first Android app for tracking medication inventory and refills across multiple people (profiles). It answers the questions "how many pills are left?", "when do we run out?", and "when do I need to reorder?" — built and actively used to manage real family medication schedules.

- **Developer:** Robert Stacy (`rstacy@gmail.com`)
- **Google Play developer account:** "Mojito4paz" — identity verified 2026-06-15
- **Privacy model:** 100% local-only. No servers, no cloud sync, no analytics, no third-party SDKs, no internet permission required. See `docs/privacy-policy.html` (effective 2026-06-09).

## Current state: WORKING, pre-release

The app is feature-complete enough for daily real-world use (in use since at least 2026-06-08, managing 16 medications across 2 profiles). It has **not yet been published** to Google Play.

### Confirmed working features (evidenced by the 2026-06-16 data export)

- **Multiple profiles** — track medications for several people from one device
- **Medication records** — name, strength + unit, form (tablet, etc.), pharmacy, Rx number, refills remaining, notes
- **Dosing schedules** — `DAILY`, `BID` (2×/day), `TID` (3×/day), `PRN` (as-needed), with named dose times (Morning, Midday, Bedtime, custom times like 4:00 PM) and units-per-dose
- **Inventory math** — quantity on hand (with as-of date), days remaining, projected run-out date, and a reorder-by date computed from a configurable lead time (default 7 days)
- **Restock events** — log added quantities with date and notes
- **Usage logs** — per-medication usage entries
- **Audit log** — every change recorded: action type (`MED_CREATED`, `MED_EDITED`, `RESTOCK_ADDED`, …), quantity before/after/delta, expected quantity, actor
- **Export all data** — writes a human-readable summary (`.txt`) plus 4 CSVs (medications, restock_events, usage_logs, audit_log) to cache and opens the Android share sheet
- **Local notifications** — refill and dose reminders (per the privacy policy)

### Data model (CSV export schema — mirrors the local DB)

| Export file | Columns |
|---|---|
| `medications` | id, profile_name, name, strength, strength_unit, form, schedule_type, units_per_dose, quantity_on_hand, quantity_as_of, days_remaining, runs_out, lead_time_days, refills_remaining, pharmacy, rx_number, notes |
| `restock_events` | id, medication_name, profile_name, date, quantity_added, notes |
| `usage_logs` | id, medication_name, profile_name, date, quantity_used |
| `audit_log` | id, date, profile_name, medication_name, action, quantity_before, quantity_after, quantity_delta, expected_quantity, note, actor |

## Where everything lives

| Asset | Location | Status |
|---|---|---|
| **App source code** | Robb's development machine (Android Studio) only | ⚠️ **NOT in version control — single point of failure. See README for push instructions.** |
| Privacy policy | `docs/privacy-policy.html` in this repo | ✅ committed; verify the public GitHub Pages URL is live (see ROADMAP) |
| Play Console account | play.google.com/console, account "Mojito4paz" | ✅ identity verified 2026-06-15; app listing not yet created/completed |
| Real usage data | On-device DB; sample export from 2026-06-16 in Google Drive | Private — contains family medication data; never commit it to this repo |
| This repo | github.com/robbstacy/MedCounter | Docs only until source is pushed |

## Biggest risks / gaps

1. **Source code exists only on one machine.** One disk failure loses the app. Pushing it to this repo is the top priority (README has step-by-step instructions).
2. **Google Play's 12-tester requirement** — personal developer accounts created after Nov 2023 must run a closed test with at least 12 testers for 14 continuous days before they can apply for production access. This account was verified June 2026, so the requirement almost certainly applies and is the long pole in the launch schedule. Start the closed test ASAP (see ROADMAP).
3. No known automated tests or CI (unverifiable until source is committed).

## Related documents

- `ROADMAP.md` — exact remaining steps to ship on Google Play
- `docs/play-store/` — store listing copy, data-safety form answers, release checklist
- `PROJECTS.md` — status of Robb's other projects (Replit apps, 3D printing, etc.)
