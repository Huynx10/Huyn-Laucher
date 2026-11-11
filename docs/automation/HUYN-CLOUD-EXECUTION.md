# Huyn Launcher · Cloud execution contract (MVP)

## What is automatic now
1. A single authorized Chat Cloud Dev writer edits the feature branch when explicitly invoked. There is **no continuously running AI coding agent**.
2. `push` / `pull_request` automatically start Android CI (`build-and-test`), preserving the branch-protection check and exact SHA attribution. The PR checkout can be a synthetic merge, so read **source SHA and checkout SHA** separately.
3. CI independently runs signing regressions, Android lint, Unit/Robolectric and internal debug build. Lint retries **at most once**, and only when its output contains recognized dependency/network failures (HTTP 429/502/503/504, timeouts or DNS errors). Compile/test failures are **never silently retried**, and no failing check is downgraded.
4. An `always()` step writes a read-only GitHub Actions Job Summary containing event, SHA, attempt and individual step outcomes. It does not alter the required `build-and-test` conclusion.
5. The existing single hourly ChatGPT PM schedule reads GitHub and reports **only verified deltas or new blockers**. It is not an autonomous worker or a source-code writer.

## Task lifecycle and gates
`ISSUE/AC -> CHAT_DEV (single writer) -> source PUSH CI -> PR CI -> code/UX/security review -> OWNER merge authorization -> MAIN CI -> OWNER release authorization`

- Review covers input ownership, corner gestures, fixed-provider constraints, native-widget touch guard, Room transaction, widget IDs, backup/restore, setting defaults, layout and accessibility. Record evidence and the source SHA.
- **Device UAT is explicitly outside this process**. The owner tests independently and files follow-up bugs if needed; UAT is not a CI, PR, or release prerequisite.
- No auto-merge, signed preview/release, Drive upload, email, duplicate ChatGPT tasks, self-hosted iMac runner, or separately billed OpenAI API.
- `main` uses strict PR `build-and-test` ruleset #26689260, GitHub Actions app ID 15368. The signed-beta workflow could publish on a successful main push when `BETA_RELEASE_ENABLED=true`; verify/disable this explicitly before authorized merge if release is not intended.
- Current hosted worker files are scoped to Finder/PR #41, not F3. They do not create an unattended ChatGPT Dev runner.

## Future autonomous coding (not yet activated)
An unattended Dev executor requires an approved, funded and authenticated code-capable runtime. Before enabling it, prove: immutable task+allowed paths, single-writer durable lease, trusted event validation, bounded retry, real commit/push + exact-SHA CI, independent review and owner-controlled merge/release. The GitHub Actions policy and PM schedule cannot provide AI coding on their own.
