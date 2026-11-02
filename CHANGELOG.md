# v2.2.36

This release adds step-by-step feed URL diagnostics to help identify whether a connection problem comes from the address, network, server, authentication, TLS certificate, or feed response.

## Changes

- Check a feed address and explain each connection step in plain language.
- Discover RSS and Atom feed addresses on a website and suggest nearby feed URLs.
- Explain authentication and TLS certificate problems, with safe troubleshooting guidance.
- Include privacy-safe technical details that users can copy into a bug report.

# v2.2.35

This release reduces memory use when restoring the timeline.

## Changes

- Keep timeline previews lightweight and load full article content only when needed.

# v2.2.34

This release fixes Miniflux feed icons and a reader loading issue.

## Fixes

- Load and display Miniflux feed favicons, including when Android has cleared the image cache.
- Fix the article reader remaining on a loading spinner when opened from the timeline.

# v2.2.33

This release improves article reading and adds local storage cleanup tools.

## Changes

- Show article refresh progress and explain when syncing is unavailable offline.
- Zoom and rotate images while reading articles.
- Clean up old local article copies by age, read status, and feed, while protecting favourites and pending sync changes.

# v2.2.32

This release makes large Miniflux libraries safer to synchronize.

## Changes

- Import Miniflux articles progressively in configurable batches and resume from the previous sync position.
- Report page progress, imported counts, server totals, and the current cursor without logging article content or credentials.
- Improve memory handling and troubleshooting guidance for initial Miniflux syncs.

# v2.2.31

This release improves synchronization diagnostics across supported services.

## Changes

- Show privacy-safe sync progress and result counts for FreshRSS, Google Reader, Nextcloud News, Fever, and Miniflux.
- Include Miniflux page offsets and the last sync stage in failure and out-of-memory reports.
- Improve diagnostic guidance and Miniflux troubleshooting documentation.

# v2.2.30

This release improves the feel and feedback of timeline swipe actions.

## Changes

- Add responsive motion and haptic feedback to article read and favourite actions.
- Return swiped cards smoothly to their original position.

# v2.2.29

This release makes timeline cards remain visible and usable after swipe actions, while respecting the selected read/unread filter.

# v2.2.28

This release fixes timeline swipe behavior.

## Fixes

- Return article cards to their original position after a swipe.
- Keep the read/unread filter synchronized with swipe actions.

# v2.2.27

This release adds more useful network diagnostics.

## Changes

- Distinguish DNS failures, refused connections, timeouts, TLS errors, and HTTP responses in diagnostic reports.
- Send a complete diagnostic report by email as a text attachment.
- Respect the option to hide server addresses in reports.

# v2.2.26

This release adds native Miniflux support and updates the Android target level.

## Changes

- Connect to Miniflux through its REST API, with password or API key authentication.
- Synchronize Miniflux accounts, feeds, folders, articles, read states, and bookmarks.
- Target Android 16 (API 36).

# v2.2.25

This release fixes startup crashes and clarifies timeline and widget settings.

## Changes

- Add a home-screen widget setting, disabled by default.
- Make the large timeline layout the default.

## Fixes

- Fix a startup crash caused by release shrinking and a Compose dependency mismatch.

# v2.2.24

This release improves keyboard navigation in the article reader.

## Changes

- Keep keyboard shortcut focus invisible in the timeline and article reader.
- Improve keyboard navigation when switching articles in the two-pane reader.

# v2.2.23

This release improves restored article labels and tablet reading.

## Fixes

- Preserve article labels when restoring a backup.
- Keep the selected article visible and highlighted in the tablet two-pane reader.
- Improve backup compatibility and article navigation on large screens.

# v2.2.22

This release adds backup, full-article reading, and large-screen improvements.

## Changes

- Back up and restore accounts, feeds, article history, and app settings.
- Load full article text for feeds that provide summaries only.
- Read articles beside the timeline on tablets and other large screens.
- Add optional hardware keyboard shortcuts for common reading actions.
- Customize feed-list density, article previews, and metadata.

## Fixes

- Improve tablet navigation, reader state, and image-only article handling.

