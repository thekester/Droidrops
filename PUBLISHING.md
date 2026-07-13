# Publishing Checklist

## Google Play

- Use `Droidrops` as the app name.
- Use `com.droidrops.app` as the application id.
- Upload a signed Android App Bundle generated from the `release` variant.
- Configure Play App Signing in Play Console.
- Link the privacy policy URL:
  - `https://raw.githubusercontent.com/thekester/Droidrops/develop/PRIVACY_POLICY.md`
- Fill in the Data Safety section accurately.
- Make sure the store listing states that this is a fork of Readrops.

## F-Droid

- F-Droid needs a public source repo, a FOSS license, FOSS dependencies, and a release tag for the version you want to publish.
- Forks must have a distinct Android application id, and the name/icon/string changes should clearly identify the fork.
- The upstream source repo should contain the store metadata files in `fastlane/metadata/android/en-US/`.
- The actual package submission lives in the separate `fdroiddata` repo on GitLab, not in this app repo.
- Use the helper script to automate the repeatable parts:
  - `.\scripts\prepare-fdroid-release.ps1`
  - It rebuilds the release APK.
  - It writes a metadata template to `build/fdroid/metadata/com.droidrops.app.yml`.
  - It writes a release report with the commit, expected tag, APK path, and SHA-256.

Manual steps that still need you:

1. Create and push the release tag if it does not already exist, for example `v2.2.3`.
2. Go to `https://gitlab.com/fdroid/fdroiddata`.
3. Create or update `metadata/com.droidrops.app.yml` from the generated template in `build/fdroid/metadata/`.
4. Run `fdroid lint com.droidrops.app` and `fdroid build com.droidrops.app` in `fdroiddata` to validate the recipe.
5. Open the merge request in `fdroiddata` and respond to maintainer feedback.

## Notes

- The project currently uses `com.droidrops.app`.
- The release notes for the fork start at `v2.2.3`.
- The privacy policy is stored in `PRIVACY_POLICY.md`.
