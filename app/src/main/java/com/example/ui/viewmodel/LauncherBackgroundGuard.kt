package com.example.ui.viewmodel

import kotlinx.coroutines.CancellationException

/**
 * Isolates a non-critical launcher background read from other startup/refresh work.
 *
 * The caller publishes the result only on success, retaining its last known state on failure.
 * Cancellation must propagate so obsolete scans stop when a new refresh or ViewModel cleanup runs.
 */
internal suspend fun <T : Any> runLauncherBackgroundRead(
    onError: (Exception) -> Unit,
    read: suspend () -> T
): T? = try {
    read()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    onError(error)
    null
}
