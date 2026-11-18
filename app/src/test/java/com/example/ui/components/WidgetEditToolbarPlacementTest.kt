package com.example.ui.components

import androidx.compose.ui.unit.dp
import com.example.data.model.WidgetEditToolbarMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetEditToolbarPlacementTest {
    @Test fun floatingToolbarUsesSafeTopAndBottomOrFallsBackToAnchored() {
        assertTrue(canFloatWidgetEditToolbar(WidgetEditToolbarMode.FLOATING, 80.dp, 800.dp))
        assertFalse(canFloatWidgetEditToolbar(WidgetEditToolbarMode.FLOATING, 8.dp, 800.dp))
        assertFalse(canFloatWidgetEditToolbar(WidgetEditToolbarMode.FLOATING, 750.dp, 800.dp))
        assertFalse(canFloatWidgetEditToolbar(WidgetEditToolbarMode.ANCHORED, 80.dp, 800.dp))
        assertFalse(canFloatWidgetEditToolbar(WidgetEditToolbarMode.FLOATING, (-1).dp, 800.dp))
    }

    @Test fun anchoredToolbarClearsPageDotsAndHomeEditBar() {
        assertEquals(56.dp, widgetEditToolbarBottomPadding(false))
        assertEquals(112.dp, widgetEditToolbarBottomPadding(true))
    }
}
