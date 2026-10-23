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
ACTUAL="$(printf '%s\n' "$SIGNATURE_INFO" | sed -nE 's/^[[:space:]]*Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]{64})[[:space:]]*$/\1/p' | head -n 1 | tr '[:upper:]' '[:lower:]')"
if [[ "$ACTUAL" != "$EXPECTED" ]]; then
  echo "APK signing certificate does not match the configured beta identity" >&2
  exit 1
fi
echo "APK signature and signing certificate verified"
