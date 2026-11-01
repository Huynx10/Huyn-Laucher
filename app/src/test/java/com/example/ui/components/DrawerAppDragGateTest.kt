package com.example.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerAppDragGateTest {
    @Test fun homeDropConsumesOnePhysicalReleaseAndLateSourceEnd() {
        val gate = DrawerAppDragGate()
        gate.begin("app:v2:com.acme/.Editor:10", 4f, 20f)
        assertTrue(gate.recordMove(25f, 20f, 14f))
        // Lazy source disappeared while finger is still down.
        assertTrue(gate.suppressSourceEnd("app:v2:com.acme/.Editor:10", true))
        assertTrue(gate.releaseOnHome())
        assertFalse(gate.releaseOnHome())
        assertTrue(gate.suppressSourceEnd("app:v2:com.acme/.Editor:10", false))
    }

    @Test fun backOrMultiTouchCancellationRejectsLateUp() {
        val gate = DrawerAppDragGate()
        gate.begin("app:v2:com.acme/.Editor:10", 4f, 20f)
        assertTrue(gate.recordMove(25f, 20f, 14f))
        gate.cancel()
        assertFalse(gate.releaseOnHome())
        assertTrue(gate.suppressSourceEnd("app:v2:com.acme/.Editor:10", false))
    }

    @Test fun inDrawerReleaseKeepsExistingContextAndGroupFlow() {
        val gate = DrawerAppDragGate()
        gate.begin("app:v2:com.acme/.Editor:10", 4f, 20f)
        gate.releaseInDrawer()
        assertFalse(gate.suppressSourceEnd("app:v2:com.acme/.Editor:10", false))
        assertFalse(gate.releaseOnHome())
        gate.begin("app:v2:com.acme/.Editor:10", 4f, 20f)
        assertFalse(gate.suppressSourceEnd("app:v2:com.acme/.Other:10", true))
    }

    @Test fun edgeLongHoldWithoutMovementCannotNavigateOrPin() {
        val gate = DrawerAppDragGate()
        gate.begin("app:v2:com.acme/.Editor:10", 4f, 20f) // already in left-edge zone
        assertFalse(gate.recordMove(4f, 20f, 14f))
        assertFalse(gate.recordMove(12f, 20f, 14f))
        assertFalse(drawerHomeEdgeDirection(true, false, true, false, 0.02f) != 0)
        assertFalse(drawerHomeEdgeDirection(true, false, true, false, 0.98f) != 0)
        assertFalse(gate.releaseOnHome())
        assertFalse(gate.suppressSourceEnd("app:v2:com.acme/.Editor:10", false))
        assertTrue(drawerHomeEdgeDirection(true, true, true, false, 0.02f) == -1)
        assertTrue(drawerHomeEdgeDirection(true, true, true, false, 0.98f) == 1)
    }
}
