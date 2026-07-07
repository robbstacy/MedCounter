# Data Safety Form — Answers

Play Console → App content → Data safety. Med Counter's answers are simple because the app is fully local.

## Overview questions

| Question | Answer | Why |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | All data stays in a local on-device database. Google's definition of "collect" means transmitting data off the device — Med Counter never does. |
| Is all of the user data collected by your app encrypted in transit? | N/A (not asked when nothing is collected) | |
| Do you provide a way for users to request that their data is deleted? | N/A (not asked when nothing is collected) | |

That's the whole form: answering **No** to collection/sharing skips the per-data-type sections. The store listing will show the "No data collected / No data shared" badge — a genuine selling point; the listing copy leans into it.

## Points that do NOT change the answers (if Google reviewers ask)

- **Local storage** of medication data is not "collection" — collection means off-device transmission.
- **The export feature** is user-initiated sharing through the Android share sheet; the developer never receives the data. This does not count as the app "sharing" data under Play's definitions ("user-initiated action" exemption).
- **Local notifications** are generated on-device; nothing is transmitted.
- The app requests **no INTERNET permission** (verify in the manifest before submitting — if the manifest does include INTERNET for opening the privacy policy link, that's still fine: opening a browser link is not data collection, but removing the permission entirely makes the privacy story airtight since ACTION_VIEW doesn't need it).

## Health apps declaration

Play Console → App content → Health apps: declare the app relates to **medication management/reminders**. Med Counter:
- provides organizational/reminder functionality only,
- gives no dosing recommendations, diagnosis, or treatment advice,
- is not a regulated medical device.

Keep the in-app/store disclaimer ("not medical advice — follow your doctor and pharmacist") consistent with this declaration.

## Related forms (App content section)

- **Ads:** No, the app contains no ads
- **Content rating questionnaire:** Utility/Productivity; no user-generated content, no violence/sexual content/gambling; references to drugs = "medication information entered by the user" → expected rating: Everyone
- **Target audience:** 18 and over (it manages prescriptions; choosing 18+ avoids child-directed policy requirements)
- **News app:** No
- **COVID-19 tracing/status:** No
- **Government app:** No
