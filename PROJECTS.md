# Robb's Projects — Portfolio Snapshot (2026-07-07)

Where everything lives and what each project needs next. MedCounter (this repo) is the flagship — see `PROJECT_STATUS.md` / `ROADMAP.md`. The projects below are catalogued here so nothing gets lost.

## Replit apps

These live in the Replit account **@RobbStacy** and are built/iterated by Replit's own agent, so they remain fully editable without any other AI assistant. Deploying/publishing happens from the Replit site.

| App | Last touched | Notes / next step |
|---|---|---|
| **Sous Vide Companion** | 2026-07-07 | Active. Next step when ready: deploy from Replit to get a shareable URL. |
| **Puro Journal** | 2026-07-07 | Active (cigar journal — an idea dating back to a 2024 Android sketch). Same: deploy from Replit when ready. |
| **day-1-printing-start** | 2026-05-06 | Older 3D-printing starter project; superseded by the Drive-based design work below. Archive or delete if unused. |

## PuroVida (GitHub)

Cigar journal app in `robbstacy/PuroVida` — the grown-up sibling of the Puro Journal Replit sketch.

- `main` — Flutter implementation (current app), with the original Expo/React Native version preserved on the `react-native` branch.
- `claude/native-android-rebuild` — native Kotlin/Jetpack Compose rebuild in `native-android/` (2026-07-08); engine unit tests pass, needs a first build/run in Android Studio.
- Optional AI backend (`server/api.js`, OpenAI-powered lookup + photo identify) is shared by both implementations; everything else is fully local on-device.

## 3D printing (Google Drive)

- **D3P folder** (`Drive > D3P`) — 3D-printing project home.
- **laptop-stand** (`D3P > laptop-stand`) — parametric OpenSCAD design (`laptop_stand.scad`) with exported `laptop_stand.stl` and ~12 design-iteration preview renders (organic/fin/facet variants). The `.scad` file is the source of truth — parameters can be edited in OpenSCAD and re-exported without any AI help.
- Next step: print the chosen variant; keep the `.scad` files for every future design (they're the editable "source code" of a print).

## AI workstation build

- **Parts list for quote**: `Drive > AI-Workstation-Parts-List.pdf` (2026-06-30) — Core Ultra 9 285K, Z890, 2× RTX 5060 Ti 16GB, 64GB DDR5, 8TB NVMe total, 1500W PSU, Lancool 216, Windows 11 Pro, with build instructions for the shop.
- Status: ready to send to a PC shop for a quote. Dual 16GB GPUs = 32GB VRAM for local LLM inference.

## CloudBrain

- `Drive > CloudBrain` folder (created 2026-06-17). Contents not catalogued here — worth a look to decide whether it's active or can be archived.

## Household data note

Med Counter's own export (summary + CSVs from 2026-06-16) is in Drive and contains the family's real medication schedules — treat as private; useful as a restore reference if the phone is ever lost before a backup/restore feature ships.