# v2.2.21

This release adds a home-screen widget and more ways to customize reading.

## Changes

- Choose from four text sizes when reading articles, with pinch-to-zoom support.
- Add a home-screen widget for the latest unread articles.
- Customize timeline cards by hiding images, feed details, publication dates, or reading times.
- Improve reader restoration, article paging, and handling of large articles.
- Improve RSS and Atom compatibility, including entries with missing titles or content.

## Fixes

- Fix article display, feed synchronization, and backup handling of private session data.

# v2.2.20

This release improves article reading and login troubleshooting.

## Changes

- Support pinch-to-zoom on article pages without showing zoom buttons.
- Improve contrast for feed colours and correct Droidrops branding in crash reports.
- Show actionable guidance for TLS certificate errors during login.

# v2.2.19

This release improves article reading, timeline navigation, and feed history.

## Changes

- Add a recently read filter and restore timeline position per account.
- Improve reading with image viewing, long-press link actions, excerpt sharing, and smoother scrolling.
- Configure feed history while preserving starred articles.
- Accept more OPML exports and handle files with no subscriptions gracefully.

## Fixes

- Fix blank article cards, Atom entries without links, FreshRSS URL normalization, sync/query edge cases, and a crash when opening Add Feed.

# v2.2.18

This release adds a true black theme and improves article state handling.

## Changes

- Add a true black theme.
- Keep read articles visible in Favorites when the unread-only filter is enabled.
- Make Undo restore the unread state reliably.
- Simplify folder/feed queries and add regression tests.

# v2.2.17

This release improves image captions and pull-to-refresh, and fixes a startup crash.

## Changes

- Show image title text below article images, using alt text when needed.
- Add a monochrome adaptive launcher icon for themed icons.

## Fixes

- Fix pull-to-refresh when the feed list is shorter than the screen.
- Fix an app startup crash caused by a missing Compose icons dependency.

# v2.2.16

This release improves feed parsing, FreshRSS behavior, and release identity.

## Changes

- Improve RSS and Atom parsing for feeds with nonstandard XML encodings.
- Update the app identity to Droidrops and modernize the optimized Android release build.

## Fixes

- Fix FreshRSS subscription reconciliation and timeline search typing.

# v2.2.15

This release improves article links and feed behavior.

## Changes

- Respect per-feed external browser settings.
- Support Atom entries with an ID and content but no article link.

## Fixes

- Prevent duplicate timeline articles when accounts share item IDs.

# v2.2.14

This release fixes importing RSS 1.0 feeds from OPML when the feed URL differs from the website URL.

# v2.2.13

This release fixes subscriptions imported from RSS 1.0 feeds.

## Fixes

- Preserve the requested feed URL when importing RSS 1.0 subscriptions, including feeds whose channel metadata points to the site's homepage.

# v2.2.12

This release improves timeline discovery, privacy, and landscape usability.

## Changes

- Add article search across titles and content.
- Explain the empty timeline and provide a direct action to discover feeds.
- Make the mark-all-as-read confirmation explicit.

## Fixes

- Hide server addresses throughout diagnostic logs when the privacy option is enabled.
- Fix the clipped timeline navigation pane in landscape orientation.
- Link the in-app changelog to the installed release version.

# v2.2.11

This release improves article reading, feed synchronization, and tablet navigation.

## Changes

- Add the Material You dynamic color theme on Android 12 and later.
- Allow the timeline navigation pane to be closed and reopened on tablets.
- Show article publication times and restore reading positions when reopening articles.
- Make timeline swipe actions less likely to trigger by accident and add undo for marking articles read.
- Support Atom entries with externally hosted content and JSON feeds served as `application/json`.

## Fixes

- Update edited FreshRSS articles in place instead of creating duplicates, while preserving read and favorite states.
- Keep synchronization of articles with very large content within SQLite cursor limits.
- Recover from unavailable encrypted preferences and preserve credentials when editing accounts.
- Accept complete FreshRSS API endpoint URLs and improve image downloads and article navigation.

# v2.2.10

This release makes it easier to discover feeds and improves article reading and synchronization.

