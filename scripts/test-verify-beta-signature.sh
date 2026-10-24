#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
touch "$TMP/sample.apk"
cat > "$TMP/apksigner" <<'MOCK'
#!/usr/bin/env bash
if [[ "${MOCK_SIGNER_FAIL:-0}" == "1" ]]; then exit 1; fi
printf 'Signer #1 certificate SHA-256 digest: %s\n' "$MOCK_DIGEST"
MOCK
chmod +x "$TMP/apksigner"
ZERO="$(printf '%064d' 0)"
ONE="$(printf '1%.0s' {1..64})"
MOCK_DIGEST="$ZERO" bash "$ROOT/scripts/verify-beta-signature.sh" "$TMP/sample.apk" "$ZERO" "$TMP/apksigner"
if MOCK_DIGEST="$ONE" bash "$ROOT/scripts/verify-beta-signature.sh" "$TMP/sample.apk" "$ZERO" "$TMP/apksigner" 2>"$TMP/mismatch.log"; then
  echo "Mismatch was not rejected" >&2
  exit 1
fi
if ! grep -Fq "Actual APK signing certificate SHA-256: $ONE" "$TMP/mismatch.log"; then
  echo "Mismatch diagnostics did not show actual public certificate fingerprint" >&2
  exit 1
fi
if MOCK_DIGEST="$ZERO" bash "$ROOT/scripts/verify-beta-signature.sh" "$TMP/sample.apk" "invalid" "$TMP/apksigner"; then
  echo "Invalid fingerprint was not rejected" >&2
  exit 1
fi
if MOCK_SIGNER_FAIL=1 MOCK_DIGEST="$ZERO" bash "$ROOT/scripts/verify-beta-signature.sh" "$TMP/sample.apk" "$ZERO" "$TMP/apksigner"; then
  echo "Invalid APK signature was not rejected" >&2
  exit 1
fi
echo "Beta signature regression checks passed"
