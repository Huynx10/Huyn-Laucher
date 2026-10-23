# Product and engineering baseline

## Decision
Native-first · Minimalist · Performance-first, with Metro identity and continuous improvement.

## Scope
This launcher owns Home layout, app discovery/launch, Metro Tiles, All Apps A–Z and widget hosting. Prefer installed apps and Android system flows for calls, messages, browser, calendar, permissions and widget configuration. Samsung providers may be unavailable to third-party launchers; show an honest unavailable/rebind state. Do not rely on One UI private APIs.

New folders must contain selected items or matching installed applications, not sample apps or fabricated live content. Previously saved content is not automatically deleted. Folder previews may show fewer items than the folder actually contains; unpacking must preserve every saved item. Widget-linked tiles remain outside app folders.

Keep Home titles and permanent editing toolbars hidden, enter edit mode by long press, and use a compact page indicator. Keep settings limited to useful choices. Avoid adding services, timers, permissions or animations without a demonstrated need.

## Release acceptance
1. Review changed data, lifecycle, permissions, error and accessibility paths.
2. Run relevant regression tests and build the exact commit; record CI URL and source SHA.
3. Merge only after successful checks and source review.
4. Release through the persistent-signing workflow; verify APK signature and checksum. A CI debug artifact is not a compatible signed beta release.
5. Report fixed issues, remaining limitations, APK link if actually published, and device QA status.
6. Never uninstall a user's launcher to bypass signature incompatibility.

## Performance protocol
Measurements are required before claims of smoothness or battery savings. This protocol introduces no production background telemetry.

Compare a baseline APK and candidate on the same physical device, Android/One UI version, refresh rate, resolution, brightness, thermal state, power mode, installed apps and layout. Record APK SHA/version and test conditions. Measure both without widgets and with a fixed set of real provider widgets, logging provider versions and update cadence.

Scenarios: cold/warm launcher start; Home paging; All Apps scroll/search; folder expand/unpack; enter edit and resize; app launch; screen-on idle; screen-off idle. Use repeated runs and report sample count and median/tail distributions.

| Metric | Evidence | Reporting |
| --- | --- | --- |
| Frame time / jank | Perfetto FrameTimeline or Android frame metrics | p50/p95/p99, missed deadlines, actual refresh rate |
| RAM | adb shell dumpsys meminfo for launcher | PSS after warm-up and after repeated navigation; provider processes separately |
| CPU | Perfetto scheduler/process traces | launcher and widget-provider CPU time separately |
| Battery | Controlled equal-duration runs with batterystats/Battery Historian or power instrumentation | elapsed time, device conditions, wakeups; short runs are not proof of savings |
| App launch | ActivityTaskManager timing and trace to first app frame | cold/warm separately, target app, sample count; do not attribute target-app work to launcher |

Do not set arbitrary universal pass thresholds. Establish a device baseline first; flag repeatable regressions beyond observed run variability and investigate before release. Accessibility checks include font scaling, TalkBack labels, contrast, touch targets and system gesture insets.

## Evaluation status
Use PASS / FAIL / NOT VERIFIED for data safety, native integration, Metro/minimalism, performance and physical-device QA. Overall acceptance remains pending if required evidence is missing. Do not turn CI success into a performance score.

## Current implementation increment
- Protect widget-linked tiles in all folder absorption paths.
- Unpack every saved folder item.
- Reserve existing tile positions when migrating missing coordinates.
- Stop generating sample folder entries.
- Add regression coverage for these behaviors.

Physical S24 Ultra performance and widget-provider compatibility remain unmeasured by this increment.
