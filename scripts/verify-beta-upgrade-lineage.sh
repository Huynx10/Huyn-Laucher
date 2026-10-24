#!/usr/bin/env bash
# Fail closed when a new beta APK cannot safely replace the previous beta APK.
# Checks public signing identity, applicationId and strictly increasing versionCode.
set -euo pipefail
if [ "$#" -ne 5 ]; then
  echo "usage: verify-beta-upgrade-lineage.sh PREVIOUS_APK NEW_APK EXPECTED_CERT_SHA256 APKSIGNER AAPT" >&2
  exit 2
fi
PREVIOUS_APK="$1"
NEW_APK="$2"
EXPECTED_CERT="$3"
APKSIGNER="$4"
AAPT="$5"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Both packages must be valid and signed by the same pinned release certificate.
bash "$SCRIPT_DIR/verify-beta-signature.sh" "$PREVIOUS_APK" "$EXPECTED_CERT" "$APKSIGNER"
bash "$SCRIPT_DIR/verify-beta-signature.sh" "$NEW_APK" "$EXPECTED_CERT" "$APKSIGNER"

apk_identity() {
  local apk="$1"
  local badging parsed
  badging="$("$AAPT" dump badging "$apk")" || {
    echo "Cannot inspect APK package metadata" >&2
    return 1
  }
  parsed="$(printf '%s\n' "$badging" | sed -nE "s/^package: name='([^']+)' versionCode='([0-9]+)'.*/\1 \2/p" | head -n 1)"
  if ! [[ "$parsed" =~ ^[A-Za-z0-9._]+[[:space:]][0-9]+$ ]]; then
    echo "Missing or malformed APK applicationId/versionCode" >&2
    return 1
  fi
  printf '%s\n' "$parsed"
}
PREVIOUS_IDENTITY="$(apk_identity "$PREVIOUS_APK")"
NEW_IDENTITY="$(apk_identity "$NEW_APK")"
read -r PREVIOUS_PACKAGE PREVIOUS_CODE <<< "$PREVIOUS_IDENTITY"
read -r NEW_PACKAGE NEW_CODE <<< "$NEW_IDENTITY"
if [ "$PREVIOUS_PACKAGE" != "$NEW_PACKAGE" ]; then
  echo "APK applicationId changed; in-place upgrade is unsafe" >&2
  exit 1
fi
if (( 10#$NEW_CODE <= 10#$PREVIOUS_CODE )); then
  echo "New APK versionCode is not greater than previous beta; refusing release" >&2
  exit 1
fi
echo "Beta upgrade lineage verified: same signing certificate and applicationId; increasing versionCode"
