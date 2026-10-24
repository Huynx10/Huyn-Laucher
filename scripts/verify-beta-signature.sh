#!/usr/bin/env bash
# Validate APK signature and exact expected SHA-256 certificate fingerprint.
set -euo pipefail
APK="${1:?APK path required}"
EXPECTED="${2:?Expected certificate SHA-256 required}"
APKSIGNER="${3:-apksigner}"

if [[ ! -f "$APK" ]]; then
  echo "APK not found" >&2
  exit 2
fi
EXPECTED="$(printf '%s' "$EXPECTED" | tr -d '[:space:]:' | tr '[:upper:]' '[:lower:]')"
if [[ ! "$EXPECTED" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Expected certificate fingerprint must be 64 hex characters" >&2
  exit 2
fi
# apksigner verify must succeed before parsing the signer certificate.
SIGNATURE_INFO="$("$APKSIGNER" verify --print-certs "$APK")"
# Android Build Tools 37 may label certificate digests as "V2 Signer:" or
# "V3.0 Signer:" instead of the legacy "Signer #1" format.
# Accept only signer *certificate* SHA-256 digests, never public-key digests.
mapfile -t CERT_DIGESTS < <(printf '%s\n' "$SIGNATURE_INFO" | sed -nE 's/^[[:space:]]*(Signer #[0-9]+|V[0-9]+(\.[0-9]+)? Signer):?[[:space:]]+certificate SHA-256 digest:[[:space:]]*([0-9a-fA-F]{64})[[:space:]]*$/\3/p' | tr '[:upper:]' '[:lower:]' | sort -u)
if (( ${#CERT_DIGESTS[@]} != 1 )); then
  echo "Unable to identify exactly one APK signing certificate; refusing release" >&2
  echo "Distinct certificate fingerprints found: ${#CERT_DIGESTS[@]}" >&2
  exit 1
fi
ACTUAL="${CERT_DIGESTS[0]}"
if [[ "$ACTUAL" != "$EXPECTED" ]]; then
  echo "APK signing certificate does not match the configured beta identity" >&2
  echo "Actual APK signing certificate SHA-256: $ACTUAL" >&2
  echo "Compare this public certificate fingerprint with the intended release keystore and update BETA_CERT_SHA256 only after confirming update compatibility." >&2
  exit 1
fi
echo "APK signature and signing certificate verified"
