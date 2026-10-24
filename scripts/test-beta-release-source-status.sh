#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
A="$(printf 'a%.0s' {1..40})"
B="$(printf 'b%.0s' {1..40})"
test "$(bash scripts/beta-release-source-status.sh "$A" "$A" "$A")" = ready
test "$(bash scripts/beta-release-source-status.sh "$A" "$A" "$B")" = stale
if bash scripts/beta-release-source-status.sh "$A" "$B" "$A" >/dev/null 2>&1; then
  echo "Expected checkout mismatch to fail" >&2
  exit 1
fi
if bash scripts/beta-release-source-status.sh "$A" "$A" "" >/dev/null 2>&1; then
  echo "Expected missing main SHA to fail" >&2
  exit 1
fi
if bash scripts/beta-release-source-status.sh "$A" "$A" "invalid" >/dev/null 2>&1; then
  echo "Expected malformed main SHA to fail" >&2
  exit 1
fi
echo "beta release source status checks passed"
