#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
touch "$TMP/sample.apk"
cat > "$TMP/apksigner" <<'MOCK'
#!/usr/bin/env bash
if [[ "${MOCK_SIGNER_FAIL:-0}" == "1" ]]; then exit 1; fi
if [[ "${MOCK_ONLY_PUBLIC_KEY:-0}" == "1" ]]; then
  printf 'Signer #1 public key SHA-256 digest: %s\n' "$MOCK_DIGEST"
else
  printf '%s certificate SHA-256 digest: %s\n' "${MOCK_PREFIX:-Signer #1}" "$MOCK_DIGEST"
fi
if [[ -n "${MOCK_EXTRA_DIGEST:-}" ]]; then
  printf 'V3.0 Signer: certificate SHA-256 digest: %s\n' "$MOCK_EXTRA_DIGEST"
fi
MOCK
chmod +x "$TMP/apksigner"
ZERO="$(printf '%064d' 0)"
ONE="$(printf '1%.0s' {1..64})"
MOCK_DIGEST="$ZERO" bash "$ROOT/scripts/verify-beta-signature.sh" "$TMP/sample.apk" "$ZERO" "$TMP/apksigner"
for prefix in "V2 Signer:" "V3.0 Signer:" "V3.1 Signer:"; do
  MOCK_PREFIX="$prefix" MOCK_DIGEST="$ZERO" bash "$ROOT/scripts/verify-beta-signature.sh" "$TMP/sample.apk" "$ZERO" "$TMP/apksigner"
done
if MOCK_DIGEST="$ZERO" MOCK_EXTRA_DIGEST="$ONE" bash "$ROOT/scripts/verify-beta-signature.sh" "$TMP/sample.apk" "$ZERO" "$TMP/apksigner" >/dev/null 2>&1; then
  echo "Distinct multiple signing certificates were accepted" >&2
  exit 1
fi
if MOCK_ONLY_PUBLIC_KEY=1 MOCK_DIGEST="$ZERO" bash "$ROOT/scripts/verify-beta-signature.sh" "$TMP/sample.apk" "$ZERO" "$TMP/apksigner" >/dev/null 2>&1; then
  echo "Public key digest was incorrectly treated as certificate digest" >&2
  exit 1
fi
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
