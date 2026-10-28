package com.example

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutBoundsTest {
    @Test
    fun springOvershootCannotReachComposePaddingAsNegativeDp() {
        assertEquals(0.dp, nonNegativeLayoutDp((-0.5f).dp))
        assertEquals(0.dp, nonNegativeLayoutDp(Dp.Unspecified))
        assertEquals(0.dp, nonNegativeLayoutDp(0.dp))
        assertEquals(28.dp, nonNegativeLayoutDp(28.dp))
    }
}
