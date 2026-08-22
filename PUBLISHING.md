# Publishing Checklist

## Google Play

- Use `Droidrops` as the app name.
- Use `com.droidrops.app` as the application id.
- Upload a signed Android App Bundle generated from the `release` variant.
- Include native debug symbols in the release bundle for Play Console crash analysis.
- Configure Play App Signing in Play Console.
- Link the privacy policy URL:
  - `https://raw.githubusercontent.com/thekester/Droidrops/develop/PRIVACY_POLICY.md`
- Fill in the Data Safety section accurately.
- Make sure the store listing states that this is a fork of Readrops.

### GitLab CI

The repository can publish to Google Play through GitLab CI:

- `unit_tests` runs Gradle unit tests and lint.
- `bundle_release` builds the signed release AAB.
- `upload_internal` pushes the AAB to the `internal` track automatically on `develop`.
- `upload_production` is reserved for tagged releases and uploads to `production`.

Required GitLab CI variables:

- `RELEASE_KEYSTORE_FILE` as a file variable pointing to the upload keystore.
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`
- `PLAY_SERVICE_ACCOUNT_JSON` as a file variable pointing to the Google Play service account JSON.

Optional GitLab CI variables:

- `PLAY_TRACK` to override the default track.
- `PLAY_RELEASE_STATUS` to override the default release status.

Notes:

- The `bundle_release` job creates `local.properties` from the CI variables so Gradle can sign the bundle.
- The Fastlane lane reads the bundle from `app/build/outputs/bundle/release/app-release.aab`.
- On first use, make sure Play App Signing is enabled and the service account has access to the app in Play Console.
- The release build requests `ndk.debugSymbolLevel = FULL`, so any available native debug symbols are packaged in the AAB and do not need a separate upload. Some third-party `.so` files are already stripped upstream, so Play Console may still show a non-blocking warning for those libraries.

### First Google Play release

The repository already has a local upload keystore configured for development:

- File: `release-keystore.jks` (ignored by Git)
- Alias: `droidrops-release`

Do not commit this keystore or its passwords. Upload the same keystore as the
GitLab file variable `RELEASE_KEYSTORE_FILE`, then add the matching passwords
and alias as CI/CD variables. The first signed AAB must be uploaded to the
internal testing track from Play Console. During that first release, choose
Play App Signing if Play Console asks. Google then keeps the app-signing key,
while GitLab continues to use the upload keystore for future uploads.

Once the first release is accepted, pushes to `develop` run the automated
tests, build a signed AAB, and upload it to internal testing. The testers must
be added in Play Console under `Testing > Internal testing`.

## F-Droid

- F-Droid needs a public source repo, a FOSS license, FOSS dependencies, and a release tag for the version you want to publish.
- Forks must have a distinct Android application id, and the name/icon/string changes should clearly identify the fork.
- The upstream source repo should contain the store metadata files in `fastlane/metadata/android/en-US/`.
- F-Droid pulls the summary and description from that Fastlane metadata, so do not duplicate them in `fdroiddata`.
- If you want a reproducible-build submission with `Binaries` and `AllowedAPKSigningKeys`, keep a release signing key for the fork and back it up safely.
- In that setup, also upload the signed release APK to the matching GitHub release tag so `Binaries` can point to it.
- When generating that signed release APK, use `apksigner --alignment-preserved`; a plain re-sign rewrites ZIP layout and breaks F-Droid reproducibility.
- The actual package submission lives in the separate `fdroiddata` repo on GitLab, not in this app repo.
- Use the helper script to automate the repeatable parts:
  - `.\scripts\prepare-fdroid-release.ps1`
  - It rebuilds the release APK.
  - It writes a metadata template to `build/fdroid/metadata/com.droidrops.app.yml`.
  - It writes a release report with the commit, expected tag, APK path, and SHA-256.
- Use the publish script when you want to push the metadata to your GitLab fork and open the merge request automatically:
  - `.\scripts\publish-fdroid-release.ps1`
  - It uses `GITLAB_TOKEN` or `-GitLabToken`.
  - It creates a branch in your fork, uploads `metadata/com.droidrops.app.yml`, and opens the MR against `fdroid/fdroiddata`.
- Use the status helper if you want a quick read on the local `fdroiddata` checkout:
  - `.\scripts\fdroiddata-status.ps1`

Manual steps that still need you:

1. Make sure the release tag exists in the source repo, for example `v2.2.3`.
2. If you use `publish-fdroid-release.ps1`, review the merge request it creates and respond to maintainer feedback.
3. If you submit manually instead, go to `https://gitlab.com/fdroid/fdroiddata`, copy the generated `metadata/com.droidrops.app.yml`, run `fdroid lint com.droidrops.app` and `fdroid build com.droidrops.app`, then open the merge request yourself.

## Notes

- The project currently uses `com.droidrops.app`.
- The release notes for the fork start at `v2.2.3`.
- The privacy policy is stored in `PRIVACY_POLICY.md`.
