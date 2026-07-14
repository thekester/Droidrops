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