## Changes

- Browse and search a curated directory of RSS feeds, filter by topic, and add several feeds at once.
- Open article images in a full-screen viewer with pinch and double-tap zoom.

## Fixes

- Fix Miniflux sign-in through the Google Reader API and Fever read-status updates.
- Restore mouse-wheel pull-to-refresh after a sync completes.

# v2.2.9

- Target Android 16 (API 36) to meet Google Play requirements.
- Raise the minimum supported Android version to Android 7.0 (API 24) for Google Play automatic protection.

# v2.2.7

This release fixes upgrade reliability, improves synchronization performance, and makes more feeds readable.

## Fixes

- Add the missing database migration required when upgrading older installations.
- Index article remote ids to speed up synchronization on large databases.
- Import OPML files from other readers regardless of their declared version.
- Support feeds whose entries have no title, including Mastodon feeds.
- Explain certificate errors instead of exposing a raw network failure.
- Remove empty timeline space for descriptions containing only markup.
- Normalize titles in Atom, RSS 1.0, and JSON feeds.
- Keep credentials out of device backups.

## Changes

- Add pull-to-refresh from the Feeds screen.
- Add undo support when removing an article from favourites.
- Expose image hover text on touch devices.
- Add a fade transition between app tabs.
- Improve contrast for custom feed colours.
- Increase the font size of multi-choice setting options.
- Improve crash notice wording and login debugging information.

# v2.2.6

This release improves feed synchronization, article rendering, accessibility, and date handling.

## Fixes

- Refresh newly subscribed feeds and their posts immediately.
- Handle server-side category renames without losing the selected timeline.
- Batch Google Reader item-state updates reliably.
- Handle RSS documents served with an incorrect content type.
- Improve article links, video handling, and local asset loading.
- Fix date conversion for feeds using timezone offsets.
- Add accessibility descriptions to key controls and improve account URL handling.

# v2.2.4

This release fixes synchronization after server-side category changes and new feed subscriptions.

## Fixes

- Keep the local category and timeline filter when a category is renamed on the server
- Refresh newly subscribed feeds and their existing posts immediately
- Prevent duplicate posts when a newly subscribed feed is returned by the next synchronization

# v2.2.3

This release rebrands the fork as Droidrops and keeps the recent stability fixes.

## Changes

- Rename the app to Droidrops and switch to a fork-specific `applicationId`
- Update repository, changelog, and issue links to the fork
- Update CI launch targets for the new package name

## Fixes

- Keep the release fixes for `#353`, `#356`, and `#360`

# v2.2.2

This release focuses on stability, feed compatibility, and crash fixes.

## Fixes

