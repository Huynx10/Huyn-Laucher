package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossPageDropGuardTest {
    @Test fun failedDropRestoresOnlyCurrentCompletedSessionOnOriginalTarget() {
        assertTrue(canRestoreFailedCrossPageDrop(5, 5, null, "home-2", "home-2"))
    }

    @Test fun oldFailedCallbackCannotScrollNewerActiveDrag() {
        assertFalse(canRestoreFailedCrossPageDrop(5, 6, "new-tile", "home-2", "home-2"))
    }

    @Test fun oldFailedCallbackCannotScrollNewerFinishedDrag() {
        assertFalse(canRestoreFailedCrossPageDrop(5, 6, null, "home-2", "home-2"))
    }

    @Test fun anotherActiveDragNeverAllowsFailedDropRestore() {
        assertFalse(canRestoreFailedCrossPageDrop(5, 5, "other-tile", "home-2", "home-2"))
    }

    @Test fun pageNavigatedAwayAfterDropDoesNotGetOverridden() {
        assertFalse(canRestoreFailedCrossPageDrop(5, 5, null, "home-2", "home-3"))
        assertFalse(canRestoreFailedCrossPageDrop(5, 5, null, "home-2", null))
    }
}
