package com.example.ui.components

import android.view.Gravity
import android.widget.TextClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AppCategory
import com.example.data.model.AppItem
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import java.util.Locale

/**
 * Resolves the real installed [AppItem] linked to a [TileEntity].
 * Prioritizes exact package match, then falls back to matching an installed app
 * on the device by category/type so Live Tiles always use real app data rather than standalone mock widgets.
 */
fun resolveLinkedAppForTile(
    tile: TileEntity,
    installedApps: List<AppItem>
): AppItem? {
    if (installedApps.isEmpty()) return null
    if (tile.tileType == TileType.APP) {
        when (tile.launcherIdentityState) {
            "EXACT" -> return installedApps.firstOrNull {
                it.launcherComponent == tile.launcherComponent &&
                    it.launcherUserSerial == tile.launcherUserSerial &&
                    it.packageName == tile.packageName
            }
            "UNRESOLVED" -> return null
        }
    }

    if (tile.packageName.isNotBlank() && !tile.packageName.startsWith("{")) {
        val matches = installedApps.filter { it.packageName.equals(tile.packageName, ignoreCase = true) }
        // Legacy APP must not display a random work-profile or secondary activity.
        if (tile.tileType == TileType.APP && matches.size > 1) return null
        matches.firstOrNull()?.let { return it }
    }

    return when (tile.tileType) {
        TileType.WIDGET -> null
        TileType.CLOCK_CALENDAR -> installedApps.find {
            val pkg = it.packageName.lowercase(Locale.ROOT)
            pkg.contains("clock") || pkg.contains("calendar") || pkg.contains("alarm")
        } ?: installedApps.find { it.category == AppCategory.TOOLS }

        TileType.PHOTOS -> installedApps.find {
            val pkg = it.packageName.lowercase(Locale.ROOT)
            pkg.contains("photos") || pkg.contains("gallery") || pkg.contains("camera")
        } ?: installedApps.find { it.category == AppCategory.MEDIA }

        TileType.WEATHER -> installedApps.find {
            val pkg = it.packageName.lowercase(Locale.ROOT)
            pkg.contains("weather") || pkg.contains("maps") || pkg.contains("chrome") || pkg.contains("browser")
        } ?: installedApps.find { it.category == AppCategory.TOOLS }

        TileType.NOTES -> installedApps.find {
            val pkg = it.packageName.lowercase(Locale.ROOT)
            pkg.contains("keep") || pkg.contains("note") || pkg.contains("docs")
        } ?: installedApps.find { it.category == AppCategory.PRODUCTIVITY }

        TileType.NEWS_QUOTE -> installedApps.find {
            it.category == AppCategory.SOCIAL || it.packageName.contains("youtube", ignoreCase = true)
        } ?: installedApps.firstOrNull()

        TileType.BATTERY, TileType.PERFORMANCE -> installedApps.find {
            it.packageName.contains("settings", ignoreCase = true) || it.category == AppCategory.SYSTEM
        }

        TileType.APP -> {
            val titleKey = tile.title.lowercase(Locale.ROOT)
            installedApps.find { it.label.lowercase(Locale.ROOT) == titleKey }
        }

        TileType.SMART_FOLDER -> null
    }
}

/**
 * Unified, lightweight App-Driven Live Tile renderer.
 * Uses real app icon, label, category, notification count, and Android OS telemetry
 * without spawning per-tile coroutine loops or standalone simulated widget state machines.
 */
