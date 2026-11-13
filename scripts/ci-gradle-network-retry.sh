#!/usr/bin/env bash
# Deterministic, bounded Gradle network retry. Never retry Kotlin/Android test failures.
set -uo pipefail
if (( $# == 0 )); then
  echo "Usage: ci-gradle-network-retry.sh command [args...]" >&2
  exit 64
fi
max_attempts="${HUYN_CI_NETWORK_MAX_ATTEMPTS:-2}"
delay="${HUYN_CI_NETWORK_RETRY_DELAY_SEC:-6}"
case "$max_attempts" in 1|2) ;; *) echo "Expected max attempts 1 or 2" >&2; exit 64 ;; esac
case "$delay" in ''|*[!0-9]*) echo "Invalid retry delay" >&2; exit 64 ;; esac
attempt=1
while :; do
  log_file="$(mktemp)"
  echo "::notice::Gradle command attempt ${attempt}/${max_attempts}"
  "$@" 2>&1 | tee "$log_file"
  status=${PIPESTATUS[0]}
  if (( status == 0 )); then
    rm -f "$log_file"
    exit 0
  fi
  if (( attempt >= max_attempts )) || ! grep -Eiq \
    '(HTTP[^[:cntrl:]]*(429|502|503|504)|Received status code (429|502|503|504)|504 Gateway|Gateway Time-out|Connection reset by peer|Read timed out|Temporary failure in name resolution|Could not resolve host)' \
    "$log_file"; then
    rm -f "$log_file"
    exit "$status"
  fi
  rm -f "$log_file"
  echo "::warning::Dependency/network error detected; retrying once without weakening checks." >&2
  attempt=$((attempt + 1))
  sleep "$delay"
done
