package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCategory
import com.example.data.model.AppItem
import com.example.data.model.LauncherSettings
import com.example.data.model.SmartFolderConfig
import com.example.data.model.SmartFolderItem
import com.example.data.model.SmartFolderLayoutMode
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import com.example.ui.theme.LumiaPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun resolveCategoryIcon(category: AppCategory): ImageVector {
    return when (category) {
        AppCategory.COMMUNICATION -> Icons.Default.Phone
        AppCategory.SOCIAL -> Icons.Default.Public
        AppCategory.GAMES -> Icons.Default.SportsEsports
        AppCategory.MEDIA -> Icons.Default.PhotoLibrary
        AppCategory.TOOLS -> Icons.Default.Build
        AppCategory.PRODUCTIVITY -> Icons.Default.Work
        AppCategory.SYSTEM -> Icons.Default.Settings
        AppCategory.ALL -> Icons.Default.FolderSpecial
    }
}

fun resolveSmartFolderItemIcon(item: SmartFolderItem): ImageVector {
    return when (item.tileType) {
        TileType.CLOCK_CALENDAR -> Icons.Default.CalendarMonth
        TileType.WEATHER -> Icons.Default.WbSunny
        TileType.BATTERY -> Icons.Default.BatteryChargingFull
        TileType.PERFORMANCE -> Icons.Default.Speed
        TileType.PHOTOS -> Icons.Default.PhotoLibrary
        TileType.NOTES -> resolveIconForTile("note", item.title)
        TileType.NEWS_QUOTE -> Icons.Default.FormatQuote
        TileType.WIDGET -> Icons.Default.Widgets
        TileType.SMART_FOLDER -> Icons.Default.FolderSpecial
        TileType.APP -> resolveIconForTile(item.customIconName, item.title)
    }
}

