#!/usr/bin/env bash
# Pure release eligibility check: "stale" is an expected no-release outcome.
set -euo pipefail
if [ "$#" -ne 3 ]; then
  echo "usage: beta-release-source-status.sh SOURCE_SHA CHECKOUT_SHA MAIN_SHA" >&2
  exit 2
fi
SOURCE_SHA="$1"
CHECKOUT_SHA="$2"
MAIN_SHA="$3"
for sha in "$SOURCE_SHA" "$CHECKOUT_SHA" "$MAIN_SHA"; do
  if ! [[ "$sha" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Invalid or missing commit SHA" >&2
    exit 2
  fi
done
if [ "$CHECKOUT_SHA" != "$SOURCE_SHA" ]; then
  echo "Checked-out commit does not match CI-tested SHA" >&2
  exit 1
fi
if [ "$MAIN_SHA" != "$SOURCE_SHA" ]; then
  printf 'stale\n'
else
  printf 'ready\n'
fi
