#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Disposable test-only key; never used for a real APK or committed to git.
KEYSTORE="$TMP/disposable-test.p12"
PASSWORD="ci-test-only-password"
STORE_PASSWORD="$PASSWORD" keytool -genkeypair -noprompt -storetype PKCS12 \
  -keystore "$KEYSTORE" -alias upload -keyalg RSA -keysize 2048 \
  -validity 1 -dname "CN=Test,O=CI,C=US" \
  -storepass "$PASSWORD" -keypass "$PASSWORD" >/dev/null 2>&1
EXPECTED="$(STORE_PASSWORD="$PASSWORD" keytool -exportcert -keystore "$KEYSTORE" \
  -alias upload -storepass:env STORE_PASSWORD | openssl dgst -sha256 | awk '{print $NF}')"
ZERO="$(printf '%064d' 0)"

STORE_PASSWORD="$PASSWORD" bash "$ROOT/scripts/verify-beta-keystore.sh" "$KEYSTORE" "$EXPECTED"
if STORE_PASSWORD="$PASSWORD" bash "$ROOT/scripts/verify-beta-keystore.sh" "$KEYSTORE" "$ZERO" 2>"$TMP/mismatch.log"; then
  echo "Mismatched keystore identity was accepted" >&2
  exit 1
fi
if ! grep -Fq "Actual release keystore certificate SHA-256: $EXPECTED" "$TMP/mismatch.log"; then
  echo "Mismatch did not report the public certificate fingerprint" >&2
  exit 1
fi
if STORE_PASSWORD="$PASSWORD" bash "$ROOT/scripts/verify-beta-keystore.sh" "$KEYSTORE" "invalid" >/dev/null 2>&1; then
  echo "Malformed expected fingerprint was accepted" >&2
  exit 1
fi
if STORE_PASSWORD="$PASSWORD" bash "$ROOT/scripts/verify-beta-keystore.sh" "$TMP/missing.p12" "$EXPECTED" >/dev/null 2>&1; then
  echo "Missing keystore was accepted" >&2
  exit 1
fi
if STORE_PASSWORD="wrong-password" bash "$ROOT/scripts/verify-beta-keystore.sh" "$KEYSTORE" "$EXPECTED" >/dev/null 2>&1; then
  echo "Wrong keystore password was accepted" >&2
  exit 1
fi
echo "Release keystore preflight regression checks passed"