- Fix RSS article opening errors when a feed exposes non-web links (#353)
- Fix crash when resuming the app from recent apps on some devices (#356)
- Replace the default `okhttp` user agent with a neutral app-specific one to avoid server blocklists (#360)
- Fix date parsing for RSS feeds using numeric timezone offsets like `+1300`
- Preserve leading zeroes in MD5 hashes used by some authentication flows
- Improve URL handling for local assets and other non-web intents

# v2.1.1

- Fix unclickable feeds at the bottom of the drawer  on long lists (#301)
- Fix folders not being updatable when having no feeds for local accounts (#312)
- Remove AlphaSlider in ColorPickerDialog as alpha colors are not supported by Android color contrast utils (#308)
- Fix an annoying bug where certain items could not being set read/starred when coming back from the pager
- Do not display Feedly account type in account selection dialog

# v2.1.0

This release focuses mainly on QOL improvements. You will find among them long time requested features. I hope in the future to be able to deliver new releases in less than six months.

## New features

- 🆕 Pager in article screen: You can now swipe left/ right to go to the previous/next article without going back to the main list #62
- 🆕 New feed screen. A new screen replaces the dialog to add a feed.
  - All declared RSS resources in a website will appear in a list. You will be able to select only the ones you would like to add
  - You can now directly choose a folder for each feed to add
- 🆕 Feed color screen: You can now change each feed color in a new screen. #104
- 🆕 Per feed open parameter: you can now choose for each feed to open its articles in the article screen or directly in the external view. #105, #125
- 🆕 Initial tablet mode: left navigation bar and permanent navigation drawer in Timeline tab. More improvements will come for a 100% big screen support
- 🆕 Parameter to launch synchronization at startup #158
- 🆕 Parameter to set the default drawer filter at launch
- 🆕 Two parameters to customize left and right swipe actions in Timeline tab #117
- 🆕 Google Reader API which powers FreshRSS support can now be used as a standalone API
- 🆕 Modify shared article text with a custom template
- 🆕 Android 15 support

## Improvements

- Local account:
  - Synchronization speed improvements
  - Icons quality improved (currently only new icons will have better quality, a global option to reload all icons will come in the future)
  - Display feed banner in feed bottom sheet (currently new feeds only)
  - Improve media support for RSS2 and ATOM (Youtube...)
  - Date parsing improvements
- OPML import speed improvements
- Display feed notifications parameter in feed bottom sheet
- Make login checks less restrictive #193
- Move Folders beginning with \_ on top #78
- Improve feed color handling #172
- Autofill managers are now usable in login screen #253
- Translation updates

## Fixes

- Downloaded image now appears in media gallery #226
- Various image share/download fixes in article screen
- Fix scroll jump in article screen when touching the screen for the first time #184
- Fix a rare case where local parsing could fail #246
- Fix Nextcloud News feed creation where all local feeds could be deleted
- Fix crash when opening empty OPML file #244, #245
- Fix hide feeds without new items parameter for some accounts #255
- Fix crash when no item link was provided #247

## Contributions

- Thanks to all translators who worked on Weblate! #283, #274, #256, #241
- FreshRSS casing #230 by @Alkarex
- Fix endpoint slash #231 by @Alkarex
- Fix formatting of plain text items #236 by @equeim
- Two minor improvements for the German localization #237 by @BorisBrock
- Integrate login screen with autofill managers (#253) by @christophehenry
- UX: increase DrawerFolderItem's expand button to avoid missclicks (#257) by @christophehenry
- Corrects a gramatical form (#248) by @StuntsPT
- Customize shared text using template setting (#254) by @christophehenry
- Fix gap above bottom navigation bar (#266) by @equeim
- Fix colors on More tab (#270) by @equeim
- Fix HTML parsing (#273) by @equeim
- Share Intent template: Add french typography filter + improve template dialog (#269) by @christophehenry
- Add mecanism to deduce feeds location in special cases (#272) by @christophehenry
- Add managed punctuation marks to fr_typo (#279) by @christophehenry
- Allow sourcing account credentials from local.properties during development (#275) by @christophehenry
- Fix unread feed selection in FoldersAndFeedsQueryBuilder (#276) by @christophehenry

# v2.0.3
- Fix Fever API compatibility with TinyTiny RSS and yarr, should also fix other providers (#228 + #229) 
- Fix Nextcloud News item duplicates when syncing which would made the app unusable
- Fix Nextcloud News item parsing: items with no title will be ignored

# v2.0.2
- Fix crash when opening app from a notification (#223)
- Fix Fever API synchronization error (#228)

# v2.0.1

- Make Timeline tab filters persistent (#138)
- Change Timeline tab order field default value (#202)
- Fix crash when adding a Fever API account (#200)
- Be less strict with feed and folder names (#206)

# v2.0

- Restore swipe to mark as read (#188)
- Restore Ordering by article id in Timeline tab
- Improve OPML file picker filtering (#195)
- Translation updates
- See previous beta versions to get full changelog since v1.3

# v2.0-beta02

- Fix migration issues from v1.3 and older (especially for F-Droid builds)
- Make Preferences screen scrollable (#190)
- Fix wrong translation in RadioButtonPreferenceDialog (#185)
- Translation updates

# v2.0-beta01

## General

- 🆕 design:
  - 🆕 Material3: Readrops implements last material design system version 
  - 🆕 Bottom bar navigation: you can now navigate to feeds and account management way more easily, with 4 tabs in total:
    - Timeline 
    - Feeds 
    - Account 
    - More 
  - Timeline tab:
    - 🆕 Article size: you can now choose among three article sizes: compact, regular and large 
    - 🆕 You can now show only articles less than 24h old 
    - 🆕 Mark all articles as read FAB: the floating action button now lets you mark all articles read, taking into account the current filter, replacing opening new feed activity action 
    - 🆕 Mark articles read on scroll: an option is now available to mark items read on scroll 
    - 🆕 Drawer: hide folders and feeds without unread articles 
    - 🆕 Local account: sync now respects the current filter and will only synchronize affected feeds 
  - Feeds Tab:
    - 🆕 Feeds and folder management have been merged into a single screen 
  - Account Tab:
    - 🆕 Add, manage and remove any account from Account Tab 
  - More Tab:
    - 🆕 This new screen gathers some app infos, parameters, open source libraries and a donation dialog 
  - Articles screen:
    - The global UI has been improved with a new title layout 
    - 🆕 Action bottom bar: you now have access to a collapsable bottom bar containing the following actions:
      - Mark as read/non read 
      - Add to favorites/remove from favorites 
      - Share 
      - Open in external navigator/navigator view 
    - 🆕 A new font, Inter is used for the article content 
    - 🆕 Some html tags look have been improved:
      - blockquote 
      - figure/figcaption 
      - iframe 
      - table
    - "Open in" option has been reduced to two options: navigator view and external navigator 
- 🆕 FEVER API implementation, should work with any provider which supports it 
- Migrate to Nextcloud News API 1.3 
- 🆕 Follow system theme option (default)
- 🆕 Option to disable battery optimization for background synchronization
- Add support for new Android versions until Android 14 (API 34)

## Technical

- The UI has been entirely rewritten in Kotlin using Jetpack Compose, moving from old traditional view system
- All other Java parts have also been rewritten in Kotlin, including API implementations, repositories, etc
- RXJava was replaced by Kotlin coroutines and flows
- Migrate to Gradle Kotlin DSL
- Migrate dependencies to Version Catalog
- 🆕 Support user certificates

# v1.3.1

- FreshRSS : Fix items being fav unintentionally
- FreshRSS : Fix 401 error when synchronising for the second time

# v1.3.0

- New local RSS parser, much reliable
- New external navigator view for items (Custom tabs)
- FreshRSS and Nextcloud News favorites
- FreshRSS read state synchronisation
- New folder view when clicking on a drawer expandable item (#56)

# v1.2.1

- Accept null value for Nextcloud News feed attribute folderId #87
- Fix local RSS url parsing 

# v1.2.0

- Adaptive icon
- Background synchronisation
- Notifications
- FreshRSS and Nextcloud News synchronisation speed improvements
- Add feed direct share action
- Improve some html tags look
- Diplay image title or alt as caption (#63)
- Local RSS fixes (#70, #71)
- Other bug fixes and UI improvements

# v1.1.4

- Fix app crash when using an account url without http scheme #55
- Fix FreshRSS folder name parsing #61
- Fix feeds being deleted when adding a new feed with a NC News account #59

# v1.1.3

- Fix crash for API < 24 #51
- Fix item read state not syncing with Nextcloud News #49

# v1.1.2

- Fix opml not working in release version
- Fix account url not being changed when text field in add account activity was edited

# v1.1.1

Request write storage permission before downloading image.

# v1.1.0

- OPML import/export for local account
- Dark theme
- Share or download item image
- Open item in webview
- Minor bug fixes and improvements

# v1.0.2.2

Disable Proguard as it makes fail some functionalities.

# v1.0.2.1

Fix a crash related to Proguard Rules.

# v1.0.2

- Add swipe background to main list items
- Add preference to parse a fixed number of items when adding a local feed
- Change feed/folders way to interact
- Minor bug fixes and improvements

# v1.0.1

# v1.0 Initial release

- Local RSS parsing 
- RSS 2.0, ATOM and JSON formats support 
- Multiple accounts 
- Feeds and folders management (create, update and delete feeds/folders if your service API supports it)
- Nextcloud news support 
- FreshRSS support
