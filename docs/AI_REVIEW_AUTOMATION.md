# Automated Lumia Launcher review

This optional GitHub Actions workflow reviews source changes pushed to `main` with the Gemini API and creates a GitHub Issue containing a prioritized review and copy-ready implementation prompt.

## Enable
1. Review and merge the setup PR.
2. In **Settings → Secrets and variables → Actions**, add repository secret `GEMINI_API_KEY` obtained from Google AI Studio's API key area. **Never paste a key into source code or chat.**
3. Push a Kotlin/XML source change to `main`, or run **Actions → AI Code Review (Widget-first) → Run workflow**. A manual run reviews the most recent commit only, and only when Android source files changed.
4. Read the resulting GitHub Issue. Send its implementation prompt to Google AI Studio manually, or configure a separate, officially supported coding agent for GitHub Issues/PRs.

## Security and boundaries
- The automation sends code diffs to Google's Gemini API. Review organizational data policy before enabling.
- It creates issues only. It does **not** type into AI Studio, modify source code, push fixes or auto-merge.
- Configure `GEMINI_API_KEY` and review API billing/quotas.
- Uses the previous commit as the diff baseline; merge commits and very large diffs may need manual review.
- Avoids reviewing its own workflow-only changes, skips GitHub Actions bot pushes and deduplicates issues by commit SHA (last 100 issues).
- Use PRs, CI and manual review before merging AI-generated fixes.
