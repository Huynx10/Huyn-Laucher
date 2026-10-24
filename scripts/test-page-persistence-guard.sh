#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
file=app/src/main/java/com/example/ui/viewmodel/LauncherViewModel.kt
grep -Fq 'private val pendingPages = Channel<List<LauncherPage>>(Channel.CONFLATED)' "$file"
grep -Fq 'for (snapshot in pendingPages) {' "$file"
grep -Fq 'id = "page_home_${UUID.randomUUID()}"' "$file"
test "$(grep -c 'repository.savePages(' "$file")" -eq 1
grep -Fq 'private fun orderedPages(pages: List<LauncherPage>)' "$file"
grep -Fq 'if (current[fromIndex].pageType != PageType.HOME) return' "$file"
grep -Fq 'persistPages(updated)' "$file"
grep -Fq 'persistPages(remaining)' "$file"
echo "Page persistence regression guards passed"
