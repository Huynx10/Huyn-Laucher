# Lumia Launcher — Metro clean / One UI interaction acceptance

Target reference device: **Samsung Galaxy S24 Ultra**, using the Android/One UI build actually installed. This is a **manual QA checklist**, not proof of device testing.

## Visual structure — App Drawer
- List mode defaults to Windows Phone 10-inspired **A–Z logo + app name only**. No launcher-painted square behind a real app icon.
- Header order: title, A–Z shortcut when applicable, list/grid toggle, Search. The next row contains only user-created folders and the new-folder target; there is no Quick Actions entry or automatic category filter.
- Tapping Search must try Samsung Finder immediately through a public exported search or launcher activity. If unavailable, show the launch failure and open local app search. Enter/Done in local search must keep the query in the launcher. Confirm Finder actually opens on the S24 Ultra; Android public APIs and the package probe alone do not prove OEM behavior. Grid/list switching must not discard folders, pin state or custom order.
- All Apps draws its background edge to edge; app rows remain outside status, navigation and keyboard insets. No fixed oversized footer obscures the last app.
- Large font and display scaling on S24 Ultra: ensure header does not wrap or clip in portrait with 4/6 tile columns. Accessibility labels and 44+ dp touch targets must be verified; target 48dp where practicable.
- List and grid use the same long-press/release menu for an app. Drag into a user-created folder to group, drag a folder to its delete target to dissolve it, and keep every installed app visible in **Tất cả**. Automatic category grouping is not offered.

## Home behaviour
- Default layout is **free placement**; gaps survive moving, adding or deleting existing tiles.
- Opt-in **Tự dồn ô** compacts gaps; turning it on once repacks, and later tile removal/add/drag maintains the arrangement.
- Drag tile over an occupied region must not discard either tile. Widget ID and provider are unchanged by coordinate-only compacting.
- No forced compact during upgrades or backup restores when free placement remains selected. Test long-press/edit, resize, multi-home, horizontal page gesture and scrollable third-party widgets.
- Lock layout prevents accidental tile moves and deletions.
- Edit mode uses an X on each selected Tile to remove it from the page; resize X and Y independently within widget provider limits. An app Tile can join a manual Home folder by intentional drop on another app/folder Tile.
- **Manual one-time sort + Undo** is a future improvement; not claimed as implemented in this PR.

## Wallpaper and source authenticity
- New installs default to using the real Android Home system wallpaper via FLAG_SHOW_WALLPAPER; test Samsung static and live wallpapers, light/dark contrast, edge-to-edge insets and gesture navigation.
- Selecting a custom image sets CUSTOM; selecting an internal Lumia gradient sets LUMIA; selecting **Theo hệ thống** restores SYSTEM without copying protected wallpaper bitmaps.
- Existing chosen custom/gradient wallpaper is preserved when migrating user settings.
- Automatic accent attempts to use Android 12+ system accent when wallpaper source is SYSTEM.
- On new installs don't seed synthetic weather/news/notes content; add real AppWidget providers using the Android widget picker. Old user tiles remain untouched to preserve data.
- Glance displays clock and actual battery metrics, no hard-coded notification counts. Launcher choices belong in Settings; Wi-Fi and Bluetooth stay in Android's system quick settings.

## UX writing — 3-pass code review
1. **Meaning:** Labels match actual actions. Never say “Đóng app ngầm” for clearing launcher cache; use “Xóa bộ nhớ đệm”.
2. **Natural Vietnamese:** Prefer “Hình nền”, “Màn hình chính”, “Lọc theo nhóm”, “Tìm ứng dụng”, “Tự dồn ô”; avoid technical/marketing filler.
3. **Consistency:** Keep “Khôi phục” distinct from “Đặt lại bố cục”; use sentence case and short subtitles. Confirm destructive actions before shipping.

## Release gates
- GitHub CI :app:testDebugUnitTest and :app:assembleDebug green on exact head SHA. Explicitly disclose skipped Firebase Emulator tests and any warning.
- Install CI APK on S24 Ultra; verify live wallpaper, widget interaction, Home paging, image source persistence, onboarding, folder drag/drop, 120/60Hz frame-time tracing and TalkBack.
- Inspect CI logs and run Android Studio profiler/Perfetto before claiming smooth 120Hz or battery improvement.
- Do not merge or ship stable solely because a debug build succeeded. A trial APK may be prepared for later device acceptance when physical testing is paused by the owner; mark Finder, gestures, accessibility and performance as unverified.
