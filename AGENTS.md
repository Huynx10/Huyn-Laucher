# Lumia Launcher engineering principles

Native-first, Minimalist, Performance-first are standing product constraints.

- Prefer Android APIs, user-selected system defaults, and capabilities of installed apps. One UI private APIs are not a supported dependency.
- Host real third-party widgets through AppWidgetHost/AppWidgetManager and provider RemoteViews only when the provider permits it. Preserve binding, permission, configuration, cancellation and rebind flows. Never absorb a widget-linked tile into an app folder.
- Preserve Metro Tiles, feasible Live Tiles and All Apps A–Z. Do not implement a replacement operating system.
- Avoid fabricated app entries/live data, duplicate features, unnecessary settings, animations and background services. Preserve user layouts; do not silently remove existing saved content.
- Performance claims require measurements. System widgets are not assumed cheaper: include provider update frequency and rendering cost.
- Follow review → fix → tests → merge main → build/release → report links and evidence. Respect existing persistent signing gates; do not bypass them with ephemeral release keys.
- Separate unit/Robolectric results, emulator measurements and physical S24 Ultra QA. Unknown measurements stay “not measured”. Never fabricate a score.
- Before shipping layout changes, test data preservation (including large folders, native widget bindings, empty layouts and upgrades).
- Read docs/PRODUCT_PRINCIPLES.md for acceptance and measurement procedures.