@Composable
fun AppDrivenLiveTileContent(
    tile: TileEntity,
    installedApps: List<AppItem>,
    telemetry: SystemTelemetry,
    useRealAppIcons: Boolean,
    isHighlyTransparent: Boolean
) {
    if (tile.tileType == TileType.CLOCK_CALENDAR) {
        MetroDigitalClockTile(tile.size, isHighlyTransparent)
        return
    }

    val linkedApp = remember(tile.id, tile.packageName, tile.tileType,
        tile.launcherComponent, tile.launcherUserSerial, tile.launcherIdentityState, installedApps) {
        resolveLinkedAppForTile(tile, installedApps)
    }

    val effectivePackage = linkedApp?.packageName ?: tile.packageName
    val appIconBitmap = if (useRealAppIcons && effectivePackage.isNotBlank() && !effectivePackage.startsWith("{")) {
        when {
            linkedApp != null -> rememberAppIcon(linkedApp)
            tile.tileType == TileType.APP -> null // No arbitrary package icon for missing exact target.
            else -> rememberAppIcon(effectivePackage)
        }
    } else null

    val displayTitle = remember(tile.title, tile.tileType, linkedApp?.label) {
        if (tile.tileType == TileType.APP) {
            linkedApp?.label ?: tile.title.ifBlank { "Ứng dụng" }
        } else {
            tile.title.ifBlank { linkedApp?.label ?: "Ứng dụng" }
        }
    }

    val categoryBadge = remember(tile.tileType, linkedApp?.category) {
        linkedApp?.category?.shortLabel ?: when (tile.tileType) {
            TileType.CLOCK_CALENDAR -> "Thời gian"
            TileType.BATTERY, TileType.PERFORMANCE -> "Hệ thống"
            TileType.PHOTOS -> "Ảnh & Media"
            TileType.NOTES -> "Ghi chú"
            TileType.WEATHER -> "Tiện ích"
            TileType.NEWS_QUOTE -> "Tin tức"
            else -> "Ứng dụng"
        }
    }

    val liveSubtitle = remember(
        tile.tileType,
        tile.notificationCount,
        linkedApp?.label,
        linkedApp?.packageName,
        telemetry.batteryPct,
        telemetry.availableRamMb,
        telemetry.ramUsagePct
    ) {
        when {
            tile.notificationCount > 0 -> "${tile.notificationCount} hoạt động mới • ${linkedApp?.label ?: displayTitle}"
            tile.tileType == TileType.BATTERY ->
                "Pin ${telemetry.batteryPct}% (${telemetry.batteryTempC}°C) • ${if (telemetry.isCharging) "Đang sạc" else "Sẵn sàng"}"
            tile.tileType == TileType.PERFORMANCE ->
                "RAM trống ${telemetry.availableRamMb} MB (${100 - telemetry.ramUsagePct}%) • ${telemetry.backgroundAppsCount} app ngầm"
            linkedApp != null -> "${linkedApp.label} • ${linkedApp.category.label}"
            else -> "Chạm để mở ứng dụng"
        }
    }

    val textShadow = if (isHighlyTransparent) {
        Shadow(
            color = Color.Black.copy(alpha = 0.75f),
            offset = Offset(0f, 1.5f),
            blurRadius = 4f
        )
    } else null

    val fallbackIcon = remember(tile.customIconName, tile.title, tile.tileType) {
        when (tile.tileType) {
            TileType.CLOCK_CALENDAR -> Icons.Default.CalendarMonth
            TileType.BATTERY -> Icons.Default.BatteryFull
            TileType.PERFORMANCE -> Icons.Default.Speed
            TileType.WEATHER -> Icons.Default.WbSunny
            TileType.PHOTOS -> Icons.Default.PhotoLibrary
            TileType.NOTES -> Icons.Default.Notes
            else -> resolveIconForTile(tile.customIconName, displayTitle)
        }
    }

    when (tile.size) {
        TileSize.SMALL -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                    )
                } else {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = displayTitle,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        TileSize.MEDIUM -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Top-right subtle app category or live metric tag
                if (tile.tileType == TileType.BATTERY || tile.tileType == TileType.PERFORMANCE) {
                    Text(
                        text = if (tile.tileType == TileType.BATTERY) "${telemetry.batteryPct}%" else "${telemetry.ramUsagePct}% RAM",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                } else if (linkedApp != null) {
                    Text(
                        text = categoryBadge,
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }

                // Center Real App Icon
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = displayTitle,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(38.dp)
                    )
                }

                // Bottom-left App Title
                Text(
                    text = displayTitle,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = textShadow),
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        TileSize.WIDE, TileSize.LARGE -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Real App Icon + App Name + Category Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (appIconBitmap != null) {
                            Image(
                                bitmap = appIconBitmap,
                                contentDescription = displayTitle,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(if (tile.size == TileSize.LARGE) 44.dp else 34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                            )
                        } else {
                            Icon(
                                imageVector = fallbackIcon,
                                contentDescription = displayTitle,
                                tint = Color.White,
                                modifier = Modifier.size(if (tile.size == TileSize.LARGE) 38.dp else 30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = displayTitle,
                                color = Color.White,
                                fontSize = if (tile.size == TileSize.LARGE) 16.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(shadow = textShadow)
                            )
                            Text(
                                text = linkedApp?.packageName?.takeIf { it.isNotBlank() } ?: categoryBadge,
                                color = Color.White.copy(alpha = 0.68f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = categoryBadge,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Body / Footer: App-driven live status or system telemetry bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (tile.tileType == TileType.BATTERY || tile.tileType == TileType.PERFORMANCE) {
                        val progress = if (tile.tileType == TileType.BATTERY) {
                            (telemetry.batteryPct / 100f).coerceIn(0f, 1f)
                        } else {
                            (telemetry.ramUsagePct / 100f).coerceIn(0f, 1f)
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }

                    Text(
                        text = liveSubtitle,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = if (tile.size == TileSize.LARGE) 13.sp else 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = if (tile.size == TileSize.LARGE) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = textShadow)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetroDigitalClockTile(size: TileSize, isHighlyTransparent: Boolean) {
    val clockSizeSp = when (size) {
        TileSize.SMALL -> 21f
        TileSize.MEDIUM -> 32f
        TileSize.WIDE -> 42f
        TileSize.LARGE -> 56f
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(if (size == TileSize.SMALL) 5.dp else 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AndroidView(
            factory = { context ->
                TextClock(context).apply {
                    setTextColor(android.graphics.Color.WHITE)
                    gravity = Gravity.CENTER_VERTICAL
                    includeFontPadding = false
                    isClickable = false
                }
            },
            update = { clock ->
                clock.textSize = clockSizeSp
                if (isHighlyTransparent) {
                    clock.setShadowLayer(4f, 0f, 1.5f, android.graphics.Color.BLACK)
                } else {
                    clock.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        if (size != TileSize.SMALL) {
            Text(
                text = "Đồng hồ",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

fun resolveIconForTile(customIcon: String, title: String): ImageVector {
    val key = (customIcon.ifEmpty { title }).lowercase(Locale.getDefault())
    return when {
        key.contains("phone") || key.contains("thoại") || key.contains("gọi") -> Icons.Default.Phone
        key.contains("message") || key.contains("nhắn") || key.contains("sms") -> Icons.Default.Message
        key.contains("camera") || key.contains("chụp") -> Icons.Default.PhotoCamera
        key.contains("photo") || key.contains("ảnh") || key.contains("gallery") -> Icons.Default.PhotoLibrary
        key.contains("web") || key.contains("edge") || key.contains("chrome") || key.contains("browser") -> Icons.Default.Language
        key.contains("setting") || key.contains("cài đặt") -> Icons.Default.Settings
        key.contains("store") || key.contains("cửa hàng") || key.contains("play") -> Icons.Default.ShoppingBag
        key.contains("music") || key.contains("nhạc") -> Icons.Default.MusicNote
        key.contains("note") || key.contains("ghi chú") -> Icons.Default.Notes
        key.contains("record") || key.contains("ghi âm") -> Icons.Default.Mic
        key.contains("map") || key.contains("bản đồ") || key.contains("định vị") -> Icons.Default.Navigation
        key.contains("file") || key.contains("tập tin") -> Icons.Default.Folder
        else -> Icons.Default.ShoppingBag
    }
}
