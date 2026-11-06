package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.model.AppCategory
import com.example.data.model.AppFolder
import com.example.data.model.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerFolderMergePolicyTest {
    private val unique = mapOf("pkg.a" to 1, "pkg.b" to 1, "pkg.work" to 2)
    private fun decide(
        source: String = "pkg.a", target: String = "pkg.b",
        dwellMs: Long = 400L, center: Boolean = true, root: Boolean = true
    ) = DrawerFolderMergePolicy.decide(source, target, unique, center, dwellMs, root)

    @Test fun stationaryCenterDwellCreatesFolderOnlyAfterThreshold() {
        assertEquals(DrawerAppMergeDecision.REORDER, decide(dwellMs = 399))
        assertEquals(DrawerAppMergeDecision.CREATE_FOLDER, decide(dwellMs = 400))
    }

    @Test fun briefTransitAndOuterCellKeepReorder() {
        assertEquals(DrawerAppMergeDecision.REORDER, decide(dwellMs = 0))
        assertEquals(DrawerAppMergeDecision.REORDER, decide(center = false))
        assertEquals(DrawerAppMergeDecision.REORDER, decide(root = false))
    }

    @Test fun ambiguousPackagesAreNeverSilentlyGrouped() {
        assertEquals(DrawerAppMergeDecision.REJECT_AMBIGUOUS, decide(source = "pkg.work"))
        assertEquals(DrawerAppMergeDecision.REJECT_AMBIGUOUS, decide(target = "pkg.work"))
        assertEquals(DrawerAppMergeDecision.REJECT_AMBIGUOUS, decide(source = "pkg.a", target = "pkg.a"))
        assertEquals(DrawerAppMergeDecision.REJECT_AMBIGUOUS, decide(target = "pkg.missing"))
    }

    @Test fun centerRegionIsStrictlyCentralHalf() {
        val rect = Rect(10f, 20f, 110f, 120f)
        assertTrue(DrawerFolderMergePolicy.inMergeCenter(rect, Offset(60f, 70f)))
        assertTrue(DrawerFolderMergePolicy.inMergeCenter(rect, Offset(35f, 45f)))
        assertFalse(DrawerFolderMergePolicy.inMergeCenter(rect, Offset(15f, 70f)))
        assertFalse(DrawerFolderMergePolicy.inMergeCenter(rect, Offset(60f, 115f)))
    }

    @Test fun pairMutationPreservesUnrelatedSettingsAndExistingFolder() {
        val previous = AppFolder(
            id = "old", title = "Old", appPackages = listOf("pkg.a", "pkg.c"),
            category = AppCategory.ALL, orderIndex = 0
        )
        val original = LauncherSettings(
            lockHomeLayout = true,
            appDrawerFolders = listOf(previous),
            appDrawerCustomOrder = listOf("folder:old", "app:pkg.a", "app:pkg.b", "app:pkg.c")
        )
        val updated = createDrawerPairFolder(
            original, "pkg.a", "pkg.b", "app:pkg.a", "app:pkg.b", "pair1"
        )
        assertTrue(updated.lockHomeLayout)
        assertEquals(listOf("pkg.c"), updated.appDrawerFolders.first().appPackages)
        assertEquals(listOf("pkg.b", "pkg.a"), updated.appDrawerFolders.last().appPackages)
        assertEquals(listOf("folder:old", "app:pkg.c", "folder:pair1"), updated.appDrawerCustomOrder)
        assertEquals(listOf("pkg.a", "pkg.c"), original.appDrawerFolders.first().appPackages)
    }

    @Test fun staleRemovedFolderHasNoOrphanedOrderKey() {
        val original = LauncherSettings(
            appDrawerFolders = listOf(AppFolder("old", "Old", listOf("pkg.a"),
                AppCategory.ALL, 0)),
            appDrawerCustomOrder = listOf("folder:old", "app:pkg.b")
        )
        val changed = createDrawerPairFolder(original, "pkg.a", "pkg.b",
            "app:pkg.a", "app:pkg.b", "pair2")
        assertEquals(1, changed.appDrawerFolders.size)
        assertEquals("pair2", changed.appDrawerFolders.single().id)
        assertEquals(listOf("folder:pair2"), changed.appDrawerCustomOrder)
    }

    @Test fun invalidOrDuplicateFolderIdDoesNotMutateSettings() {
        val original = LauncherSettings(appDrawerFolders = listOf(
            AppFolder("pair", "Exist", listOf("pkg.c"), AppCategory.ALL, 0)
        ))
        assertSame(original, createDrawerPairFolder(original, "pkg.a", "pkg.a",
            "app:a", "app:a", "new"))
        assertSame(original, createDrawerPairFolder(original, "pkg.a", "pkg.b",
            "app:a", "app:b", "pair"))
    }
}
