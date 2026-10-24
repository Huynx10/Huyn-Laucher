#!/usr/bin/env bash
# Fail fast if the persistent release keystore does not match the approved certificate.
# Only public certificate digests are printed; never log keystore/password/private key.
set -euo pipefail

KEYSTORE="${1:?Keystore path required}"
EXPECTED="${2:?Expected certificate SHA-256 required}"
: "${STORE_PASSWORD:?STORE_PASSWORD must be set}"

if [[ ! -f "$KEYSTORE" ]]; then
  echo "Release keystore not found" >&2
  exit 2
fi

EXPECTED="$(printf '%s' "$EXPECTED" | tr -d '[:space:]:' | tr '[:upper:]' '[:lower:]')"
if [[ ! "$EXPECTED" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Expected certificate fingerprint must be 64 hex characters" >&2
  exit 2
fi

# Export the public X.509 certificate as DER. Never export the private key.
# keytool reads the password from the environment, not a command-line argument.
ACTUAL="$(keytool -exportcert -keystore "$KEYSTORE" -alias upload -storepass:env STORE_PASSWORD | openssl dgst -sha256 | awk '{print $NF}' | tr '[:upper:]' '[:lower:]')"
if [[ ! "$ACTUAL" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Could not read a valid release signing certificate fingerprint" >&2
  exit 1
fi

if [[ "$ACTUAL" != "$EXPECTED" ]]; then
  echo "Release keystore certificate does not match BETA_CERT_SHA256; aborting before build" >&2
  echo "Actual release keystore certificate SHA-256: $ACTUAL" >&2
  echo "Confirm the intended update-compatible certificate before changing the GitHub secret." >&2
  exit 1
fi

echo "Persistent release keystore certificate verified"
