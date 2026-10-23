# Galaxy S24 Ultra — Lumia Launcher acceptance plan

**Status: test plan only.** This is the initial reference phone, not a device allow-list. Keep all layouts responsive in dp so the app can support more models later.

## Manual acceptance on the actual S24 Ultra (not performed yet)
- Record installed One UI/Android version, screen dp, resolution and density; test FHD+ and QHD+, standard and enlarged Display size and font size.
- Test gesture and three-button navigation, insets, dark/light themes, Home default role, lock/unlock, back/Home/recents and rapid app switching.
- Test 4-column and optional 6-column Metro start screens: no clipped tiles, adequate hit targets (aim >=48dp), vertical tile scroll and horizontal Home paging.
- Test native AppWidget picker, bind approval, configure success/cancel, RemoteViews interaction, widget-internal scrolling, tile resize/drag, provider uninstall and recovery.
- Test App Drawer, alphabet jumps, app install/uninstall, smart folders, drag and reorder persistence, page add/remove and reboot.
- Test existing database migration, local backup/restore, cloud failure with an oversized layout, no-Firebase first launch and clear sign-in error state.
- Test TalkBack semantics and focus order, large font, reduce-animations/power-saving mode; ensure touch areas remain usable.

## Profiling targets (NOT measurements)
- Measure cold/warm launcher startup, frame time/jank while scrolling or dragging widgets, widget gesture latency and app PSS memory on S24 Ultra using Android Studio Profiler / Perfetto / Macrobenchmark.
- 120 Hz corresponds to an 8.3 ms refresh interval, 60 Hz to 16.7 ms; do not equate this with a demonstrated continuous 120 fps app.
- Use controlled idle/warm sessions and batterystats/wakeups to compare CPU, background work and battery use against the pre-fix beta.
- Pay attention to repeated HOME events, AppWidgetHost lifecycle, system-provided widget animations and recycled Compose/Android views.

## CI/merge gates
1. Unit/Robolectric + debug APK build pass on the fix branch; check warning and skipped-test details.
2. Validate startup of a CI APK that does not contain google-services.json; Home must work without Firebase.
3. Backups never upload truncated/invalid JSON. Room upgrades preserve user content.
4. S24 Ultra smoke-test evidence is required before labeling the release as daily-driver-ready.
5. Preserve native widget RemoteViews rather than fabricating or reskinning their internals.
6. Do not merge or release without owner approval.
