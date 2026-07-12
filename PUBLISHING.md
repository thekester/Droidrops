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

- Keep the source public and tag the release.
- Publish the corresponding source code for each binary release.
- Prefer a package name that is unique and clearly tied to the fork.
- Keep the app free of proprietary dependencies and tracking SDKs.
- Provide the build instructions used by the repo.
- Make the fork identity clear in the metadata and description.

## Notes

- The project currently uses `com.droidrops.app`.
- The release notes for the fork start at `v2.2.3`.
- The privacy policy is stored in `PRIVACY_POLICY.md`.
