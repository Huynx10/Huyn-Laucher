#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
touch "$TMP/old.apk" "$TMP/new.apk"
cat > "$TMP/apksigner" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
[ "$1" = verify ] && [ "$2" = --print-certs ]
if [[ "$3" == *old.apk ]]; then
  CERT="$MOCK_OLD_CERT"
else
  [ "$MOCK_BAD_SIGNATURE" = no ] || exit 1
  CERT="$MOCK_NEW_CERT"
fi
printf 'Signer #1 certificate SHA-256 digest: %s\n' "$CERT"
MOCK
cat > "$TMP/aapt" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
[ "$1" = dump ] && [ "$2" = badging ]
if [[ "$3" == *old.apk ]]; then
  CODE="$MOCK_OLD_CODE"
  PACKAGE="$MOCK_OLD_PACKAGE"
else
  CODE="$MOCK_NEW_CODE"
  PACKAGE="$MOCK_NEW_PACKAGE"
fi
printf "package: name='%s' versionCode='%s' versionName='1.0'\n" "$PACKAGE" "$CODE"
MOCK
chmod +x "$TMP/apksigner" "$TMP/aapt"
CERT="$(printf 'a%.0s' {1..64})"
export MOCK_OLD_CERT="$CERT" MOCK_NEW_CERT="$CERT"
export MOCK_OLD_CODE=100 MOCK_NEW_CODE=101
export MOCK_OLD_PACKAGE=com.aistudio.lumialauncher.wp10m
export MOCK_NEW_PACKAGE=com.aistudio.lumialauncher.wp10m
export MOCK_BAD_SIGNATURE=no
CHECK="$ROOT/scripts/verify-beta-upgrade-lineage.sh"
set -- "$TMP/old.apk" "$TMP/new.apk" "$CERT" "$TMP/apksigner" "$TMP/aapt"
bash "$CHECK" "$@" >/dev/null
expect_fail() {
  if "$@" >/dev/null 2>&1; then
    echo "Expected beta upgrade lineage validation failure" >&2
    exit 1
  fi
}
expect_fail env MOCK_NEW_CODE=100 bash "$CHECK" "$@"
expect_fail env MOCK_NEW_CODE=99 bash "$CHECK" "$@"
expect_fail env MOCK_NEW_PACKAGE=com.other.launcher bash "$CHECK" "$@"
expect_fail env MOCK_OLD_CERT="$(printf 'b%.0s' {1..64})" bash "$CHECK" "$@"
expect_fail env MOCK_BAD_SIGNATURE=yes bash "$CHECK" "$@"
expect_fail env MOCK_NEW_CODE=broken bash "$CHECK" "$@"
echo "Beta upgrade lineage regression tests passed"
