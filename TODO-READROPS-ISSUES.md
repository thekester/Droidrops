# Readrops Issues - Droidrops Follow-up

Last verified: 2026-10-21

This file is a local tracker of all issues in the upstream
[Readrops](https://github.com/readrops/Readrops) repository, compared with the Droidrops fork.

It is not intended to be committed or pushed into the public repository.

## Legend

- **Fixed in Droidrops**: An identifiable fix exists in the fork history.
- **Local fix, not committed**: A correction is in the working tree and its focused tests pass.
- **Partially addressed**: A documented compatibility path exists, but the full requested feature is not implemented.
- **Priority TODO**: issue explicitly examined, without sufficient correction at this stage.
- **TODO - no fix identified**: open issue upstream, without corresponding proof of work in Droidrops.
- **Awaiting upstream**: notification or response action, not a product correction.
- **Closed upstream - verify in Droidrops**: issue closed in Readrops; This does not prove that the fork
  was valid for this case.

## Summary

- Issues analyzed: **324**
- Corrected and validated in Droidrops: **62**
- Local fixes awaiting commit: **50**
- Analyzed without correction required: **6** (#348 already fixed, #115 legacy selection UI absent, #233 current input behavior verified, #224 encrypted-preferences recovery already present, #220 unsupported article URL schemes already rejected, #200 current Fever failure handled without crash)
- Open TODO, decision, or reproduction required: **26**
- Waiting upstream: **1**
- Closed upstream, still to verify in Droidrops: **190**
- Readrops issues opened by `thekester`: [#362](https://github.com/readrops/Readrops/issues/362).
- Earlier CI-equivalent verification (2026-10-16): clean Gradle `build` passed with
  `warningsAsErrors`; app/API unit tests passed.
- Latest verification (2026-10-17): 63 app unit tests, 158 API unit tests, 38 DB instrumentation
  tests, and 56 app instrumentation tests all passed. On the Pixel 9 / API 35 emulator, opened
  “Why Apple is finally making its own smart home devices,” force-stopped the app while the reader
  was open, then relaunched it; the same article was restored and logcat showed no fatal exception.
  The timeline and reader clock cues were visually checked on the same emulator. `:app:assembleRelease`
  also passed with `warningsAsErrors=true`.
- Current verification (2026-10-17): 63 app unit tests passed; DB (38) and app (56) instrumentation
  tests passed on Pixel 9 / API 35. `:app:assembleDebug` passed with `warningsAsErrors=true`.
  Manually verified the new timeline-image preference can be disabled and remains disabled when
  returning to the timeline. The seeded sample articles contain no article images, so their visual
  removal could not be demonstrated. A reader gesture check was attempted, but opening the sample
  article stayed on its loading indicator; issues #187 and #196 therefore remain pending their
  specific long-article/image interactions.
- Verification (2026-10-18): API (161), app (72), and DB (9) unit tests passed; app (63) and DB (39)
  instrumentation tests passed on Pixel 9 / API 35. `:app:assembleDebug` passed with
  `warningsAsErrors=true`. Added RSS 2.0 regressions for blank content and Unicode ellipsis truncation;
  instrumented checks confirmed the dedicated local-account icon is 24 dp and the launcher adaptive
  icon includes its themed monochrome layer. `git diff --check` passed.
- Verification (2026-10-18): #119/#122 updates passed app unit and instrumentation suites. App unit
  tests: 73; API: 161; DB: 9. App instrumentation: 65; DB instrumentation: 39 on Pixel 9 / API 35.
  `:app:assembleDebug` passed with warnings as errors. The new feed-source preference round-tripped
  through DataStore on the emulator. The app also launched at a simulated 914 dp window width without
  a fatal exception; emulator size and density were restored afterward.
- Verification (2026-10-21): `:db:connectedDebugAndroidTest` passed all 39 tests on Pixel 9 / API 35,
  including `tagFilterReturnsOnlyArticlesAssignedToSelectedTag` (#54). On the same emulator, changed
  Item size to Compact in Preferences and visually confirmed the timeline uses compact rows (#89).
  DecSync (#73) remains unimplemented; upstream describes it as a local feed backup and automatic
  synchronization mechanism, so it must integrate local feeds, folders, read state, and starred state.
- Verification (2026-10-18): triaged upstream reports #342, #343, and #351; all three are untouched
  crash-report templates without repro steps or stack traces, so no code fix can be tied to them yet.
  Re-ran API (161), app (73), and DB (9) unit tests, app (65) and DB (39) instrumentation tests on
  Pixel 9 / API 35, and `:app:assembleDebug` with warnings as errors. App launch produced no fatal
  exception in the inspected logcat window.
- Verification (2026-10-18): added emulator regressions for #318, #325, and #332; app (60) and DB (39) instrumentation tests pass on Pixel 9 / API 35, as do 65 app, 158 API, and 9 DB unit tests. `:app:assembleDebug` passes. Article WebView pinch-zoom settings are asserted by instrumentation; the app launches cleanly, but a real two-finger gesture could not be demonstrated because the seeded articles do not provide a stable image/article fixture.
- Follow-up verification (2026-10-17): #114's selected-folder label suppression was visually checked
  in the **Tech** folder on Pixel 9 / API 35. #200's Fever sign-in was submitted with a deliberately
  unreachable local URL; the form displayed **Unreachable URL** and remained usable, with no app
  fatal exception. Added French (`fr-FR`) Fastlane title and descriptions for #137. The app unit suite
  now has 65 passing tests; all 56 app instrumentation tests passed on the same emulator.
- Verification (2026-10-18): app unit tests passed, including URL normalization cases for #178; the
  focused app instrumentation run passed all 6 `LocalRSSRepositoryTest` cases on Pixel 9 / API 35.
  Manually checked Fever form text entry and the OPML choice dialog; no crash was reproduced.
- Verification (2026-10-18): #67 widget, #324 picker and #196 WebView zoom settings checked. App unit
  tests passed (66); app instrumentation passed (60) and DB instrumentation passed (39) on Pixel 9 /
  API 35. `:app:assembleDebug` passed with `warningsAsErrors=true`. The widget refresh button showed
  unread articles and tapping one opened the corresponding reader screen. An embedded-image pinch
  gesture remains unverified because the available article was link-only.
- Verification (2026-10-18): re-ran app (66), API (158), and DB (9) unit tests; all passed. App (60)
  and DB (39) instrumentation tests passed on Pixel 9 / API 35, and `:app:assembleDebug` passed with
  `warningsAsErrors=true`. Reviewed #187, #119, and #261: the reader bottom-bar slot now releases
  space for WebView gestures; the timeline image-visibility preference is persisted; and large
  article fields are bounded in database projections. The #261 multi-megabyte DAO regression ran on
  the emulator. The current sample feed has no suitable long, image-bearing article, so the actual
  #187 gesture and the visual image-hidden card layout remain unverified in this run.
  Installed the debug APK and launched it on the same emulator; no fatal exception appeared in
  the inspected logcat window.
- Follow-up verification (2026-10-18): #178/#200 URL normalization edge cases and #357's reported
  YouTube `file://` link were added to regression coverage. App (68), API (158), and DB (9) unit
  tests passed; app (60) and DB (39) instrumentation tests passed on Pixel 9 / API 35. The debug
  build passed with `warningsAsErrors=true`.
- Follow-up verification (2026-10-18): #66, #69, and #338 regression coverage refreshed. App (68),
  API (159), and DB (9) unit tests passed; app (61) and DB (39) instrumentation tests passed on
  Pixel 9 / API 35. The debug build passed with `warningsAsErrors=true`. Public feeds send no
  Authorization header, protected-feed Basic Auth works, self-hosted URL syntax passes, and
  multiline title normalization is covered by parser and whitespace tests.
- Follow-up verification (2026-10-18): #187, #333, and #356 received additional regression coverage.
  App unit tests: 72 passed; API: 159; DB: 9. App instrumentation: 61 passed and DB instrumentation:
  39 passed on Pixel 9 / API 35. Debug build passed with warnings as errors. The #187 gesture and
  reported Z Flip 5 OOM are still not directly reproduced on this emulator.
- Follow-up verification (2026-10-18): #114 adds a persisted article font size control; #187 bottom
  bar slot-sizing and #356 bounded pager regressions remain green. Full suite: 161 API, 73 app, and
  9 DB unit tests; 69 app and 39 DB instrumentation tests passed on Pixel 9 / API 35. Debug build
  passed with warnings as errors. The #187 long-article gesture and #356 foldable OOM still need
  suitable device/content reproduction.
- Revalidation (2026-10-18): selected upstream issues #347, #346, and #338. On Pixel 9 / API 35,
  the backup exclusion regression, image-only feed item handling, and app instrumentation suite all
  passed. The Atom multiline-title regression is covered by the API suite. Full results: 161 API,
  73 app, and 9 DB unit tests; 69 app and 39 DB instrumentation tests; `:app:assembleDebug` passed
  with `-PwarningsAsErrors=true`. These fixes were already present in the current Droidrops release;
  this pass revalidated them and corrected stale tracker statuses. The Android backup restore cycle
  itself remains unverified.

- Verification (2026-10-18): added local fixes for #114 feed-list density and #119 article-preview
  visibility. Revalidated #120 true-black theme. The full suites passed: API (161), app (73), and DB
  (9) unit tests; app (71) and DB (39) instrumentation tests on Pixel 9 / API 35; `:app:assembleDebug`
  passed with warnings as errors. Manually checked that Compact shortens feed-row spacing (95 px to
  75 px on this emulator), hiding previews removes large-card excerpts but preserves titles, and
  True black renders black surfaces.

- Verification (2026-10-18): implemented #252 (full article), #122 (two-pane reader) and #339
  (local-account backup/restore). Full suites passed with `-PwarningsAsErrors=true`: API (161), app
  (88) and DB (9) unit tests; app (73) and DB (39) instrumentation tests on Pixel 9 / API 35;
  `:app:assembleDebug`. Each feature was then exercised in the emulator UI as described in its
  section; two issues found that way (missing restore confirmation, restored account named like the
  original) were fixed and rechecked.
- Follow-up (2026-10-18): completed the remaining parts of #122, #252 and #339 (stacked portrait
  panes, keyboard shortcuts, per-feed full article, server-account and settings backups, restore from
  the account selection screen, plural messages). Full suites passed with `-PwarningsAsErrors=true`:
  API (161), app (96) and DB (9) unit tests; app (74) and DB (39) instrumentation tests on Pixel 9 /
  API 35; `:app:assembleDebug`; `git diff --check`. Emulator checks surfaced and fixed three more
  issues: a pre-existing modal-drawer strip overdrawing the navigation rail on portrait tablets, a
  timeline pager rebuilt (emptied and scrolled) on every article opened while "keep recently read
  visible" is on, and the default focus highlight greying the whole reader on a key press.

### #194 - Support importing old `.opml` files

Status: **legacy OPML import covered by regression test; API unit tests pass**

- Added a Feeder-style OPML 1.1 fixture with nested folders and namespaced metadata attributes.
- Verified that the feed name, feed URL, site URL, and folder survive parsing. The parser accepts the
  version without rejecting valid extension attributes or requiring the OPML 2.0 version number.
- `:api:testDebugUnitTest`: 157 tests passed.

### #111 - Link actions dialog on long pressing on a link

Status: **implemented locally; emulator UI verified**

- Long pressing a link in an article opens a dialog that shows its URL and offers Open, Copy, and Share.
- Kept image long-press actions separate. Copying puts the URL on the clipboard; opening respects the
  configured browser/custom-tab preference.
- Verified the dialog and its URL on Pixel 9 / API 35; all three actions are present.

### #233 - Missing expected cursor scroll behaviour

Status: **current URL field behavior verified on Pixel 9 / API 35; no code change needed**

- Entered a long URL in Add feed. The one-line field scrolled horizontally to show the end.
- Long-pressed near the beginning to select the prefix; the field scrolled back to keep the selection
  handle and beginning of the URL visible. No reproducible cursor-scroll failure remains in this build.

### #187 - Let article scrolling start in the hidden action-bar area

Status: **local slot-sizing fix covered by new layout tests; manual gesture remains unverified**

- The action bar moved out of view, but Scaffold still reserved its full height, leaving the bottom
  strip outside the article WebView's touch area. The Scaffold bottom-bar slot now shrinks with the
  bar's hidden offset so the article view fills and can receive gestures in that area.
- Kept the slot at its full height for articles that do not scroll, where the bar is intentionally
  kept visible.
- Extracted the slot-height calculation into `visibleBottomBarSlotHeight`; unit tests cover fully
  hidden, partially hidden, and non-scrollable states.
- App instrumentation (61 tests) and DB instrumentation (39 tests) pass on Pixel 9 / API 35.
- The current sample feed still has no stable long article to exercise the swipe beginning in the
  hidden action-bar area; the gesture-level behavior remains unverified.

### #209 - Keep the timeline accessible during synchronization

Status: **local change implemented; app instrumentation suite passes**

- Removed the empty-pager reset at the start of refresh. The existing timeline remains in place
  while synchronization runs, and local-feed progress is shown in a compact banner above it.
- If there are no cached articles yet, the original full-screen progress state remains in use.
- App instrumentation passed on Pixel 9 / API 35. A live network sync on the emulator completed
  quickly, so the transient progress banner itself was not captured during that check.

### #215 - Show article thumbnails next to timeline titles

Status: **implemented locally for regular and compact cards; app suite passes**

- Regular and compact timeline cards now show a small cropped thumbnail beside the title when the
  feed entry provides an image. Large cards keep their existing full-width image preview.
- The emulator's current sample feed entries did not provide thumbnails, so the new thumbnail
  rendering could not be visually demonstrated with this test data.

### #304 - Share a selected excerpt with its source

Status: **implemented locally; citation formatting unit tests and app suite pass**

- Added **Share selection with reference** to the Android text-selection action menu in articles.
  The shared text includes the selected excerpt, available author/title/feed metadata, and article
  URL.
- Unit tests cover complete and missing metadata. The emulator feed samples expose link-only
  entries, so the native selection action menu was not manually exercised in this run.

### #196 - Zoom and scroll bars

Status: **image viewer, article pinch zoom, and scroll indicators are implemented locally; instrumentation passed**

- Image taps already open Droidrops' image viewer, which supports pinch zoom and panning. The
  article WebView now shows Android's transient vertical scroll indicator, and the timeline shows a
  transient position indicator while scrolling.
- `ItemWebViewZoomTest` verifies pinch support is enabled without on-screen zoom buttons. The app
  instrumentation suite passed (60 tests) on Pixel 9 / API 35.
- The reader article opened from the widget had link-only content, so a physical pinch on an embedded
  image was not demonstrated in this emulator session.

### #119 - Layout options

Status: **timeline content visibility and card-size controls implemented locally; preference regressions added**

- Added a **Show article images in timeline** preference. Turning it off hides both inline thumbnails
  on compact/regular cards and the large preview image, while retaining text, feed details, and tags.
- The preference is stored in DataStore and defaults to on, preserving the existing appearance.
- Added a **Show feed source** preference. When disabled, feed icon, name, and folder labels are omitted
  from compact, regular, and large timeline cards. Its DataStore default is on. Instrumentation verifies
  default, off, and reset-to-on behavior.
- Added separate **Show publication date** and **Show estimated reading time** preferences. Both
  default to on and work independently; hiding both removes the metadata badge.
- Added **Show article previews**, a persisted switch that hides excerpts on large timeline cards
  while retaining their titles and source details. This allows a shorter timeline without changing
  the selected card layout.
- Verified the switch in Preferences, toggled it off, returned to the timeline, and confirmed the
  choice remained off. The seeded emulator articles had no preview images, so the resulting card
  layout could not be visually compared with an image-bearing entry.
- `:app:connectedDebugAndroidTest` (71 tests) and `:db:connectedDebugAndroidTest` (39 tests) passed
  on Pixel 9 / API 35. The current full verification above also includes API (161), app (73), and DB
  (9) unit tests and a successful debug build with warnings as errors.

### #122 - Tablet mode

Status: **two-pane reader implemented locally (opt-in); unit tests and emulator UI check passed**

- The timeline uses a permanent navigation drawer on tablet-sized landscape windows and a modal drawer
  in portrait. Tablet classification now starts at 600 dp, matching the medium-window breakpoint;
  regression tests cover 599, 600, 719, and 720 dp.
- Added the opt-in **Two-pane reading on large screens** preference (off by default, as the issue
  asks). When the window is at least 840 dp wide on a tablet (smallest width ≥ 600 dp), opening a
  local article shows it beside the timeline instead of pushing a full screen. A phone in landscape
  (≈923 × 411 dp) keeps the full-screen reader.
- The pane hosts the regular `ItemScreen` in its own navigator, disposed when the article changes, so
  pending read/star changes are written exactly as when leaving the full-screen reader. Back and the
  reader's back arrow close the pane. If the window becomes narrow (or the option is turned off) with
  an article open, it continues in the full-screen reader.
- Portrait tablets (smallest width ≥ 600 dp, height ≥ 840 dp) stack the panes as the issue suggests:
  timeline above, article below. The lower pane does not reserve status-bar space.
- Hardware keyboard shortcuts (requested in an upstream comment): J/N next, K/P previous, M read,
  S favorite, F full article, O/V open in browser. Only keys no view consumed reach them, so typing in
  search is unaffected, and Ctrl/Alt/Meta/Shift combinations are ignored. In the full-screen reader
  J/K page through articles; with the side pane they walk the timeline itself.
- A typed key leaves touch mode, and Android then focused the first control (the permanent drawer's
  close button), which showed a grey focus ring after every shortcut. Clearing focus does not work
  outside touch mode (Android gives it back to that control, as logged on the emulator), so shortcut
  key presses now move focus to an invisible, indication-free anchor in the reader and timeline,
  waiting for the layout when the pane just switched article. Verified on the 1280 × 800 dp emulator:
  no ring after J, M, or M with no article open; typing "jk" in search still filled the field.
- Fixed while testing: on portrait tablets the closed modal drawer overdrew the navigation rail as a
  strip (pre-existing; the drawer is now clipped to its area), and opening an article with "keep
  recently read visible" rebuilt the timeline pager, emptying and scrolling the visible list. The
  existing pager is now reloaded in place when only the recently read ids change.
- `ItemPaneTest` covers both layouts, the opt-in and phones in both orientations;
  `KeyboardShortcutsTest` covers the key mapping and modifiers.
- On Pixel 9 / API 35 simulated as 1280 × 800 and 800 × 1280 dp tablets: articles opened beside and
  below the timeline and were marked read; Back closed the pane; J moved to the following articles
  with the list staying in place; M toggled read; in the phone reader S starred, K/J paged and the
  star was removed again. Emulator size, density and preferences were restored afterward.
- The article open in the side pane is highlighted in the timeline (primary outline, tinted card,
  `selected` accessibility state) in all three card sizes, and the list keeps it in view as J/K move
  through articles. Checked on the 1280 × 800 dp emulator: the outline followed three J presses and
  the list scrolled to keep the selected card fully visible.

### #252 - Show the full article content

Status: **implemented locally; extractor unit tests pass and emulator check passed on a real article**

- Added a **Load full article** button to the reader bar for articles with a link. It downloads the
  page and extracts the main text with a Readability-style jsoup scorer (no new dependency): page
  chrome, comments, share bars, sidebars and link lists are dropped, lazy images are resolved and
  URLs made absolute. Tapping the button again returns to the feed content.
- Non-destructive: the extracted text is kept for the reader session only and never replaces the
  stored entry, so a poor extraction can always be dismissed and syncs are unaffected.
- Uses a dedicated HTTP client: the shared API client may attach the account's `Authorization`
  header, which must not be sent to article sites. Only http(s), HTML responses up to 5 MB.
- When no article text is found, the reader shows **No article text could be found on this page**
  and keeps the feed content.
- `FullArticleExtractorTest` (4 cases) covers chrome removal, absolute URLs, lazy images, link lists
  and the no-content case.
- On Pixel 9 / API 35 with the Hacker News feed: the Guardian article loaded in full and cleanly
  (no navigation or footer), and the button switched back to the feed entry. A twitter.com link
  correctly reported that no text could be found (JavaScript-only page).
- Added a per-feed **Always load the full article** switch in the feed sheet. Articles of those feeds
  load the extraction as soon as they are shown; a failure is silent and leaves the feed content, the
  bar button still allowing a retry. The list of feeds is kept in DataStore (no database migration)
  and carried in backups (#339).
- Verified on the emulator with Hacker News: LWN, GitHub and other linked articles opened directly in
  full, including in the two-pane reader; the option was turned off again afterward.
- Not implemented: double-tap to load (the bar button and F key cover it). Ids of deleted feeds stay
  in the stored list; they are never reused, so this has no effect.

### #339 - Back up and restore all data

Status: **implemented locally for all accounts and app settings; unit, instrumentation and emulator
UI checks passed**

- Every account gets **Back up and restore** in the Account tab. A backup is a plain JSON file with
  folders, feeds and their per-feed settings (notifications, open-in choice, color), and every
  article with its read and favorite state.
- No secrets are exported: no account token, and feed HTTP credentials stay in the encrypted
  preferences. Feed cache headers are dropped so the first sync after a restore refetches feeds.
- Restore is non-destructive: it creates a new local account named "(restored)" after the original,
  which becomes the current account, in a single Room transaction. An invalid or truncated file
  leaves the database untouched; existing accounts are never modified.
- Export and import are streamed (keyset-paginated reads, batched inserts), so large histories are not
  loaded in memory at once. Article bodies longer than 100,000 characters are exported with the same
  bounded projection the reader uses (#261) and are therefore truncated.
- `LocalAccountBackupFormatTest` (7 cases) covers round trip, absence of secrets, batching, empty
  accounts and rejected files. `LocalAccountBackupRepositoryTest` (2 instrumented cases) restores
  250 articles across two feeds and a folder, and checks a truncated file restores nothing.
- On Pixel 9 / API 35: exported the emulator's local account through the system file picker, checked
  the JSON, restored it as **Local account (restored)** with its confirmation message, then deleted
  that test account and restored the original current account.
- Server accounts can be backed up too and are restored as a local account (a way to leave a server
  with one's history): separate read/star states are merged into the articles, server feed ids are
  dropped and article links become the ids, so later local syncs recognise existing entries.
- App settings are included, except installation state (last version, notification prompt). Restoring
  them is never automatic: after a restore, a dialog asks to replace the current settings or keep
  them. A setting stored with a different type is skipped so the app cannot fail reading it.
- Per-feed "always load the full article" choices are carried and remapped to the new feed ids.
- **Restore a backup** is also offered on the account selection screen, for a new device.
- Messages use plurals ("1 feed", "2,770 articles").
- Tests: `LocalAccountBackupFormatTest` (9 cases, including the new fields and first-version files),
  `BackupPreferenceTest` (3), and `LocalAccountBackupRepositoryTest` (3 instrumented, including a
  FreshRSS account with separate states and no exported token).
- On the emulator, the real FreshRSS account (15 feeds, 2,770 articles) was backed up to a 12 MB file:
  the account token was absent. Restoring it created **FreshRSS (restored)** as a local account with
  the same 15 feeds, 2,770 articles, 74 read and 1 favorite, the Hacker News full-article choice on
  its new feed id, and asked about the settings. The test account and backup file were then deleted
  and FreshRSS made current again.
- Article tags (FreshRSS labels) are exported with the account and restored as local tags linked to
  the same articles, so the drawer's tag filters keep working. Covered by a format round-trip test and
  by the instrumented FreshRSS restore. The emulator's FreshRSS account has no labels, so this part
  was not exercised through the UI.

### #260 - Feed source for launchers

Status: **partially addressed by the latest-articles home-screen widget; launcher overlay service remains unsupported**

- Droidrops provides a launcher widget listing the newest unread articles, with refresh and article-open
  actions. An emulator regression checks that the widget is registered in the system widget picker and
  has a valid size/layout. Its query and refresh/open flow were manually verified on the emulator.
- This is the widget alternative raised in the upstream discussion. It does not implement Lawnchair's
  `DrawerOverlayService` or provide a feed page inside launchers that expose a Discover-style overlay.

### #211 - Editable feed history

Status: **local history-retention control implemented; DB and app instrumentation tests pass**

- Added a global **Maximum articles per feed** setting with 250, 500, 1,000, 2,500, 5,000, and
  Unlimited choices. Old unstarred rows are pruned after local-feed synchronization; starred items
  are retained even when they fall outside the selected limit.
- Added a database regression test proving newest-item retention and favorite preservation, plus an
  app synchronization test for the configured limit. Both instrumentation suites pass on Pixel 9 /
  API 35. The settings row and choice dialog were visually checked in the emulator.

### #213 - Limit RSS item history

Status: **local item retention is configurable; app synchronization test passes**

- Local RSS synchronization now inserts at most the configured newest items per feed and prunes
  older unstarred rows; the same retention setting is shared with #211. Existing starred items are
  preserved.
- This bounds the app's stored history and timeline data. The RSS response itself is still
  downloaded and parsed in full, so this does not reduce server transfer size or parsing time.

### #216 - Filter to recently read articles

Status: **implemented locally; query instrumentation and filter UI verified on Pixel 9 / API 35**

- Added a separate **Recently read only (this session)** filter. It reuses the capped session history
  of up to 100 recently opened or read articles, so users can return to an article without finding
  its feed again. The existing “keep recently read visible” option remains independent.
- The query returns only IDs from the current session and safely returns no results when the session
  history is empty. Added database instrumentation for populated and empty histories.
- Verified that the new option appears and toggles in the timeline filter sheet. The history is
  intentionally session-only and clears when the app process ends.

### #338 - Keep timeline titles on one line

Status: **existing fix verified on Pixel 9 / API 35**

- Compact, regular, and large timeline cards share `TimelineItemTitle`, which limits titles to one
  line and uses an ellipsis for overflow. The emulator timeline showed the same single-line behavior.


## Fixes identified

### #89 and #114 - Compact timeline layout

Status: **#89 covered; #114's timeline and feed-list density controls implemented locally**

- Timeline preferences already expose Compact, Regular, and Large item sizes. Compact cards omit
  feed/category and preview details, and use reduced inter-item spacing.
- #89's compact timeline request is covered. #114 now also hides a folder name from each article
  card when that folder is already selected and shown in the timeline header. A small presentation
  helper has unit tests for matching and non-matching folders.
- Added a persisted reader text-size preference with Small, Normal, Large, and Extra large choices.
  The reader applies the corresponding CSS body font size in its article WebView; the default keeps
  the existing rendering. English and French settings labels are included.
- Added a persisted **Feed list spacing** preference with Comfortable and Compact choices. Compact
  reduces vertical row and folder-header padding and slightly reduces feed-icon size; Comfortable
  remains the default.
- Emulator regressions now check the density and preview visibility defaults and their persistence.

### #137 - Translatable Fastlane entries

Status: **French Play Store metadata added locally; translation automation remains out of scope**

- Added `fr-FR` title, short description, and full description alongside the existing `en-US` listing.
- The translated text fits Google Play's title, short-description, and full-description limits.
- The screenshots continue to use the existing English-default assets until localized screenshots
  are prepared.

### #200 - Fever setup error in 2.0-beta02

Status: **Fever URL normalization hardened; historical Koin crash still not reproduced**

- Opened the Fever account form on Pixel 9 / API 35 and submitted test credentials against an
  unreachable local URL. The app showed **Unreachable URL**, stayed on the form, and logcat contained
  no app fatal exception.
- Follow-up hardening: URL normalization now inserts the required trailing slash before query
  parameters or fragments, and leaves blank input blank. Regression tests cover both cases. This
  avoids corrupting a Fever endpoint supplied with query parameters; it does not identify the cause
  of the old Koin trace, which remains unavailable.

### #120 - True black theme

Status: **fixed in the current Droidrops code; unit test and emulator visual check passed**

- Added a True black theme choice. It uses black for the window background, cards, and Material 3
  surface containers, with a near-black surface variant for separation.
- Added `TrueBlackThemeTest` to verify the scheme's surface colors and added the French label.
- Pixel 9 / API 35 showed a black preferences screen and black timeline surfaces after selecting
  **True black**.

### #217 - Show the article publication time

Status: **implemented in Droidrops; emulator UI verified**

- Timeline badges format the publication date and local time with `DateUtils.formattedDateTimeByLocal`.
- `DateUtilsTest.formattedDateTimeIncludesPublicationTimeInLocalLocale` checks that both date and
  time are present. On Pixel 9 / API 35, the timeline visibly showed entries such as
  `Oct 16, 2026 6:02 AM · 5 min read`.

### #232 - Filter the timeline to unread articles

Status: **implemented in Droidrops; emulator UI verified**

- The timeline filter sheet exposes the **Show read articles** switch; disabling it limits the
  timeline to unread items. The existing query tests cover the `read = 0` selection and execute the
  resulting SQL.
- Verified on Pixel 9 / API 35 by toggling the switch: the previously visible read item disappeared
  from the timeline. The original setting was restored after the check.

### #277 - Keep recently read articles visible

Status: **implemented locally; database tests and emulator UI check passed on Pixel 9 / API 35**

- Added an opt-in **Keep recently read articles visible** filter option, shown when read articles
  are hidden. Recently opened or marked-read items remain in the filtered timeline until the app
  process closes; the session list is capped at 100 item IDs. The option is off by default.
- Extended the regular and separate remote-state SQL queries to include those recent item IDs while
  retaining the unread-only filter for all other articles.
- Added two `ItemsQueryBuilderTest` cases. Verified on the emulator by enabling the option, opening
  an unread article, and returning to the timeline: the article remained visible.
- #216 asks for a distinct Recently Read view, which this option does not provide; that separate
  request remains open.

### #250 - Restore article reading position

Status: **fixed locally; database and app instrumentation suites passed; emulator UI verified**

- The position was saved, but restoration ran before the WebView content had its final height, so
  Android clamped the requested scroll back to the top. Retry restoration while the content lays
  out, for up to five seconds, then clamp to the available range if the saved position is no longer
  reachable.
- Existing `ArticleScrollPositionsTest` verifies persistence and non-negative values. On Pixel 9 /
  API 35, scrolled an article to 1,501 px, returned to the timeline, reopened the same article, and
  confirmed the reader resumed at the saved mid-article position.

### #314 - Keep Favorites independent from the unread-only filter

Status: **implemented in Droidrops; database instrumentation tests passed on Pixel 9 / API 35**

- The Favorites query already bypasses the global unread-only condition, so saved articles remain
  visible after being read while the Articles timeline continues to show unread items only.
- Verified by the regular and separate remote-state Favorites query tests in
  `ItemsQueryBuilderTest`. This is the existing #291 correction; the tracker status was stale.

### Add Feed screen runtime crash (newly found during emulator testing)

Status: **fixed locally; emulator UI verified on Pixel 9 / API 35**

- The Add Feed screen crashed with `NoSuchMethodError` in Material 3's
  `ExposedDropdownMenuBox`: AboutLibraries pulled a newer Compose Multiplatform Material 3 runtime
  than the Android Compose BOM used to compile the app.
- Excluded that transitive runtime so the Android Material 3 compile and runtime versions align.
  Reinstalled the debug build, opened **Feeds → Add**, and confirmed the discovery screen loaded
  without a fatal exception.

### #225 - Jump to most recent read

Status: **fixed locally; emulator restoration verified**

- The root, unsearched timeline now remembers its scroll index and pixel offset independently
  per account, and restores them after refresh and app restart. Other filters keep their normal
  scroll-to-top behavior.
- Added `TimelineScrollPositionTest` coverage for persistence/account isolation and the eligible
  filter scope. On Pixel 9 / API 35, scrolled to an article, force-stopped and reopened the app;
  it restored the previously visible article.
- Verified with the full DB and app instrumentation suites: 35 DB tests and 49 app tests passed.

### #224 - Crash on opening

Status: **already handled by encrypted-preferences recovery; no additional change needed**

- `createEncryptedPreferences` already detects invalidated/corrupt encrypted preferences, removes
  the stale store/key and retries, preventing startup from failing on this condition.
- `EncryptedPreferencesRecoveryTest.staleEncryptedPreferencesDoNotPreventStartupTest` covers recovery.
  It passed as part of the 51 app instrumentation tests on Pixel 9 / API 35.

### #218 - Crash immediately on open from an unsupported legacy account type

Status: **fixed locally; legacy Feedly sync regression test passes on Pixel 9 / API 35**

- Feedly's account configuration is local, but `Account.isLocal` previously recognized only the
  `LOCAL` enum value. An old Feedly account could therefore enter the network credential path and
  throw `Unknown account type` during synchronization.
- `Account.isLocal` now follows the account's configured mode, so legacy Feedly data uses the local
  repository path. `SyncWorkerTest.legacyFeedlyAccountDoesNotCrashSynchronization` verifies a
  successful sync result.

### #238 - Undo marking an article as read

Status: **local fix applied; Undo verified on emulator**

- Timeline read actions show a snackbar with an **Undo** action. Undo now explicitly writes the
  unread state instead of toggling a possibly stale in-memory state.
- On Pixel 9 / API 35, swiping an article displayed **Marked as read** and **Undo** before the code
  change. The explicit unread-state write restores the unread state rather than toggling stale data.
- Retested the swipe snackbar and tapped **Undo** on Pixel 9 / API 35; the snackbar closed and the
  app remained on the timeline instead of opening the article. Full instrumented suites passed.

### #227 - FreshRSS login reports HTTP 404 for an API URL

Status: **fixed locally; FreshRSS URL normalization unit test passes**

- FreshRSS account URLs now accept the server root, `/api/greader.php`, or the full
  `/api/greader.php/reader/api/0` path, trimming surrounding whitespace and producing the Retrofit
  base URL without duplicating the endpoint.
- `CredentialsTest.freshRssReaderApiUrlIsNormalizedToRetrofitBaseTest` covers the full API URL.
  `:api:testDebugUnitTest` passes (158 tests).

### #220 - Crash when an article contains a `data:` link

Status: **existing URL-scheme guard verified; no additional code change needed**

- Article links are handed to `Context.openUrl` or `openInCustomTab`, which reject non-HTTP(S)
  schemes before creating an intent. This prevents `ActivityNotFoundException` for `data:` URLs.
- Added `ContextExtensionsTest.openUrlRejectsDataUrisWithoutCrashing`; it passed in the 51 app
  instrumentation tests on Pixel 9 / API 35.

### #262 - Simplify `FolderDao.selectFoldersAndFeeds`

Status: **fixed locally; DB and full app instrumentation suites passed on Pixel 9 / API 35**

- Replaced the filtered Feed subquery with a direct `Feed LEFT JOIN Folder` and an explicit
  `Feed.account_id` selection. Removed the tautological folder-null check.
- Added `FolderDaoTest.foldersAndFeedsQueryFiltersByAccountWithoutRedundantFeedSubquery` to assert
  the generated SQL and execute it against the test database.
- Verification: 31 DB instrumentation tests, 47 app instrumentation tests, and app/API unit tests
  passed with `:db:connectedDebugAndroidTest :app:connectedDebugAndroidTest :app:testDebugUnitTest
  :api:testDebugUnitTest` on Pixel 9 / API 35.

### #235 - Keep feeds when deleting a FreshRSS folder

Status: **behavior verified; folder deletion regression test passed on Pixel 9 / API 35**

- Deleting a folder detaches its feeds to the default/root folder; the feed rows are not deleted.
- Room's `Feed.folder_id` foreign key uses `SET_NULL`. Regression coverage in
  `FolderDaoTest.deletingFolderMovesItsFeedsToTheDefaultFolder` checks that behavior on device.
- This matches FreshRSS folder deletion semantics described in the upstream issue discussion.

### #243 - Do not hide read favorites with the global unread filter

Status: **fixed and covered on Pixel 9 / API 35; same correction as #291**

- The Favorites query now ignores the global hide-read filter, so previously saved articles remain
  available after they are read.
- Covered by both `ItemsQueryBuilderTest` cases for regular and separate remote read-state storage.

### #264 - Prevent article links with `file://` URLs from crashing the app

Status: **fixed in Droidrops; custom-tab guard test and app instrumentation suite passed**

- Article links are restricted to HTTP(S) before opening a browser or custom tab. Local file and data
  URLs are rejected with a user-facing message instead of being sent in an intent.
- `ContextExtensionsTest.customTabRejectsLocalFileUrisFromArticleLinks` covers file and data URLs.
- The hardening is in commit `78416f3c`.

### #291 - Keep read articles visible in Favorites

Status: **fixed locally; Android database tests passed on Pixel 9 / API 35**

- The global "hide read articles" preference was incorrectly applied to Favorites.
- Favorite queries now continue to show starred articles regardless of read state, including when
  read/star state is stored separately by the remote service.
- Regression coverage: `ItemsQueryBuilderTest.favoritesIncludeReadItemsWhenReadItemsAreHiddenGlobally`
  and `favoritesWithSeparateStateIncludeReadItemsWhenReadItemsAreHiddenGlobally`.

### #293 - Use a modal feed drawer on portrait tablets

Status: **fixed in Droidrops; verified on a tablet-sized Pixel 9 / API 35 emulator**

- The feed drawer is permanent only when both tablet width and landscape orientation are active.
  Portrait tablet layout keeps the timeline full-width and opens the drawer from the app bar.
- The orientation-specific behavior is implemented in `TimelineTab` and was verified by temporarily
  configuring the emulator to 720 dp portrait; the screen showed the modal drawer control and no
  permanent half-width navigation pane.
- The issue remains open upstream; the orientation fix is in commit `950c731f`.

### #290 - Ignore nameless empty Nextcloud News folders

Status: **fixed in Droidrops; API adapter tests passed**

- Nextcloud News can return an empty folder with a blank name. The adapter skips that unusable folder
  instead of failing parsing and hiding all feeds.
- Regression coverage: `NextcloudNewsFoldersAdapterTest.emptyUnnamedFoldersAreIgnored`.
- The issue remains open upstream; the fix is present in commit `bc03f576`.

### #280 - Accept a complete FreshRSS API endpoint URL

Status: **fixed in Droidrops; API unit tests passed**

- FreshRSS account setup now accepts either the server root or a URL already ending in
  `/api/greader.php`, avoiding a duplicated endpoint and the resulting login failure.
- Regression coverage: `CredentialsTest.freshRssServerRootGetsApiEndpointTest` and
  `freshRssApiEndpointIsNotDuplicatedTest`.
- The issue remains open upstream; the fix is present in commit `dc37f286`.

### #350 - FreshRSS credentials screen crash

Status: **fixed and verified on Pixel 9 / API 35**

- The credentials editor previously force-unwrapped login and password fields on an `Account`.
  These fields are excluded from Room and are therefore null after loading an existing account,
  matching the reported `NullPointerException` in `AccountCredentialsScreenModel`.
- Commit [`11ab8c7a`](https://github.com/thekester/Droidrops/commit/11ab8c7a) now restores credentials
  from encrypted preferences and handles missing values safely.
- On the emulator, the saved FreshRSS credentials screen opened, validation returned to the app,
  and logcat showed no runtime crash.

### #309 - Pull-to-refresh animation crash

Status: **fixed and verified on Pixel 9 / API 35**

- `TimelineTab` keeps `PullToRefreshBox` composed while the timeline moves through loading, error,
  and content states. This prevents its animation node from detaching while it reads a Compose
  `CompositionLocal`.
- The correction is in [`ea73be77`](https://github.com/thekester/Droidrops/commit/ea73be77).
- Pull-to-refresh on the FreshRSS timeline completed on the emulator with no fatal exception.
  The full app instrumentation suite passed **47/47**, and app unit tests passed **54/54**.

### #303 - Disable horizontal article paging

Status: **implemented and verified on Pixel 9 / API 35**

- Preferences already includes a **Swipe between articles** toggle. `ItemScreen` binds it to
  `HorizontalPager.userScrollEnabled`, so disabling it prevents accidental article changes while
  scrolling.
- The preference wiring is in [`a55ea260`](https://github.com/thekester/Droidrops/commit/a55ea260).
- On the emulator, with the toggle off, a horizontal swipe left the current article open. The
  original enabled setting was restored after the check.

### #365 - Duplicate timeline keys when item IDs overlap between accounts

Status: **fixed in Droidrops; regression instrumentation passed**

- `ItemState.remote_id` is unique per account, but the timeline query joined state on `remote_id`
  alone. If two accounts had the same ID, one article produced multiple rows and Compose received
  duplicate lazy-list keys.
- State joins now also match the owning account in timeline, article, feed-count, and pending-state
  queries.
- Regression test: `ItemStateAccountIsolationTest.sameRemoteIdInAnotherAccountDoesNotDuplicateTimelineItem`
  inserts matching IDs in separate accounts and verifies the selected timeline has one row with the
  correct account's state.
- `:db:connectedDebugAndroidTest` passed on the Pixel 9 / API 35 emulator, including the duplicate-ID
  regression case.

### #344 - Per-feed external-browser preference is ignored

Status: **fixed in Droidrops; unit and emulator checks passed**

- Timeline opening used the global Custom Tab preference even when a feed explicitly selected
  `EXTERNAL_VIEW`.
- The per-feed external-browser choice now takes precedence. Articles without a link open in the
  in-app reader instead of force-unwrapping a missing URL.
- Regression coverage: `TimelineOpenActionTest` covers the per-feed override, no-link fallback, and
  default Custom Tab behavior.
- On Pixel 9 / API 35, set The Verge feed to **External view**, opened its feed, then opened an article;
  Chrome displayed the article URL. Restored the feed setting to **Local view** afterward.

### Follow-up verification (2026-10-18)

- Rechecked the Android backup rules for #347 and found the Room database was still backed up even
  though it stores session tokens. Added explicit database exclusions for cloud backup and device
  transfer, alongside the existing encrypted-credentials exclusions. Android backup will no longer
  restore local feeds/articles/settings; OPML remains the supported feed-list export.
- Completed two remaining #119 layout options: independent publication-date and estimated-reading-time
  visibility switches, both enabled by default. Added emulator coverage for their defaults and toggles.
- Full verification passed with `-PwarningsAsErrors=true`: 161 API unit tests, 73 app unit tests,
  9 DB unit tests, 67 app instrumentation tests, and 39 DB instrumentation tests on Pixel 9 / API 35.
  `:app:assembleDebug` also passed.
- Installed and launched the debug build on `emulator-5554`; no `FATAL EXCEPTION` or app exception
  appeared in the inspected logcat window.
- #122 and #260 remain open: respectively, the requested tablet list/detail pane, and the launcher
  overlay service. They need implementation work beyond the fixes in this pass.

### Follow-up verification (2026-10-17)

- Selected three issues not covered in the previous pass: #365, #344, and #361. Confirmed the fork fixes
  for each and updated their tracker rows so they no longer incorrectly say “not committed.”
- `:app:assembleDebug`, `:app:testDebugUnitTest`, `:db:testDebugUnitTest`,
  `:db:connectedDebugAndroidTest`, and `:app:connectedDebugAndroidTest` all passed with
  `-PwarningsAsErrors=true`.
- Results: 65 app unit tests, 9 DB unit tests, 56 app instrumentation tests, and 39 DB instrumentation
  tests passed. Emulator: Pixel 9 / API 35.
- Manual checks: feed-list pull-to-refresh visibly triggered and queued `SyncWorker`; a feed configured
  for external opening launched its article in Chrome; the setting was restored afterward.

### #297 - Valid Atom entry without a link is rejected

Status: **fixed locally; unit tests passed**

- Atom permits entries with an ID and content but no alternate link. The parser rejected these even
  when the entry could be shown in the local reader.
- Atom validation now accepts an ID in place of a link, and timeline opening routes linkless items to
  the local article screen.
- Regression test: `ATOMAdapterTest.entryWithIdButNoLinkTest` parses the report's no-link fixture and
  confirms that its ID and article content are retained.
- An Android repository test also syncs a linkless Atom item and confirms it is persisted correctly.
  The test passed on the Pixel 9 / API 35 emulator as part of the 54-test app instrumentation suite.

### #367, #369, and #370 - Recent reports already corrected in Droidrops

Status: **fixed and verified in the current branch**

- #367 is covered by `ImageDownloadStorageTest`; images are saved with `MediaStore` on scoped-storage
  Android versions instead of writing directly to a protected path.
- #369 is covered by `ContextExtensionsTest`; local `file:` and `data:` links are rejected before
  launching a browser, preventing `FileUriExposedException`.
- #370 is covered by `GReaderRepositoryTest.modifiedArticleUpdatesExistingItemTest`; modified
  FreshRSS articles update the existing row instead of appearing as duplicates.
- The app instrumentation suite passed on the Pixel 9 / API 35 emulator.

### #366 - DrawerState density crash report

Status: **fixed locally; instrumentation and portrait-to-landscape emulator checks passed**

- The orientation handler previously called `DrawerState.snapTo()` even after switching to the
  permanent tablet drawer or landscape overlay, where that state is not attached to a Material
  drawer. This could throw `IllegalArgumentException: The density on DrawerState was not set`.
- `TimelineTab` now closes the screen-model drawer and skips `snapTo()` for those layouts; modal
  phone drawers still snap closed after their anchors are laid out.
- On Pixel 9 / API 35, opened the drawer in portrait and rotated to landscape. The drawer closed,
  the timeline stayed active, and logcat showed no DrawerState exception or `CrashActivity`.
- `:app:connectedDebugAndroidTest`: 53 tests passed.

### #328 - Fever read state is lost after synchronization

Status: **fixed and verified**

- A Fever sync replaces local item-state rows with the server's unread/starred snapshots. If a
  user's read-state request failed, this could undo the local read action and the article returned
  after synchronization.
- `FeverRepository.synchronize()` now snapshots queued state changes before applying the server
  snapshot, restores those local states, and retries the queued requests.
- Regression test: `SynchronizerTest.feverSyncRetriesQueuedReadStateWithoutRevertingLocalArticle`.
- Fix is included in commit [`699a3ae7`](https://github.com/thekester/Droidrops/commit/699a3ae7)
  and release 2.2.9.

### #331 - Mouse-wheel pull-to-refresh

Status: **fixed and verified**

- `MouseWheelRefreshDetector` accumulates downward wheel movement only while a list is at its top,
  triggers after a small threshold, and resets for a later gesture. `TimelineTab` and `FeedTab` both
  use the detector with `PullToRefreshBox`.
- Regression coverage in `MouseWheelRefreshDetectorTest` checks the threshold, direction reset,
  list position, and one-trigger-per-gesture behavior.
- Fix is included in commit [`e76b23af`](https://github.com/thekester/Droidrops/commit/e76b23af).

### #334 - Miniflux login through the Google Reader API

Status: **fixed and verified**

- `GReaderService.login()` sends `Email`, `Passwd`, `output`, `client`, `accountType`, and `service`
  as `application/x-www-form-urlencoded`; Miniflux rejected the earlier multipart request.
- `GReaderDataSourceTest.loginTest` checks the form content type and each required field.
- Fix is included in commit [`4f934ee5`](https://github.com/thekester/Droidrops/commit/4f934ee5).

### #134 - Miniflux native backend support

Status: **partially addressed; native Miniflux API is not implemented**

- Miniflux can be used through its Google Reader API compatibility, and the login request fix is tracked
  under #334. The Google Reader account warning now explains where to enable that API.
- A dedicated Miniflux backend remains outstanding; this entry must not be treated as fully fixed.

### #68 - Tiny Tiny RSS support

Status: **partially addressed; native Tiny Tiny RSS API is not implemented**

- The Google Reader account warning now explains that the server's Google Reader compatibility plugin
  must be enabled before connecting.
- This does not implement TT-RSS's native API. Keep the feature request open until that backend is added
  or the upstream issue is otherwise resolved.

### #67 - Home-screen widget

Status: **implemented locally and visually verified on Pixel 9 / API 35**

- Added a resizable home-screen widget showing the three newest unread articles, their feed names,
  a manual refresh action, and article taps that open the selected article in Droidrops.
- The widget queries only the local database and does not add network access or a database migration.
- Added the widget from the launcher picker, verified that its three newest unread article titles and
  feed names render, used its refresh button, and opened a listed article successfully.
- Widget rows refresh after successful manual and scheduled synchronization. Article taps clear the
  old activity task before opening the selected article, so an open settings dialog cannot intercept
  the request.
- The empty state is handled by the provider; the emulator test database contained articles, so the
  empty-state rendering was not manually demonstrated.

### Follow-up verification (2026-10-17)

- `:app:assembleDebug`, `:app:testDebugUnitTest`, `:db:testDebugUnitTest`,
  `:app:connectedDebugAndroidTest`, and `:db:connectedDebugAndroidTest` passed with
  `-PwarningsAsErrors=true`.
- Results: 65 app unit tests, 9 DB unit tests, 56 app instrumentation tests, and 39 DB instrumentation
  tests passed on Pixel 9 / API 35.
- Added the Droidrops home-screen widget from the emulator launcher. Confirmed the three newest unread
  article titles and feeds are displayed, refresh works without a permission or process error, and
  tapping an article opens that article in the app.
- #134 and #68 now have explicit setup guidance for their existing Google Reader API compatibility;
  their requested native backends remain open and are not claimed as completed.

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

Status: **fixed and verified**

- `UserAgentInterceptor` replaces the OkHttp header with `Droidrops Android`.
- The User-Agent no longer contains `okhttp`.
- `UserAgentInterceptorTest.userAgentIsCustomTest` asserts the Droidrops-specific header.
- Unit tests pass. Validation against a FreshRSS server behind nginx remains outstanding.

### #258 - Accidental swipe marks an article as read

Status: **fixed and verified**

- A swipe now needs to travel at least 45% of the card width on wide cards, with an 80 dp minimum
  for narrow cards and a cap that keeps the threshold reachable.
- `TimelineItemSwipeThresholdTest` covers wide and narrow cards.
- Fix is included in commit [`12e6a373`](https://github.com/thekester/Droidrops/commit/12e6a373).

### #234 - FreshRSS reports an error after adding a feed

Status: **fixed locally; emulator regression test passed**

- FreshRSS can return an HTTP error after it has already added the requested subscription. The app
  previously trusted only the POST result, showed an error, and skipped refreshing the server's
  subscription list.
- Feed addition now reconciles failed POSTs against the refreshed FreshRSS feed list. A feed that
  exists remotely is imported and treated as successful; genuine failures remain visible.
- `GReaderRepositoryTest.feedReportedAsErrorButPresentOnServerIsTreatedAsAddedTest` simulates this
  response and verifies the feed and its articles are stored without a user-facing error. It passed on
  Pixel 9 / API 35 as part of the 56-test app instrumentation suite (0 failures).

### #210 - Search bar input loses characters while typing

Status: **fixed locally and verified on the emulator**

- The text field now keeps its immediate editable value locally instead of waiting for the
  asynchronously filtered timeline state to update. Clearing and closing the search also update
  both the field and filter query.
- Manual verification on the Pixel 9 / API 35 emulator entered `robot` rapidly. The field retained
  the exact text and the timeline filtered to matching articles without reordering or dropping
  characters.

### #295 - Direct URL to a JSON Feed

Status: **fixed and verified**

- Generic `application/json` responses are recognized as JSON Feed only when they contain the JSON
  Feed `version` and `items` fields, then parsed by the JSON Feed adapter.
- `LocalRSSDataSourceTest.queryJsonFeedWithGenericJsonContentTypeTest` verifies a direct JSON Feed
  URL using `application/json`; the focused case and full API unit suite pass.

### #160 - Null author in Nextcloud News items

Status: **fixed and verified**

- `NextcloudNewsItemsAdapter` reads `author` with `nextNullableString()`, so a `null` author does not
  abort parsing the item list.
- Added `NextcloudNewsItemsAdapterTest.nullAuthorDoesNotPreventItemParsingTest` for the reported
  response shape. The focused case and full API unit suite pass.

### #161 - HTML `<br>` in Atom XHTML content

Status: **fixed and verified**

- GitLab Atom feeds can contain an HTML-style `<br>` inside XHTML content, which is invalid XML and
  previously stopped both feed detection and parsing.
- `LenientXmlInputStream` repairs that exact break tag to `<br/>` while streaming the response, without
  buffering the entire feed or changing other content.
- `LocalRSSDataSourceTest.atomXhtmlWithHtmlBreakTagIsAcceptedTest` checks feed detection and article
  parsing using the issue's malformed markup. The API unit suite passes.

### #335 - Crash after using the back gesture

Status: **fixed and verified**

- The report's stack trace shows an article `data:` URL being sent to a Custom Tab, which throws
  `ActivityNotFoundException` when no app handles it.
- `ContextExtensions` now permits only HTTP(S) URLs before launching either a Custom Tab or an
  external browser; unsupported schemes show an explanation instead of crashing.
- Regression coverage: `ContextExtensionsTest.customTabRejectsLocalFileUrisFromArticleLinks` exercises
  both `file:` and `data:` links.
- Fix is included in commit `78416f3c`.

### #268 - Valid Atom entries with external content

Status: **fixed and verified**

- Atom allows an entry's content to be supplied by the `content@src` attribute, without an alternate
  `<link>`.
- `ATOMItemAdapter` uses that source as the article destination when the entry has no link.
- Regression coverage: `LocalRSSDataSourceTest.atomExternalContentWithoutAlternateLinkTest` parses the
  reported Atom shape and checks the resulting article URL.
- Fix is included in commit `8f43c0b6`.

### #261 - Crash on very large article content

Status: **fixed and verified**

- Item queries project large text columns as a bounded preview, avoiding `SQLiteBlobTooBigException`
  when Android reads the row into a `CursorWindow`. The article URL remains available for full content.
- Added `ItemDaoLargeContentTest.selectingItemWithMultiMegabyteContentReturnsBoundedPreview`, which
  stores 3.1 MB of article content and verifies the DAO returns the bounded preview.
- The new test passed on the Pixel 9 / API 35 emulator again on 2026-10-18.
- Fix is included in commit `70bda197`.

## Recently verified existing support

### #315 - Undo accidental removal from Favorites

Status: **fixed locally; emulator action verified**

- Removing a saved item from Favorites shows an Undo snackbar.
- The timeline action now writes the requested favorite state explicitly instead of toggling the
  mutable item a second time. Undo restores the previously captured state, even after the row leaves
  the Favorites paging list.
- Verified on Pixel 9 / API 35: removed the Phoronix item from Favorites, pressed Undo, and confirmed
  the item and filled star returned.

### #316 - Show image hover text

Status: **implemented locally; parser and emulator suites pass**

- Article HTML exposes image `title` text, falling back to `alt`, as a caption below the image.
- `ImageHoverTextTest` covers title precedence, alt fallback, escaping, and placement outside linked
  images; all three tests passed. The 56-test app instrumentation suite also passed on Pixel 9 / API 35.
  The end-to-end xkcd WebView check is recorded in the detailed entry below.

### #323 - Animate tab changes

Status: **implemented locally; emulator navigation and app tests pass**

- Main destinations use a short Material-style fade-through transition while preserving each tab's
  saveable state.
- Switched between Timeline, Feeds, Account, and More on Pixel 9 / API 35 without a crash; the app
  instrumentation suite passed.

### #46 - HTTP Basic authentication for individual feeds

Status: **implemented in Droidrops; authenticated-feed regression passed on the emulator**

- Add Feed accepts optional credentials; discovery sends Basic authentication, and credentials are
  kept in encrypted preferences keyed to the feed rather than in its database row or OPML.
- Feed synchronization retrieves those credentials and sends the `Authorization: Basic …` header.
- `LocalRSSRepositoryTest.authenticatedLocalHttpFeedSendsBasicAuthorizationHeader` verifies both
  the header and retrieval of a protected article from an HTTP endpoint on Pixel 9 / API 35.

### #54 - Filter articles by Google Reader API tags

Status: **tag filtering is implemented; database regression passed on the emulator**

- FreshRSS/Google Reader tags are stored with article-to-tag relationships and listed in the
  timeline drawer. Selecting a tag scopes the timeline query to matching article IDs.
- Added `ItemsQueryBuilderTest.tagFilterReturnsOnlyArticlesAssignedToSelectedTag`, which inserts
  tagged and untagged articles and verifies only the tagged article is returned.
- `:db:connectedDebugAndroidTest`: 37 tests passed on Pixel 9 / API 35.

## Priority TODOs

### #342, #343, and #351 - incomplete upstream crash reports

Status: **not actionable; reporter details required before a code correction can be identified**

- Each upstream issue contains only the untouched GitHub bug-report template. The steps still say
  `Go to '...'`, the expected behavior is the template prompt, and no stack trace or account/app/device
  details are provided.
- Checked the current Droidrops build and regression suites; without a described action or failure,
  there is no reproducible behavior to map to a code path. No speculative code change is recorded as a
  fix. Ask the reporters for reproduction steps and the crash-screen stack trace.
- Sources: [#342](https://github.com/readrops/Readrops/issues/342),
  [#343](https://github.com/readrops/Readrops/issues/343),
  [#351](https://github.com/readrops/Readrops/issues/351).

### #249 - FreshRSS on a local `.local` host

Status: **HTTP transport verified; `.local` DNS and the reporter's TLS setup remain unverified**

- Android network security configuration allows explicit HTTP endpoints and trusts system and
  user-installed certificate authorities. The emulator's authenticated HTTP feed test passed.
- This confirms the app can use an HTTP server without a public certificate. It does not validate
  whether a particular device resolves the reporter's `.local` hostname or trusts that server's
  certificate; the original report supplies no server URL or certificate chain.

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

Status: **local memory mitigation; process restoration passed; reported foldable OOM not reproduced**

- The article pager used to set `initialLoadSize` to `pageSize * pageNumber`, so restoring an article
  deep in a feed could fetch hundreds of full article rows immediately.
- It now loads one 50-item page around the selected article and lets Paging fetch adjacent pages as
  needed. The selected article's current position is resolved from ID-only query results, avoiding
  large content columns during position lookup.
- On Pixel 9 / API 35, force-stopped the app with an article open and relaunched it; the same article
  returned with no fatal exception. This verifies the reader restoration path and bounded initial
  load, not the reported Z Flip 5 memory failure; the exact foldable/configuration-change scenario
  and its OOM trace remain unavailable.
- Added a regression for the maximum supported timeline index to ensure the page anchor remains a
  valid page boundary without integer overflow. This verifies anchor arithmetic, not the foldable
  device's full memory behavior.

### #294 - Restore the same article after backgrounding

Status: **local fix; query regression and emulator process-restoration check passed**

- The screen carried the article's old timeline index into the pager. A synchronization while the app
  was away can insert newer rows or remove the now-read article, making that index point at a different
  item when the reader is rebuilt.
- On reader initialization, the app now resolves the selected item ID's current index using the active
  filters. The ID-only query includes the selected article in the session's unread filter, so opening
  an unread article does not shift the target out of the list after it is marked read.
- `ItemsQueryBuilderTest.itemIdsQueryKeepsSelectedReadItemInUnreadSessionOrder` verifies that the
  selected read item remains at the correct position while unrelated read items stay filtered.
- The new pager loads a bounded page at that resolved index rather than materializing every preceding
  row. `ItemPagerAnchorTest` covers page boundaries and articles without a timeline index.
- On the Pixel 9 / API 35 emulator, opened an article, force-stopped and relaunched the app; the same
  article was displayed. The current verification run passed all 38 DB and 56 app instrumentation
  tests, plus 63 app and 158 API unit tests.

### #364 - Restore the reader after returning from another app

Status: **local fix; instrumentation and emulator process-restoration checks passed**

- The reader now stores only the active article ID in a private preference while it is open. If Android
  recreates the root screen without restoring the reader screen, startup reopens that article once.
  Returning with Back clears the saved ID, and notification/share navigation takes precedence.
- `ReaderSessionTest` covers one-time restore and explicit clearing.
- `ReaderSessionTest` passed on the emulator. Manually opened an article, force-stopped the app, and
  relaunched it; the same article returned and logcat contained no Android runtime exception.

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

Status: **not a data bug; reading-time visual cue added locally**

- The `<1 min` value is the **estimated read time**, not an age. It comes from `Utils.readTimeFromString`
  (number of words / words per minute).
- Check: the four repositories (Local, Nextcloud News, GReader, Fever) populate `Item.readTime`. No path
  leaves it at zero.
- `<1 min` is therefore accurate for feeds whose entries carry almost no text, typically Hacker News
  which only publishes a link.
- Actual cause of the report: the line shows `<date> Â· <read time>` without a label, so readers can mistake
  the duration for the article's age. This is the same kind of ambiguity as the â€œNew articlesâ€ filter,
  which was renamed â€œLast 24 hoursâ€.
- Added a clock icon before the reading duration in both timeline badges and the article header. The
  existing localized text still says "min read" / "min de lecture"; the icon makes that duration
  distinct from the adjacent publication date at a glance.
- The underlying `<1 min` estimate remains accurate for link-only entries. The Pixel 9 / API 35
  emulator visibly showed the clock cue between the publication timestamp and read-time label. All
  63 app unit, 158 API unit, 38 DB instrumentation, and 56 app instrumentation tests passed.

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
- Fix is included in the current branch.

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
- Fix is included in the current branch.

### Crash - IllegalArgumentException in ItemScreenModel.onDispose

Status: **fixed and verified** - delivered in `78416f3c` (reported directly, excluding upstream
inventory)

- Symptom: `IllegalArgumentException: Do not add an item state change for an item which is already read`,
  raised from `ItemScreenModel.onDispose`.
- Cause: `StateChange.readChange` records a **toggle**, not a direction. `onDispose` passed
  indiscriminately all `readChange` to `Repository.setItemsRead`, whose precondition `require(items.all {
  it.isRead == false })` refuses any article already read. Opening an already read article, pressing
  â€œmark unreadâ€, then leaving the screen was enough to crash the application.
- Note: the treatment of favorites located just below would already correctly manage the toggle. Only the
  reading path was inconsistent.
- Correction applied: partition of changes according to the current state. Unread articles become read go
  through `setItemsRead` (preserved batch processing), read articles become unread go through
  `setItemReadState`, which handles both ways.
- Verification: three cold passes from an article already read, with double switch each time. Return to
  `MainActivity`, no `CrashActivity`, zero exceptions in logcat.

### #333 - Crash on opening article with YouTube embed

Status: **crash not reproduced - embed fault fixed and verified**, delivered in `78416f3c`

- Added the exact failing URL from the report (`file://www.youtube.com/embed/Sh8yqcozfn8?enablejsapi=1`)
  to `ContextExtensionsTest`. `openUrl()` now rejects the non-HTTP(S) scheme before creating an
  external intent; the instrumented regression passes on Pixel 9 / API 35.

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
     the video thumbnail and a â€œWatch on YouTubeâ€ link. Useful for readers who have the native app or a
     Premium subscription and prefer their own reader.
- Verifications on the screen: functional integral embed (thumbnail, title, play button), Error 153
  disappeared, and newlines identical to the original therefore font intact. Thumbnail mode checked
  separately, setting active from Settings screen.
- Also note: no `WebChromeClient` is installed, which is required for full-screen HTML5 video.

### #317 - Error when importing OPML

Status: **fixed and verified**

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
- Follow-up review of the reported stack trace found a separate unhandled case: a folder-only OPML
  caused `values.first().first()` to throw `NoSuchElementException` in both account-selection import and
  import into an existing local account. Both entry points now reject documents without subscriptions
  with a localized message before opening the progress dialog. The shared predicate has regression
  coverage for folder-only and non-empty documents (`OPMLImportUtilsTest`, 2 tests).
- Full emulator test run after this correction: 37 DB instrumentation and 54 app instrumentation tests
  passed; app unit suite also passed.
- Fix is included in the current branch.

### #347 - [Security] Important data breach

Status: **backup rules fixed and revalidated in Droidrops; real restore cycle still unverified**

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
  `account_credentials.xml` from cloud backup **and** from device transfer. Follow-up review found that
  the Room database itself remained included, so `readrops-db` is now excluded from cloud backup and
  device transfer as well, preventing session tokens from being copied in plaintext.
- The SQLite side files `readrops-db-wal` and `readrops-db-shm` are also excluded: restoring an outdated
  `-wal` next to the database may produce an inconsistent state.
- Verification: manifest merges from the APK release correctly `fullBackupContent` and
  `dataExtractionRules`. The reference `@xml/backup_rules` being resolved at compilation, a missing file
  would have caused manifest processing to fail.
- Trade-off: local accounts, feeds, articles, and settings in Room are not restored from Android backup.
  OPML export remains available for feed subscriptions, without session tokens.
- Verification: an emulator instrumentation regression parses both backup rule resources and checks
  that the Room database and encrypted credentials are excluded. A real Auto Backup restore cycle
  remains unverified.
- Fix is included in release 2.2.21. The current emulator regression passed again; it verifies the
  exclusion rules, not a full Android Auto Backup restore cycle.

### #325 - Improve grammar in the crash notice, "Github" -> "GitHub"

Status: **fixed in Droidrops; English/French copy and emulator checks pass**

- Observed directly on the screen during a real crash: the message displayed â€œ**I** you see this, it
  means the app ran into an unexpected error. Please if you can, report the error on **Github**. Â» Two
  grammatical errors and incorrect capitalization.
- The application was inconsistent with itself: `greader_warning` and `fever_warning` already correctly
  wrote "GitHub".
- Corrections applied:
  - `crash_message` (English) rewrites: â€œIf you see this, the app ran into an unexpected error. Please
    report it on GitHub if you can. Â»
  - Case â€œGithubâ€ -> â€œGitHubâ€ on **17 occurrences** distributed in 6 languages â€‹â€‹(en, es, fr, ja, pt-rBR,
    zh-rTW). The case of a brand name does not depend on the language, the correction is therefore valid
    everywhere and is not part of the translation work.
  - URLs are not affected: they use `github.com` in lowercase.
- Verifications: 14 translated string files are valid XML and the obsolete `Github` spelling is absent.
  `IssueRegressionTest.crashMessageUsesCorrectGitHubBrandingInEnglishAndFrench` checks English and
  French resources on the emulator.
- Fix is included in the current branch.

### #336 - [Bug] fosstodon

Status: **fixed locally; RSS2 parser tests and emulator suites pass**

- Identified cause: `RSS2ItemAdapter.validateItem` raised `ParseException("Item title is required")`.
  However, the RSS 2.0 specification says the opposite: all elements of an item are optional, but **at
  least one title or description** must be present. The application was therefore stricter than the norm.
- Microblogging feeds are titleless by nature. Verified on the real feed `fosstodon.org/@fosstodon.rss`:
  **20 items out of 20 without `<title>` tag**, all with a `<description>`. The entire feed was rejected
  even though it is perfectly valid.
- Aggravating: five `item.title!!` assertions exist in the interface. A null title would have caused the
  display to drop anyway, so the validation masked a second problem.
- Correction applied: the item is refused only if title **and** description are missing. When the title
  is absent, it is derived from the cleaned-up description, truncated to 100 characters with a Unicode
  ellipsis. Blank title/description content is rejected, instead of generating an empty timeline title.
  The five `title!!` therefore remain satisfied.
- Scope voluntarily limited to RSS 2.0: the RSS 1.0 and Atom specifications really require a title, the
  same constraint therefore remains legitimate and has not been affected.
- Tests: `noTitleTest` checks title derivation; `noTitleAndNoDescriptionTest` and
  `blankTitleAndDescriptionAreRejected` cover invalid blank content; `longDescriptionWithoutTitleIsTruncatedWithUnicodeEllipsis`
  checks the exact 100-character fallback. The API unit suite passed all 161 tests, including
  `RSS2AdapterTest`, with 0 failures; the 39-test DB
  instrumentation suite and 56-test app instrumentation suite also passed on Pixel 9 / API 35.
- End-to-end verification: real addition of the fosstodon feed on emulator. It is accepted, synchronized,
  and its posts are displayed with a derived title, the icon and color of the feed, and the hashtags in
  tags. The same feed was refused before the correction.
- Fix is included in the current branch.

### #318 - Do not use dynamic color for text

Status: **fixed in Droidrops; low-contrast fallback regression passes on emulator**

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
- Verification: `IssueRegressionTest.lowContrastFeedColorFallsBackAndForegroundChoosesReadableText`
  confirms the orange feed color falls below the 3:1 floor and the foreground helper picks black for it.
  The test passes on Pixel 9 / API 35.
- Fix is included in the current branch.

### #346 - Empty space in items

Status: **fixed in Droidrops 2.2.21; repository regression revalidated on Pixel 9 / API 35**

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
- Added an Android repository regression test that syncs an Atom item whose summary contains only an
  image and verifies the item remains stored while `cleanDescription` is null. The case passed on the
  Pixel 9 / API 35 emulator.
- Verification: targeted `:app:connectedDebugAndroidTest` passed on Pixel 9 / API 35 (6 tests), including
  the image-only description regression case.
- Fix is included in the current branch.

### #319 - â€œOPML Import/Exportâ€ stylization

Status: **fixed locally; OPML choice dialog verified on Pixel 9 / API 35**

- Verification done byte by byte: the string did not contain **any** invisible characters, contrary to
  what the title of the issue suggests.
- The real fault is a case inconsistency within the same dialog: the header displayed â€œOPML
  Import/Exportâ€ while the two actions below display â€œOPML importâ€ and â€œOPML exportâ€.
- Correction applied: header aligned in sentence case, "OPML import/export", in accordance with Material
  recommendations and the two actions it labels.
- Emulator verification: the dialog shows "OPML import/export", "OPML import" and "OPML export".

### #178 - Fever account

Status: **URL input hardened; original crash still needs a reproduction or stack trace**

- The report only says that the app crashes "when filling field for fever" and contains no stack trace.
- Opened the Fever account form on Pixel 9 / API 35 and entered values in the name, API URL, login and
  password fields. The form stayed responsive and no app crash appeared in logcat.
- Hardened URL normalization for pasted input: surrounding whitespace is trimmed, schemes are recognized
  only at the beginning and case-insensitively, the trailing slash is applied to the path before any
  query or fragment, and blank input remains blank.
- Added `UtilsTest.normalizeUrlKeepsQueryAndFragmentOutsideThePath` and
  `normalizeUrlLeavesBlankInputBlank`, in addition to the scheme and whitespace regression test.
  App unit tests pass.
- This does not establish the original crash's cause. A reproduction path or stack trace is still needed
  before claiming that crash is fixed.

### #321 / #322 - RSS client to feed reader

Status: **wording corrected locally in the English Play/F-Droid listing**

- The short listing now says “Droidrops feed reader”; the full description calls it an independent
  Android feed reader supporting multiple services and formats.
- This reflects RSS, Atom, and JSON Feed support without repeating the upstream Readrops branding
  as the product category.

### #338 - [Bug] title display line break

Status: **fixed in Droidrops 2.2.21; multiline-title regressions revalidated**

- Inconsistency found in the code: only RSS 2.0 cleaned its titles. `RSS2ItemAdapter` applies
  `ApiUtils.cleanText`, which normalizes spaces via Jsoup, while `ATOMItemAdapter`, `RSS1ItemAdapter` and
  `JSONItemsAdapter` took the plain text. Same for stream names.
- Consequence: a feed whose XML is formatted on several lines, which many static site generators do, kept
  its newlines in the title and pushed them as is in the timeline.
- **Reproduction scope**: the symptom has not been observed in live feeds tested (Planet KDE, LinuxFr
  and Debian); the parser fixture reproduces the multiline XML title deterministically.
- Verification of a rejected hypothesis: numerical entities such as `&#8217;` are already decoded by the
  XML parser itself, the difference between formats therefore did not concern the entities, only the
  normalization of spaces.
- Correction applied despite the absence of reproduction: `ApiUtils.cleanText` is now applied to the
  titles and stream names of the four formats. The treatment is idempotent on an already clean title, and
  removes an entire class of display defects.
- Tests: fixture `atom_items_multiline_title.xml` adds and test `multilineTitleTest` verifying that a
  title written on three lines appears on only one. Without the correction the assertion fails. Complete
  `:api` suite: 159 tests, 0 failures, no existing adapters affected. `ApiUtilsTest` additionally checks
  mixed newlines, tabs, and inline HTML collapse without losing word boundaries.
- Fix is included in the current branch.

### #324 - Larger font for the options of multi-choice pickers

Status: **fixed and verified** - commit `bc111af5`

- Observed in `RadioButtonPreferenceDialog.RadioButtonItem`: the label of each option used
  `MaterialTheme.typography.bodyMedium`, i.e. 14sp, whereas Material 3 provides `bodyLarge`, 16sp, for a
  selectable list line in a dialog.
- Concrete effect: the options were **smaller** than the settings entry that opens the dialog, which is
  exactly the opposite of the expected hierarchy.
- Correction applied: `bodyMedium` -> `bodyLarge`. No custom typography in the project, so the default
  Material sizes apply.
- On-screen check: â€œDefault categoryâ€ dialog opened, the three options are now on the same scale as the
  adjustment entries visible behind.
- Fix is included in the current branch.

### #320 - Use another icon for a local account

Status: **fixed and verified locally** - commits `bc111af5` and `70bda197`

- Local accounts use the dedicated `ic_local_account` phone icon instead of the Droidrops launcher logo.
- The account screen tints it for contrast in light and dark themes. An emulator regression check
  confirms the packaged drawable is a 24 dp icon.

### #332 - Have more information to debug login issues

Status: **fixed and verified** - commit `305cd61a`

- The existing `DebugScreen` only covers sending test notifications, nothing about logging into accounts.
- Cause identified in the most common case among self-hosters, who are the public of the application:
  `SSLHandshakeException` inherits from `IOException`. A TLS failure therefore fell into the generic
  branch `is IOException` and displayed the raw exception message, such as â€œTrust anchor for
  certification path not foundâ€. Incomprehensible, and above all without any indication of what to do.
- But `network_security_config.xml` already declares `<certificates src="user" />`: the application
  trusts the authorities added by the user. So the actionable advice existed, it just wasn't formulated.
- Fix applied: `is SSLException` maps to explicit message, places **above** `is IOException`. The order
  is essential, `when` evaluates from top to bottom and `SSLException` inherits from `IOException`:
  placed after, the branch would never be reached. `SSLException` rather than `SSLHandshakeException` to
  also cover hostname mismatch.
- Regression test: `IssueRegressionTest.tlsLoginFailureShowsActionableCertificateGuidance` passes on
  Pixel 9 / API 35 and verifies an `SSLHandshakeException` maps to certificate guidance rather than the
  raw handshake exception.
- End-to-end verification: Adding an account pointing to a real self-signed certificate host. The message
  displayed is â€œThe server certificate could not be verified. If your server uses a self-signed
  certificate, install its authority in Android settings under Security, then try again. Â»
- Also check that the chain is present in the `resources.arsc` of the APK release, and that the order of
  the branches is correct in the compiled file.
- Fix is included in the current branch.

### #316 - Easily access hovertext for images

Status: **local fix, tested** - commits `2620f6d0` and `4f934ee5` contain the original behavior; alt-only fallback and regression tests are local

- The `title` attribute of an image is the text on hover. Online comics use it for a second punchline. A
  touchscreen has no hover, so this text was completely unreachable, and `ItemWebView` never read this
  attribute.
- The existing dialogue on long press only offers Share and Download.
- Approach chosen: reveal the text in the caption under the image, via a transformation in `formatText`,
  as for video embeds. The wiring of the long support crosses four layers, and a visible legend without
  interaction better meets the â€œeasily accessâ€ request.
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
- Fix is included in the current branch.

### Article reader pinch zoom

Status: **enabled in the article WebView; emulator settings regression test passes**

- The image viewer already supports pinch zoom, pan, and double-tap zoom when an article image is tapped.
- The article WebView also had built-in zoom controls configured, but `setSupportZoom(false)` disabled
  pinch gestures in the article itself. It is now enabled while the on-screen zoom buttons remain hidden.
- `ItemWebViewZoomTest.articleWebViewAllowsPinchZoomWithoutShowingZoomButtons` verifies the gesture
  configuration on Pixel 9 / API 35.

### #315 - Undo popup to remove an article from favorites

Status: **fixed in Droidrops; emulator gesture reverified**

- Preliminary check: no cancellation mechanism existed for favorites. `SnackbarResult.ActionPerformed`
  only appeared in `AccountTab`, for something else.
- The necessary infrastructure was already in place in the timeline: a `SnackbarHostState` and a snackbar
  pattern with action, used for sync errors.
- Correction applied: removing an article from favorites from the timeline displays â€œRemoved from
  favoritesâ€ with an â€œUndoâ€ action which restores it.
- Choice assumes: **only withdrawal** offers cancellation. Adding a favorite is a visible and trivially
  reversible action, while losing it is not. Showing a snack bar in both directions would have been
  noise.
- End-to-end verification, with the control test that counts:
  - removal then â€œUndoâ€: the button returns to â€œRemove from favoritesâ€, the item is restored.
  - withdrawal then expiration of the snackbar without touching it: the button remains on â€œAdd to
    favoritesâ€, the item remains withdrawn. Without this second case, the first proved nothing: an
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

Status: **fixed and verified locally**

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
  - The populated feed `LazyColumn` fills the refresh container so blank space below a short list accepts
    pull gestures too.
- **Applied lesson from the crash fixed earlier**: the `PullToRefreshBox` wraps the state `when` instead
  of being placed in one of its branches. It therefore remains mounted whatever the state, which exactly
  avoids the node detachment which caused the timeline to crash.
- Emulator verification on 2026-10-17: a downward swipe in blank space below the collapsed feed groups
  displayed the refresh indicator and logcat confirmed `WM-WorkerWrapper: Starting work for
  com.readrops.app.sync.SyncWorker`; the app stayed in the foreground without a fatal exception.
- The app's instrumented suite passed **47/47** tests on the Pixel 9 API 35 AVD.

### #323 - Add an animation for switching app tabs

Status: **fixed and verified**

- Observation: `HomeScreen` called `CurrentTab()` directly, without transition. The tab change was
  therefore instantaneous and abrupt.
- Difficulty: animating requires rendering the content of the **target** tab, not just the current tab. A
  simple `Crossfade` around `CurrentTab()` would have given nothing, since `CurrentTab()` always makes
  the tab active, therefore the same content on both sides.
- Correction applied: `AnimatedContent` on `tabNavigator.current`, whose contents pass through
  `tabNavigator.saveableState(key = "currentTab", tab = tab)`, exactly the key that `CurrentTab()` uses
  internally. Each tab therefore maintains its own state.
- Transition retained: â€œfade throughâ€, the motion Material for a navigation bar. Output in 90 ms, input
  in 220 ms after 90 ms delay.
- **Checking the main risk**, loss of tab state:
  - timeline at the top, then scroll to a given article,
  - switch to the feed tab,
  - return to the timeline: the scroll position is **identical** to the one left. Without this test, the
    transition could have masked a state reset.
- Checking the animation itself: a capture taken during the change shows an intermediate state, with an
  average deviation of 66 levels from the final image. Change is therefore no longer instantaneous.
- Fix is included in the current branch.

### #326 - Close category selection pane on tablet

Status: **fixed and verified on the tablet-sized Pixel 9 AVD** - commit `cd6da5a4`

- In landscape tablet layout, the drawer has an X control to close it and a menu control to reopen it.
- Manual UI check on 2026-10-15: configured the Pixel 9 AVD to `sw914dp` landscape, opened the drawer,
  tapped X, and confirmed the article list regained the available width. The menu reopened the drawer.
### #359 - Readrops sync with nextcloud slows down

Status: **fixed and verified** - commit `cc56ae2c`

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
- Verification on emulator: `migrate7To8` validates Room's schema and queries `sqlite_master` to
  confirm the index exists after migration. The complete `:db` instrumented suite passed **27/27**
  tests on the Pixel 9 API 35 AVD. This verifies the migration and index, not a live Nextcloud
  synchronization benchmark.
- Not intentionally processed: `Feed.remote_id` and `Tag.remote_id` have no index either, but these
  tables have tens to hundreds of rows, the effect would be marginal.
- The migration and index validation passed; no live Nextcloud synchronization benchmark was run.

### Emulator launch crash - missing Compose icons runtime module

Status: **fixed locally; emulator launch verified**

- Launching the debug APK after instrumentation uncovered a `ClassNotFoundException` for
  `androidx.compose.material.icons.Icons$Filled`. The instrumented suite had not launched `MainActivity`.
- Added `material-icons-core` to the Compose runtime dependency bundle.
- Rebuilt and installed the APK on the Pixel 9 API 35 AVD. The timeline and feeds screens rendered, the
  activity stayed in the foreground, and logcat showed no fatal exception.
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
- Fix is included in the current branch.

### #357 - Readrops keeps crashing when I open an article

Status: **reported `FileUriExposedException` prevented; exact regression test passes on emulator**

- The attached trace identifies a YouTube embed URL incorrectly presented as `file://www.youtube.com/...`.
- Browser launch helpers now accept only HTTP(S) schemes and show a message for unsupported schemes,
  preventing Android from receiving the offending file URI.
- Extended `ContextExtensionsTest.customTabRejectsLocalFileUrisFromArticleLinks` with the exact
  YouTube embed path and query parameters from the report. The test passes on Pixel 9 / API 35.
- The wider multi-feed article-opening exercise also observed no crash. This closes the reported crash
  path; it does not prove that every unrelated article crash is impossible.

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
- **Related defect found and corrected**: the â€œOpenâ€ action proposed after an OPML export constructed its
  Intent **without** `FLAG_GRANT_READ_URI_PERMISSION`. Since the URI belonged to the document provider,
  the receiving application could not read it. The flag is now propagated.
- Limit of the check: on the emulator, the selector responds â€œNo apps can perform this actionâ€, due to
  the absence of an application registered for `text/xml`. The fix could therefore not be observed from
  end to end, only established by reasoning on the Android permissions model.
- All that remains to be done: commit, and request the complete trace from the rapporteur to confirm that
  the original crash is indeed a thing of the past.

### #54 â€“ Support for tags

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

Guardrail on â€œmark everything as readâ€:

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
- â€œmark all as readâ€ button: present without filter in `[933,2004][996,2067]`, **absent** under tag
  filter while the â€œSynchronizeâ€ button remains present (witness proving that the dump is valid), then
  **returned to the same coordinates** after erasing the filter.

Accessibility corrected in passing:

- The **four** `FloatingActionButton`s in the application had `contentDescription = null`, so were muted
  for TalkBack. My previous accessibility audit missed them, it only scanned `IconButton`. Added labels:
  â€œMark all articles as readâ€, â€œAdd accountâ€, â€œAdd folderâ€, â€œAdd feedâ€ (new string `add_folder`).
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
- The folded line **always states the state**: â€œThis feed is publicâ€ or â€œSaved for <login>â€. This is the
  only place where the user can know that a stream carries identifiers.
- **The stored password is never reread in the form.** Empty field + help text â€œLeave blank to keep the
  current passwordâ€: renaming a feed does not require retyping a secret.
- Section **hidden for server accounts** (`isAuthAvailable = account.isLocal`): with FreshRSS, Nextcloud
  News or Fever, it is the server that fetches the feed, the application never presents these identifiers
  itself.
- Validation rules: HTTP basic authentication requires both halves, so a single password is refused, and
  a single login is also refused unless a password is already stored. Emptying the login deletes the
  identifiers; a â€œClear credentialsâ€ button does this in one gesture, without writing anything before
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
  accessibility audit only scanned `IconButton` and missed them all. Added labels: â€œMark all articles as
  readâ€, â€œAdd accountâ€, â€œAdd folderâ€, â€œAdd feedâ€ (new string `add_folder`). Checked on screen.
- Show/hide toggle added on the password of **two** screens (addition and edition), with the labels â€œShow
  passwordâ€ / â€œHide passwordâ€ already present in the application.

### #69 - Leave link validation to server

Status: **local correction verified; self-hosted URL syntax regression tests pass**

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
- Regression coverage accepts local hostnames, custom ports, uppercase HTTP schemes and query
  parameters; it rejects spaces in the hostname and a non-numeric port.
- `Patterns` imports that were no longer needed were removed from all three files as CI required builds
  without warning.
- End-to-end verification: addition of `http://nas/feed.xml`, host without point or TLD. The message
  displayed is now **â€œUnreachable URLâ€**, a network error, and no longer â€œBad URLâ€ returned without
  attempt. The request therefore really starts.
- Fix is included in the current branch.

### #66 - [Feature Request] Access Password-Protected RSS Feeds

Status: **implemented locally; protected and public feed authorization regressions pass on emulator**

- Design choice: identifiers are **not** stored in the database. `Feed` exposes calculated keys
  `loginKey` and `passwordKey`, modeled after `Account`, and the values â€‹â€‹live in the
  `EncryptedSharedPreferences` already in place. Direct consequence: nothing sensitive goes into a backup
  or an OPML export.
- **No basic migration was necessary**: calculated properties are not columns, the schema remains in
  version 8. Verified after compilation.
- Scope covered, because usable authentication is not limited to synchronization:
  - `LocalRSSDataSource.isUrlRSSResource` and `HtmlParser.getFeedLink` now accept headers, without which
    a protected stream could never have been **added**,
  - the addition screen displays an â€œAuthentication (optional)â€ section with username and mask password,
  - the identifiers are used for the discovery request, then are recorded once the feed is inserted,
    found by its URL since its identifier only exists after insertion,
  - `LocalRSSRepository` adds the `Authorization` header each time the stream is synchronized.
- End-to-end verification against a server actually requiring Basic authentication,
  `httpbin.org/basic-auth/user/passwd`:
  - **without** credentials: â€œHTTP error 401, please check your credentialsâ€,
  - **with** identifiers: the 401 disappears and the error becomes a format problem, this service
    responding to JSON. The request therefore goes well, the header is sent. It is the change in
    **nature** of the error that provides proof, not its disappearance.
- Non-regression verification: addition of a public feed with identifiers provided, then complete
  synchronization. The feed is added, 117 articles arrive, no incident. The additional header therefore
  does not disrupt ordinary feeds.
- **Assumed limit**: the reuse of **stored** identifiers during a subsequent synchronization could not be
  observed, due to the lack of a protected server serving a real RSS feed. The storage path executes
  without breaking the addition, but its rereading is only established by the code.
- Existing-feed credential edits, blank-password retention during rename, explicit credential clearing,
  and encrypted-value cleanup on feed deletion are implemented and verified in the continued #66
  section above. Repository instrumentation now covers both authenticated and public feeds.

### #362 - F-Droid inclusion request for Droidrops

- Issue opened by `thekester` to notify the original author.
- No upstream responses identified at time of check.
- Keep the link in the F-Droid merge request and wait for an objection or confirmation.

### #298 - Short tap vs long tap in the Feeds tab

Status: **fixed and verified on Pixel 9 API 35 AVD**

- The issue described short tap opening feed settings and long press opening the feed website.
- Current `FeedTab` does the opposite: a short tap opens `siteUrl` (falling back to the feed URL), while a long press opens the feed properties sheet.
- The properties sheet retains an explicit **Open** action, so both actions remain available.
- Emulator check: a short tap on The Verge opened Chrome; a long press opened the feed properties sheet, including its explicit Open action.

### #284 - Allow the auto-generated Material You theme

Status: **fixed and verified on Pixel 9 API 35 AVD**

- `PreferencesScreen` offers the Material You theme only on Android 12 and newer.
- `ReadropsTheme` selects `dynamicLightColorScheme` or `dynamicDarkColorScheme` according to the active theme.
- Emulator check: Material You was available on Android 15; selecting it updated the setting and changed the visible UI palette.

### #281 - Make the launcher icon adapt to themed icons

Status: **fixed locally and verified on Pixel 9 API 35 AVD**

- Added a monochrome mask derived from Droidrops' existing launcher artwork and referenced it from the adaptive icon XML.
- The regular launcher artwork remains unchanged. The Pixel launcher themed-icons option recolored the installed Droidrops icon using the new monochrome mask.
- Instrumented regression check confirms the packaged adaptive launcher drawable exposes a monochrome
  layer on the API 35 emulator.

## Complete inventory

| Issue | Upstream status | Droidrops status | Title |
|---|---|---|---|
| [#370](https://github.com/readrops/Readrops/issues/370) | Open | Fixed and verified | [Bug] Duplicate articles when they are edited/updated in the RSS feed |
| [#369](https://github.com/readrops/Readrops/issues/369) | Open | Fixed and verified | Crash while reading |
| [#367](https://github.com/readrops/Readrops/issues/367) | Open | Fixed and verified | Image download fails with EPERM on Android 17 |
| [#366](https://github.com/readrops/Readrops/issues/366) | Open | Fixed locally; emulator rotation verified | Crash Report |
| [#365](https://github.com/readrops/Readrops/issues/365) | Open | Fixed in Droidrops; regression instrumentation passed | crashed |
| [#364](https://github.com/readrops/Readrops/issues/364) | Open | Local fix; instrumentation and process-restoration checks passed | crashola |
| [#362](https://github.com/readrops/Readrops/issues/362) | Open | Awaiting upstream | F-Droid inclusion request for Droidrops |
| [#361](https://github.com/readrops/Readrops/issues/361) | Open | Fixed in Droidrops; emulator gesture reverified | [Feature] Standardize Pull-to-Refresh Behavior |
| [#360](https://github.com/readrops/Readrops/issues/360) | Open | Fixed and verified | [Bug] App uses User-Agent containing 'okhttp' that is common in blocklists |
| [#359](https://github.com/readrops/Readrops/issues/359) | Open | Fixed and verified | Readrops sync with nextcloud slows down |
| [#358](https://github.com/readrops/Readrops/issues/358) | Closed | Closed upstream - verify in Droidrops | [Bug] Crash when opening article |
| [#357](https://github.com/readrops/Readrops/issues/357) | Open | Reported YouTube `file://` crash path blocked; exact regression passes | Readrops keeps crashing when I open an article |
| [#356](https://github.com/readrops/Readrops/issues/356) | Open | Bounded pager and reader restoration regressions pass; foldable OOM not reproduced | Crash when opening the app from recent apps |
| [#355](https://github.com/readrops/Readrops/issues/355) | Open | Fixed and verified | getting this error when I want to open a freshRSS post in external browser |
| [#354](https://github.com/readrops/Readrops/issues/354) | Open | Fixed and verified | [Bug] Error when opening feeds |
| [#353](https://github.com/readrops/Readrops/issues/353) | Open | Fixed and verified | Readrops shows error on opening an RSS post |
| [#352](https://github.com/readrops/Readrops/issues/352) | Open | Fixed and verified | [Bug] Date parsed incorrectly when format includes time offset |
| [#351](https://github.com/readrops/Readrops/issues/351) | Open | Needs reporter information; upstream body is the untouched bug template | [Bug] |
| [#350](https://github.com/readrops/Readrops/issues/350) | Open | Fixed and verified | [Bug] Error |
| [#349](https://github.com/readrops/Readrops/issues/349) | Open | Fixed and verified | [Bug] Cannot subscribe Debian news RSS |
| [#348](https://github.com/readrops/Readrops/issues/348) | Open | Already fixed in Droidrops | [Bug] ApiUtils.md5hash discards leading 0s on hash (AKA: login failures via Fever integration) |
| [#347](https://github.com/readrops/Readrops/issues/347) | Open | Backup rules exclude Room and encrypted credentials; emulator regression passed; full restore cycle unverified | [Security] Important data breach |
| [#346](https://github.com/readrops/Readrops/issues/346) | Open | Fixed in Droidrops 2.2.21; repository regression passed on emulator | Empty space in items |
| [#345](https://github.com/readrops/Readrops/issues/345) | Open | Already implemented in Droidrops | [Feature] Mark all read |
| [#344](https://github.com/readrops/Readrops/issues/344) | Open | Fixed in Droidrops; unit and emulator checks passed | [Bug]  Open in external browser |
| [#343](https://github.com/readrops/Readrops/issues/343) | Open | Needs reporter information; upstream body is the untouched bug template | [Bug] |
| [#342](https://github.com/readrops/Readrops/issues/342) | Open | Needs reporter information; upstream body is the untouched bug template | [Bug] |
| [#341](https://github.com/readrops/Readrops/issues/341) | Open | Fixed and verified | [Bug] Mark all read does not work with FreshRSS |
| [#339](https://github.com/readrops/Readrops/issues/339) | Open | Local fix, not committed; local-account JSON backup and non-destructive restore, emulator UI verified | [Feature] backup/restore all data |
| [#338](https://github.com/readrops/Readrops/issues/338) | Open | Fixed in Droidrops 2.2.21; multiline Atom/API regressions pass | [Bug] title display line break |
| [#337](https://github.com/readrops/Readrops/issues/337) | Closed | Closed upstream - verify in Droidrops | [Feature] Sync read state with FreshRSS server |
| [#336](https://github.com/readrops/Readrops/issues/336) | Open | Fixed locally; RSS2 and emulator suites pass | [Bug] fosstodon |
| [#335](https://github.com/readrops/Readrops/issues/335) | Open | Fixed and verified | App crashed after using back gesture |
| [#334](https://github.com/readrops/Readrops/issues/334) | Open | Fixed and verified | [Bug] Can't add Google Reader API account (Miniflux) |
| [#333](https://github.com/readrops/Readrops/issues/333) | Open | Exact reported YouTube `file://` launch regression passes on emulator | Crash on opening article with YouTube embed |
| [#332](https://github.com/readrops/Readrops/issues/332) | Open | Fixed in Droidrops; TLS guidance regression passes on emulator | Have more information to debug login issues |
| [#331](https://github.com/readrops/Readrops/issues/331) | Open | Fixed and verified | [Bug] Scroll wheel doesn't refresh properly |
| [#330](https://github.com/readrops/Readrops/issues/330) | Open | Reading-time clock cue added locally | [Bug] Feed items always show "<1min" instead of actual publication time |
| [#329](https://github.com/readrops/Readrops/issues/329) | Closed | Closed upstream - verify in Droidrops | Error importing opml file |
| [#328](https://github.com/readrops/Readrops/issues/328) | Open | Fixed and verified | [Bug] Cannot mark article as read with fever api of miniflux instance |
| [#327](https://github.com/readrops/Readrops/issues/327) | Open | Fixed and verified | Se detiene al deslizar un articulo |
| [#326](https://github.com/readrops/Readrops/issues/326) | Open | Fixed and verified on tablet-sized AVD | [Feature] Close category selection pane on tablet |
| [#325](https://github.com/readrops/Readrops/issues/325) | Open | Fixed in Droidrops; English/French copy tests pass on emulator | Improve grammar in the crash notice and change stylization "Github" â€”> "GitHub" |
| [#324](https://github.com/readrops/Readrops/issues/324) | Open | Fixed in Droidrops; larger picker text visually verified on emulator | Use larger font size for the chooseable things in multi-choice setting pickers |
| [#323](https://github.com/readrops/Readrops/issues/323) | Open | Implemented locally; emulator navigation verified | Add an animation for switching app tabs |
| [#322](https://github.com/readrops/Readrops/issues/322) | Open | Wording corrected locally | "multi-services RSS client" -> "Feed reader supporting multiple services" |
| [#321](https://github.com/readrops/Readrops/issues/321) | Open | Wording corrected locally | Call it a feed reader |
| [#320](https://github.com/readrops/Readrops/issues/320) | Open | Fixed locally; dedicated 24 dp icon verified on emulator | Use another icon for a local account |
| [#319](https://github.com/readrops/Readrops/issues/319) | Open | OPML dialog capitalization corrected; verified on emulator | "OPML Import/Export" -> "OPML import/export" |
| [#318](https://github.com/readrops/Readrops/issues/318) | Open | Fixed in Droidrops; low-contrast fallback regression passes on emulator | Do not use dynamic color for text |
| [#317](https://github.com/readrops/Readrops/issues/317) | Open | Fixed locally; empty-document guard tests and emulator suites passed | Error when importing OPML |
| [#316](https://github.com/readrops/Readrops/issues/316) | Open | Implemented locally; parser and app emulator suites pass | [Feature] Easily access hovertext for images |
| [#315](https://github.com/readrops/Readrops/issues/315) | Open | Undo verified on emulator; explicit-state restoration implemented | [Feature] Undo-Popup for removing article from favourites |
| [#314](https://github.com/readrops/Readrops/issues/314) | Open | Fixed in Droidrops; verified by DB tests (same correction as #291) | [Feature] Separate View settings in Favourites |
| [#313](https://github.com/readrops/Readrops/issues/313) | Closed | Closed upstream - verify in Droidrops | [Bug] |
| [#312](https://github.com/readrops/Readrops/issues/312) | Closed | Fixed in Droidrops | [Bug] SQLiteConstraintException when trying to update folder name |
| [#311](https://github.com/readrops/Readrops/issues/311) | Closed | Closed upstream - verify in Droidrops | [Bug] |
| [#309](https://github.com/readrops/Readrops/issues/309) | Open | Fixed and verified | Crash error |
| [#308](https://github.com/readrops/Readrops/issues/308) | Closed | Closed upstream - verify in Droidrops | [Bug] Alpha colors not supported by Android color contrast tools |
| [#307](https://github.com/readrops/Readrops/issues/307) | Closed | Closed upstream - verify in Droidrops | [Bug] Some feeds intangible and unresponsive, tapping them does nothing |
| [#305](https://github.com/readrops/Readrops/issues/305) | Open | Related issue fixed | App crashes with FileUriExposedException on some articles that embed openstreetmap |
| [#304](https://github.com/readrops/Readrops/issues/304) | Open | Local excerpt share action and citation formatter added; tests pass | [Feature] Add "Share with Reference" option for selected text |
| [#303](https://github.com/readrops/Readrops/issues/303) | Open | Implemented and verified | [Feature] Option to disable swipe left/right to go to the previous/next article |
| [#302](https://github.com/readrops/Readrops/issues/302) | Closed | Closed upstream - verify in Droidrops | [Feature] Search |
| [#300](https://github.com/readrops/Readrops/issues/300) | Closed | Closed upstream - verify in Droidrops | [Feature] Material 3 Compatability |
| [#298](https://github.com/readrops/Readrops/issues/298) | Open | Fixed and verified on Pixel 9 API 35 AVD | [Feature] Short tap vs long tap in the `Feeds` tab |
| [#297](https://github.com/readrops/Readrops/issues/297) | Open | Fixed locally; parser and repository regression tests passed on emulator | [Bug] Atom handling |
| [#296](https://github.com/readrops/Readrops/issues/296) | Open | Fixed in Droidrops | [Feature] Auto-update when adding feed |
| [#295](https://github.com/readrops/Readrops/issues/295) | Open | Fixed and verified | [Bug] Adding direct URL to JSON Feed |
| [#294](https://github.com/readrops/Readrops/issues/294) | Open | Local fix; query regression and emulator restoration check passed | [Regression] When in the background for some time on ItemScreenPage, the app reopens on the wrong item when coming to the foreground |
| [#293](https://github.com/readrops/Readrops/issues/293) | Open | Fixed in Droidrops; portrait emulator verified | [Bug] Initial tablet mode portrait |
| [#292](https://github.com/readrops/Readrops/issues/292) | Closed | Closed upstream - verify in Droidrops | [Bug] an Error Occurred when adding mastodon.rss |
| [#291](https://github.com/readrops/Readrops/issues/291) | Open | Fixed locally; DB tests passed | [Feature] Keep read articles visible in Favorites |
| [#290](https://github.com/readrops/Readrops/issues/290) | Open | Fixed in Droidrops; API tests passed | [Bug] No feeds from Nextcloud News |
| [#289](https://github.com/readrops/Readrops/issues/289) | Open | Fixed and verified | [Bug] When adding a new feed, it is not sorted in the feed tab |
| [#288](https://github.com/readrops/Readrops/issues/288) | Closed | Closed upstream - verify in Droidrops | [Bug] When adding a new feed, it doesn't appear in drawer |
| [#286](https://github.com/readrops/Readrops/issues/286) | Closed | Closed upstream - verify in Droidrops | [Feature] Change settings for `open_in` per feed |
| [#285](https://github.com/readrops/Readrops/issues/285) | Closed | Closed upstream - verify in Droidrops | [Feature] Remove duplicates links froms differents feeds |
| [#284](https://github.com/readrops/Readrops/issues/284) | Open | Fixed and verified on Pixel 9 API 35 AVD | [Feature] Allow the use of the Auto-Generated Material You theme |
| [#282](https://github.com/readrops/Readrops/issues/282) | Open | Needs feed URL and missing item IDs | [Bug] Some items are for some feeds are not fetched |
| [#281](https://github.com/readrops/Readrops/issues/281) | Open | Fixed locally; monochrome adaptive icon verified on emulator | [Feature] Make readrops icon inline with pixel launcher icon theme |
| [#280](https://github.com/readrops/Readrops/issues/280) | Open | Fixed in Droidrops; API tests passed | [Bug] Cannot login to FreshRSS session, error 400 |
| [#278](https://github.com/readrops/Readrops/issues/278) | Closed | Closed upstream - verify in Droidrops | [Feature] Fix Dark mode text contrast |
| [#277](https://github.com/readrops/Readrops/issues/277) | Open | Implemented locally; DB tests and emulator UI verified | [Feature] Show recently read articles |
| [#271](https://github.com/readrops/Readrops/issues/271) | Open | Needs server unread count and settings | Retrieving 1250 items only |
| [#268](https://github.com/readrops/Readrops/issues/268) | Open | Fixed and verified | [Bug] cannot parse valid atom feed |
| [#267](https://github.com/readrops/Readrops/issues/267) | Closed | Closed upstream - verify in Droidrops | [Feature] dark mode colors |
| [#265](https://github.com/readrops/Readrops/issues/265) | Closed | Closed upstream - verify in Droidrops | Failed |
| [#264](https://github.com/readrops/Readrops/issues/264) | Open | Fixed in Droidrops; emulator suite passed | Crash at clicking on a Readrops element. |
| [#262](https://github.com/readrops/Readrops/issues/262) | Open | Local fix, emulator tests passed | `FolderDao.selectFoldersAndFeeds`: why is SQL query so complex? |
| [#261](https://github.com/readrops/Readrops/issues/261) | Open | Fixed and verified | [Bug] Too long content causes app crashes with SQLiteBlobTooBigException |
| [#260](https://github.com/readrops/Readrops/issues/260) | Open | Partially addressed by the latest-articles launcher widget; overlay integration remains TODO | [Feature] Add support for being a feed source for launchers |
| [#258](https://github.com/readrops/Readrops/issues/258) | Open | Fixed in Droidrops | [Bug] UX: swiping away an item is too easy |
| [#255](https://github.com/readrops/Readrops/issues/255) | Closed | Closed upstream - verify in Droidrops | [Bug] The feed list displayed in the drawer is confusing |
| [#252](https://github.com/readrops/Readrops/issues/252) | Open | Local fix, not committed; on-demand full-article extraction, emulator verified on a real article | [Feature] Add Full Content Scraping/Try to Show Full Content |
| [#251](https://github.com/readrops/Readrops/issues/251) | Closed | Closed upstream - verify in Droidrops | [Feature] Customize the share intent text using a template engine |
| [#250](https://github.com/readrops/Readrops/issues/250) | Open | Fixed locally; app tests and emulator reader verified | [Feature] Remember scroll position in article view |
| [#249](https://github.com/Readrops/Readrops/issues/249) | Open | HTTP transport verified; .local DNS/TLS need reporter details | [Bug] Can't add local FreshRSS account hosted on a .local YunoHost |
| [#247](https://github.com/readrops/Readrops/issues/247) | Closed | Closed upstream - verify in Droidrops | app crashes consistently when opening one specific article |
| [#246](https://github.com/readrops/Readrops/issues/246) | Closed | Closed upstream - verify in Droidrops | [Bug] https://media.mts.ru/rss - empty list |
| [#245](https://github.com/readrops/Readrops/issues/245) | Closed | Closed upstream - verify in Droidrops | App crashes while importing local opml file |
| [#244](https://github.com/readrops/Readrops/issues/244) | Closed | Closed upstream - verify in Droidrops | OPML Import from Newsflash crashes |
| [#243](https://github.com/readrops/Readrops/issues/243) | Open | Fixed; same behavior as #291 | [Feature] Ignore filter hide read feeds for favorites folder |
| [#242](https://github.com/readrops/Readrops/issues/242) | Closed | Closed upstream - verify in Droidrops | [Question] sync read status with FreshRSS |
| [#240](https://github.com/readrops/Readrops/issues/240) | Closed | Closed upstream - verify in Droidrops | Filter for Readrops |
| [#238](https://github.com/readrops/Readrops/issues/238) | Open | Local fix; Undo action verified on Pixel 9 / API 35 | [Feature] Undo news read |
| [#235](https://github.com/readrops/Readrops/issues/235) | Open | Fixed behavior; folder deletion test passed | [Bug] Delete feeds |
| [#234](https://github.com/readrops/Readrops/issues/234) | Open | Fixed locally; FreshRSS regression test passed on emulator | [Bug] Adding feeds reports error when none exists |
| [#233](https://github.com/readrops/Readrops/issues/233) | Open | Current URL field cursor and horizontal scrolling verified on Pixel 9 / API 35; no reproducible bug | [Bug] Missing expected cursor scroll behaviour |
| [#232](https://github.com/readrops/Readrops/issues/232) | Open | Implemented and emulator UI verified | [Feature] display only unread articles |
| [#229](https://github.com/readrops/Readrops/issues/229) | Closed | Closed upstream - verify in Droidrops | [Bug]  Retrofit Error Using Fever API in RSS Client |
| [#228](https://github.com/readrops/Readrops/issues/228) | Closed | Closed upstream - verify in Droidrops | [Bug] Error parsing yarr fever API feeds response |
| [#227](https://github.com/readrops/Readrops/issues/227) | Open | Local fix, FreshRSS full API URL normalized; unit test passed | [Bug] http 404, URL not found |
| [#226](https://github.com/readrops/Readrops/issues/226) | Closed | Closed upstream - verify in Droidrops | [Bug] Image download |
| [#225](https://github.com/readrops/Readrops/issues/225) | Open | Fixed locally; scroll position restored after app restart on Pixel 9 / API 35 | [Feature] Jump to most recent read |
| [#224](https://github.com/readrops/Readrops/issues/224) | Open | Existing encrypted-preferences recovery verified by instrumentation test | [Bug] Crash on opening |
| [#223](https://github.com/readrops/Readrops/issues/223) | Closed | Closed upstream - verify in Droidrops | App crash after opening an article from notification menu |
| [#222](https://github.com/readrops/Readrops/issues/222) | Closed | Closed upstream - verify in Droidrops | [Feature] Allow to configure swipe |
| [#221](https://github.com/readrops/Readrops/issues/221) | Closed | Closed upstream - verify in Droidrops | [Bug] ãªã‚“ã§ã€Œãƒ•ã‚£ãƒ¼ãƒ‰å‡¦ç†ã‚¨ãƒ©ãƒ¼ã€ã¨å‡ºã¦ãã‚‹ã®ã‹ã‚ã‹ã‚‰ãªã„ |
| [#220](https://github.com/readrops/Readrops/issues/220) | Open | Existing HTTP(S)-only article URL guard verified; regression instrumentation test passed | crash log |
| [#219](https://github.com/readrops/Readrops/issues/219) | Closed | Closed upstream - verify in Droidrops | [Feature]: http://127.0.0.1:8080 results in HTTP 503 |
| [#218](https://github.com/readrops/Readrops/issues/218) | Open | Local fix; legacy Feedly sync regression test passed on Pixel 9 / API 35 | Crash immediately on open v2.0.1 |
| [#217](https://github.com/readrops/Readrops/issues/217) | Open | Implemented and emulator UI verified | [Feature]Also display publish time of the feed items |
| [#216](https://github.com/readrops/Readrops/issues/216) | Open | Local session-only Recently read filter implemented; tests and emulator UI check pass | [Feature] Recently read filter |
| [#215](https://github.com/readrops/Readrops/issues/215) | Open | Inline thumbnails added to regular and compact timeline cards | [Request] Article Thumbnail |
| [#214](https://github.com/readrops/Readrops/issues/214) | Closed | Closed upstream - verify in Droidrops | [Bug] App crashing on start after upgrading from 2.0 beta01 |
| [#213](https://github.com/readrops/Readrops/issues/213) | Open | Local item-retention cap implemented; does not limit upstream response download/parsing | [Feature]Limit number of RSS items to fetch |
| [#212](https://github.com/readrops/Readrops/issues/212) | Closed | Closed upstream - verify in Droidrops | [Feature] Remember filters |
| [#211](https://github.com/readrops/Readrops/issues/211) | Open | Local maximum-history setting added; preserves starred items; instrumentation verified | [Feature] Editable feed history |
| [#210](https://github.com/readrops/Readrops/issues/210) | Open | Local fix, not committed | [Feature] Add a search bar |
| [#209](https://github.com/readrops/Readrops/issues/209) | Open | Keeps cached timeline visible during sync; compact local-sync progress banner added | [Feature] Accessing feeds during synchronisation. |
| [#208](https://github.com/readrops/Readrops/issues/208) | Closed | Closed upstream - verify in Droidrops | [Bug] |
| [#207](https://github.com/readrops/Readrops/issues/207) | Closed | Closed upstream - verify in Droidrops | [Bug] settings lost after closing the app |
| [#206](https://github.com/readrops/Readrops/issues/206) | Closed | Closed upstream - verify in Droidrops | [Bug] Crash at start |
| [#205](https://github.com/readrops/Readrops/issues/205) | Closed | Closed upstream - verify in Droidrops | [Bug] image title text no longer accessible |
| [#204](https://github.com/readrops/Readrops/issues/204) | Closed | Closed upstream - verify in Droidrops | [Feature] Black theme for OLED displays |
| [#203](https://github.com/readrops/Readrops/issues/203) | Closed | Closed upstream - verify in Droidrops | [Bug] Unreadable feed name in timeline |
| [#202](https://github.com/readrops/Readrops/issues/202) | Closed | Closed upstream - verify in Droidrops | [Bug] Wrong article dates and times |
| [#201](https://github.com/readrops/Readrops/issues/201) | Closed | Closed upstream - verify in Droidrops | [Bug] Files with .opml extension are unavailable for import |
| [#200](https://github.com/readrops/Readrops/issues/200) | Open | Fever URL normalization hardened; old Koin crash not reproduced | Error on Fever setup 2.0-beta02 |
| [#199](https://github.com/readrops/Readrops/issues/199) | Closed | Closed upstream - verify in Droidrops | Sync favorites with nextcloud |
| [#198](https://github.com/readrops/Readrops/issues/198) | Closed | Closed upstream - verify in Droidrops | "Show read items" not saved on v2-beta2 |
| [#197](https://github.com/readrops/Readrops/issues/197) | Closed | Closed upstream - verify in Droidrops | Default Android font is not used |
| [#196](https://github.com/readrops/Readrops/issues/196) | Open | Pinch zoom settings test passes; manual image gesture not demonstrated | [Feature Request] Zoom and scroll bars |
| [#195](https://github.com/readrops/Readrops/issues/195) | Closed | Closed upstream - verify in Droidrops | OPML Import not working in 2.0-beta02 |
| [#194](https://github.com/readrops/Readrops/issues/194) | Open | Feeder-style OPML 1.1 import covered by new regression test; API suite passed | Support importing old `.opml` files |
| [#193](https://github.com/readrops/Readrops/issues/193) | Closed | Closed upstream - verify in Droidrops | [Feature Request] mDNS support for Account URLs |
| [#192](https://github.com/readrops/Readrops/issues/192) | Closed | Closed upstream - verify in Droidrops | Error when refreshing feed |
| [#191](https://github.com/readrops/Readrops/issues/191) | Closed | Closed upstream - verify in Droidrops | 2.0-beta01: Error when migrating from 1.3.1 |
| [#190](https://github.com/readrops/Readrops/issues/190) | Closed | Closed upstream - verify in Droidrops | 2.0-beta1: Unable to scroll in Parameters |
| [#189](https://github.com/readrops/Readrops/issues/189) | Closed | Closed upstream - verify in Droidrops | 2.0-beta01 crash: IllegalArgumentException: baseUrl must end in / |
| [#188](https://github.com/readrops/Readrops/issues/188) | Closed | Closed upstream - verify in Droidrops | [Feature Request] Restore Swipe-to-mark-as-read |
| [#187](https://github.com/readrops/Readrops/issues/187) | Open | Slot-height regression tests pass; article gesture still needs a long-content fixture | Inconsistent scrolling |
| [#186](https://github.com/readrops/Readrops/issues/186) | Closed | Closed upstream - verify in Droidrops | FreshRSS accounts don't work under 2.0-beta01 |
| [#185](https://github.com/readrops/Readrops/issues/185) | Closed | Closed upstream - verify in Droidrops | Item size dialogue not auto-confirming |
| [#184](https://github.com/readrops/Readrops/issues/184) | Closed | Closed upstream - verify in Droidrops | Initial scroll on reading |
| [#183](https://github.com/readrops/Readrops/issues/183) | Closed | Closed upstream - verify in Droidrops | Autofill no longer working for login fields |
| [#182](https://github.com/readrops/Readrops/issues/182) | Closed | Closed upstream - verify in Droidrops | error with 2.0 beta 01 from fdroid |
| [#178](https://github.com/readrops/Readrops/issues/178) | Open | Fever URL normalization regression tests pass; original crash needs trace to diagnose | fever account |
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
| [#161](https://github.com/readrops/Readrops/issues/161) | Open | Fixed and verified | Strict XHTML parsing ? |
| [#160](https://github.com/readrops/Readrops/issues/160) | Open | Fixed and verified | Error: "Expected a string but was NULL at path $.items[3].author" |
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
| [#137](https://github.com/readrops/Readrops/issues/137) | Open | Added fr-FR title and descriptions; automated translation workflow remains open | Translatable fastlane entries |
| [#135](https://github.com/readrops/Readrops/issues/135) | Closed | Closed upstream - verify in Droidrops | FR: Switch theme based on device theme |
| [#134](https://github.com/readrops/Readrops/issues/134) | Open | Partially addressed; native backend still unsupported | feature request: miniflux backend support |
| [#133](https://github.com/readrops/Readrops/issues/133) | Closed | Closed upstream - verify in Droidrops | Change floating button action - replace Add New Feed (+) with Mark As Read (âœ“) |
| [#132](https://github.com/readrops/Readrops/issues/132) | Closed | Closed upstream - verify in Droidrops | Remove sort button |
| [#131](https://github.com/readrops/Readrops/issues/131) | Closed | Closed upstream - verify in Droidrops | Remove floating button |
| [#130](https://github.com/readrops/Readrops/issues/130) | Closed | Closed upstream - verify in Droidrops | Hide preview text |
| [#129](https://github.com/readrops/Readrops/issues/129) | Closed | Closed upstream - verify in Droidrops | Hide date |
| [#128](https://github.com/readrops/Readrops/issues/128) | Closed | Closed upstream - verify in Droidrops | Hide read time estimates |
| [#127](https://github.com/readrops/Readrops/issues/127) | Closed | Closed upstream - verify in Droidrops | Hide folder information |
| [#126](https://github.com/readrops/Readrops/issues/126) | Closed | Closed upstream - verify in Droidrops | Hide top bar on scroll |
| [#125](https://github.com/readrops/Readrops/issues/125) | Closed | Closed upstream - verify in Droidrops | Per feed "open with" settings |
| [#122](https://github.com/readrops/Readrops/issues/122) | Open | Local fix, not committed; opt-in two-pane reader on landscape tablets, emulator verified | Tablet mode |
| [#120](https://github.com/readrops/Readrops/issues/120) | Open | Existing fix revalidated in current Droidrops code; unit and emulator checks passed | True black theme |
| [#119](https://github.com/readrops/Readrops/issues/119) | Open | Image, preview, source, publication-date, and reading-time visibility preferences implemented locally | Layout options |
| [#118](https://github.com/readrops/Readrops/issues/118) | Closed | Closed upstream - verify in Droidrops | Cannot enable storage permission |
| [#117](https://github.com/readrops/Readrops/issues/117) | Closed | Closed upstream - verify in Droidrops | Able to change what slide right will do |
| [#116](https://github.com/readrops/Readrops/issues/116) | Closed | Closed upstream - verify in Droidrops | Readrops asks for too much articles, overloads the server and no article is retrieved |
| [#115](https://github.com/readrops/Readrops/issues/115) | Open | Legacy multi-select scenario not applicable: current Droidrops has no article multi-select mode | [Bug] mark as read does not work when all articles are selected |
| [#114](https://github.com/readrops/Readrops/issues/114) | Open | Compact timeline and feed-list density controls implemented locally | [Request] More compact layout |
| [#113](https://github.com/readrops/Readrops/issues/113) | Closed | Closed upstream - verify in Droidrops | [Bug] Shortcut to open article with webview requires several click to work |
| [#112](https://github.com/readrops/Readrops/issues/112) | Closed | Closed upstream - verify in Droidrops | [Feature] Add ability to mark article as unread from the article itself |
| [#111](https://github.com/readrops/Readrops/issues/111) | Open | Implemented locally; link dialog and actions verified on emulator | Link actions dialog on long pressing on a link |
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
| [#89](https://github.com/readrops/Readrops/issues/89) | Open | Compact timeline visually verified on Pixel 9 API 35 emulator (2026-10-21) | Compact view |
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
| [#73](https://github.com/readrops/Readrops/issues/73) | Open | Not implemented; Android SAF-capable DecSync library identified, integration still required | Add support for DecSync |
| [#72](https://github.com/readrops/Readrops/issues/72) | Closed | Closed upstream - verify in Droidrops | FR: add an option for auto-sync |
| [#71](https://github.com/readrops/Readrops/issues/71) | Closed | Closed upstream - verify in Droidrops | Can't subscribe a feed provided from feedburner |
| [#70](https://github.com/readrops/Readrops/issues/70) | Closed | Closed upstream - verify in Droidrops | A variety of different errors |
| [#69](https://github.com/readrops/Readrops/issues/69) | Open | Local fix verified; self-hosted URL boundary tests pass | Leave link validation to server |
| [#68](https://github.com/readrops/Readrops/issues/68) | Open | Partially addressed; native API still unsupported | [Feature request] Tiny Tiny RSS |
| [#67](https://github.com/readrops/Readrops/issues/67) | Open | Widget added; refresh and article launch verified on emulator | Add support for homescreen widget? |
| [#66](https://github.com/readrops/Readrops/issues/66) | Open | Implemented locally; authenticated/public header tests pass on emulator | [Feature Request] Access Password-Protected RSS Feeds |
| [#62](https://github.com/readrops/Readrops/issues/62) | Closed | Closed upstream - verify in Droidrops | [Feature request] Swipe to next article |
| [#61](https://github.com/readrops/Readrops/issues/61) | Closed | Closed upstream - verify in Droidrops | incorrect category name parsing |
| [#60](https://github.com/readrops/Readrops/issues/60) | Closed | Closed upstream - verify in Droidrops | Sync already read articles |
| [#59](https://github.com/readrops/Readrops/issues/59) | Closed | Closed upstream - verify in Droidrops | Adding new feed causes all other articles to disappear - NC |
| [#58](https://github.com/readrops/Readrops/issues/58) | Closed | Closed upstream - verify in Droidrops | FreshRSS login: change password to api password |
| [#56](https://github.com/readrops/Readrops/issues/56) | Closed | Closed upstream - verify in Droidrops | [Feature Request] FreshRSS Read by category |
| [#55](https://github.com/readrops/Readrops/issues/55) | Closed | Closed upstream - verify in Droidrops | Application crashes when entering an url without http scheme |
| [#54](https://github.com/readrops/Readrops/issues/54) | Open | Tag filtering implemented; 39 DB instrumentation tests passed on Pixel 9 API 35 (2026-10-21) | Support for tags |
| [#53](https://github.com/readrops/Readrops/issues/53) | Closed | Closed upstream - verify in Droidrops | Sync read later / starred articles |
| [#52](https://github.com/readrops/Readrops/issues/52) | Closed | Closed upstream - verify in Droidrops | Readrops does not show old articles |
| [#51](https://github.com/readrops/Readrops/issues/51) | Closed | Closed upstream - verify in Droidrops | Crash of the application on startup |
| [#50](https://github.com/readrops/Readrops/issues/50) | Open | TODO - no fix identified | [Feature Request] Sync with FeedBin |
| [#49](https://github.com/readrops/Readrops/issues/49) | Closed | Closed upstream - verify in Droidrops | Sync with NC |
| [#48](https://github.com/readrops/Readrops/issues/48) | Closed | Closed upstream - verify in Droidrops | Support for self-signed certificates |
| [#46](https://github.com/readrops/Readrops/issues/46) | Open | Basic auth implemented; HTTP feed regression passed | [Feature Request] Support Basic Auth |
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

## External follow-ups

- [ ] Check for a response from AndroidX maintainers on [issue 579989838](https://issuetracker.google.com/issues/579989838), requesting matching native debug symbols for `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so` (Android Public Tracker, Graphics component; submitted 2026-11-01).
- [x] Resolved and reporter-confirmed: [Droidrops issue #3](https://github.com/thekester/Droidrops/issues/3) reported missing Miniflux feed favicons after initial sync. Version 2.2.34 restored them, and the reporter confirmed they appear (2026-11-01).

## Update rules

- Only mark an issue **fixed** with an identifiable commit, test, or manual check.
- A closed upstream issue must remain **to be verified** until the behavior has been validated in
  Droidrops.
- Uncommitted local changes are not considered a published fix.
- This file remains local and should not be added to a release commit.
