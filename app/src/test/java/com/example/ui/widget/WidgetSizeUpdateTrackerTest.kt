package com.example.ui.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizeUpdateTrackerTest {
    @Test fun failedPublishIsRetriedRatherThanCached() {
        val tracker = WidgetSizeUpdateTracker()
        assertTrue(tracker.needsUpdate(17, 120, 160))
        // AppWidgetManager.updateAppWidgetOptions threw: markUpdated was never called.
        assertTrue(tracker.needsUpdate(17, 120, 160))
        tracker.markUpdated(17, 120, 160)
        assertFalse(tracker.needsUpdate(17, 120, 160))
    }

    @Test fun smallLayoutJitterIsIgnoredButRealResizeIsPublished() {
        val tracker = WidgetSizeUpdateTracker()
        tracker.markUpdated(17, 120, 160)
        assertFalse(tracker.needsUpdate(17, 123, 157))
        assertTrue(tracker.needsUpdate(17, 124, 160))
        assertTrue(tracker.needsUpdate(17, 120, 156))
    }

    @Test fun releasedWidgetIdCanBeUsedAgainWithoutStaleSize() {
        val tracker = WidgetSizeUpdateTracker()
        tracker.markUpdated(17, 120, 160)
        tracker.forget(17)
        assertTrue(tracker.needsUpdate(17, 120, 160))
        assertFalse(tracker.needsUpdate(-1, 120, 160))
    }
}
