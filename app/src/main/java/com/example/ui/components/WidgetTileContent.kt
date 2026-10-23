package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.WidgetProviderItem
import com.example.ui.widget.LumiaWidgetHostManager
import com.example.ui.widget.WidgetHostState

/**
 * Renders a native Android AppWidget inside a Metro Tile container using [AndroidView] and [LumiaAppWidgetHostView].
 *
 * Rules enforced:
 * - Never crops or artificially scales the widget canvas; passes real dp bounds via `updateAppWidgetOptions`.
 * - Preserves third-party widget internal colors, typography, and click/scroll PendingIntents.
 * - Handles invalid widget IDs, backup/restore re-linking, and uninstalled widget providers gracefully.
 */
@Composable
fun WidgetTileContent(
    tile: TileEntity,
    isTileSelected: Boolean,
    isHomeEditMode: Boolean,
    useRealAppIcons: Boolean,
    accentColor: Color,
    onLongClickTile: () -> Unit,
    onRequestRebindWidget: (TileEntity, WidgetProviderItem) -> Unit = { _, _ -> },
    onSilentWidgetRebound: (String, Int, WidgetProviderItem) -> Unit = { _, _, _ -> },
    onOpenWidgetPickerForTile: (TileEntity) -> Unit = {},
    onRemoveBrokenWidgetTile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val hostManager = remember(context) { LumiaWidgetHostManager.getInstance(context) }

    var activeAppWidgetId by remember(tile.id, tile.appWidgetId) {
        mutableIntStateOf(tile.appWidgetId)
    }
    var attemptedSilentRebind by remember(tile.id, tile.widgetProvider) {
        mutableStateOf(false)
    }

    val hostState = remember(tile.id, activeAppWidgetId, tile.widgetProvider) {
        hostManager.inspectWidgetState(activeAppWidgetId, tile.widgetProvider)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("widget_tile_container_${tile.id}")
    ) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(60)
        val heightDp = maxHeight.value.toInt().coerceAtLeast(60)

        when (val state = hostState) {
            is WidgetHostState.Ready -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            hostManager.createHostView(
                                activityOrAppContext = ctx,
                                appWidgetId = state.appWidgetId,
                                providerInfo = state.providerInfo,
                                widthDp = widthDp,
                                heightDp = heightDp
                            ).apply {
                                isEditOverlayActive = isTileSelected || isHomeEditMode
                                onLongPressTile = onLongClickTile
                            }
                        },
                        update = { view ->
                            view.isEditOverlayActive = isTileSelected || isHomeEditMode
                            view.onLongPressTile = onLongClickTile
                            hostManager.updateWidgetSizeOptionsIfChanged(
                                appWidgetId = state.appWidgetId,
                                widthDp = widthDp,
                                heightDp = heightDp
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            
                            .testTag("native_widget_host_view_${tile.id}")
                    )

                    // Subtle Metro Widget corner label when in Edit Mode so user knows which app owns the widget
                    if (isHomeEditMode && !isTileSelected && tile.size != TileSize.SMALL) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tile.title,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            is WidgetHostState.NeedsRebind -> {
                val item = state.providerItem
                val appIcon = if (useRealAppIcons && item.packageName.isNotBlank()) {
                    rememberAppIcon(item.packageName)
                } else null

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = !isTileSelected && !isHomeEditMode) {
                            onRequestRebindWidget(tile, item)
                        }
                        .padding(10.dp)
                        .testTag("rebind_widget_button_${tile.id}"),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = item.appName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = tile.title.ifBlank { item.label },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.appName,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Kết nối lại Widget",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (tile.size == TileSize.SMALL) "Kết nối" else "Chạm để liên kết lại Widget",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            is WidgetHostState.ProviderMissing -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .testTag("missing_widget_state_${tile.id}"),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = Color(0xFFFFB900),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tile.title.ifBlank { "Widget không khả dụng" },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (tile.size != TileSize.SMALL) {
                        Text(
                            text = "Ứng dụng cung cấp Widget chưa cài đặt hoặc đã thay đổi",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable(enabled = !isTileSelected) {
                                    onOpenWidgetPickerForTile(tile)
                                }
                                .padding(vertical = 5.dp)
                                .testTag("missing_widget_replace_button_${tile.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Đổi Widget",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (tile.size != TileSize.SMALL) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFFE81123).copy(alpha = 0.75f))
                                    .clickable(enabled = !isTileSelected) {
                                        onRemoveBrokenWidgetTile(tile.id)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Xóa",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
