# Default widgets — Galaxy S24 Ultra acceptance

## Behaviour
- Recommend an **installed** native AppWidget provider for factory Clock, Photos, legacy Weather and Notes tiles by matching *both* trusted app package and widget label/class. Samsung Clock/Gallery/Weather/Notes take priority when present; Google provider is fallback.
- On first launch with eligible factory tiles, show **one** clear opt-in dialog: **Kết nối widget** or **Để sau**. No widget is changed unless the user chooses Connect.
- Auto-select the provider; call `bindAppWidgetIdIfAllowed` first and show Android's `ACTION_APPWIDGET_BIND` permission UI if required. Follow the provider's configuration activity if required; respect `configuration_optional|reconfigurable` on Android 12+.
- Cancellation releases the newly allocated widget ID and stops the remaining setup queue. Existing widget IDs, custom tiles, folders and user layouts are never replaced.
- A normal Tile edit sheet opens compactly with a recommended one-tap connection row. Name/colour/manual provider picker is under **Tùy chỉnh thêm**. Native performance/battery tiles identify themselves as system-data tiles instead of advertising unrelated widgets.
- Providers missing on-device are never fabricated. The default gallery app shortcut and real native clock remain usable without matching widget.
- Do not auto-create a misleading weather tile: weather is available from the installed provider via the native widget picker; further tailored opt-in presets can be implemented after device QA.

## Device QA (not yet performed)
- On S24 Ultra Android/One UI: screenshot Home on clean install with Samsung Clock and Gallery present; verify one-time setup offer is shown *after providers and tiles load*.
- Confirm system permission dialog appears only if Android needs it; cancel and verify no widget ID leaks. Confirm no unconsented change when choosing **Để sau**; relaunch and verify no re-prompt.
- Approve on-device and verify true native RemoteViews for Gallery/Clock, both widget interaction and host lifecycle after screen off/reboot.
- For widget with mandatory configuration (e.g. choose album), verify it launches, succeeds and cancels correctly. Optional configuration must not interrupt first-time use.
- Check user-customized/default linked tiles are not overwritten, and restore/Room migration does not recycle old widget IDs.
- Test mini Tile edit sheet, **Tùy chỉnh thêm**, and manual widget selection for non-default tiles.
- CI unit/Robolectric tests and assembleDebug on exact PR SHA must pass. S24 Ultra acceptance is manual and still pending.
