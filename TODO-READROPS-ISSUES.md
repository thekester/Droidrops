# Readrops Issues - Droidrops Follow-up

Last verified: 2026-08-22

This file is a local tracker of all issues in the upstream
[Readrops](https://github.com/readrops/Readrops) repository, compared with the Droidrops fork.

It is not intended to be committed or pushed into the public repository.

## Legend

- **Fixed in Droidrops**: An identifiable fix exists in the fork history.
- **Priority TODO**: issue explicitly examined, without sufficient correction at this stage.
- **TODO - no fix identified**: open issue upstream, without corresponding proof of work in Droidrops.
- **Awaiting upstream**: notification or response action, not a product correction.
- **Closed upstream - verify in Droidrops**: issue closed in Readrops; This does not prove that the fork
  was valid for this case.

## Summary

- Issues analyzed: **307**
- Corrected and validated in Droidrops: **9**
- Local fixes awaiting commit: **2**
- Analyzed without correction required: **3** (#348 already fixed, #330 not a bug, #356 not reproduced)
- TODO without identified correction: **102**
- Waiting upstream: **1**
- Closed upstream, still to verify in Droidrops: **190**
- Readrops issues opened by `thekester`: [#362](https://github.com/readrops/Readrops/issues/362).


## Fixes identified

### #296 - Auto-update when adding feed

Status: **fixed in Droidrops**

- Fix in commit [`86c688a9`](https://github.com/thekester/Droidrops/commit/86c688a9).
- Folders and feeds are reloaded immediately after adding.
- Old articles in the feed are retrieved without waiting for the next synchronization.
- Duplicates are avoided during the next synchronization.
- Tests added in `GReaderRepositoryTest`.
- The upstream issue remains open: propose an upstream pull request if necessary.

### #312 - Rename or update a local folder

Status: **fixed in Droidrops**

- The upstream issue is closed by Readrops in commit
  [`39a54d8d`](https://github.com/readrops/Readrops/commit/39a54d8d9cc64176c9b05368fb167fc33378194b).
- Droidrops adds reconciliation of a renamed category on the server side.
- The local identifier of the folder is retained so as not to break the Timeline filter.
- Remaining action: manually verify the renaming on a real FreshRSS server.

### #360 - User-Agent containing okhttp

Status: **technically fixed in Droidrops**

- `UserAgentInterceptor` replaces the OkHttp header with `Readrops Android`.
- The User-Agent no longer contains `okhttp`.
- Remaining action: test on FreshRSS behind nginx and comment on the outcome.
- Recommended improvement: use `Droidrops Android` instead of `Readrops Android`.

## Priority TODOs

### #353 - Error when opening an RSS post

Status: **fixed and verified** - released in `78416f3c`

- Confirmed cause: `Context.openUrl()` filtered non-HTTP(S) schemes correctly, but then launched
  `startActivity()` without protection. Without a browser installed, the uncaught
  `ActivityNotFoundException` sent the app to `CrashActivity`.
- Second fault found in passing: the non-HTTP(S) case produced a silent `return`. The user typed in a
  link and absolutely nothing happened, with no explanation.
- Correction applied in `ContextExtensions.kt`:
  - `openUrl()` intercepts `ActivityNotFoundException` and displays `no_app_to_open_link`.
  - The unsupported URI scheme displays `link_not_supported` instead of failing silently.
  - `openInCustomTab()` catches the same exception and falls back to `openUrl()`, which then handles the
    complete absence of a browser.
- Manual verification on a Pixel 9 emulator / API 35 with Chrome disabled (`pm disable-user
  com.android.chrome`, no remaining `http` intent handlers): 3 taps on "Open URL" produced 3 Toasts
  attributed to `com.droidrops.app` in logcat, and no launch of `CrashActivity`.
- Note: the Toast does not appear in `adb screencap` (rendered by the system in a separate surface since
  Android 11); the proof is the logcat correlation.
- All that remains to be done: commit, then add an automated test. The module's unit tests are in pure
  JUnit4, without Robolectric or mockk: simulating the absence of Activity would require adding
  Robolectric to the test dependencies.

### #356 - Crash when opening the app from recent apps

Status: **Priority TODO - not reproduced, no leak measured**

- The issue reports a Compose `OutOfMemoryError` when restoring from Recent Applications.
- The Pull-to-Refresh patch in v2.2.5 addresses another crash and does not prove the resolution of this
  issue.
- Reproduction attempt on Pixel 9 / API 35 emulator, faithful scenario (death of the process in the
  background via `am kill`, then reopening from Recents):
  - The restoration works: the app returns to the article screen with its state, which shows that the
    serialization of the Voyager stack is behaving correctly.
  - 5 consecutive kill/restore cycles: PSS stable at ~113 MB (113095 KB in cycle 1, 112941 KB in cycle
    5), no growth.
  - 0 `OutOfMemoryError`, 0 `TransactionTooLarge` attributable to the app, 0 `CrashActivity`.
  - Heap Java has the restoration: 8 MB used out of 44 MB allocated.
- Intermediate conclusion: **there is no leak in the restoration path**. The reported OOM is therefore
  probably linked to the context of the device (Z Flip 5: switching between cover screen and main screen,
  therefore configuration changes and external memory pressure) rather than to a fault in the restoration
  path itself.
- The app does not contain any `rememberSaveable`: nothing large passes through the Bundle.
- All that remains to be done: obtain the complete OOM stack from the reporter, or profile on a real
  foldable by forcing configuration changes during the restoration.

### #355 - Error opening a FreshRSS post in external browser

Status: **fixed and verified** by the same correction as #353, delivered in `78416f3c`

- The relevant button goes through `ItemScreen.onOpenUrl`, which calls `context.openUrl()` or
  `context.openInCustomTab()` depending on `openInExternalBrowser` preference.
- This is exactly the hardened path for #353: both functions now intercept `ActivityNotFoundException`
  instead of letting the crash escalate.
- It remains to be done: confirm with the rapporteur that his error corresponds to this case, the
  description of the outcome not giving the stack.

### #348 - md5hash discards leading 0s (Fever login failures)

Status: **already corrected in Droidrops** (previous classification wrong)

- `ApiUtils.md5hash` applies `BigInteger(1, bytes).toString(16).padStart(32, '0')`. `padStart(32, '0')`
  restores leading zeros that `BigInteger` removes.
- A dedicated non-regression test exists: `ApiUtilsTest.md5hashPreservesLeadingZerosTest`, with the
  expected value `user4013:pass4013` at `0003296c0fa9a2bad56701b3fff82f21` (3 leading zeros).
- Independent verification: `md5("user4013:pass4013")` calculated outside the project gives
  `0003296c0fa9a2bad56701b3fff82f21`.
- Fix present since commit `690dc43f`. `:api:testDebugUnitTest` tests green.
- There remains to be done: nothing on the code side. Possibly comment on the upstream issue.

### #352 - Date parsed incorrectly when format includes time offset

Status: **fixed and verified** - released in `78416f3c`

- `DateUtils.parse()` ends with `parsed.toLocalDateTime()` on `ZonedDateTime` and `OffsetDateTime`. This
  method **discards the offset without converting**: the wall time written in the stream is kept as is.
- This is not an oversight: three existing tests explicitly require it. `rssDateWithNumericOffsetTest`
  waits for `22:21:46` for `Fri, 04 Jan 2019 22:21:46 +1300`, and `isoPatternTest` waits for `11:39:37`
  for `2020-06-30T11:39:37.206-07:00`.
- Measured magnitude: up to 11 hours apart, and possible switching from one day to the next (`Sat, 22 Aug
  2026 23:30:00 -0500` = 11:30 p.m. on the 22nd internally, 06:30 a.m. on the 23rd in Paris).
- Consequences beyond the display: the sorting of articles between feeds of different zones is wrong, and
  the last-24-hours filter calculates its time window from `Item.pub_date`, therefore on unconverted
  values.
- Correction applied: `withZoneSameInstant` for `ZonedDateTime` and `atZoneSameInstant` for
  `OffsetDateTime`, before `toLocalDateTime()`. The moment is now converted to the reader's local time
zone
  instead of being truncated.
- `DateUtilsTest` rewritten: expectations are derived from the instant via
  `Instant.parse(...).atZone(ZoneId.systemDefault())`, rather than hard-coded. The test suite no longer
  depends on the machine's time zone, avoiding flaky CI results. All 8 tests pass.
- Expected and assumed effect: the timestamp displayed for articles already stored shifts to the next
  synchronization.

### #330 - Feed items always show "<1min" instead of actual publication time

Status: **not a bug - display ambiguity**

- The `<1 min` value is the **estimated read time**, not an age. It comes from `Utils.readTimeFromString`
  (number of words / words per minute).
- Check: the four repositories (Local, Nextcloud News, GReader, Fever) populate `Item.readTime`. No path
  leaves it at zero.
- `<1 min` is therefore accurate for feeds whose entries carry almost no text, typically Hacker News
  which only publishes a link.
- Actual cause of the report: the line shows `<date> · <read time>` without a label, so readers can mistake
  the duration for the article's age. This is the same kind of ambiguity as the “New articles” filter,
  which was renamed “Last 24 hours”.
- Possible correction: visually distinguish the reading duration, for example by an icon or a label,
  rather than juxtaposing it with the date with a simple separator dot.

### #341 - Mark all read does not work with FreshRSS

Status: **fixed and verified** - released in `78416f3c`

- Identified cause: `GReaderService.setItemsState` declares `@Field("i") itemIds: List<String>`, so
  **each id travels to its own form field**, and everything was leaving in a single request. No batching
  existed in the GReader path.
- FreshRSS is in PHP, with `max_input_vars` set to 1000 by default. Beyond that, PHP stops analyzing the
  parameters **without error**: the request responds 200, the app considers the operation successful, the
  server has only processed part of the ids. At the next synchronization, the unprocessed articles return
  unread.
- Correction applied in `GReaderDataSource`: ids are sent in batches of 250 (`MAX_IDS_PER_REQUEST`), for
  reading as well as for favorites. Batching also protects body size limits, proxy limits, and timeouts.
- Non-regression test added: `GReaderDataSourceTest.readIdsAreSentInBatches` verifies that 600 ids
  produce 3 calls `edit-tag` and that no batch exceeds 250. Complete green result: 19 tests, 0 failures,
  `classicSync` unchanged.
- Unverified: the exact value of `max_input_vars` on a given server. The division is beneficial
  independently of this ceiling, so the correction does not depend on this measure.
- All that remains to be done: commit.

### #349 - Cannot subscribe Debian news RSS

Status: **fixed and verified** - released in `78416f3c`

- Identified cause: `LocalRSSDataSource.parseResponse` trusted the `Content-Type` header and only looked
  at the document root element if that type was unknown.
- `debian.org/News/news` returns a document **RSS 1.0 / RDF** (`<rdf:RDF>` with
  `xmlns="http://purl.org/rss/1.0/"`) but announces it as `application/rss+xml`, that is to say the type
  of RSS 2.0. The app therefore chose `RSS2FeedAdapter` for an RDF document, and the parsing failed.
- RSS 1.0 was already supported (`RSSType.RSS_1`, `RSS1FeedAdapter`): only the detection of the format
  was in question, not the ability to read it.
- Correction applied: for XML feeds, the root element now takes precedence over the `Content-Type` header;
  the header is used only when the root element provides no useful information. `checkElement` supports
  both the root parser and an external parser, so the adapters work in either case.
- Verifications: `:api` complete green, 140 tests 0 failures, including `LocalRSSDataSourceTest` (13),
  `LocalRSSHelperTest` (7), `RSS1AdapterTest` (5), `RSS2AdapterTest` (9). Real addition of the feed on
  emulator: it appears under the title **Debian News**, read from `<channel><title>` of the RDF document.
- All that remains to be done: commit.

### Crash - IllegalArgumentException in ItemScreenModel.onDispose

Status: **fixed and verified** - delivered in `78416f3c` (reported directly, excluding upstream
inventory)

- Symptom: `IllegalArgumentException: Do not add an item state change for an item which is already read`,
  raised from `ItemScreenModel.onDispose`.
- Cause: `StateChange.readChange` records a **toggle**, not a direction. `onDispose` passed
  indiscriminately all `readChange` to `Repository.setItemsRead`, whose precondition `require(items.all {
  it.isRead == false })` refuses any article already read. Opening an already read article, pressing
  “mark unread”, then leaving the screen was enough to crash the application.
- Note: the treatment of favorites located just below would already correctly manage the toggle. Only the
  reading path was inconsistent.
- Correction applied: partition of changes according to the current state. Unread articles become read go
  through `setItemsRead` (preserved batch processing), read articles become unread go through
  `setItemReadState`, which handles both ways.
- Verification: three cold passes from an article already read, with double switch each time. Return to
  `MainActivity`, no `CrashActivity`, zero exceptions in logcat.

### #333 - Crash on opening article with YouTube embed

Status: **crash not reproduced - embed fault fixed and verified**, delivered in `78416f3c`

- Reproduction attempt on Pixel 9 / API 35 emulator, with a real article carrying a YouTube embed
  (Hackaday, `<iframe src="https://www.youtube.com/embed/...">`): the article opens, the content is
  displayed, no crash, no `CrashActivity`.
- The observed logcat noise (`cr_VideoCapture: getCameraCharacteristics ... Unable to retrieve camera
  characteristics`, ten times) comes from Chromium enumerating the cameras because the iframe declares
  `allow="...camera..."`, and the emulator does not have one. This is not an application error.
- **Real fault found**: the embed never works. The YouTube player displays `Error 153 - Video player
  configuration error`, which indicates an origin denial. Cause: `ItemWebView` calls
  `loadDataWithBaseURL("file:///android_asset/", ...)`, so the page has an origin `file://` which the
  embedded reader rejects.
- Verified by experience: by passing the URL base to the article link, the embed loads completely
  (thumbnail, title, play button, "Watch on YouTube") and the error disappears.
- But this naive correction breaks the typography: the template `webview_html_template` loads the font
  via the **relative** URL `fonts/Inter-Regular.woff2`, resolved against the base. With the base changed
  it points to the article domain and fails. Observed by the change in the line breaks of the same
  paragraph between the two renderings, therefore by a change in font metrics. The experiment was
  canceled.
- Correction applied, both approaches being retained:
  1. `WebViewAssetLoader` (`androidx.webkit` 1.12.1 dependency added) serves assets on
     `https://appassets.androidplatform.net/assets/`, which becomes the base URL. The page therefore has
     an https origin, the embedded reader starts, and the relative URLs of the template continue to
     resolve to the font, which remains loaded.
  2. Preference `openVideosInYoutube` (default: false). When activated, YouTube iframes are replaced by
     the video thumbnail and a “Watch on YouTube” link. Useful for readers who have the native app or a
     Premium subscription and prefer their own reader.
- Verifications on the screen: functional integral embed (thumbnail, title, play button), Error 153
  disappeared, and newlines identical to the original therefore font intact. Thumbnail mode checked
  separately, setting active from Settings screen.
- Also note: no `WebChromeClient` is installed, which is required for full-screen HTML5 video.

### #317 - Error when importing OPML

Status: **local correction not committed - to validate**

- Identified cause: `OPMLAdapter.fromXml` rejected any document whose attribute `version` was not exactly
  equal to `"2.0"`: `if (version != "2.0") throw ParseException("Only 2.0 OPML is supported")`. An OPML
  without the `version` attribute was also rejected, with `getValueOrNull` returning `null`.
- However, for a list of subscriptions, OPML 1.0, 1.1 and 2.0 have the same structure of `outline`. The
  test fixture `wrong_version.opml` was proof of this: a 1.0 document strictly identical to the 2.0
  fixture, which the parser knew how to read but refused.
- As with #352, the refusal was **enforced by a test** (`opmlVersionTest` expected an exception), so it was
  deliberate. It nevertheless blocked exports from Feedly and Inoreader, which declare `version="1.0"`,
  as well as hand-written OPML without attributes.
- Fix applied: version checking is removed, with a comment explaining why it should not come back.
- Tests: `opmlVersionTest` replaces with `readOpmlVersion1Test` and `readOpmlWithoutVersionTest`, which
  verifies that both forms import and that the feed URLs are correct. Fixture renames
  `wrong_version.opml` -> `version_1_0.opml`, since there was never anything wrong, and fixture
  `no_version.opml` adds. `OPMLParserTest`: 5 tests, 0 failures.
- End-to-end verification: real import on emulator of an OPML `version="1.0"` containing a folder and two
  feeds. The **Imported (2)** folder appears in the Feeds tab. The same file was refused before the
  correction.
- All that remains to be done: commit.

### #347 - [Security] Important data breach

Status: **local correction not committed - to validate**

- Storage check: account credentials are **not** in base. `login` and `password` are annotated `@Ignore`
  in the Room entity and live in `EncryptedSharedPreferences` (`account_credentials`, AES256-SIV for
  keys, AES256-GCM for values), with a `MasterKey` of Android keystore. This specific point is therefore
  healthy.
- **Two real issues found elsewhere**, both due to lack of save rules while the manifest declared
  `android:allowBackup="true"` alone:
  1. `account_credentials.xml` was backed up, but its `MasterKey` lives in the Keystore, which is
     **never** backed up or transferred. After restoration, the file is presented to the application
     without its key: the identifiers become indecipherable. Since `createEncryptedPreferences` is called
     to construct the Koin graph, a decryption failure occurs at startup.
  2. The session tokens `token` and `writeToken` are stored in the Room database, therefore saved **in
     plain text** with the backup provider.
- Correction applied: added `res/xml/backup_rules.xml` (API 30 and earlier) and
  `res/xml/data_extraction_rules.xml` (API 31 and later), both references from the manifest. They exclude
  `account_credentials.xml` from cloud backup **and** from device transfer, which solves both points at
  once: no more identifying material in the backup, and no more encrypted file restored without its key.
- The SQLite side files `readrops-db-wal` and `readrops-db-shm` are also excluded: restoring an outdated
  `-wal` next to the database may produce an inconsistent state.
- Verification: manifest merges from the APK release correctly `fullBackupContent` and
  `dataExtractionRules`. The reference `@xml/backup_rules` being resolved at compilation, a missing file
  would have caused manifest processing to fail.
- Unverified: the actual behavior of an Auto Backup restore, which would require two devices and a full
  Google backup cycle.
- All that remains to be done: commit.

### #325 - Improve grammar in the crash notice, "Github" -> "GitHub"

Status: **local correction not committed - to validate**

- Observed directly on the screen during a real crash: the message displayed “**I** you see this, it
  means the app ran into an unexpected error. Please if you can, report the error on **Github**. » Two
  grammatical errors and incorrect capitalization.
- The application was inconsistent with itself: `greader_warning` and `fever_warning` already correctly
  wrote "GitHub".
- Corrections applied:
  - `crash_message` (English) rewrites: “If you see this, the app ran into an unexpected error. Please
    report it on GitHub if you can. »
  - Case “Github” -> “GitHub” on **17 occurrences** distributed in 6 languages ​​(en, es, fr, ja, pt-rBR,
    zh-rTW). The case of a brand name does not depend on the language, the correction is therefore valid
    everywhere and is not part of the translation work.
  - URLs are not affected: they use `github.com` in lowercase.
- Verifications: 14 analysis string files, all valid in XML, zero faulty occurrences remaining, clean
  build release.
- All that remains to be done: commit.

### #336 - [Bug] fosstodon

Status: **local correction not committed - to validate**

- Identified cause: `RSS2ItemAdapter.validateItem` raised `ParseException("Item title is required")`.
  However, the RSS 2.0 specification says the opposite: all elements of an item are optional, but **at
  least one title or description** must be present. The application was therefore stricter than the norm.
- Microblogging feeds are titleless by nature. Verified on the real feed `fosstodon.org/@fosstodon.rss`:
  **20 items out of 20 without `<title>` tag**, all with a `<description>`. The entire feed was rejected
  even though it is perfectly valid.
- Aggravating: five `item.title!!` assertions exist in the interface. A null title would have caused the
  display to drop anyway, so the validation masked a second problem.
- Correction applied: the item is refused only if title **and** description are missing. When the title
  is absent, it is derived from the cleaned-up description, truncated to 100 characters with an ellipsis.
  The five `title!!` therefore remain satisfied.
- Scope voluntarily limited to RSS 2.0: the RSS 1.0 and Atom specifications really require a title, the
  same constraint therefore remains legitimate and has not been affected.
- Tests: `noTitleTest` now checks that the title is derived from the description instead of waiting for
  an exception, and `noTitleAndNoDescriptionTest` covers the truly invalid case, with a new fixture.
  `RSS2AdapterTest`: 10 tests, 0 failures, including `nullTitleTest` which relates to another case and
  remains unchanged.
- End-to-end verification: real addition of the fosstodon feed on emulator. It is accepted, synchronized,
  and its posts are displayed with a derived title, the icon and color of the feed, and the hashtags in
  tags. The same feed was refused before the correction.
- All that remains to be done: commit.

### #318 - Do not use dynamic color for text

Status: **local correction not committed - to validate**

- Precision: the application does not use **any** Material You dynamic colors. The issue actually
  concerns the **personalized color per feed**, applied to the name of the feed, the article title and
  the badges.
- A guardrail existed, `canDisplayOnBackground`, but its threshold was **1.75:1**, well below the WCAG AA
  floor of 3.0 for large text or an interface element, and 4.5 required for normal text.
- Measurement on bright background `#F8FDFF`: a cyan `#00C8FF` (1.91:1), a green `#7ED321` (1.82:1) and
  an orange `#FF9500` (2.14:1) **passed** the threshold. An orange stream therefore displayed its name at
  2.1:1.
- Fix 1: default threshold raised from 1.75 to **3.0**, the absolute floor that no visible element should
  cross. The recalibrated colors fall on the primary of the theme, measured at 6.42:1.
- Fix 2: Three calls chose white or black text over accent color with a fixed threshold of 2.5
  (`ItemScreenBottomBar`, `TimelineItemParts`, `TagSurface`). A fixed threshold chooses poorly: for a
  purple `#6364FF` white gives 4.38 and black 4.80, and the old rule retained white. Replaces with
  `bestForegroundOn`, which compares the two and picks the best, without a magic number.
- Honest magnitude of the second correction: for orange, cyan and green the old threshold was already
  right. The real gain concerns mid-range colors, purple and red, where it retained the worst of the two.
  The main gain comes from correction 1.
- Assumed limit: the threshold is applied uniformly at 3.0. Small text would formally require 4.5, but
  the same color also serves as the background of the pastille, where the contrast against the background
  of the page is not the right criterion. Going up to 4.5 is decided on a case by case basis.
- Verification: rendering of the timeline unchanged, tablets still readable, clean build.
- All that remains to be done: commit.

### #346 - Empty space in items

Status: **local correction not committed - to validate**

- Identified cause: `LocalRSSRepository` was storing `item.cleanDescription =
  Jsoup.parse(item.description!!).text()`. When the description contains only markup, a single image for
  example, `text()` returns an **empty but not null** string.
- The two interface tests compared to `null`:
  - `TimelineItemParts:212` checked for `!= null`, true for `""`. An empty `ShortSpacer` and `Text` were
    therefore rendered, which produces the reported visible hole.
  - `TimelineItemParts:168` checked `== null` to choose compact layout, wrong for `""`. The card
    therefore kept the long layout without anything to put on it.
- Correction applied:
  - at the source, `.ifBlank { null }`: an empty description is no longer stored at all.
  - the subsequent reading time calculation was `item.cleanDescription!!`, which would have raised an NPE
    once the value was normalized to null. It is rewritten to `item.cleanDescription?.let { ... }`, which
    leaves `readTime` as zero when there is nothing to read, which is correct.
  - in the interface, both tests pass to `isNullOrBlank()`, so that existing databases already containing
    empty strings are corrected without migration.
- Verification: clean build release, 154 unit tests cleared.
- All that remains to be done: commit.

### #319 - “OPML Import/Export” stylization

Status: **local correction not committed - to validate**

- Verification done byte by byte: the string did not contain **any** invisible characters, contrary to
  what the title of the issue suggests.
- The real fault is a case inconsistency within the same dialog: the header displayed “OPML
  Import/Export” while the two actions below display “OPML import” and “OPML export”.
- Correction applied: header aligned in sentence case, "OPML import/export", in accordance with Material
  recommendations and the two actions it labels.

### #321 / #322 - “RSS client” -> “feed reader”

Status: **well-founded, not applied - decision required**

- Background check: the application reads **four** formats, `RSS_1`, `RSS_2`, `ATOM` and `JSONFEED`.
  Calling it an “RSS client” is therefore factually incomplete, and both issues are valid.
- Current wording: “Droidrops is a fork of Readrops and a multi-services RSS client for Android. " in
  `full_description.txt`, and "Droidrops RSS client" in `short_description.txt`.
- Proposed wording: “Droidrops is a fork of Readrops and a feed reader supporting multiple services for
  Android. » and “Droidrops feed reader”.
- **Intentionally not applied**: these texts only live in `fastlane/metadata/`, that is to say the
  F-Droid and Play public file. They go beyond the requested scope and are a communication decision, not
  a technical correction.

### #338 - [Bug] title display line break

Status: **local correction not committed - symptom not reproduced in real conditions**

- Inconsistency found in the code: only RSS 2.0 cleaned its titles. `RSS2ItemAdapter` applies
  `ApiUtils.cleanText`, which normalizes spaces via Jsoup, while `ATOMItemAdapter`, `RSS1ItemAdapter` and
  `JSONItemsAdapter` took the plain text. Same for stream names.
- Consequence: a feed whose XML is formatted on several lines, which many static site generators do, kept
  its newlines in the title and pushed them as is in the timeline.
- **Honesty about reproduction**: the symptom has not been observed in real conditions. On three Atom
  streams tested, Planet KDE, LinuxFr and Debian, none of the 50 titles contained line breaks or
  extraneous spaces.
- Verification of a rejected hypothesis: numerical entities such as `&#8217;` are already decoded by the
  XML parser itself, the difference between formats therefore did not concern the entities, only the
  normalization of spaces.
- Correction applied despite the absence of reproduction: `ApiUtils.cleanText` is now applied to the
  titles and stream names of the four formats. The treatment is idempotent on an already clean title, and
  removes an entire class of display defects.
- Tests: fixture `atom_items_multiline_title.xml` adds and test `multilineTitleTest` verifying that a
  title written on three lines appears on only one. Without the correction the assertion fails. Complete
  `:api` suite: 143 tests, 0 failures, no existing adapters affected.
- All that remains to be done: commit.

### #324 - Larger font for the options of multi-choice pickers

Status: **local correction not committed - to validate**

- Observed in `RadioButtonPreferenceDialog.RadioButtonItem`: the label of each option used
  `MaterialTheme.typography.bodyMedium`, i.e. 14sp, whereas Material 3 provides `bodyLarge`, 16sp, for a
  selectable list line in a dialog.
- Concrete effect: the options were **smaller** than the settings entry that opens the dialog, which is
  exactly the opposite of the expected hierarchy.
- Correction applied: `bodyMedium` -> `bodyLarge`. No custom typography in the project, so the default
  Material sizes apply.
- On-screen check: “Default category” dialog opened, the three options are now on the same scale as the
  adjustment entries visible behind.
- All that remains to be done: commit.

### #320 - Use another icon for a local account

Status: **diagnosis made, not applied - design decision required**

- The problem is real and verified: `AccountType.LOCAL` uses `R.mipmap.ic_launcher`, that is to say the
  icon of the application itself. On the account selection screen, the Droidrops logo is displayed in the
  header **and** next to the “Local account” line. The same image twice for two different meanings.
- Why this doesn't apply here: the module `db`, where `AccountType` lives, only contains service logos.
  You would have to create a drawable, and especially the account icons are rendered with `Image()`
  **without a tint**, because they are brand logos. A monochrome icon added there would therefore be
frozen
  in a single color, unreadable either in a light theme or in a dark theme.
- Making this single icon tintable would require distinguishing its rendering from that of the brand
  logos in the relevant composables. This is a design choice, not a mechanical correction, hence the work
  stops here.
- Proposal: a device icon, of the “smartphone” type, which clearly expresses “stores on this device”,
  rendered via `Icon()` tinted for the LOCAL type only.

### #332 - Have more information to debug login issues

Status: **local correction not committed - to validate**

- The existing `DebugScreen` only covers sending test notifications, nothing about logging into accounts.
- Cause identified in the most common case among self-hosters, who are the public of the application:
  `SSLHandshakeException` inherits from `IOException`. A TLS failure therefore fell into the generic
  branch `is IOException` and displayed the raw exception message, such as “Trust anchor for
  certification path not found”. Incomprehensible, and above all without any indication of what to do.
- But `network_security_config.xml` already declares `<certificates src="user" />`: the application
  trusts the authorities added by the user. So the actionable advice existed, it just wasn't formulated.
- Fix applied: `is SSLException` maps to explicit message, places **above** `is IOException`. The order
  is essential, `when` evaluates from top to bottom and `SSLException` inherits from `IOException`:
  placed after, the branch would never be reached. `SSLException` rather than `SSLHandshakeException` to
  also cover hostname mismatch.
- End-to-end verification: Adding an account pointing to a real self-signed certificate host. The message
  displayed is “The server certificate could not be verified. If your server uses a self-signed
  certificate, install its authority in Android settings under Security, then try again. »
- Also check that the chain is present in the `resources.arsc` of the APK release, and that the order of
  the branches is correct in the compiled file.
- All that remains to be done: commit.

### #316 - Easily access hovertext for images

Status: **local correction not committed - to validate**

- The `title` attribute of an image is the text on hover. Online comics use it for a second punchline. A
  touchscreen has no hover, so this text was completely unreachable, and `ItemWebView` never read this
  attribute.
- The existing dialogue on long press only offers Share and Download.
- Approach chosen: reveal the text in the caption under the image, via a transformation in `formatText`,
  as for video embeds. The wiring of the long support crosses four layers, and a visible legend without
  interaction better meets the “easily access” request.
- Two errors made and corrected during the verification, noted for the record:
  1. **Regression introduced by the fix for #336**: the `hasNoProse` heuristic only counted the
     text, so an entry made of a single image in a link, which is exactly the form of an xkcd entry,
     wrongly received the mention "this stream only provides the link". Corrected: an image, video or
     player now counts as content in its own right.
  2. **Poorly designed safeguard**: the legend was skipped when `title` was equal to `alt`, to avoid
     supposed duplication. However, `alt` is only displayed by a browser if the image fails to load, so
     there is nothing to duplicate. And xkcd, the same case that the fix targets, puts the same
     string in both attributes. Comparison removed.
- The caption is inserted **after the encompassing link** when there is one, otherwise after the image,
  so as not to include it in the clickable area.
- Dedicated style `.hover-text` added to the HTML template, italicized and slightly toned down, so that
  it stands out from the body of text.
- End-to-end verification: xkcd stream added on emulator, the legend is displayed correctly under the
  drawing, and the erroneous mention has disappeared.
- All that remains to be done: commit.

### #315 - Undo popup to remove an article from favorites

Status: **local correction not committed - to validate**

- Preliminary check: no cancellation mechanism existed for favorites. `SnackbarResult.ActionPerformed`
  only appeared in `AccountTab`, for something else.
- The necessary infrastructure was already in place in the timeline: a `SnackbarHostState` and a snackbar
  pattern with action, used for sync errors.
- Correction applied: removing an article from favorites from the timeline displays “Removed from
  favorites” with an “Undo” action which restores it.
- Choice assumes: **only withdrawal** offers cancellation. Adding a favorite is a visible and trivially
  reversible action, while losing it is not. Showing a snack bar in both directions would have been
  noise.
- End-to-end verification, with the control test that counts:
  - removal then “Undo”: the button returns to “Remove from favorites”, the item is restored.
  - withdrawal then expiration of the snackbar without touching it: the button remains on “Add to
    favorites”, the item remains withdrawn. Without this second case, the first proved nothing: an
    ineffective withdrawal tap would have given the same apparent result.
- Note: my first three verification attempts were invalid, the navigation having drifted towards an
  article screen, which uses a deferred status model without a snackbar.

### #345 - [Feature] Mark all read

Status: **already implemented in Droidrops** (previous classification wrong)

- The function exists and is complete: `setAllItemsRead`, as well as the variants `setAllItemsReadByFeed`
  and `setAllItemsReadByFolder`, exposed by a floating timeline button with confirmation dialog
  `mark_all_articles_read_question`.
- It respects the active filter: all, folder, feed, favorites, or 24 hour window.
- Verified in real use during testing in this session, on a local account.
- There remains to be done: nothing on the code side.

### #361 - [Feature] Standardize Pull-to-Refresh Behavior

Status: **local correction not committed - to validate**

- Observation: `PullToRefreshBox` only existed in `TimelineTab`. The feeds tab, however, displays the
  unread counters by folder and by feed, which only a synchronization updates, and did not offer any
  gesture to trigger it.
- `FeedScreenModel` had no sync capabilities, unlike the timeline model which queues a `SyncWorker`.
- Correction applied:
  - `FeedState.isRefreshing` adds.
  - `FeedScreenModel.refreshFeeds()` enqueues the same `SyncWorker`, with the same connectivity check.
    `SyncWorker.startNow` uses `enqueueUniqueWork` in policy `KEEP`, so pulling during a sync in progress
    does not stack a second.
  - `FeedTab` wraps its list in a `PullToRefreshBox`.
- **Applied lesson from the crash fixed earlier**: the `PullToRefreshBox` wraps the state `when` instead
  of being placed in one of its branches. It therefore remains mounted whatever the state, which exactly
  avoids the node detachment which caused the timeline to crash.
- End-to-end verification:
  - the refresh indicator appears clearly when drawing,
  - logcat confirms `WM-WorkerWrapper: Starting work for com.readrops.app.sync.SyncWorker`, so real
    synchronization and not just an animation,
  - four successive draws, including three during a synchronization in progress: no incident, neither
    `CompositionLocal`, nor `CrashActivity`, nor fatal exception.
- All that remains to be done: commit.

### #323 - Add an animation for switching app tabs

Status: **local correction not committed - to validate**

- Observation: `HomeScreen` called `CurrentTab()` directly, without transition. The tab change was
  therefore instantaneous and abrupt.
- Difficulty: animating requires rendering the content of the **target** tab, not just the current tab. A
  simple `Crossfade` around `CurrentTab()` would have given nothing, since `CurrentTab()` always makes
  the tab active, therefore the same content on both sides.
- Correction applied: `AnimatedContent` on `tabNavigator.current`, whose contents pass through
  `tabNavigator.saveableState(key = "currentTab", tab = tab)`, exactly the key that `CurrentTab()` uses
  internally. Each tab therefore maintains its own state.
- Transition retained: “fade through”, the motion Material for a navigation bar. Output in 90 ms, input
  in 220 ms after 90 ms delay.
- **Checking the main risk**, loss of tab state:
  - timeline at the top, then scroll to a given article,
  - switch to the feed tab,
  - return to the timeline: the scroll position is **identical** to the one left. Without this test, the
    transition could have masked a state reset.
- Checking the animation itself: a capture taken during the change shows an intermediate state, with an
  average deviation of 66 levels from the final image. Change is therefore no longer instantaneous.
- All that remains to be done: commit.

### #326 - Close category selection pane on tablet

Status: **problem confirmed, correction attempted then canceled - not resolved**

- The problem is real and localized: `TimelineDrawer` uses `PermanentNavigationDrawer` when
  `isTabletUi()` is true. A permanent sign, by definition, **cannot be closed**. The menu button is also
  explicitly hidden on the tablet (`if (!isTabletUi())` in `TimelineAppBar`), so no interaction allows it
  to be folded and the reading width recovered.
- Correction attempted: replace with `DismissibleNavigationDrawer`, the Material component intended
  exactly for this case, open by default, with the menu button made visible on all screen sizes to be
  able to reopen it.
- **Result: failure, twice.** The panel went to a strip of approximately 30 pixels on the left,
  unreadable content, in a stable and non-transient state.
  - First hypothesis, the type of “sheet”: `TimelineDrawerContent` wraps itself in a `ModalDrawerSheet`,
    which passed into the permanent container. I separated the envelope from the contents to provide a
    `DismissibleDrawerSheet` to the tablet container and a `ModalDrawerSheet` to the phone container.
    **Without effect**, the rendering remains identical. So that wasn't the cause.
- **Changes rolled back.** The three affected files, `TimelineDrawer`, `TimelineAppBar` and
  `TimelineTab`, returned to their original state, verified after the fact. Leaving a broken tablet
  layout would have been worse than changing nothing.
- Test conditions: no tablet AVD available, tablet simulated by `wm size 2560x1600` and `wm density 240`,
  i.e. `smallestScreenWidthDp` at 1067. It is possible that the cause is due to this simulation rather
  than the code, which remains to be determined on a real device.
- Idea for the future: measure the width actually allocated to the panel in the resealable container, and
  check the behavior on a real AVD tablet before concluding on the component.

### #359 - Readrops sync with nextcloud slows down

Status: **local correction not committed - to validate**

- Root cause identified, and it goes well beyond Nextcloud: the `Item` table had **only one index**, on
  `feed_id`. None on `remote_id`.
- Now `remote_id` is the search key for `itemExists` and `updateReadAndStarState`, called **once per
  article** during each synchronization, and for **19 requests** in total in the DAOs. Each call
  therefore made a complete scan of the table `Item`.
- The cost is in O(N x M), N incoming items for M stored items. Synchronization therefore slows down as
  the base grows, which corresponds exactly to the title of the issue. The problem affects all account
  types, not just Nextcloud, as well as the local RSS path which also calls `itemExists` in a loop.
- Hint that the author knew the pattern: `ItemState` already has an index `(remote_id, account_id)`, and
  `MigrationFrom6To7` was just adding indexes. `Item`, the largest and most popular table, had been
  forgotten.
- Correction applied:
  - `Item.remoteId` annotates `index = true`,
  - basic version 7 -> 8,
  - `MigrationFrom7To8` creates `index_Item_remote_id`, saved in `DbModule`,
  - schema 8 exports by Room, showing both indexes.
- Verification on device: test `migrate7To8` added to `MigrationsTest`. It doesn't just validate Room's
  schema, it queries `sqlite_master` to confirm that the index exists after migration. `:db` complete
  instrumented suite: **22 tests, 0 failures**, the five pre-existing migrations included.
- Not intentionally processed: `Feed.remote_id` and `Tag.remote_id` have no index either, but these
  tables have tens to hundreds of rows, the effect would be marginal.
- All that remains to be done: commit. The migration 5 to 6, absent, has since been written: see the
  dedicated section.

### Migration 5 to 6 missing - crash on startup after update

Status: **local correction not committed - to be validated** (defect found on site, excluding upstream
inventory)

- Observation: the database declared version 7 and recorded migrations 1-2, 2-3, 3-4, 4-5 and 6-7.
  **Migration 5 to 6 did not exist**, and never existed in the history of the repository.
  `fallbackToDestructiveMigration` is not configured either.
- Consequence: Room cannot find any path from a database that remains in version 5, refuses to open it
  and raises an exception. The application **crashes on startup** for any user updating from a
  sufficiently old installation. The fault is silent as long as no one is in this case.
- Analysis of the gap 5 to 6, done before writing anything: two tables added, `Tag` and `TagJoin`, with
  their indexes. **No modifications** to existing tables, no columns, no views. The change is therefore
  purely additive and a migration that creates these tables is complete.
- Correction applied: `MigrationFrom5To6` written from the **DDL exported from schema 6** itself, rather
  than written by hand, so that the result validates exactly against it. Saved in `DbModule`, the string
  is now continuous from 1 to 8.
- Verification on device:
  - `migrate5To6`: `runMigrationsAndValidate` compares the result to the exported schema, and the test
    additionally verifies that the two tables actually exist in `sqlite_master`.
  - `migrate1To8`: The **complete channel** is replayed from start to finish, which is the real scenario
    of a user coming from an old installation.
  - Instrumented suite `:db`: **24 tests, 0 failures**, eight migrations covered.
- All that remains to be done: commit.

### #357 - Readrops keeps crashing when I open an article

Status: **not reproduced** - no crash observed on wide coverage

- Test environment created for the occasion: 9 feeds voluntarily covering the forms which posed problems
  elsewhere in this monitoring, i.e. RSS 2.0, Atom, RSS 1.0 / RDF for Debian, Mastodon whose entries have
  no title, xkcd with text on hover, Hacker News whose entries only contain a link, plus Hackaday which
  embeds video players.
- First pass, opening articles in series: **14 articles opened, 0 crashes**, no `CrashActivity`, no fatal
  exceptions in logcat.
- Second pass, solicitation of the pager, which is a code path distinct from the simple opening: **25
  rapid horizontal scans** in succession, then backtracks in bursts. No crashes.
- A single incident appears in logcat, `EmojiCompatManager: EmojiCompat is not initialized`, but it comes
  from another process, a Google Play component recognizable by its obfuscated class names. **Nothing
  attributable to the application.**
- Useful note: two real crashes were found and corrected during this session, one on the pull-to-refresh
  of the timeline, the other on `ItemScreenModel.onDispose` when switching playback state. It cannot be
  ruled out that report #357 corresponds to one of the two, the description being too lacking in detail
  to make a decision.
- All that remains to be done: ask the reporter for the complete trace, or check if the symptom persists
  after the corrections of this session.

### #305 - Crash FileUriExposedException on articles embedding openstreetmap

Status: **crash path not found - related defect corrected**

- Systematic search for file URIs passed to other applications:
  - image sharing goes correctly through `FileProvider.getUriForFile`,
  - OPML export uses a URI `content://` provided by the document selector,
  - `Context.openUrl` and `openInCustomTab` already filter schemes other than http and https through
    `isWebUrl()`, which closes the path by which an article link could reach `startActivity`.
- Hypothesis on the historical origin: before the URL base of the WebView became an https origin, it was
  worth `file:///android_asset/`. A relative link in an article therefore resolved to `file://`, and a
  version prior to the `isWebUrl` filter would have launched an Intent with such a URI. This cannot be
  confirmed without history.
- **Related defect found and corrected**: the “Open” action proposed after an OPML export constructed its
  Intent **without** `FLAG_GRANT_READ_URI_PERMISSION`. Since the URI belonged to the document provider,
  the receiving application could not read it. The flag is now propagated.
- Limit of the check: on the emulator, the selector responds “No apps can perform this action”, due to
  the absence of an application registered for `text/xml`. The fix could therefore not be observed from
  end to end, only established by reasoning on the Android permissions model.
- All that remains to be done: commit, and request the complete trace from the rapporteur to confirm that
  the original crash is indeed a thing of the past.

### #54 – Support for tags

Status: **fixed and verified** (reading support already existed; tag filtering was added in this session)

- The reading chain already existed and works:
  - model: entities `Tag` and `TagJoin`, with `TagDao` and `TagJoinDao`,
  - extraction: the **four** format adapters build `Tag`, Atom, JSON Feed, RSS 1.0 and RSS 2.0,
  - persistence: `insertItemsTags` for GReader, direct insertion for local RSS,
  - display: in the article via `SimpleTitle`, and in the timeline via `TagSurface`.
- Gap that remained: tags were **read-only**. `SubFilter` only knew about `FEED`, `FOLDER` and `ALL`,
  and `ItemsQueryBuilder` never mentioned them, so filtering the timeline by tag was impossible. It is
  this half that was added.

Correction applied:

- `Filters.kt`: `TAG` value in `SubFilter`, `tagId` field in `QueryFilters`.
- `ItemsQueryBuilder`: validation `tagId != 0` when `subFilter == TAG`, then filter by **subquery**
  `Item.id In (Select item_id From TagJoin Where tag_id = ...)`. A join on `TagJoin` would have
  duplicated any article with multiple tags, and the column list should remain unchanged.
- `TagDao`: `selectAllFlow(accountId)`, sort by name.
- `TimelineScreenModel`: state `tags` and `filterTagName`, method `updateDrawerTagSelection`, and reset
  of `tagId` in the ALL / FOLDER / FEED selectors.
- `TimelineDrawer`: **Tags** section at the end of the drawer, after folders and feeds.
- `TimelineAppBar`: case `SubFilter.TAG` to display the tag name in the subtitle.
- No migration: nothing is added to the schema, `Tag` and `TagJoin` already existed.

Guardrail on “mark everything as read”:

- The action is **removed as long as a tag filter is active**. It is wired to the current filter and no
  variant by tag exists; without this safeguard, a user filtering on a tag then pressing the button would
  have mark **the entire account** as read, with no possible cancellation.
- Tagging is **not** implemented, it is a choice: removing a poorly framed destructive action costs less
  than delivering an approximate version.

Checked on screen (emulator, FreshRSS account, APK release):

- the drawer lists real tags from Hackaday / The Verge / Ars Technica feeds (`3d Printer hacks`, `555`,
  `AI`, `Amazon`, `Apple`, `Biology`, ...);
- filter on `AI`: the bar displays `Articles` / subtitle `AI`;
- **proof that the filter is based on the tag and not on the feed**: the remaining articles come from
  **different feeds**, The Verge ("Over 1 million people have clicked LinkedIn's AI slop button") and Ars
  Technica ("As demand for Meta AI glasses explodes...");
- “mark all as read” button: present without filter in `[933,2004][996,2067]`, **absent** under tag
  filter while the “Synchronize” button remains present (witness proving that the dump is valid), then
  **returned to the same coordinates** after erasing the filter.

Accessibility corrected in passing:

- The **four** `FloatingActionButton`s in the application had `contentDescription = null`, so were muted
  for TalkBack. My previous accessibility audit missed them, it only scanned `IconButton`. Added labels:
  “Mark all articles as read”, “Add account”, “Add folder”, “Add feed” (new string `add_folder`).
- It was this lack which invalidated my first two measures of the safeguard: I was looking for a text on
  a icon button which had none.

### #66 (continued) - Modifying the identifiers of an existing feed

Status: **fixed and verified**

- Gap left by #66: identifiers could only be entered when **creating** the feed. Changing them required
  deleting the stream and recreating it, therefore losing its folder, its color and its reading history.
- Two faults found when instructing the subject, even before writing the screen:
  1. **Orphan identifiers**: `Repository.deleteFeed` only deletes the base line. Identifiers live in
     encrypted preferences, which deletion does not affect, so they survive the feed indefinitely. Check:
     `Feed.id` is `INTEGER PRIMARY KEY AUTOINCREMENT`, so SQLite never recycles an identifier. **No
     risk** that a new feed inherits the password from an old one. The fault is hygiene, not a flaw.
  2. **No show/hide toggle** on the password on the add screen, even though the account login screen
     already had one.

Design :

- Location: the existing **Edit feed** dialog, not one more screen. Everything that concerns a feed is
  already changing there; adding a screen would have added a navigation level for two fields.
- **Section collapsed by default.** This is not an aesthetic preference: `BaseDialog` arranges its
  content in a `Column` **without scrolling**, and the dialog already has three fields and a button. An
  always-open block pushed the Validate button off-screen on a short viewport. Progressive disclosure was
  therefore a structural constraint.
- The folded line **always states the state**: “This feed is public” or “Saved for <login>”. This is the
  only place where the user can know that a stream carries identifiers.
- **The stored password is never reread in the form.** Empty field + help text “Leave blank to keep the
  current password”: renaming a feed does not require retyping a secret.
- Section **hidden for server accounts** (`isAuthAvailable = account.isLocal`): with FreshRSS, Nextcloud
  News or Fever, it is the server that fetches the feed, the application never presents these identifiers
  itself.
- Validation rules: HTTP basic authentication requires both halves, so a single password is refused, and
  a single login is also refused unless a password is already stored. Emptying the login deletes the
  identifiers; a “Clear credentials” button does this in one gesture, without writing anything before
  validation, to remain consistent with the rest of the dialogue.

Check on emulator, complete cycle:

| Step | Expected | Observed |
|---|---|---|
| Feed without credentials | ?This feed is public? | as expected |
| Login only + Validate | error, dialog remains open | ?Field can?t be empty? |
| Login + password | saved, password hidden | absent from the accessibility tree |
| Reopen | ?Saved for testuser?, password field empty, Clear button shown | as expected |
| **Rename with password left blank** | **password is retained** | ?Saved for testuser? remains |
| Clear credentials + Validate | credentials removed | ?This feed is public? |
| Feed under a FreshRSS account | section hidden | 0 occurrences |

Cleaning on deletion, **measured on the real encrypted preferences file** (debug build, `run-as`,
`account_credentials.xml`):

- 2 inputs at the start (the two sets of Androidx keys),
- **4** after recording the identifiers,
- **2** after deleting the stream.

The debug build carrying a real test FreshRSS account was **uninstalled** afterwards.

Accessibility corrected in passing:

- The **four** `FloatingActionButton`s in the application had `contentDescription = null`. My previous
  accessibility audit only scanned `IconButton` and missed them all. Added labels: “Mark all articles as
  read”, “Add account”, “Add folder”, “Add feed” (new string `add_folder`). Checked on screen.
- Show/hide toggle added on the password of **two** screens (addition and edition), with the labels “Show
  password” / “Hide password” already present in the application.

### #69 - Leave link validation to server

Status: **local correction not committed - to validate**

- Finding: Three entry points used `Patterns.WEB_URL` to validate a feed URL, add a feed, modify an
  existing URL, and receive a shared URL.
- `Patterns.WEB_URL` is very restrictive and refuses perfectly usable addresses: a host without a point
  like `http://nas/feed`, `localhost`, certain addresses with ports, and recent TLDs. This is precisely
  the case for self-hosted installations, which are the audience of the application.
- Worsening on the third point: a shared URL that did not pass the filter was **silently ignored**, the
  add screen did not even open.
- Fix applied: `isValidFeedUrl` helper, which only checks **syntax**, constructing a `java.net.URI` and
  requiring a non-empty host. No dependencies added. Reachability is left to the network, which already
  reports it clearly since the corrections of #353 and #332.
- `Patterns` imports that were no longer needed were removed from all three files as CI required builds
  without warning.
- End-to-end verification: addition of `http://nas/feed.xml`, host without point or TLD. The message
  displayed is now **“Unreachable URL”**, a network error, and no longer “Bad URL” returned without
  attempt. The request therefore really starts.
- All that remains to be done: commit.

### #66 - [Feature Request] Access Password-Protected RSS Feeds

Status: **implemented locally, not committed - partial verification**

- Design choice: identifiers are **not** stored in the database. `Feed` exposes calculated keys
  `loginKey` and `passwordKey`, modeled after `Account`, and the values ​​live in the
  `EncryptedSharedPreferences` already in place. Direct consequence: nothing sensitive goes into a backup
  or an OPML export.
- **No basic migration was necessary**: calculated properties are not columns, the schema remains in
  version 8. Verified after compilation.
- Scope covered, because usable authentication is not limited to synchronization:
  - `LocalRSSDataSource.isUrlRSSResource` and `HtmlParser.getFeedLink` now accept headers, without which
    a protected stream could never have been **added**,
  - the addition screen displays an “Authentication (optional)” section with username and mask password,
  - the identifiers are used for the discovery request, then are recorded once the feed is inserted,
    found by its URL since its identifier only exists after insertion,
  - `LocalRSSRepository` adds the `Authorization` header each time the stream is synchronized.
- End-to-end verification against a server actually requiring Basic authentication,
  `httpbin.org/basic-auth/user/passwd`:
  - **without** credentials: “HTTP error 401, please check your credentials”,
  - **with** identifiers: the 401 disappears and the error becomes a format problem, this service
    responding to JSON. The request therefore goes well, the header is sent. It is the change in
    **nature** of the error that provides proof, not its disappearance.
- Non-regression verification: addition of a public feed with identifiers provided, then complete
  synchronization. The feed is added, 117 articles arrive, no incident. The additional header therefore
  does not disrupt ordinary feeds.
- **Assumed limit**: the reuse of **stored** identifiers during a subsequent synchronization could not be
  observed, due to the lack of a protected server serving a real RSS feed. The storage path executes
  without breaking the addition, but its rereading is only established by the code.
- Not covered: modify the identifiers of an existing feed. Today it must be deleted and added again. The
  feed sheet would be the natural place for this screen.

### #362 - F-Droid inclusion request for Droidrops

- Issue opened by `thekester` to notify the original author.
- No upstream responses identified at time of check.
- Keep the link in the F-Droid merge request and wait for an objection or confirmation.

## Complete inventory

| Issue | Upstream status | Droidrops status | Title |
|---|---|---|---|
| [#364](https://github.com/readrops/Readrops/issues/364) | Open | TODO - no fix identified | crashola |
| [#362](https://github.com/readrops/Readrops/issues/362) | Open | Awaiting upstream | F-Droid inclusion request for Droidrops |
| [#361](https://github.com/readrops/Readrops/issues/361) | Open | Local fix, not committed | [Feature] Standardize Pull-to-Refresh Behavior |
| [#360](https://github.com/readrops/Readrops/issues/360) | Open | Fixed in Droidrops | [Bug] App uses User-Agent containing 'okhttp' that is common in blocklists |
| [#359](https://github.com/readrops/Readrops/issues/359) | Open | Local fix, not committed | Readrops sync with nextcloud slows down |
| [#358](https://github.com/readrops/Readrops/issues/358) | Closed | Closed upstream - verify in Droidrops | [Bug] Crash when opening article |
| [#357](https://github.com/readrops/Readrops/issues/357) | Open | Not reproduced | Readrops keeps crashing when I open an article |
| [#356](https://github.com/readrops/Readrops/issues/356) | Open | Priority TODO - not reproduced | Crash when opening the app from recent apps |
| [#355](https://github.com/readrops/Readrops/issues/355) | Open | Fixed and verified | getting this error when I want to open a freshRSS post in external browser |
| [#354](https://github.com/readrops/Readrops/issues/354) | Open | TODO - no fix identified | [Bug] Error when opening feeds |
| [#353](https://github.com/readrops/Readrops/issues/353) | Open | Fixed and verified | Readrops shows error on opening an RSS post |
| [#352](https://github.com/readrops/Readrops/issues/352) | Open | Fixed and verified | [Bug] Date parsed incorrectly when format includes time offset |
| [#351](https://github.com/readrops/Readrops/issues/351) | Open | TODO - no fix identified | [Bug] |
| [#350](https://github.com/readrops/Readrops/issues/350) | Open | TODO - no fix identified | [Bug] Error |
| [#349](https://github.com/readrops/Readrops/issues/349) | Open | Fixed and verified | [Bug] Cannot subscribe Debian news RSS |
| [#348](https://github.com/readrops/Readrops/issues/348) | Open | Already fixed in Droidrops | [Bug] ApiUtils.md5hash discards leading 0s on hash (AKA: login failures via Fever integration) |
| [#347](https://github.com/readrops/Readrops/issues/347) | Open | Local fix, not committed | [Security] Important data breach |
| [#346](https://github.com/readrops/Readrops/issues/346) | Open | Local fix, not committed | Empty space in items |
| [#345](https://github.com/readrops/Readrops/issues/345) | Open | Already implemented in Droidrops | [Feature] Mark all read |
| [#344](https://github.com/readrops/Readrops/issues/344) | Open | TODO - no fix identified | [Bug]  Open in external browser |
| [#343](https://github.com/readrops/Readrops/issues/343) | Open | TODO - no fix identified | [Bug] |
| [#342](https://github.com/readrops/Readrops/issues/342) | Open | TODO - no fix identified | [Bug] |
| [#341](https://github.com/readrops/Readrops/issues/341) | Open | Fixed and verified | [Bug] Mark all read does not work with FreshRSS |
| [#339](https://github.com/readrops/Readrops/issues/339) | Open | TODO - no fix identified | [Feature] backup/restore all data |
| [#338](https://github.com/readrops/Readrops/issues/338) | Open | Local fix, not committed | [Bug] title display line break |
| [#337](https://github.com/readrops/Readrops/issues/337) | Closed | Closed upstream - verify in Droidrops | [Feature] Sync read state with FreshRSS server |
| [#336](https://github.com/readrops/Readrops/issues/336) | Open | Local fix, not committed | [Bug] fosstodon |
| [#335](https://github.com/readrops/Readrops/issues/335) | Open | TODO - no fix identified | App crashed after using back gesture |
| [#334](https://github.com/readrops/Readrops/issues/334) | Open | TODO - no fix identified | [Bug] Can't add Google Reader API account (Miniflux) |
| [#333](https://github.com/readrops/Readrops/issues/333) | Open | Crash not reproduced - embed issue fixed | Crash on opening article with YouTube embed |
| [#332](https://github.com/readrops/Readrops/issues/332) | Open | Local fix, not committed | Have more information to debug login issues |
| [#331](https://github.com/readrops/Readrops/issues/331) | Open | TODO - no fix identified | [Bug] Scroll wheel doesn't refresh properly |
| [#330](https://github.com/readrops/Readrops/issues/330) | Open | Not a bug - display ambiguity | [Bug] Feed items always show "<1min" instead of actual publication time |
| [#329](https://github.com/readrops/Readrops/issues/329) | Closed | Closed upstream - verify in Droidrops | Error importing opml file |
| [#328](https://github.com/readrops/Readrops/issues/328) | Open | TODO - no fix identified | [Bug] Cannot mark article as read with fever api of miniflux instance |
| [#327](https://github.com/readrops/Readrops/issues/327) | Open | TODO - no fix identified | Se detiene al deslizar un articulo |
| [#326](https://github.com/readrops/Readrops/issues/326) | Open | Confirmed - attempted fix reverted | [Feature] Close category selection pane on tablet |
| [#325](https://github.com/readrops/Readrops/issues/325) | Open | Local fix, not committed | Improve grammar in the crash notice and change stylization "Github" —> "GitHub" |
| [#324](https://github.com/readrops/Readrops/issues/324) | Open | Local fix, not committed | Use larger font size for the chooseable things in multi-choice setting pickers |
| [#323](https://github.com/readrops/Readrops/issues/323) | Open | Local fix, not committed | Add an animation for switching app tabs |
| [#322](https://github.com/readrops/Readrops/issues/322) | Open | Valid point - decision required | "multi-services RSS client" —> "Feed reader supporting multiple services" |
| [#321](https://github.com/readrops/Readrops/issues/321) | Open | Valid point - decision required | Call it a feed reader |
| [#320](https://github.com/readrops/Readrops/issues/320) | Open | Diagnosis complete - decision required | Use another icon for a local account |
| [#319](https://github.com/readrops/Readrops/issues/319) | Open | Local fix, not committed | "OPML Import/Export" —> "OPML Import/Export" |
| [#318](https://github.com/readrops/Readrops/issues/318) | Open | Local fix, not committed | Do not use dynamic color for text |
| [#317](https://github.com/readrops/Readrops/issues/317) | Open | Local fix, not committed | Error when importing OPML |
| [#316](https://github.com/readrops/Readrops/issues/316) | Open | Local fix, not committed | [Feature] Easily access hovertext for images |
| [#315](https://github.com/readrops/Readrops/issues/315) | Open | Local fix, not committed | [Feature] Undo-Popup for removing article from favourites |
| [#314](https://github.com/readrops/Readrops/issues/314) | Open | TODO - no fix identified | [Feature] Separate View settings in Favourites |
| [#313](https://github.com/readrops/Readrops/issues/313) | Closed | Closed upstream - verify in Droidrops | [Bug] |
| [#312](https://github.com/readrops/Readrops/issues/312) | Closed | Fixed in Droidrops | [Bug] SQLiteConstraintException when trying to update folder name |
| [#311](https://github.com/readrops/Readrops/issues/311) | Closed | Closed upstream - verify in Droidrops | [Bug] |
| [#309](https://github.com/readrops/Readrops/issues/309) | Open | TODO - no fix identified | Crash error |
| [#308](https://github.com/readrops/Readrops/issues/308) | Closed | Closed upstream - verify in Droidrops | [Bug] Alpha colors not supported by Android color contrast tools |
| [#307](https://github.com/readrops/Readrops/issues/307) | Closed | Closed upstream - verify in Droidrops | [Bug] Some feeds intangible and unresponsive, tapping them does nothing |
| [#305](https://github.com/readrops/Readrops/issues/305) | Open | Related issue fixed | App crashes with FileUriExposedException on some articles that embed openstreetmap |
| [#304](https://github.com/readrops/Readrops/issues/304) | Open | TODO - no fix identified | [Feature] Add "Share with Reference" option for selected text |
| [#303](https://github.com/readrops/Readrops/issues/303) | Open | TODO - no fix identified | [Feature] Option to disable swipe left/right to go to the previous/next article |
| [#302](https://github.com/readrops/Readrops/issues/302) | Closed | Closed upstream - verify in Droidrops | [Feature] Search |
| [#300](https://github.com/readrops/Readrops/issues/300) | Closed | Closed upstream - verify in Droidrops | [Feature] Material 3 Compatability |
| [#298](https://github.com/readrops/Readrops/issues/298) | Open | TODO - no fix identified | [Feature] Short tap vs long tap in the `Feeds` tab |
| [#297](https://github.com/readrops/Readrops/issues/297) | Open | TODO - no fix identified | [Bug] Atom handling |
| [#296](https://github.com/readrops/Readrops/issues/296) | Open | Fixed in Droidrops | [Feature] Auto-update when adding feed |
| [#295](https://github.com/readrops/Readrops/issues/295) | Open | TODO - no fix identified | [Bug] Adding direct URL to JSON Feed |
| [#294](https://github.com/readrops/Readrops/issues/294) | Open | TODO - no fix identified | [Regression] When in the background for some time on ItemScreenPage, the app reopens on the wrong item when coming to the foreground |
| [#293](https://github.com/readrops/Readrops/issues/293) | Open | TODO - no fix identified | [Bug] Initial tablet mode portrait |
| [#292](https://github.com/readrops/Readrops/issues/292) | Closed | Closed upstream - verify in Droidrops | [Bug] an Error Occurred when adding mastodon.rss |
| [#291](https://github.com/readrops/Readrops/issues/291) | Open | TODO - no fix identified | [Feature]I want the articles in  favorites to always be displayed希望收藏夹中的文章始终保持显示状态 |
| [#290](https://github.com/readrops/Readrops/issues/290) | Open | TODO - no fix identified | [Bug] No feeds from Nextcloud News |
| [#289](https://github.com/readrops/Readrops/issues/289) | Open | TODO - no fix identified | [Bug] When adding a new feed, it is not sorted in the feed tab |
| [#288](https://github.com/readrops/Readrops/issues/288) | Closed | Closed upstream - verify in Droidrops | [Bug] When adding a new feed, it doesn't appear in drawer |
| [#286](https://github.com/readrops/Readrops/issues/286) | Closed | Closed upstream - verify in Droidrops | [Feature] Change settings for `open_in` per feed |
| [#285](https://github.com/readrops/Readrops/issues/285) | Closed | Closed upstream - verify in Droidrops | [Feature] Remove duplicates links froms differents feeds |
| [#284](https://github.com/readrops/Readrops/issues/284) | Open | TODO - no fix identified | [Feature] Allow the use of the Auto-Generated Material You theme |
| [#282](https://github.com/readrops/Readrops/issues/282) | Open | TODO - no fix identified | [Bug] Some items are for some feeds are not fetched |
| [#281](https://github.com/readrops/Readrops/issues/281) | Open | TODO - no fix identified | [Feature] Make readrops icon inline with pixel launcher icon theme |
| [#280](https://github.com/readrops/Readrops/issues/280) | Open | TODO - no fix identified | [Bug] Cannot login to FreshRSS session, error 400 |
| [#278](https://github.com/readrops/Readrops/issues/278) | Closed | Closed upstream - verify in Droidrops | [Feature] Fix Dark mode text contrast |
| [#277](https://github.com/readrops/Readrops/issues/277) | Open | TODO - no fix identified | [Feature] Show recently read articles |
| [#271](https://github.com/readrops/Readrops/issues/271) | Open | TODO - no fix identified | Retrieving 1250 items only |
| [#268](https://github.com/readrops/Readrops/issues/268) | Open | TODO - no fix identified | [Bug] cannot parse valid atom feed |
| [#267](https://github.com/readrops/Readrops/issues/267) | Closed | Closed upstream - verify in Droidrops | [Feature] dark mode colors |
| [#265](https://github.com/readrops/Readrops/issues/265) | Closed | Closed upstream - verify in Droidrops | Failed |
| [#264](https://github.com/readrops/Readrops/issues/264) | Open | TODO - no fix identified | Crash at clicking on a Readrops element. |
| [#262](https://github.com/readrops/Readrops/issues/262) | Open | TODO - no fix identified | `FolderDao.selectFoldersAndFeeds`: why is SQL query so complex? |
| [#261](https://github.com/readrops/Readrops/issues/261) | Open | TODO - no fix identified | [Bug] Too long content causes app crashes with SQLiteBlobTooBigException |
| [#260](https://github.com/readrops/Readrops/issues/260) | Open | TODO - no fix identified | [Feature] Add support for being a feed source for launchers |
| [#258](https://github.com/readrops/Readrops/issues/258) | Open | TODO - no fix identified | [Bug] UX: swiping away an item is too easy |
| [#255](https://github.com/readrops/Readrops/issues/255) | Closed | Closed upstream - verify in Droidrops | [Bug] The feed list displayed in the drawer is confusing |
| [#252](https://github.com/readrops/Readrops/issues/252) | Open | TODO - no fix identified | [Feature] Add Full Content Scraping/Try to Show Full Content |
| [#251](https://github.com/readrops/Readrops/issues/251) | Closed | Closed upstream - verify in Droidrops | [Feature] Customize the share intent text using a template engine |
| [#250](https://github.com/readrops/Readrops/issues/250) | Open | TODO - no fix identified | [Feature] Remember scroll position in article view |
| [#249](https://github.com/readrops/Readrops/issues/249) | Open | TODO - no fix identified | [Bug] Can't add local FreshRSS account hosted on a .local YunoHost |
| [#247](https://github.com/readrops/Readrops/issues/247) | Closed | Closed upstream - verify in Droidrops | app crashes consistently when opening one specific article |
| [#246](https://github.com/readrops/Readrops/issues/246) | Closed | Closed upstream - verify in Droidrops | [Bug] https://media.mts.ru/rss - empty list |
| [#245](https://github.com/readrops/Readrops/issues/245) | Closed | Closed upstream - verify in Droidrops | App crashes while importing local opml file |
| [#244](https://github.com/readrops/Readrops/issues/244) | Closed | Closed upstream - verify in Droidrops | OPML Import from Newsflash crashes |
| [#243](https://github.com/readrops/Readrops/issues/243) | Open | TODO - no fix identified | [Feature] Ignore filter hide read feeds for favorites folder |
| [#242](https://github.com/readrops/Readrops/issues/242) | Closed | Closed upstream - verify in Droidrops | [Question] sync read status with FreshRSS |
| [#240](https://github.com/readrops/Readrops/issues/240) | Closed | Closed upstream - verify in Droidrops | Filter for Readrops |
| [#238](https://github.com/readrops/Readrops/issues/238) | Open | TODO - no fix identified | [Feature] Undo news read |
| [#235](https://github.com/readrops/Readrops/issues/235) | Open | TODO - no fix identified | [Bug] Delete feeds |
| [#234](https://github.com/readrops/Readrops/issues/234) | Open | TODO - no fix identified | [Bug] Adding feeds reports error when none exists |
| [#233](https://github.com/readrops/Readrops/issues/233) | Open | TODO - no fix identified | [Bug] Missing expected cursor scroll behaviour |
| [#232](https://github.com/readrops/Readrops/issues/232) | Open | TODO - no fix identified | [Feature] display only unread articles |
| [#229](https://github.com/readrops/Readrops/issues/229) | Closed | Closed upstream - verify in Droidrops | [Bug]  Retrofit Error Using Fever API in RSS Client |
| [#228](https://github.com/readrops/Readrops/issues/228) | Closed | Closed upstream - verify in Droidrops | [Bug] Error parsing yarr fever API feeds response |
| [#227](https://github.com/readrops/Readrops/issues/227) | Open | TODO - no fix identified | [Bug] http 404, URL not found |
| [#226](https://github.com/readrops/Readrops/issues/226) | Closed | Closed upstream - verify in Droidrops | [Bug] Image download |
| [#225](https://github.com/readrops/Readrops/issues/225) | Open | TODO - no fix identified | [Feature] Jump to most recent read |
| [#224](https://github.com/readrops/Readrops/issues/224) | Open | TODO - no fix identified | [Bug] Crash on opening |
| [#223](https://github.com/readrops/Readrops/issues/223) | Closed | Closed upstream - verify in Droidrops | App crash after opening an article from notification menu |
| [#222](https://github.com/readrops/Readrops/issues/222) | Closed | Closed upstream - verify in Droidrops | [Feature] Allow to configure swipe |
| [#221](https://github.com/readrops/Readrops/issues/221) | Closed | Closed upstream - verify in Droidrops | [Bug] なんで「フィード処理エラー」と出てくるのかわからない |
| [#220](https://github.com/readrops/Readrops/issues/220) | Open | TODO - no fix identified | crash log |
| [#219](https://github.com/readrops/Readrops/issues/219) | Closed | Closed upstream - verify in Droidrops | [Feature]: http://127.0.0.1:8080 results in HTTP 503 |
| [#218](https://github.com/readrops/Readrops/issues/218) | Open | TODO - no fix identified | Crash immediately on open v2.0.1 |
| [#217](https://github.com/readrops/Readrops/issues/217) | Open | TODO - no fix identified | [Feature]Also display publish time of the feed items |
| [#216](https://github.com/readrops/Readrops/issues/216) | Open | TODO - no fix identified | [Feature] Recently read filter |
| [#215](https://github.com/readrops/Readrops/issues/215) | Open | TODO - no fix identified | [Request] Article Thumbnail |
| [#214](https://github.com/readrops/Readrops/issues/214) | Closed | Closed upstream - verify in Droidrops | [Bug] App crashing on start after upgrading from 2.0 beta01 |
| [#213](https://github.com/readrops/Readrops/issues/213) | Open | TODO - no fix identified | [Feature]Limit number of RSS items to fetch |
| [#212](https://github.com/readrops/Readrops/issues/212) | Closed | Closed upstream - verify in Droidrops | [Feature] Remember filters |
| [#211](https://github.com/readrops/Readrops/issues/211) | Open | TODO - no fix identified | [Feature] Editable feed history |
| [#210](https://github.com/readrops/Readrops/issues/210) | Open | TODO - no fix identified | [Feature] Add a search bar |
| [#209](https://github.com/readrops/Readrops/issues/209) | Open | TODO - no fix identified | [Feature] Accessing feeds during synchronisation. |
| [#208](https://github.com/readrops/Readrops/issues/208) | Closed | Closed upstream - verify in Droidrops | [Bug] |
| [#207](https://github.com/readrops/Readrops/issues/207) | Closed | Closed upstream - verify in Droidrops | [Bug] settings lost after closing the app |
| [#206](https://github.com/readrops/Readrops/issues/206) | Closed | Closed upstream - verify in Droidrops | [Bug] Crash at start |
| [#205](https://github.com/readrops/Readrops/issues/205) | Closed | Closed upstream - verify in Droidrops | [Bug] image title text no longer accessible |
| [#204](https://github.com/readrops/Readrops/issues/204) | Closed | Closed upstream - verify in Droidrops | [Feature] Black theme for OLED displays |
| [#203](https://github.com/readrops/Readrops/issues/203) | Closed | Closed upstream - verify in Droidrops | [Bug] Unreadable feed name in timeline |
| [#202](https://github.com/readrops/Readrops/issues/202) | Closed | Closed upstream - verify in Droidrops | [Bug] Wrong article dates and times |
| [#201](https://github.com/readrops/Readrops/issues/201) | Closed | Closed upstream - verify in Droidrops | [Bug] Files with .opml extension are unavailable for import |
| [#200](https://github.com/readrops/Readrops/issues/200) | Open | TODO - no fix identified | Error on Fever setup 2.0-beta02 |
| [#199](https://github.com/readrops/Readrops/issues/199) | Closed | Closed upstream - verify in Droidrops | Sync favorites with nextcloud |
| [#198](https://github.com/readrops/Readrops/issues/198) | Closed | Closed upstream - verify in Droidrops | "Show read items" not saved on v2-beta2 |
| [#197](https://github.com/readrops/Readrops/issues/197) | Closed | Closed upstream - verify in Droidrops | Default Android font is not used |
| [#196](https://github.com/readrops/Readrops/issues/196) | Open | TODO - no fix identified | [Feature Request] Zoom and scroll bars |
| [#195](https://github.com/readrops/Readrops/issues/195) | Closed | Closed upstream - verify in Droidrops | OPML Import not working in 2.0-beta02 |
| [#194](https://github.com/readrops/Readrops/issues/194) | Open | TODO - no fix identified | Support importing old `.opml` files |
| [#193](https://github.com/readrops/Readrops/issues/193) | Closed | Closed upstream - verify in Droidrops | [Feature Request] mDNS support for Account URLs |
| [#192](https://github.com/readrops/Readrops/issues/192) | Closed | Closed upstream - verify in Droidrops | Error when refreshing feed |
| [#191](https://github.com/readrops/Readrops/issues/191) | Closed | Closed upstream - verify in Droidrops | 2.0-beta01: Error when migrating from 1.3.1 |
| [#190](https://github.com/readrops/Readrops/issues/190) | Closed | Closed upstream - verify in Droidrops | 2.0-beta1: Unable to scroll in Parameters |
| [#189](https://github.com/readrops/Readrops/issues/189) | Closed | Closed upstream - verify in Droidrops | 2.0-beta01 crash: IllegalArgumentException: baseUrl must end in / |
| [#188](https://github.com/readrops/Readrops/issues/188) | Closed | Closed upstream - verify in Droidrops | [Feature Request] Restore Swipe-to-mark-as-read |
| [#187](https://github.com/readrops/Readrops/issues/187) | Open | TODO - no fix identified | Inconsistent scrolling |
| [#186](https://github.com/readrops/Readrops/issues/186) | Closed | Closed upstream - verify in Droidrops | FreshRSS accounts don't work under 2.0-beta01 |
| [#185](https://github.com/readrops/Readrops/issues/185) | Closed | Closed upstream - verify in Droidrops | Item size dialogue not auto-confirming |
| [#184](https://github.com/readrops/Readrops/issues/184) | Closed | Closed upstream - verify in Droidrops | Initial scroll on reading |
| [#183](https://github.com/readrops/Readrops/issues/183) | Closed | Closed upstream - verify in Droidrops | Autofill no longer working for login fields |
| [#182](https://github.com/readrops/Readrops/issues/182) | Closed | Closed upstream - verify in Droidrops | error with 2.0 beta 01 from fdroid |
| [#178](https://github.com/readrops/Readrops/issues/178) | Open | TODO - no fix identified | fever account |
| [#176](https://github.com/readrops/Readrops/issues/176) | Closed | Closed upstream - verify in Droidrops | Add removeFeeder function to somewehere that user can reach easily |
| [#175](https://github.com/readrops/Readrops/issues/175) | Closed | Closed upstream - verify in Droidrops | Release on Google Play and update F-droid version |
| [#174](https://github.com/readrops/Readrops/issues/174) | Closed | Closed upstream - verify in Droidrops | Use system theme by default |
| [#173](https://github.com/readrops/Readrops/issues/173) | Closed | Closed upstream - verify in Droidrops | OPML import error |
| [#172](https://github.com/readrops/Readrops/issues/172) | Closed | Closed upstream - verify in Droidrops | Adjust automatic colors to meet a11y standards |
| [#171](https://github.com/readrops/Readrops/issues/171) | Closed | Closed upstream - verify in Droidrops | navigate between feeds |
| [#170](https://github.com/readrops/Readrops/issues/170) | Closed | Closed upstream - verify in Droidrops | Freshrss 500 error |
| [#169](https://github.com/readrops/Readrops/issues/169) | Closed | Closed upstream - verify in Droidrops | Mark All as Read Doesn't Work |
| [#168](https://github.com/readrops/Readrops/issues/168) | Closed | Closed upstream - verify in Droidrops | Old articles on top |
| [#167](https://github.com/readrops/Readrops/issues/167) | Closed | Closed upstream - verify in Droidrops | Adding Nextcloud account |
| [#166](https://github.com/readrops/Readrops/issues/166) | Closed | Closed upstream - verify in Droidrops | [Feature Request] Mark as read the scrolled articles |
| [#162](https://github.com/readrops/Readrops/issues/162) | Closed | Closed upstream - verify in Droidrops | Expecting END_OBJECT issue |
| [#161](https://github.com/readrops/Readrops/issues/161) | Open | TODO - no fix identified | Strict XHTML parsing ? |
| [#160](https://github.com/readrops/Readrops/issues/160) | Open | TODO - no fix identified | Error: "Expected a string but was NULL at path $.items[3].author" |
| [#159](https://github.com/readrops/Readrops/issues/159) | Closed | Closed upstream - verify in Droidrops | Gesture or button to jump to next unread message |
| [#158](https://github.com/readrops/Readrops/issues/158) | Closed | Closed upstream - verify in Droidrops | [feature request] syncing when App is starting |
| [#157](https://github.com/readrops/Readrops/issues/157) | Closed | Closed upstream - verify in Droidrops | Favorites FreshRSS |
| [#156](https://github.com/readrops/Readrops/issues/156) | Closed | Closed upstream - verify in Droidrops | Unable to remove favorites |
| [#155](https://github.com/readrops/Readrops/issues/155) | Closed | Closed upstream - verify in Droidrops | Current state of the project |
| [#154](https://github.com/readrops/Readrops/issues/154) | Closed | Closed upstream - verify in Droidrops | RTL Support |
| [#153](https://github.com/readrops/Readrops/issues/153) | Closed | Closed upstream - verify in Droidrops | Allow synchronizing a particular feed/folder |
| [#151](https://github.com/readrops/Readrops/issues/151) | Closed | Closed upstream - verify in Droidrops | Display unread count in side bar |
| [#150](https://github.com/readrops/Readrops/issues/150) | Closed | Closed upstream - verify in Droidrops | Feature request: Long press menu for links in a feed |
| [#148](https://github.com/readrops/Readrops/issues/148) | Closed | Closed upstream - verify in Droidrops | [Feature request] mark as read on scroll |
| [#147](https://github.com/readrops/Readrops/issues/147) | Closed | Closed upstream - verify in Droidrops | Unable to connect to FreshRSS |
| [#146](https://github.com/readrops/Readrops/issues/146) | Closed | Closed upstream - verify in Droidrops | "Mark all as read" toolbar button |
| [#145](https://github.com/readrops/Readrops/issues/145) | Closed | Closed upstream - verify in Droidrops | Always show articles @ favorites/starred list |
| [#144](https://github.com/readrops/Readrops/issues/144) | Closed | Closed upstream - verify in Droidrops | Can't Scroll to End of Article on Pixel 4a (Android 12) |
| [#143](https://github.com/readrops/Readrops/issues/143) | Closed | Closed upstream - verify in Droidrops | [Feature] Allow user to perform initial synchronization |
| [#139](https://github.com/readrops/Readrops/issues/139) | Closed | Closed upstream - verify in Droidrops | Titles dissapear in article view |
| [#138](https://github.com/readrops/Readrops/issues/138) | Closed | Closed upstream - verify in Droidrops | Does not remember filter settings |
| [#137](https://github.com/readrops/Readrops/issues/137) | Open | TODO - no fix identified | Translatable fastlane entries |
| [#135](https://github.com/readrops/Readrops/issues/135) | Closed | Closed upstream - verify in Droidrops | FR: Switch theme based on device theme |
| [#134](https://github.com/readrops/Readrops/issues/134) | Open | TODO - no fix identified | feature request: miniflux backend support |
| [#133](https://github.com/readrops/Readrops/issues/133) | Closed | Closed upstream - verify in Droidrops | Change floating button action - replace Add New Feed (+) with Mark As Read (✓) |
| [#132](https://github.com/readrops/Readrops/issues/132) | Closed | Closed upstream - verify in Droidrops | Remove sort button |
| [#131](https://github.com/readrops/Readrops/issues/131) | Closed | Closed upstream - verify in Droidrops | Remove floating button |
| [#130](https://github.com/readrops/Readrops/issues/130) | Closed | Closed upstream - verify in Droidrops | Hide preview text |
| [#129](https://github.com/readrops/Readrops/issues/129) | Closed | Closed upstream - verify in Droidrops | Hide date |
| [#128](https://github.com/readrops/Readrops/issues/128) | Closed | Closed upstream - verify in Droidrops | Hide read time estimates |
| [#127](https://github.com/readrops/Readrops/issues/127) | Closed | Closed upstream - verify in Droidrops | Hide folder information |
| [#126](https://github.com/readrops/Readrops/issues/126) | Closed | Closed upstream - verify in Droidrops | Hide top bar on scroll |
| [#125](https://github.com/readrops/Readrops/issues/125) | Closed | Closed upstream - verify in Droidrops | Per feed "open with" settings |
| [#122](https://github.com/readrops/Readrops/issues/122) | Open | TODO - no fix identified | Tablet mode |
| [#120](https://github.com/readrops/Readrops/issues/120) | Open | TODO - no fix identified | True black theme |
| [#119](https://github.com/readrops/Readrops/issues/119) | Open | TODO - no fix identified | Layout options |
| [#118](https://github.com/readrops/Readrops/issues/118) | Closed | Closed upstream - verify in Droidrops | Cannot enable storage permission |
| [#117](https://github.com/readrops/Readrops/issues/117) | Closed | Closed upstream - verify in Droidrops | Able to change what slide right will do |
| [#116](https://github.com/readrops/Readrops/issues/116) | Closed | Closed upstream - verify in Droidrops | Readrops asks for too much articles, overloads the server and no article is retrieved |
| [#115](https://github.com/readrops/Readrops/issues/115) | Open | TODO - no fix identified | [Bug] mark as read does not work when all articles are selected |
| [#114](https://github.com/readrops/Readrops/issues/114) | Open | TODO - no fix identified | [Request] More compact layout |
| [#113](https://github.com/readrops/Readrops/issues/113) | Closed | Closed upstream - verify in Droidrops | [Bug] Shortcut to open article with webview requires several click to work |
| [#112](https://github.com/readrops/Readrops/issues/112) | Closed | Closed upstream - verify in Droidrops | [Feature] Add ability to mark article as unread from the article itself |
| [#111](https://github.com/readrops/Readrops/issues/111) | Open | TODO - no fix identified | Link actions dialog on long pressing on a link |
| [#110](https://github.com/readrops/Readrops/issues/110) | Closed | Closed upstream - verify in Droidrops | Account notifications menu glitch |
| [#109](https://github.com/readrops/Readrops/issues/109) | Closed | Closed upstream - verify in Droidrops | [Feature request] Synchronize background color with system settings |
| [#108](https://github.com/readrops/Readrops/issues/108) | Closed | Closed upstream - verify in Droidrops | Suddenly can't sync any-more |
| [#107](https://github.com/readrops/Readrops/issues/107) | Closed | Closed upstream - verify in Droidrops | Export OPML |
| [#106](https://github.com/readrops/Readrops/issues/106) | Closed | Closed upstream - verify in Droidrops | Nextcloud host resolution fails |
| [#105](https://github.com/readrops/Readrops/issues/105) | Closed | Closed upstream - verify in Droidrops | [Feature request] Option to open articles in web browser |
| [#104](https://github.com/readrops/Readrops/issues/104) | Closed | Closed upstream - verify in Droidrops | [Feature Request] Custom feed colors |
| [#102](https://github.com/readrops/Readrops/issues/102) | Closed | Closed upstream - verify in Droidrops | [Feature request] Synchronize 'read' state with FreshRSS |
| [#101](https://github.com/readrops/Readrops/issues/101) | Closed | Closed upstream - verify in Droidrops | Feature suggestion: hide feeds/folders with 0 unread items |
| [#100](https://github.com/readrops/Readrops/issues/100) | Closed | Closed upstream - verify in Droidrops | remoteId is the only criterion in deleteByIds |
| [#99](https://github.com/readrops/Readrops/issues/99) | Closed | Closed upstream - verify in Droidrops | SyncWorker executed multiple times simultaneously |
| [#98](https://github.com/readrops/Readrops/issues/98) | Closed | Closed upstream - verify in Droidrops | When I try to login I get 400 error (FreshRSS) |
| [#97](https://github.com/readrops/Readrops/issues/97) | Closed | Closed upstream - verify in Droidrops | FR: OPML import & export |
| [#96](https://github.com/readrops/Readrops/issues/96) | Closed | Closed upstream - verify in Droidrops | FR: Pull to load item's linked article within the app |
| [#95](https://github.com/readrops/Readrops/issues/95) | Closed | Closed upstream - verify in Droidrops | "Open items in" setting has no effect |
| [#94](https://github.com/readrops/Readrops/issues/94) | Closed | Closed upstream - verify in Droidrops | What does right-swipe do? (+ Enhancement request) |
| [#93](https://github.com/readrops/Readrops/issues/93) | Closed | Closed upstream - verify in Droidrops | Having only one OkHttpClient with a globally changeable AuthInterceptor is a bad idea |
| [#91](https://github.com/readrops/Readrops/issues/91) | Closed | Closed upstream - verify in Droidrops | support fever api |
| [#90](https://github.com/readrops/Readrops/issues/90) | Closed | Closed upstream - verify in Droidrops | Cannot create nextcloud account: certPathValidatorException |
| [#89](https://github.com/readrops/Readrops/issues/89) | Open | TODO - no fix identified | Compact view |
| [#88](https://github.com/readrops/Readrops/issues/88) | Closed | Closed upstream - verify in Droidrops | Support Oauth2 authentication mechanisms |
| [#87](https://github.com/readrops/Readrops/issues/87) | Closed | Closed upstream - verify in Droidrops | Breaking change in News 15.1.0 |
| [#86](https://github.com/readrops/Readrops/issues/86) | Closed | Closed upstream - verify in Droidrops | Replacement for deprecated user API endpoint |
| [#83](https://github.com/readrops/Readrops/issues/83) | Closed | Closed upstream - verify in Droidrops | Help/Bug: cannot add certain RSS feeds to the app |
| [#81](https://github.com/readrops/Readrops/issues/81) | Closed | Closed upstream - verify in Droidrops | Scroll position in article list is not remembered when I scrolled after 300+ items |
| [#80](https://github.com/readrops/Readrops/issues/80) | Closed | Closed upstream - verify in Droidrops | `FOREIGN KEY constraint failed` when pulling down to refresh |
| [#78](https://github.com/readrops/Readrops/issues/78) | Closed | Closed upstream - verify in Droidrops | Move Folders beginning with _ on top |
| [#77](https://github.com/readrops/Readrops/issues/77) | Closed | Closed upstream - verify in Droidrops | Failure to parse feed? (0 articles from a feed that definately has articles) |
| [#76](https://github.com/readrops/Readrops/issues/76) | Closed | Closed upstream - verify in Droidrops | Light theme is not light when system is in dark mode |
| [#75](https://github.com/readrops/Readrops/issues/75) | Closed | Closed upstream - verify in Droidrops | Cannot remove "read it later" articles |
| [#74](https://github.com/readrops/Readrops/issues/74) | Closed | Closed upstream - verify in Droidrops | Ordering articles according to publication date |
| [#73](https://github.com/readrops/Readrops/issues/73) | Open | TODO - no fix identified | Add support for DecSync |
| [#72](https://github.com/readrops/Readrops/issues/72) | Closed | Closed upstream - verify in Droidrops | FR: add an option for auto-sync |
| [#71](https://github.com/readrops/Readrops/issues/71) | Closed | Closed upstream - verify in Droidrops | Can't subscribe a feed provided from feedburner |
| [#70](https://github.com/readrops/Readrops/issues/70) | Closed | Closed upstream - verify in Droidrops | A variety of different errors |
| [#69](https://github.com/readrops/Readrops/issues/69) | Open | Local fix, not committed | Leave link validation to server |
| [#68](https://github.com/readrops/Readrops/issues/68) | Open | TODO - no fix identified | [Feature request] Tiny Tiny RSS |
| [#67](https://github.com/readrops/Readrops/issues/67) | Open | TODO - no fix identified | Add support for homescreen widget? |
| [#66](https://github.com/readrops/Readrops/issues/66) | Open | Implemented locally | [Feature Request] Access Password-Protected RSS Feeds |
| [#62](https://github.com/readrops/Readrops/issues/62) | Closed | Closed upstream - verify in Droidrops | [Feature request] Swipe to next article |
| [#61](https://github.com/readrops/Readrops/issues/61) | Closed | Closed upstream - verify in Droidrops | incorrect category name parsing |
| [#60](https://github.com/readrops/Readrops/issues/60) | Closed | Closed upstream - verify in Droidrops | Sync already read articles |
| [#59](https://github.com/readrops/Readrops/issues/59) | Closed | Closed upstream - verify in Droidrops | Adding new feed causes all other articles to disappear - NC |
| [#58](https://github.com/readrops/Readrops/issues/58) | Closed | Closed upstream - verify in Droidrops | FreshRSS login: change password to api password |
| [#56](https://github.com/readrops/Readrops/issues/56) | Closed | Closed upstream - verify in Droidrops | [Feature Request] FreshRSS Read by category |
| [#55](https://github.com/readrops/Readrops/issues/55) | Closed | Closed upstream - verify in Droidrops | Application crashes when entering an url without http scheme |
| [#54](https://github.com/readrops/Readrops/issues/54) | Open | Already implemented - tag filtering still missing | Support for tags |
| [#53](https://github.com/readrops/Readrops/issues/53) | Closed | Closed upstream - verify in Droidrops | Sync read later / starred articles |
| [#52](https://github.com/readrops/Readrops/issues/52) | Closed | Closed upstream - verify in Droidrops | Readrops does not show old articles |
| [#51](https://github.com/readrops/Readrops/issues/51) | Closed | Closed upstream - verify in Droidrops | Crash of the application on startup |
| [#50](https://github.com/readrops/Readrops/issues/50) | Open | TODO - no fix identified | [Feature Request] Sync with FeedBin |
| [#49](https://github.com/readrops/Readrops/issues/49) | Closed | Closed upstream - verify in Droidrops | Sync with NC |
| [#48](https://github.com/readrops/Readrops/issues/48) | Closed | Closed upstream - verify in Droidrops | Support for self-signed certificates |
| [#46](https://github.com/readrops/Readrops/issues/46) | Open | TODO - no fix identified | [Feature Request] Support Basic Auth |
| [#45](https://github.com/readrops/Readrops/issues/45) | Closed | Closed upstream - verify in Droidrops | [Feature request] Automatically mark articles as read |
| [#44](https://github.com/readrops/Readrops/issues/44) | Closed | Closed upstream - verify in Droidrops | [Feature Requests] A couple actually. |
| [#43](https://github.com/readrops/Readrops/issues/43) | Closed | Closed upstream - verify in Droidrops | Fastlane improvements |
| [#42](https://github.com/readrops/Readrops/issues/42) | Closed | Closed upstream - verify in Droidrops | Login fails with nextcloud news |
| [#41](https://github.com/readrops/Readrops/issues/41) | Closed | Closed upstream - verify in Droidrops | Fail to import OPML |
| [#40](https://github.com/readrops/Readrops/issues/40) | Closed | Closed upstream - verify in Droidrops | F-droid as a source |
| [#39](https://github.com/readrops/Readrops/issues/39) | Closed | Closed upstream - verify in Droidrops | Download/share item content image on long press |
| [#38](https://github.com/readrops/Readrops/issues/38) | Closed | Closed upstream - verify in Droidrops | Open in webview |
| [#37](https://github.com/readrops/Readrops/issues/37) | Closed | Closed upstream - verify in Droidrops | Settings activity |
| [#36](https://github.com/readrops/Readrops/issues/36) | Closed | Closed upstream - verify in Droidrops | Display Freshrss feed addition result |
| [#35](https://github.com/readrops/Readrops/issues/35) | Closed | Closed upstream - verify in Droidrops | Add Item left/right swipe background |
| [#34](https://github.com/readrops/Readrops/issues/34) | Closed | Closed upstream - verify in Droidrops | [Meta bug] Test your apps in landscape mode |
| [#33](https://github.com/readrops/Readrops/issues/33) | Closed | Closed upstream - verify in Droidrops | [UI bug] Disparition of the "Add account" button |
| [#32](https://github.com/readrops/Readrops/issues/32) | Closed | Closed upstream - verify in Droidrops | [Feature Request] Add the "starred" feature of nextcloud-news |
| [#31](https://github.com/readrops/Readrops/issues/31) | Closed | Closed upstream - verify in Droidrops | [Keyboard Improvement] Have the (next) button in the bottom right of the android keyboard |
| [#30](https://github.com/readrops/Readrops/issues/30) | Closed | Closed upstream - verify in Droidrops | [UI Improvement] Avoid Keyboard Overlap EditText |
| [#29](https://github.com/readrops/Readrops/issues/29) | Closed | Closed upstream - verify in Droidrops | Read it later items |
| [#28](https://github.com/readrops/Readrops/issues/28) | Closed | Closed upstream - verify in Droidrops | LocalFeed parsing |
| [#27](https://github.com/readrops/Readrops/issues/27) | Closed | Closed upstream - verify in Droidrops | Read/unread item state |
| [#26](https://github.com/readrops/Readrops/issues/26) | Closed | Closed upstream - verify in Droidrops | New Logo for Readrops |
| [#25](https://github.com/readrops/Readrops/issues/25) | Closed | Closed upstream - verify in Droidrops | Add Feed Activity |
| [#24](https://github.com/readrops/Readrops/issues/24) | Closed | Closed upstream - verify in Droidrops | Edit Feed Dialog |
| [#23](https://github.com/readrops/Readrops/issues/23) | Closed | Closed upstream - verify in Droidrops | Feeds/folders management fragments |
| [#22](https://github.com/readrops/Readrops/issues/22) | Closed | Closed upstream - verify in Droidrops | Add support for direct feed url |
| [#21](https://github.com/readrops/Readrops/issues/21) | Closed | Closed upstream - verify in Droidrops | Folder UI |
| [#20](https://github.com/readrops/Readrops/issues/20) | Closed | Closed upstream - verify in Droidrops | Dark Theme |
| [#19](https://github.com/readrops/Readrops/issues/19) | Closed | Closed upstream - verify in Droidrops | OPML Import/export |
| [#18](https://github.com/readrops/Readrops/issues/18) | Closed | Closed upstream - verify in Droidrops | Account support |
| [#17](https://github.com/readrops/Readrops/issues/17) | Closed | Closed upstream - verify in Droidrops | Delete feed |
| [#16](https://github.com/readrops/Readrops/issues/16) | Closed | Closed upstream - verify in Droidrops | Folders |
| [#15](https://github.com/readrops/Readrops/issues/15) | Closed | Closed upstream - verify in Droidrops | Add feed Dialog |
| [#14](https://github.com/readrops/Readrops/issues/14) | Closed | Closed upstream - verify in Droidrops | Add Last-Modified and If-None-Match headers to feed requests |
| [#13](https://github.com/readrops/Readrops/issues/13) | Closed | Closed upstream - verify in Droidrops | Account list Activity |
| [#12](https://github.com/readrops/Readrops/issues/12) | Closed | Closed upstream - verify in Droidrops | Account creation Activity |
| [#11](https://github.com/readrops/Readrops/issues/11) | Closed | Closed upstream - verify in Droidrops | Splashscreen Activity |
| [#10](https://github.com/readrops/Readrops/issues/10) | Closed | Closed upstream - verify in Droidrops | Navigation Drawer |
| [#9](https://github.com/readrops/Readrops/issues/9) | Closed | Closed upstream - verify in Droidrops | Item Activity |
| [#8](https://github.com/readrops/Readrops/issues/8) | Closed | Closed upstream - verify in Droidrops | Items list Activity |
| [#7](https://github.com/readrops/Readrops/issues/7) | Closed | Closed upstream - verify in Droidrops | Database implementation |
| [#6](https://github.com/readrops/Readrops/issues/6) | Closed | Closed upstream - verify in Droidrops | Add feed feature |
| [#5](https://github.com/readrops/Readrops/issues/5) | Closed | Closed upstream - verify in Droidrops | Read it later items |
| [#4](https://github.com/readrops/Readrops/issues/4) | Closed | Closed upstream - verify in Droidrops | Json parsing |
| [#3](https://github.com/readrops/Readrops/issues/3) | Closed | Closed upstream - verify in Droidrops | Atom parsing |
| [#2](https://github.com/readrops/Readrops/issues/2) | Closed | Closed upstream - verify in Droidrops | RSS 2.0 parsing |
| [#1](https://github.com/readrops/Readrops/issues/1) | Closed | Closed upstream - verify in Droidrops | nothing to see here |

## Update rules

- Only mark an issue **fixed** with an identifiable commit, test, or manual check.
- A closed upstream issue must remain **to be verified** until the behavior has been validated in
  Droidrops.
- Uncommitted local changes are not considered a published fix.
- This file remains local and should not be added to a release commit.
