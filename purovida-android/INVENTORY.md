# PuroVida — Source Inventory (Step 0)

Inventory of the source app used for this native Kotlin rebuild. Source of
truth: `robbstacy/PuroVida` @ `f4ae7b5` (Flutter implementation, itself a
rewrite of the original Expo/React Native app preserved on that repo's
`react-native` branch). ~6,300 lines of Dart + a small Node.js AI backend.

## What the app is

A cigar journal for enthusiasts. All journal/humidor/profile data is stored
locally on the device (no accounts). Two optional AI features call a
self-hosted Node backend (`server/api.js`, OpenAI-powered); everything else
works fully offline and the AI features degrade gracefully.

## Screens (11)

| Screen | Source file | Notes |
|---|---|---|
| Main shell | `main.dart` | 5-tab bottom bar: Home, Journal, Humidor, Discover, Profile |
| Home | `home_screen.dart` | Greeting, 6 stat cards, quick actions, horizontal recommendations, recent entries |
| Journal | `journal_screen.dart` | Search, strength filter chips, 4 sort options, entry cards |
| Journal add | `journal_add_screen.dart` | Full log form: chips pickers, flavor taxonomy picker, 4 star ratings, AI Fill (lookup) |
| Journal detail | `journal_detail_screen.dart` | Read-only detail cards + delete w/ confirm dialog |
| Humidor | `humidor_screen.dart` | Stats (count/value/ready), item cards with qty +/- and aging progress |
| Humidor add | `humidor_add_screen.dart` | Form + "Scan a Cigar with AI" (camera/gallery → identify) + AI Fill |
| Humidor scan | `humidor_scan_screen.dart` | Batch scan: multi-photo → identify each → review/edit → Save All |
| Discover | `discover_screen.dart` | "For You" (10 recommendations w/ reasons) / "Catalog" tabs, search + strength filter |
| Profile | `profile_screen.dart` | Avatar, stats, flavor profile, strength/brand bar charts, preferences |
| Profile edit | `profile_edit_screen.dart` | Avatar picker (copied to app storage), multi-select preference chips |
| AI assistant | `ai_assistant_screen.dart` | Chat UI over the on-device rule-based advisor; simulated 800–1400 ms typing delay; **bold** markdown rendering; quick prompts |

## Data model (`models/models.dart`)

- `Cigar` — catalog item (id, brand, productLine, vitola, wrapper, binder, filler, country, factory?, strength, flavorTags, msrp, length, ringGauge, description, communityRating?, reviewCount?)
- `JournalEntry` — 27 fields incl. 4 ratings (draw/burn/construction/overall 1–5), flavor tags, pairing, purchase info, wouldSmokeAgain; JSON round-trip with defensive defaults
- `HumidorItem` — inventory + aging (agingStartDate, targetAgingMonths); JSON round-trip
- `UserProfile` — id fixed `local-user`, experience level, preferred strengths/flavors/wrappers/countries, humidorSize, bio, avatarUri
- `Recommendation`, `ChatMessage`, `Analytics` — in-memory only

Enum-ish strings: strength `mild|mild-medium|medium|medium-full|full`; body `light|medium|full`; experience `beginner|intermediate|advanced|aficionado`.

## Persistence (`stores/stores.dart`)

SharedPreferences, JSON strings under keys `puro_journal_entries`,
`puro_humidor_items`, `puro_user_profile`, with `puro_*_seeded` booleans.
First launch seeds 3 journal entries + 3 humidor items (dates relative to
first launch). New entries/items are prepended. Humidor quantity 0 deletes
the item. IDs: epoch millis + random base36 suffix.

## Static data (`data/seed_data.dart`)

- 30-note flavor taxonomy with category + chip color each
- 16 wrapper types, 12 countries, 16 vitolas, 14 beverage pairings
- strength/experience display maps
- 20-cigar curated catalog (Padrón, Fuente, Davidoff, Oliva, My Father, Cohiba, Montecristo, Ashton, Rocky Patel, Drew Estate)
- seed journal/humidor builders + default profile

## Logic (`logic/`)

- `recommendations.dart` — scoring: flavor overlap ×3, strength +4, wrapper +3, country +2, community rating ≥95 +3 / ≥92 +1, top-rated-entry flavor overlap ×2; excludes smoked `brand-productLine`; matchScore = clamp(60 + score×2, 0, 99); types by rank (3 you-may-enjoy, 3 trending, rest try-next). `getAnalytics`: totals, avg rating, top 5 brands, top 8 flavors, strength breakdown, last-12 rating trend, would-smoke-again rate.
- `advisor.dart` — rule-based chat: greeting, "tonight/recommend", "similar to X", mild+creamy, full/bold, bourbon/coffee/rum pairings, aging, beginner, palate summary, budget (≤$12), fallback with random suggestion. Pure function of (input, entries, profile).

## Theme (`theme.dart`)

Always-dark "luxury" palette (gold `#C9A84C` on near-black `#0D0A06`, cream
text `#F2E2BC`), 5 strength badge colors, Inter font (Google Fonts), 12dp
radius tokens.

## AI backend contract (`api/cigar_api.dart`, `server/api.js`)

Base URL via build-time `API_BASE` (default `http://10.0.2.2:3001`).
- `POST /api/cigars/lookup` `{brand, productLine}` → `{description, country, wrapper, size, strength, found}`
- `POST /api/cigars/identify` `{image: dataURL(jpeg;base64)}` → `{brand, productLine, description, country, wrapper, size, strength, confidence, found}`

Errors surface as snackbars; app is fully usable without the backend. The
Node server is reused as-is by the Kotlin app (not ported).

## Tests (`test/logic_test.dart`)

Recommendations (exclusion, score cap, type ranks), analytics (empty +
seeded aggregates), advisor (greeting, bourbon, tonight), humidor JSON
round-trip.

## Kotlin/Compose mapping used in this rebuild

| Flutter | Native Android |
|---|---|
| Provider + ChangeNotifier | Kotlin `StateFlow` stores + `collectAsState()` |
| shared_preferences JSON | `SharedPreferences` + `kotlinx.serialization` (same keys) |
| MaterialApp/Navigator | Navigation Compose, single activity |
| NavigationBar + IndexedStack | Material3 `NavigationBar` + saveable tab state |
| image_picker | Photo Picker (`PickVisualMedia`) + `TakePicture` + FileProvider |
| http | OkHttp |
| flutter_test | JUnit4 (pure-JVM logic tests) |
