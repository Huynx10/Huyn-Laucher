package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LauncherSettings
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import com.example.ui.theme.LumiaPalette

@Composable
fun TileView(
    tile: TileEntity,
    settings: LauncherSettings,
    telemetry: SystemTelemetry,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onResize: () -> Unit,
    onUnpin: () -> Unit,
    onMovePrev: () -> Unit,
    onMoveNext: () -> Unit,
    onMoveToPage: (() -> Unit)? = null,
    onCustomizeTile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = LumiaPalette.getAccent(tile.colorOverrideHex ?: settings.accentColorHex)
    val tileBgColor = accent.color.copy(alpha = settings.tileOpacity)
    val cornerShape = RoundedCornerShape(settings.tileCornerRadiusDp.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "wiggle")
    val wiggleAngle by infiniteTransition.animateFloat(
        initialValue = -1.1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(190),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggleAngle"
    )

    val glassBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.06f)
        )
    )

    Box(
        modifier = modifier
            .testTag("tile_${tile.id}")
            .graphicsLayer {
                if (isEditMode && !settings.lockHomeLayout) {
                    rotationZ = wiggleAngle
                    scaleX = 0.98f
                    scaleY = 0.98f
                }
            }
            .clip(cornerShape)
            .background(tileBgColor)
            .then(
                when {
                    isEditMode -> Modifier.border(1.5.dp, Color.White.copy(alpha = 0.85f), cornerShape)
                    settings.miuiGlassBorder -> Modifier.border(0.8.dp, glassBorderBrush, cornerShape)
                    else -> Modifier
                }
            )
            .metroTilt(
                enabled = settings.turnstileAnimationEnabled && !isEditMode,
                onLongClick = onLongClick,
                onClick = {
                    if (isEditMode) {
                        onCustomizeTile?.invoke()
                    } else {
                        onClick()
                    }
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (tile.tileType) {
                TileType.CLOCK_CALENDAR -> ClockLiveTileContent(size = tile.size)
                TileType.WEATHER -> WeatherLiveTileContent(size = tile.size)
                TileType.BATTERY -> BatteryLiveTileContent(telemetry = telemetry, size = tile.size)
                TileType.PERFORMANCE -> PerformanceLiveTileContent(telemetry = telemetry, size = tile.size)
                TileType.PHOTOS -> PhotosLiveTileContent(size = tile.size)
                TileType.NOTES -> NotesLiveTileContent(quickNote = settings.quickNotes, size = tile.size)
                TileType.NEWS_QUOTE -> NewsQuoteLiveTileContent(size = tile.size)
                TileType.APP -> {
                    val appIconBitmap = if (settings.useRealAppIcons) {
                        rememberAppIcon(tile.packageName)
                    } else null

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        val iconSize = when (tile.size) {
                            TileSize.SMALL -> 28.dp
                            TileSize.MEDIUM -> 42.dp
                            TileSize.WIDE -> 46.dp
                            TileSize.LARGE -> 62.dp
                        }

                        if (appIconBitmap != null) {
                            Image(
                                bitmap = appIconBitmap,
                                contentDescription = tile.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(iconSize)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            val iconVector = resolveIconForTile(tile.customIconName, tile.title)
                            Icon(
                                imageVector = iconVector,
                                contentDescription = tile.title,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(iconSize)
                            )
                        }

                        if (tile.size != TileSize.SMALL) {
                            Text(
                                text = tile.title,
                                color = Color.White,
                                fontSize = if (tile.size == TileSize.WIDE) 13.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.BottomStart)
                            )
                        }
                    }
                }
            }

            if (tile.notificationCount > 0 && !isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE81123)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${tile.notificationCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.36f))
            ) {
                if (settings.lockHomeLayout) {
                    // Locked layout indicator (MIUI / One UI feature)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Đã khóa bố cục",
                            tint = Color(0xFFFFC700),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    // Top-left: Reorder arrows
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.72f))
                                .clickable { onMovePrev() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Di chuyển lên",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.72f))
                                .clickable { onMoveNext() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Di chuyển xuống",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Top-right: Unpin button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE81123))
                            .clickable { onUnpin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Bỏ ghim",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Bottom-left: Move to another Home Page + Customize Tile Color
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                    ) {
                        if (onCustomizeTile != null) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF881798))
                                    .clickable { onCustomizeTile() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Đổi màu & Tên ô Tile",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (onMoveToPage != null) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF107C41))
                                    .clickable { onMoveToPage() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = "Chuyển sang trang Home khác",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Bottom-right: Resize cycle button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0078D7))
                            .clickable { onResize() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Đổi kích thước",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (tile.size != TileSize.SMALL) {
                    Text(
                        text = when (tile.size) {
                            TileSize.SMALL -> "1x1"
                            TileSize.MEDIUM -> "Vừa (2x2)"
                            TileSize.WIDE -> "Rộng (4x2)"
                            TileSize.LARGE -> "Lớn (4x4)"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