fun parseHexColorSafe(hex: String, fallback: Color = Color(0xFF0078D7)): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val value = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(0xFF000000 or value)
        } else if (cleaned.length == 8) {
            Color(value)
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

/**
 * Closed Smart Folder Live Tile Content with MIUI / HyperOS Folder-Peek Preview.
 * - In HYPEROS_PEEK mode, primary slots launch apps directly when tapped, while the bottom-right
 *   2x2 mini "Folder-Peek" cluster opens the expanded folder with MIUI/HyperOS spring physics.
 */
@Composable
fun SmartFolderLiveTileContent(
    tile: TileEntity,
    settings: LauncherSettings,
    installedApps: List<AppItem>,
    interactiveSubSlots: Boolean,
    isBatterySaverActive: Boolean = false,
    onLaunchItem: (SmartFolderItem) -> Unit,
    onOpenFolder: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val config = remember(tile.packageName, tile.customIconName) {
        SmartFolderConfig.fromTile(tile)
    }
    val effectiveItems = remember(config, installedApps, settings.customAppCategories) {
        SmartFolderConfig.resolveEffectiveItems(
            config = config,
            installedApps = installedApps,
            customCategories = settings.customAppCategories
        )
    }
    val categoryColor = remember(config.category) {
        parseHexColorSafe(config.category.badgeColorHex)
    }

    if (tile.size == TileSize.SMALL) {
        // 1x1 Small Folder: 2x2 micro-peek grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .testTag("smart_folder_peek_slot_${tile.id}"),
            contentAlignment = Alignment.Center
        ) {
            MiniPeekClusterGrid(
                items = effectiveItems.take(4),
                extraCount = (effectiveItems.size - 4).coerceAtLeast(0),
                useRealAppIcons = settings.useRealAppIcons,
                iconSize = 14.dp
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: Category pill + Title + Item Count badge (Tapping header opens folder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (interactiveSubSlots) {
                        Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenFolder()
                        }
                    } else Modifier
                )
                .testTag("smart_folder_header_${tile.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveCategoryIcon(config.category),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Text(
                    text = tile.title,
                    color = Color.White,
                    fontSize = if (tile.size == TileSize.LARGE) 13.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = "${effectiveItems.size}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Body according to SmartFolderLayoutMode & TileSize
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (config.layoutMode) {
                SmartFolderLayoutMode.HYPEROS_PEEK -> {
                    when (tile.size) {
                        TileSize.MEDIUM -> {
                            // MIUI / HyperOS 3+1 Peek Grid (2x2: 3 direct-launch icons + 1 2x2 Folder-Peek cluster)
                            val primary3 = effectiveItems.take(3)
                            val peekItems = effectiveItems.drop(3)

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Row 1: Slot 0 & Slot 1
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DirectFolderItemSlot(
                                        item = primary3.getOrNull(0),
                                        tileId = tile.id,
                                        slotIndex = 0,
                                        useRealAppIcons = settings.useRealAppIcons,
                                        showLabel = false,
                                        interactive = interactiveSubSlots,
                                        onLaunch = onLaunchItem,
                                        onFallbackOpen = onOpenFolder,
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
                                    DirectFolderItemSlot(
                                        item = primary3.getOrNull(1),
                                        tileId = tile.id,
                                        slotIndex = 1,
                                        useRealAppIcons = settings.useRealAppIcons,
                                        showLabel = false,
                                        interactive = interactiveSubSlots,
                                        onLaunch = onLaunchItem,
                                        onFallbackOpen = onOpenFolder,
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
                                }
                                // Row 2: Slot 2 + Slot 3 (Folder-Peek Preview Cluster!)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DirectFolderItemSlot(
                                        item = primary3.getOrNull(2),
                                        tileId = tile.id,
                                        slotIndex = 2,
                                        useRealAppIcons = settings.useRealAppIcons,
                                        showLabel = false,
                                        interactive = interactiveSubSlots,
                                        onLaunch = onLaunchItem,
                                        onFallbackOpen = onOpenFolder,
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
                                    // Bottom-right Folder-Peek Preview Cluster
                                    FolderPeekSlotBox(
                                        tileId = tile.id,
                                        peekItems = peekItems,
                                        useRealAppIcons = settings.useRealAppIcons,
                                        categoryColor = categoryColor,
                                        interactive = interactiveSubSlots,
                                        onOpenFolder = onOpenFolder,
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
                                }
                            }
                        }

                        TileSize.WIDE -> {
                            // 4x2 Wide HyperOS Peek: 6 direct-launch slots + 2-col Folder-Peek Preview capsule
                            val primary6 = effectiveItems.take(6)
                            val peekItems = effectiveItems.drop(6)

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Top row: 4 direct slots
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (i in 0 until 4) {
                                        DirectFolderItemSlot(
                                            item = primary6.getOrNull(i),
                                            tileId = tile.id,
                                            slotIndex = i,
                                            useRealAppIcons = settings.useRealAppIcons,
                                            showLabel = false,
                                            interactive = interactiveSubSlots,
                                            onLaunch = onLaunchItem,
                                            onFallbackOpen = onOpenFolder,
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                }
                                // Bottom row: 2 direct slots + 2-span Folder-Peek Preview Capsule
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (i in 4 until 6) {
                                        DirectFolderItemSlot(
                                            item = primary6.getOrNull(i),
                                            tileId = tile.id,
                                            slotIndex = i,
                                            useRealAppIcons = settings.useRealAppIcons,
                                            showLabel = false,
                                            interactive = interactiveSubSlots,
                                            onLaunch = onLaunchItem,
                                            onFallbackOpen = onOpenFolder,
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                    FolderPeekWideCapsule(
                                        tileId = tile.id,
                                        peekItems = peekItems.ifEmpty { effectiveItems.take(4) },
                                        totalCount = effectiveItems.size,
                                        useRealAppIcons = settings.useRealAppIcons,
                                        categoryColor = categoryColor,
                                        interactive = interactiveSubSlots,
                                        onOpenFolder = onOpenFolder,
                                        modifier = Modifier.weight(2.08f).fillMaxHeight()
                                    )
                                }
                            }
                        }

                        TileSize.LARGE -> {
                            // 4x4 XXL HyperOS Folder: 3x3 grid (8 direct slots with labels + 9th Folder-Peek cluster)
                            val primary8 = effectiveItems.take(8)
                            val peekItems = effectiveItems.drop(8).ifEmpty { effectiveItems.drop(3) }

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                for (row in 0 until 3) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        for (col in 0 until 3) {
                                            val idx = row * 3 + col
                                            if (idx < 8) {
                                                DirectFolderItemSlot(
                                                    item = primary8.getOrNull(idx),
                                                    tileId = tile.id,
                                                    slotIndex = idx,
                                                    useRealAppIcons = settings.useRealAppIcons,
                                                    showLabel = true,
                                                    interactive = interactiveSubSlots,
                                                    onLaunch = onLaunchItem,
                                                    onFallbackOpen = onOpenFolder,
                                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                                )
                                            } else {
                                                FolderPeekSlotBox(
                                                    tileId = tile.id,
                                                    peekItems = peekItems,
                                                    useRealAppIcons = settings.useRealAppIcons,
                                                    categoryColor = categoryColor,
                                                    interactive = interactiveSubSlots,
                                                    onOpenFolder = onOpenFolder,
                                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        TileSize.SMALL -> {}
                    }
                }

                SmartFolderLayoutMode.CLASSIC_GRID -> {
                    // 3x3 Classic Mini-Grid with Folder-Peek trigger on tap
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .then(
                                if (interactiveSubSlots) {
                                    Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onOpenFolder()
                                    }
                                } else Modifier
                            )
                            .padding(6.dp)
                            .testTag("smart_folder_peek_slot_${tile.id}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val rows = if (tile.size == TileSize.WIDE) 2 else 3
                            val cols = if (tile.size == TileSize.WIDE) 4 else 3
                            for (r in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (c in 0 until cols) {
                                        val idx = r * cols + c
                                        val item = effectiveItems.getOrNull(idx)
                                        if (item != null) {
                                            SmartFolderMiniIcon(
                                                item = item,
                                                useRealAppIcons = settings.useRealAppIcons,
                                                size = if (tile.size == TileSize.LARGE) 30.dp else 20.dp
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SmartFolderLayoutMode.CATEGORY_STACK -> {
                    // Live Category Stack: Animated spotlight item on left + 2x2 Folder-Peek cluster on right
                    var spotlightIdx by remember { mutableIntStateOf(0) }
                    LaunchedEffect(effectiveItems.size, isBatterySaverActive, tile.isLiveEnabled) {
                        if (effectiveItems.isNotEmpty() && tile.isLiveEnabled) {
                            val intervalMs = if (isBatterySaverActive) 15_000L else 4_500L
                            while (true) {
                                delay(intervalMs)
                                spotlightIdx = (spotlightIdx + 1) % effectiveItems.size
                            }
                        }
                    }
                    val currentItem = effectiveItems.getOrNull(spotlightIdx % effectiveItems.size.coerceAtLeast(1))

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Live Spotlight item (Direct launch)
                        Box(
                            modifier = Modifier
                                .weight(1.25f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .then(
                                    if (interactiveSubSlots && currentItem != null) {
                                        Modifier.clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onLaunchItem(currentItem)
                                        }
                                    } else Modifier
                                )
                                .padding(6.dp)
                                .testTag("smart_folder_direct_slot_${tile.id}_0"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentItem != null) {
                                AnimatedContent(
                                    targetState = currentItem,
                                    transitionSpec = {
                                        if (isBatterySaverActive) {
                                            fadeIn().togetherWith(fadeOut())
                                        } else {
                                            (slideInVertically { it / 2 } + fadeIn()).togetherWith(
                                                slideOutVertically { -it / 2 } + fadeOut()
                                            )
                                        }
                                    },
                                    label = "smartFolderStackFlip"
                                ) { item ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        SmartFolderMiniIcon(
                                            item = item,
                                            useRealAppIcons = settings.useRealAppIcons,
                                            size = if (tile.size == TileSize.LARGE) 44.dp else 28.dp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Right: Folder-Peek Preview Cluster
                        FolderPeekSlotBox(
                            tileId = tile.id,
                            peekItems = effectiveItems.drop(1).ifEmpty { effectiveItems },
                            useRealAppIcons = settings.useRealAppIcons,
                            categoryColor = categoryColor,
                            interactive = interactiveSubSlots,
                            onOpenFolder = onOpenFolder,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectFolderItemSlot(
    item: SmartFolderItem?,
    tileId: String,
    slotIndex: Int,
    useRealAppIcons: Boolean,
    showLabel: Boolean,
    interactive: Boolean,
    onLaunch: (SmartFolderItem) -> Unit,
    onFallbackOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .border(0.6.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
            .then(
                if (interactive) {
                    Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (item != null) onLaunch(item) else onFallbackOpen()
                    }
                } else Modifier
            )
            .testTag("smart_folder_direct_slot_${tileId}_$slotIndex"),
        contentAlignment = Alignment.Center
    ) {
        if (item != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(3.dp)
            ) {
                SmartFolderMiniIcon(
                    item = item,
                    useRealAppIcons = useRealAppIcons,
                    size = if (showLabel) 28.dp else 24.dp
                )
                if (showLabel) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * The signature MIUI / HyperOS Folder-Peek Preview Cluster in the corner slot of the folder widget.
 * Displays a 2x2 micro-grid of remaining apps and triggers the MIUI/HyperOS folder opening animation when tapped.
 */
@Composable
private fun FolderPeekSlotBox(
    tileId: String,
    peekItems: List<SmartFolderItem>,
    useRealAppIcons: Boolean,
    categoryColor: Color,
    interactive: Boolean,
    onOpenFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.36f),
                        categoryColor.copy(alpha = 0.32f)
                    )
                )
            )
            .border(0.8.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
            .then(
                if (interactive) {
                    Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenFolder()
                    }
                } else Modifier
            )
            .padding(3.dp)
            .testTag("smart_folder_peek_slot_$tileId"),
        contentAlignment = Alignment.Center
    ) {
        MiniPeekClusterGrid(
            items = peekItems.take(4),
            extraCount = (peekItems.size - 4).coerceAtLeast(0),
            useRealAppIcons = useRealAppIcons,
            iconSize = 11.dp
        )
    }
}

@Composable
private fun FolderPeekWideCapsule(
    tileId: String,
    peekItems: List<SmartFolderItem>,
    totalCount: Int,
    useRealAppIcons: Boolean,
    categoryColor: Color,
    interactive: Boolean,
    onOpenFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.36f),
                        categoryColor.copy(alpha = 0.36f)
                    )
                )
            )
            .border(0.8.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
            .then(
                if (interactive) {
                    Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenFolder()
                    }
                } else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("smart_folder_peek_slot_$tileId"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            peekItems.take(3).forEach { item ->
                SmartFolderMiniIcon(
                    item = item,
                    useRealAppIcons = useRealAppIcons,
                    size = 16.dp
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Peek",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Mở rộng thư mục ($totalCount)",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
private fun MiniPeekClusterGrid(
    items: List<SmartFolderItem>,
    extraCount: Int,
    useRealAppIcons: Boolean,
    iconSize: Dp
) {
    if (items.isEmpty()) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = "Mở thư mục",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(iconSize * 1.4f)
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (r in 0 until 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (c in 0 until 2) {
                    val idx = r * 2 + c
                    if (idx == 3 && extraCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$extraCount",
                                color = Color.White,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        val item = items.getOrNull(idx)
                        if (item != null) {
                            SmartFolderMiniIcon(
                                item = item,
                                useRealAppIcons = useRealAppIcons,
                                size = iconSize
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(iconSize * 0.7f)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartFolderMiniIcon(
    item: SmartFolderItem,
    useRealAppIcons: Boolean,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bitmap = if (useRealAppIcons && item.packageName.isNotBlank()) {
        rememberAppIcon(item.packageName)
    } else null

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f))
        )
    } else {
        val catColor = parseHexColorSafe(item.category.badgeColorHex)
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.25f))
                .background(catColor.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = resolveSmartFolderItemIcon(item),
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(size * 0.64f)
            )
        }
    }
}

/**
 * Expanded Smart Folder Overlay with MIUI / HyperOS Spring Opening Animation.
 * Scales & morphs smoothly from the folder tile's pivot coordinates with staggered icon spring reveal.
 */
@Composable
fun SmartFolderExpandedOverlay(
    folderTile: TileEntity,
    pivotFractionX: Float,
    pivotFractionY: Float,
    settings: LauncherSettings,
    installedApps: List<AppItem>,
    onLaunchItem: (SmartFolderItem) -> Unit,
    onUpdateFolder: (String, SmartFolderConfig) -> Unit,
    onUngroupFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var config by remember(folderTile.id, folderTile.packageName) {
        mutableStateOf(SmartFolderConfig.fromTile(folderTile))
    }
    var folderTitle by remember(folderTile.id, folderTile.title) {
        mutableStateOf(folderTile.title)
    }
    var isEditingTitle by remember { mutableStateOf(false) }
    var isManagingItems by remember { mutableStateOf(false) }

    // MIUI / HyperOS Spring Expansion Progress (0f = collapsed at tile origin, 1f = full expanded modal)
    val expansionProgress = remember { Animatable(0f) }
    LaunchedEffect(folderTile.id) {
        expansionProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 340f
            )
        )
    }

    fun animateAndDismiss(afterClose: () -> Unit = {}) {
        scope.launch {
            expansionProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 520f
                )
            )
            onDismiss()
            afterClose()
        }
    }

    val effectiveItems = remember(config, installedApps, settings.customAppCategories) {
        SmartFolderConfig.resolveEffectiveItems(
            config = config,
            installedApps = installedApps,
            customCategories = settings.customAppCategories
        )
    }

    val accent = LumiaPalette.resolveEffectiveAccent(settings)
    val categoryColor = parseHexColorSafe(config.category.badgeColorHex, accent.color)
    val progress = expansionProgress.value.coerceIn(0f, 1.15f)
    val cardScale = 0.26f + (0.74f * progress)
    val cardCornerDp = (10f + (22f * progress.coerceIn(0f, 1f))).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9080D16).copy(alpha = (progress * 0.88f).coerceIn(0f, 0.92f)))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { animateAndDismiss() }
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .testTag("smart_folder_expanded_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // MIUI / HyperOS Ambient Category Glow behind the expanded folder
        Box(
            modifier = Modifier
                .size(340.dp)
                .graphicsLayer {
                    scaleX = progress
                    scaleY = progress
                    alpha = (progress * 0.35f).coerceIn(0f, 0.4f)
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Expanded Smart Folder Glassmorphic Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .heightIn(max = 640.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(
                        pivotFractionX = pivotFractionX.coerceIn(0.1f, 0.9f),
                        pivotFractionY = pivotFractionY.coerceIn(0.1f, 0.9f)
                    )
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = progress.coerceIn(0f, 1f)
                }
                .shadow(28.dp, RoundedCornerShape(cardCornerDp))
                .clip(RoundedCornerShape(cardCornerDp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A2232).copy(alpha = 0.96f),
                            Color(0xFF111622).copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.42f),
                            categoryColor.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.14f)
                        )
                    ),
                    shape = RoundedCornerShape(cardCornerDp)
                )
                .pointerInput(Unit) {
                    // Consume taps inside the card so they don't close the overlay
                    detectTapGestures(onTap = {})
                }
                .padding(18.dp)
                .testTag("smart_folder_expanded_card"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Folder Header: Category Icon + Editable Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(categoryColor.copy(alpha = 0.25f))
                            .border(1.dp, categoryColor, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolveCategoryIcon(config.category),
                            contentDescription = config.category.label,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = folderTitle,
                            onValueChange = { newText ->
                                folderTitle = newText
                                onUpdateFolder(newText, config)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = categoryColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("smart_folder_title_input")
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                                .clickable { isEditingTitle = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Xong",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isEditingTitle = true }
                                .testTag("smart_folder_title_display")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = folderTitle,
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Đổi tên thư mục",
                                    tint = Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "${effectiveItems.size} mục",
                                color = Color.White.copy(alpha = 0.68f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { animateAndDismiss() }
                        .testTag("smart_folder_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng thư mục",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 4. Staggered MIUI / HyperOS App & Tile Grid (or Item Manager Checklist)
            if (!isManagingItems) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(min = 180.dp, max = 320.dp)
                        .testTag("smart_folder_items_grid")
                ) {
                    itemsIndexed(
                        items = effectiveItems,
                        key = { idx, item -> "${item.id}_$idx" }
                    ) { index, item ->
                        // Staggered spring entrance inspired by HyperOS folder icon bloom
                        val itemBounce by animateFloatAsState(
                            targetValue = if (progress > 0.15f) 1f else 0.4f,
                            animationSpec = spring(
                                dampingRatio = 0.68f,
                                stiffness = (420f - (index * 18f)).coerceAtLeast(180f)
                            ),
                            label = "hyperOsIconBloom_$index"
                        )
                        val isPrimaryDirectSlot = config.layoutMode == SmartFolderLayoutMode.HYPEROS_PEEK && index < 3

                        Column(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = itemBounce
                                    scaleY = itemBounce
                                    alpha = itemBounce.coerceIn(0f, 1f)
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isPrimaryDirectSlot) Color.White.copy(alpha = 0.11f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isPrimaryDirectSlot) 0.9.dp else 0.5.dp,
                                    color = if (isPrimaryDirectSlot) categoryColor.copy(alpha = 0.7f)
                                    else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    animateAndDismiss {
                                        onLaunchItem(item)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("smart_folder_expanded_item_$index"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                SmartFolderMiniIcon(
                                    item = item,
                                    useRealAppIcons = settings.useRealAppIcons,
                                    size = 44.dp
                                )
                                if (isPrimaryDirectSlot) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(13.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Item Management List: Add or Remove Apps/Tiles in this Smart Folder
                val candidateItems = remember(installedApps, config.category, settings.customAppCategories) {
                    val appCandidates = installedApps.map { app ->
                        val cat = app.effectiveCategory(settings.customAppCategories)
                        SmartFolderItem(
                            id = "app_${app.packageName}",
                            title = app.label,
                            packageName = app.packageName,
                            tileType = TileType.APP,
                            category = cat
                        )
                    }
                    val seedCandidates = AppCategory.assignableCategories.flatMap {
                        SmartFolderConfig.defaultSeedItemsForCategory(it)
                    }
                    (appCandidates + seedCandidates)
                        .distinctBy { if (it.packageName.isNotBlank()) "pkg:${it.packageName}" else "title:${it.title.lowercase()}" }
                        .sortedWith(
                            compareByDescending<SmartFolderItem> { it.category == config.category }
                                .thenBy { it.title.lowercase() }
                        )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .heightIn(min = 180.dp, max = 300.dp)
                        .testTag("smart_folder_manage_grid")
                ) {
                    itemsIndexed(candidateItems) { idx, candidate ->
                        val key = if (candidate.packageName.isNotBlank()) "pkg:${candidate.packageName}"
                        else "type:${candidate.tileType.name}:${candidate.title.lowercase()}"
                        val isIncluded = effectiveItems.any {
                            val ik = if (it.packageName.isNotBlank()) "pkg:${it.packageName}"
                            else "type:${it.tileType.name}:${it.title.lowercase()}"
                            ik == key
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isIncluded) categoryColor.copy(alpha = 0.28f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    width = if (isIncluded) 1.dp else 0.5.dp,
                                    color = if (isIncluded) categoryColor else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val updatedConfig = if (isIncluded) {
                                        config.copy(
                                            items = config.items.filterNot {
                                                val ik = if (it.packageName.isNotBlank()) "pkg:${it.packageName}"
                                                else "type:${it.tileType.name}:${it.title.lowercase()}"
                                                ik == key
                                            },
                                            excludedKeys = config.excludedKeys + key + candidate.id
                                        )
                                    } else {
                                        config.copy(
                                            items = config.items + candidate,
                                            excludedKeys = config.excludedKeys - key - candidate.id
                                        )
                                    }
                                    config = updatedConfig
                                    onUpdateFolder(folderTitle, updatedConfig)
                                }
                                .padding(horizontal = 8.dp, vertical = 7.dp)
                                .testTag("smart_folder_candidate_$idx"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SmartFolderMiniIcon(
                                item = candidate,
                                useRealAppIcons = settings.useRealAppIcons,
                                size = 22.dp
                            )
                            Text(
                                text = candidate.title,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isIncluded) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Bottom Actions Bar: "+ Thêm / Bớt ứng dụng" & "Giải tán ra màn hình"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isManagingItems) categoryColor
                            else Color.White.copy(alpha = 0.10f)
                        )
                        .clickable { isManagingItems = !isManagingItems }
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                        .testTag("smart_folder_manage_items_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isManagingItems) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isManagingItems) "Hoàn tất chọn mục" else "Thêm / Bớt mục",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!settings.lockHomeLayout) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                animateAndDismiss {
                                    onUngroupFolder()
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                            .testTag("smart_folder_ungroup_button"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = "Giải tán thư mục",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bung ra Tile",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
