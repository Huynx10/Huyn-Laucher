# Safe beta APK releases

## Why this is gated
GitHub CI debug APKs use a temporary debug signing identity. They are **test artifacts only**, not guaranteed to update an installed beta. The historical beta3 workflow generated a new key for every run. Do not automatically uninstall any installed launcher or delete layout/widget data.

## One-time owner setup
1. Create or securely retrieve a **persistent** Android upload keystore with alias `upload`. Keep its private key and passwords outside the repository. Never put a key in a GitHub Issue, chat, log or release asset.
2. Configure repository Actions encrypted secrets: `BETA_KEYSTORE_BASE64` (base64-encoded keystore), `BETA_STORE_PASSWORD`, `BETA_KEY_PASSWORD`, `BETA_CERT_SHA256` (64-character hex SHA-256 fingerprint of the **signing certificate**).
3. Set repository Actions variable `BETA_RELEASE_ENABLED=true` **only after** all four secrets are configured. Without opt-in the release workflow is skipped safely.
4. Confirm the certificate fingerprint locally using `keytool -list -v -keystore <path> -alias upload`. Never share private key material. The workflow first verifies the public certificate from the persistent keystore against `BETA_CERT_SHA256` **before** building, then verifies the final APK with `apksigner`. Both checks refuse a fingerprint mismatch. A mismatch prints only the public certificate SHA-256; confirm it is compatible with the installed app before updating any secret.

## Automatic workflow
- A successful `main` push CI run triggers the beta release workflow when enabled.
- It checks out the **exact CI-tested SHA**, refuses stale main heads and duplicate tags, sets a monotonic beta versionCode from the CI run number, builds `assembleRelease` using the persistent key, verifies its certificate and uploads APK + SHA-256 checksum.
- A unique immutable pre-release tag is generated for each successful main CI run. A failed build, invalid signature, missing secret, changed main head or existing tag **must not publish** an APK.
- `app/build.gradle.kts` preserves the AI Studio signing configuration; CI-only `BETA_VERSION_CODE` and `BETA_VERSION_NAME` environment overrides are optional.

## Existing S24 Ultra installations
A release-signed APK can update an installed APK **only if the application ID and signing certificate are compatible and versionCode increases**. Existing beta3 and older debug builds may have unrelated signing certificates. This pipeline cannot repair that identity mismatch. Before any manual migration, make and verify a layout/settings backup and preserve widget configuration details. Never uninstall the existing app automatically. Device acceptance is still required to confirm in-place update and widget persistence.

## Regression coverage
`bash scripts/test-verify-beta-signature.sh` exercises matching, mismatched, malformed and invalid APK signer outcomes. `bash scripts/test-verify-beta-keystore.sh` checks the release-keystore preflight against a disposable test-only keystore (match, mismatch, malformed, missing and wrong password). The APK signer parser accepts legacy `Signer #1` and Build Tools 37 `V2 Signer` / `V3.0 Signer` certificate labels, rejecting missing or distinct multiple certificates. Android CI runs both on every PR. The GitHub release job additionally verifies the real APK with the Android SDK apksigner.
