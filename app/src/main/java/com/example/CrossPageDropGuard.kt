package com.example

/**
 * A late failure acknowledgement may restore the source page only if its drag
 * is still the latest completed session and the user has not navigated away.
 * This is navigation only: repository/Room mutation is handled separately.
 */
internal fun canRestoreFailedCrossPageDrop(
    completedGeneration: Int,
    currentGeneration: Int,
    activeDragTileId: String?,
    committedTargetPageId: String,
    currentPageId: String?
): Boolean =
    completedGeneration == currentGeneration &&
        activeDragTileId == null &&
        committedTargetPageId == currentPageId
