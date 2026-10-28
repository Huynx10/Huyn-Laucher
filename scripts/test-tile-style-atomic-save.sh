#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# The UI must delegate appearance changes instead of saving a full Compose snapshot.
sheet=app/src/main/java/com/example/ui/components/SettingsSheet.kt
clean=app/src/main/java/com/example/ui/components/CleanSettingsScreen.kt
action=app/src/main/java/com/example/ui/components/ActionCenterOverlay.kt
home=app/src/main/java/com/example/MainActivity.kt
vm=app/src/main/java/com/example/ui/viewmodel/LauncherViewModel.kt
test=app/src/test/java/com/example/ui/viewmodel/TileAppearanceStateTest.kt

grep -Fq 'onUpdateTileAppearance(style, nextOpacity)' "$sheet"
grep -Fq 'onUpdateTileAppearance(nextStyle, nextOpacity)' "$sheet"
grep -Fq 'onUpdateTileAppearance(style, opacity)' "$clean"
grep -Fq 'onClick = onCycleTileAppearance' "$action"
if grep -Fq 'onUpdateSettings(settings.copy(' "$action"; then
  echo 'Quick Actions must not save appearance from a stale settings snapshot' >&2
  exit 1
fi
grep -Fq 'viewModel.cycleTileAppearance(' "$home"
grep -Fq 'viewModel.updateTileAppearance(style, opacity)' "$home"
grep -Fq 'viewModel.updateAdvancedSettings(baseline, edited)' "$home"
grep -Fq 'onSaveSettings(settings, settings.copy(' "$sheet"
grep -Fq 'latest.mergeAdvancedEdits(baseline, edited)' "$vm"
if grep -Fq 'viewModel.updateSettingsPreservingAppearance(it)' "$home"; then
  echo 'Advanced Settings must not pass a full Compose snapshot' >&2
  exit 1
fi

# ViewModel applies only style+opacity to the latest StateFlow value atomically.
grep -Fq 'updateAndGet { current ->' "$vm"
grep -Fq 'current.copy(wp10TileStyle = style, tileOpacity = opacity.coerceIn(0f, 1f))' "$vm"
grep -Fq 'pendingSettings.trySend(candidate)' "$vm"
grep -Fq 'newSettings.withAppearanceFrom(latest)' "$vm"
grep -Fq 'unrelatedSettingChangeThenTransparentKeepsLatestLayoutAndWidgetPreferences' "$test"
grep -Fq 'advancedSettingAfterTransparentPreservesLatestAppearance' "$test"
echo "Tile appearance latest-state regression guards passed"
