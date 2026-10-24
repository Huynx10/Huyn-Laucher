package com.example.ui.components

/**
 * Native app-level actions shown after releasing a long press in All Apps.
 * Launching is intentionally not a menu action: tapping the app already launches it.
 * Pin-to-page and widget actions are rendered separately from this system action list.
 */
internal enum class AppDrawerSystemAction {
    APP_INFO,
    UNINSTALL
}

internal fun appDrawerSystemActions(isSystemApp: Boolean): List<AppDrawerSystemAction> =
    if (isSystemApp) {
        listOf(AppDrawerSystemAction.APP_INFO)
    } else {
        listOf(AppDrawerSystemAction.APP_INFO, AppDrawerSystemAction.UNINSTALL)
    }
