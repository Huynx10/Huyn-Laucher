#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
file=app/src/main/java/com/example/ui/components/SettingsSheet.kt
grep -Fq 'styleOverride: Wp10TileStyle = wp10TileStyle' "$file"
grep -Fq 'opacityOverride: Float = tileOpacity' "$file"
grep -Fq 'tileOpacity = opacityOverride.coerceIn(0f, 1f)' "$file"
grep -Fq 'wp10TileStyle = styleOverride' "$file"
grep -Fq 'wp10TileStyle = style,' "$file"
grep -Fq 'wp10TileStyle = nextStyle,' "$file"
grep -Fq 'tileOpacity = nextOpacity' "$file"
echo "Tile style atomic-save regression guard passed"
