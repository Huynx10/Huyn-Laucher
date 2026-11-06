package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.model.AppCategory
import com.example.data.model.AppFolder
import com.example.data.model.LauncherSettings

/** Legacy app-folder storage is package-only. Never merge ambiguous activity/profile entries. */
internal enum class DrawerAppMergeDecision { REORDER, CREATE_FOLDER, REJECT_AMBIGUOUS }

internal object DrawerFolderMergePolicy {
    const val DWELL_MS = 400L

    fun inMergeCenter(bounds: Rect, finger: Offset): Boolean =
        bounds.width > 0f && bounds.height > 0f &&
            finger.x >= bounds.left + bounds.width * 0.25f &&
            finger.x <= bounds.right - bounds.width * 0.25f &&
            finger.y >= bounds.top + bounds.height * 0.25f &&
            finger.y <= bounds.bottom - bounds.height * 0.25f

    fun decide(
        sourcePackage: String,
        targetPackage: String,
        packageCounts: Map<String, Int>,
        inCenter: Boolean,
        dwellMs: Long,
        isRootGrid: Boolean
    ): DrawerAppMergeDecision {
        if (!isRootGrid || !inCenter || dwellMs < DWELL_MS) return DrawerAppMergeDecision.REORDER
        if (sourcePackage == targetPackage ||
            packageCounts[sourcePackage] != 1 || packageCounts[targetPackage] != 1
        ) return DrawerAppMergeDecision.REJECT_AMBIGUOUS
        return DrawerAppMergeDecision.CREATE_FOLDER
    }
}

/** Atomic settings mutation: never drop unrelated fields or leave orphaned folder-order anchors. */
internal fun createDrawerPairFolder(
    latest: LauncherSettings,
    sourcePackage: String,
    targetPackage: String,
    sourceKey: String,
    targetKey: String,
    newFolderId: String
): LauncherSettings {
    if (sourcePackage.isBlank() || targetPackage.isBlank() ||
        sourcePackage == targetPackage || newFolderId.isBlank() ||
        latest.appDrawerFolders.any { it.id == newFolderId }
    ) return latest

    val moved = setOf(sourcePackage, targetPackage)
    val remaining = latest.appDrawerFolders.mapNotNull { previous ->
        val keep = previous.appPackages.filterNot { it in moved }
        if (keep.isEmpty()) null else previous.copy(appPackages = keep)
    }
    val folder = AppFolder(
        id = newFolderId, title = "Thư mục mới",
        appPackages = listOf(targetPackage, sourcePackage),
        category = AppCategory.ALL, orderIndex = remaining.size
    )
    val liveFolderKeys = remaining.map { "folder:${it.id}" }.toSet()
    val deletedAppKeys = setOf(sourceKey, targetKey, "app:$sourcePackage", "app:$targetPackage")
    val order = latest.appDrawerCustomOrder.filterNot {
        it in deletedAppKeys || (it.startsWith("folder:") && it !in liveFolderKeys)
    } + "folder:$newFolderId"
    return latest.copy(appDrawerFolders = remaining + folder, appDrawerCustomOrder = order)
}
