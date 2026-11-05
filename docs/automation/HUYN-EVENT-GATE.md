# Huyn Launcher — event-driven F1 gate (staged)

**Status: Draft / not activated on main.** Source branch: `ci/huyn-event-orchestrator-20261105`. This is a separate CI/automation implementation, not a patch for Finder, Settings or drag.

## Architecture and least-privilege boundary

1. Trusted GitHub Actions event is received by `huyn-event-gate.yml`. **Only the GitHub-hosted job** has read access to the repository and Pull Request API. Never checkout the changed PR head.
2. `policy.mjs` allows three production event cases:
   - `workflow_run` completed with **success** for the canonical **Lumia Launcher Android CI**, originating inside the same repository and linked to PR **#41**.
   - Owner `Huynx10` posts exactly `/huyn f1-readonly` as a newly created comment on issue **#29** (no PR comment).
   - Owner manually runs the allowed `f1-readonly` choice.
3. Before any local dispatch, the hosted job retrieves **live PR #41** and compares exact `head.sha` against a CI event SHA. A stale/missing PR or unauthorized source **fails closed**.
4. The hosted job writes a bounded JSON audit envelope and GitHub job summary. A **read-only** Codex smoke on self-hosted runner `[self-hosted, macOS, ARM64, huyn-codex]` is eligible **only after the live gate passes**.
5. The macOS step has `permissions: {}`, no checkout, no GitHub API token, no external writes, no arbitrary text forwarded from issue comments. The Codex prompt is fixed and checks for a fixed marker; its success is **not** Finder QA or code delivery.

**Important:** `workflow_run`, `issue_comment`, `workflow_dispatch` GitHub Actions triggers require the workflow file to be on the default branch. On this staging branch, a limited `push` event runs policy tests/audit on GitHub-hosted runners only. It is explicitly marked `PILOT_ONLY`; local runner job must skip.

## Tests and evidence

`node --test tools/huyn-event-gate/*.test.mjs` uses Node.js built-in test runner. Covered: red/cancelled CI, PR #39/#40/#42, stale CI vs live HEAD, fork/cross-repo, unauthorized user, malicious suffix, issue comments, default branch restriction, and no silent writer approval.

- [Initial CI run #41996648343](https://github.com/Huynx10/Huyn-Laucher/actions/runs/41996648343) at `153f43363626ea9017000eda682075dbf365c759`: 12/12 PASS; pilot authorization passed, **local job skipped**; audit artifact ID `13399299894`.
- Always verify **latest HEAD and CI exact SHA** before relying on any result.

## Activation gate before real event-driven operation

- Independent QA/Security reviews workflow injection, permissions, untrusted PR code, and persistent macOS credentials.
- Explicit owner approval needed to merge/promote the reviewed workflow onto `main`; **do not auto-merge**.
- Trigger a bounded owner `/huyn f1-readonly` event on issue #29 **only after** the workflow is live. Capture event ID, exact PR HEAD, hosted verification, self-hosted job ID and validated Codex response.
- Audit any missing `workflow_run.pull_requests` linkage or synthetic merge SHA mismatch as `UNKNOWN/BLOCKED`, never fabricate an eligible CI event. Adjust policy only after reviewing actual event payload schema and re-testing.
- Then integrate actual code-review and QA handoff with Graphify at its indexed SHA plus live diff, permission-based one-writer leases and independent QA. Do **not** treat the fixed Codex marker as a code patch.
- Preserve the single existing Bé Tổng hourly watchdog; no additional ChatGPT schedules.

## Security/operational constraints

The existing `Huyn` runner resides on the owner's macOS account and can access that account's files: do not run untrusted source, checkout PRs, or use write-capable GitHub tokens in its job. A separate least-privilege account or isolated runner should be adopted before code-writing delegation. `main`, APK preview, PR #39–#42, signing and release workflows remain unchanged by this staged branch.

This gate deliberately has **no cross-chat wake-up API** and cannot directly notify old ChatGPT Project conversations. GitHub run artifacts and issue/PR state are the auditable interface for coordinator and QA.

## Review optimization (05/11/2026)

Hosted checkout is pinned to `github.sha` instead of a moving branch name. For production events this is the event's trusted default-branch commit; pilot pushes use their exact source commit. The local stage checks Codex binary/login readiness only, without a paid model invocation merely to echo a marker. This does not exercise code generation. Existing historical model-probe evidence remains separate. Durable cross-run deduplication, worker execution and independent security review remain activation gates; concurrency only serializes matching events and does not provide exactly-once execution.
