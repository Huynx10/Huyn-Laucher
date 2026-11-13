#!/usr/bin/env bash
# Fast hermetic test: transient network errors retry, source errors do not.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
retry="$root/scripts/ci-gradle-network-retry.sh"
scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
cat > "$scratch/fixture.sh" <<'FIXTURE'
#!/usr/bin/env bash
set -euo pipefail
n=0
if [[ -f "$COUNT_FILE" ]]; then n="$(cat "$COUNT_FILE")"; fi
n=$((n + 1))
printf '%s\n' "$n" > "$COUNT_FILE"
case "$MODE" in
  transient) if (( n == 1 )); then echo 'Could not GET dependency: Received status code 504'; exit 23; fi ;;
  source) echo 'e: Unresolved reference: sourceBug'; exit 22 ;;
  outage) echo 'HTTP 503 Service Unavailable'; exit 24 ;;
  *) exit 50 ;;
esac
echo 'Fixture command succeeded'
FIXTURE
chmod +x "$scratch/fixture.sh"
export COUNT_FILE="$scratch/count.txt"
export HUYN_CI_NETWORK_MAX_ATTEMPTS=2
export HUYN_CI_NETWORK_RETRY_DELAY_SEC=0
export MODE=transient
bash "$retry" bash "$scratch/fixture.sh" > /dev/null 2>&1
[[ "$(cat "$COUNT_FILE")" == 2 ]] || { echo 'Expected one network retry' >&2; exit 1; }
rm -f "$COUNT_FILE"
export MODE=source
set +e
bash "$retry" bash "$scratch/fixture.sh" > /dev/null 2>&1
code=$?
set -e
[[ "$code" == 22 && "$(cat "$COUNT_FILE")" == 1 ]] || { echo 'Source failures must not retry' >&2; exit 1; }
rm -f "$COUNT_FILE"
export MODE=outage
set +e
bash "$retry" bash "$scratch/fixture.sh" > /dev/null 2>&1
code=$?
set -e
[[ "$code" == 24 && "$(cat "$COUNT_FILE")" == 2 ]] || { echo 'Network failure must stop after two tries' >&2; exit 1; }
echo 'PASS: bounded retry only for known network failures'
