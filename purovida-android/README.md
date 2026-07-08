# Puro Vida — Native Android (Kotlin + Jetpack Compose)

Native Android rebuild of the Puro Vida cigar journal, ported from the
Flutter implementation in [`robbstacy/PuroVida`](https://github.com/robbstacy/PuroVida)
(itself a rewrite of the original Expo/React Native app). See
`INVENTORY.md` for the full inventory of the source app and the
Flutter→Kotlin mapping used.

All journal, humidor, and profile data is stored locally on the device — no
accounts, no analytics. The two AI features (lookup and photo
identification) call the same optional Node.js backend from the PuroVida
repo (`server/api.js`) and degrade gracefully when it's unreachable.

## Module layout

```
core/   Pure Kotlin (JVM): data model, seed catalog, flavor taxonomy,
        recommendations + analytics engine, rule-based advisor, AI API
        client, and the ported unit tests. No Android dependencies.
app/    Android app: Jetpack Compose UI (all 11 screens), Material 3
        always-dark "luxury" theme, SharedPreferences persistence,
        navigation, camera / photo-picker integration.
```

## Building

Open this folder (`purovida-android/`) in Android Studio and let it sync —
everything resolves from Google Maven and Maven Central. Or from the
command line:

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:bundleRelease      # App Bundle for the Play Store (needs signing config)
./gradlew :core:test              # engine unit tests (pure JVM, no Android SDK needed)
```

Toolchain: AGP 8.10.1, Kotlin 2.1.21, compile/target SDK 35, min SDK 26,
JDK 17+.

## AI backend configuration

The base URL defaults to `http://10.0.2.2:3001` (the Android emulator's
host loopback), matching the Flutter app. Override it in
`local.properties`:

```properties
apiBase=https://your-host.example.com
```

or on the command line with `-PapiBase=...`. Run the backend from the
PuroVida repo:

```bash
OPENAI_API_KEY=sk-... node server/api.js   # port 3001
```

Note: `usesCleartextTraffic` is enabled in the manifest so the plain-HTTP
emulator default works during development. If you ship with an HTTPS
backend, consider removing that flag.

## Port notes (parity decisions)

- **Persistence** uses the same SharedPreferences keys and JSON field
  names as the Flutter app (`puro_journal_entries`, `puro_humidor_items`,
  `puro_user_profile`, `puro_*_seeded`), including first-launch seeding
  with relative dates and "quantity 0 deletes the humidor item".
- **Engine logic** (recommendation scoring weights, match-score formula,
  analytics aggregation, advisor rules and copy) is ported verbatim;
  `core/src/test` mirrors the Flutter `test/logic_test.dart`.
- **Design tokens** (colors, strength badge palette, flavor chip colors,
  radii) are ported verbatim. The Flutter app used the Inter font via
  Google Fonts; this port uses the system default (Roboto) — bundle Inter
  in `res/font/` later if exact typography matters.
- **Image capture** uses the Android Photo Picker + camera capture via
  FileProvider, downscaling to max width 1024 / JPEG quality 80 to match
  the Flutter `image_picker` settings.
- The Cigar Advisor chat runs fully on-device (same rule engine), with the
  original's simulated 800–1400 ms typing delay.
