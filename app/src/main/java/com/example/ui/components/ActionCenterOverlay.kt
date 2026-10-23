package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.BatterySaverMode
import com.example.data.model.LauncherSettings
import com.example.data.model.SystemTelemetry
import com.example.data.model.Wp10TileStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class QuickActionTileModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isActive: Boolean,
    val tag: String,
    val onClick: () -> Unit
)

/**
 * Windows 10 Mobile / Windows 11 Fluent Action Center & Quick Settings Panel.
 * Features a dynamic wallpaper-adaptive Glassmorphism (Acrylic + Mica) surface.
 */
@Composable
fun ActionCenterOverlay(
    settings: LauncherSettings,
    telemetry: SystemTelemetry,
    customWallpaperBitmap: ImageBitmap? = null,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onOptimizeMemory: () -> SystemTelemetry,
    onOpenGlance: () -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val glassStyle = rememberAdaptiveGlassStyle(
        settings = settings,
        customWallpaperBitmap = customWallpaperBitmap
    )

    var lastOptimizationFeedback by remember { mutableStateOf<String?>(null) }

    val timeString = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    val dateString = remember {
        SimpleDateFormat("EEEE, dd/MM", Locale.getDefault()).format(Date())
    }

    val isBatterySaverActive = settings.isBatterySaverActive(telemetry)

    val quickActions = remember(
        settings,
        telemetry,
        isBatterySaverActive
    ) {
        listOf(
            QuickActionTileModel(
                id = "wifi",
                title = "Mạng & Wi-Fi",
                subtitle = "Mở cài đặt Wi-Fi",
                icon = Icons.Default.Wifi,
                isActive = false,
                tag = "action_center_tile_wifi",
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {}
                }
            ),
            QuickActionTileModel(
                id = "bluetooth",
                title = "Bluetooth",
                subtitle = "Mở cài đặt Bluetooth",
                icon = Icons.Default.Bluetooth,
                isActive = false,
                tag = "action_center_tile_bluetooth",
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {}
                }
            ),
            QuickActionTileModel(
                id = "battery_saver",
                title = "Tiết kiệm Pin",
                subtitle = settings.batterySaverMode.shortLabel,
                icon = Icons.Default.BatterySaver,
                isActive = isBatterySaverActive,
                tag = "action_center_tile_battery_saver",
                onClick = {
                    val nextMode = when (settings.batterySaverMode) {
                        BatterySaverMode.AUTO -> BatterySaverMode.ALWAYS_ON
                        BatterySaverMode.ALWAYS_ON -> BatterySaverMode.OFF
                        BatterySaverMode.OFF -> BatterySaverMode.AUTO
                    }
                    onUpdateSettings(settings.copy(batterySaverMode = nextMode))
                }
            ),
            QuickActionTileModel(
                id = "optimize_ram",
                title = "Xóa bộ nhớ đệm",
                subtitle = "Chỉ dọn dữ liệu tạm của launcher",
                icon = Icons.Default.CleaningServices,
                isActive = false,
                tag = "action_center_tile_optimize_ram",
                onClick = {
                    onOptimizeMemory()
                    lastOptimizationFeedback = "Đã xóa bộ nhớ đệm của launcher"
                }
            ),
            QuickActionTileModel(
                id = "tile_transparency",
                title = "Kiểu ô",
                subtitle = when (settings.wp10TileStyle) {
                    Wp10TileStyle.CRYSTAL_CLEAR -> "Trong suốt 100%"
                    Wp10TileStyle.FLUENT_ACRYLIC -> "Kính Acrylic"
                    Wp10TileStyle.TINTED_TRANSLUCENT -> "Nhuộm màu"
                    Wp10TileStyle.CLASSIC_METRO -> "Đặc Metro"
                },
                icon = Icons.Default.BlurOn,
                isActive = settings.wp10TileStyle != Wp10TileStyle.CLASSIC_METRO,
                tag = "action_center_tile_transparency",
                onClick = {
                    val nextStyle = settings.wp10TileStyle.next()
                    val nextOpacity = when (nextStyle) {
                        Wp10TileStyle.CRYSTAL_CLEAR -> 0f
                        Wp10TileStyle.CLASSIC_METRO -> 1f
                        Wp10TileStyle.FLUENT_ACRYLIC -> 0.38f
                        Wp10TileStyle.TINTED_TRANSLUCENT -> 0.42f
                    }
                    onUpdateSettings(settings.copy(wp10TileStyle = nextStyle, tileOpacity = nextOpacity))
                }
            ),
            QuickActionTileModel(
                id = "glance",
                title = "Xem nhanh",
                subtitle = "Giờ và trạng thái pin",
                icon = Icons.Default.NightlightRound,
                isActive = settings.glanceScreenEnabled,
                tag = "action_center_tile_glance",
                onClick = {
                    onDismiss()
                    onOpenGlance()
                }
            ),
            QuickActionTileModel(
                id = "lock_layout",
                title = "Khóa Bố cục",
                subtitle = if (settings.lockHomeLayout) "Đang khóa" else "Mở khóa",
                icon = if (settings.lockHomeLayout) Icons.Default.Lock else Icons.Default.LockOpen,
                isActive = settings.lockHomeLayout,
                tag = "action_center_tile_lock_layout",
                onClick = {
                    onUpdateSettings(settings.copy(lockHomeLayout = !settings.lockHomeLayout))
                }
            ),
            QuickActionTileModel(
                id = "all_settings",
                title = "Cài đặt",
                subtitle = "Tùy chỉnh Lumia Launcher",
                icon = Icons.Default.Settings,
                isActive = true,
                tag = "action_center_tile_all_settings",
                onClick = {
                    onDismiss()
                    onOpenFullSettings()
                }
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(55f)
            .background(Color.Black.copy(alpha = 0.24f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        FluentGlassPanel(
            glassStyle = glassStyle,
            customWallpaperBitmap = customWallpaperBitmap,
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            blurRadius = 32.dp,
            borderWidth = 1.2.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .shadow(28.dp, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume taps inside Action Center */ }
                .testTag("action_center_overlay")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top Header: Clock + Adaptive Wallpaper Glass Indicator + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = timeString,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Light
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateString,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = glassStyle.primaryTint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Action Center • Kính Acrylic đồng bộ: ${glassStyle.sourceLabel}",
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("action_center_glass_source_label")
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .testTag("close_action_center_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng Action Center",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Windows 11 / W10M Quick Action Glass Tiles (4x2 Grid)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    items(quickActions, key = { it.id }) { item ->
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (item.isActive) {
                                        Modifier.background(glassStyle.activeTileGlassBrush)
                                    } else {
                                        Modifier.background(glassStyle.cardBackgroundBrush)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    brush = if (item.isActive) glassStyle.specularBorderBrush else glassStyle.cardBorderBrush,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { item.onClick() }
                                .padding(horizontal = 8.dp, vertical = 9.dp)
                                .testTag(item.tag),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.subtitle,
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Glassmorphic Sliders Card (Tile Acrylic Transparency & Wallpaper Dim)
                FluentGlassCard(glassStyle = glassStyle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = glassStyle.primaryTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Độ đặc Ô Tile",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(92.dp)
                        )
                        Slider(
                            value = settings.tileOpacity,
                            onValueChange = { newOpacity ->
                                val updatedStyle = when {
                                    newOpacity <= 0.03f -> Wp10TileStyle.CRYSTAL_CLEAR
                                    newOpacity >= 0.97f -> Wp10TileStyle.CLASSIC_METRO
                                    settings.wp10TileStyle == Wp10TileStyle.CLASSIC_METRO -> Wp10TileStyle.FLUENT_ACRYLIC
                                    else -> settings.wp10TileStyle
                                }
                                onUpdateSettings(
                                    settings.copy(
                                        tileOpacity = newOpacity,
                                        wp10TileStyle = updatedStyle
                                    )
                                )
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = glassStyle.primaryTint,
                                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .testTag("action_center_tile_opacity_slider")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${(settings.tileOpacity * 100).roundToInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = null,
                            tint = glassStyle.primaryTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Độ tối Hình nền",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(92.dp)
                        )
                        Slider(
                            value = settings.wallpaperDimAlpha,
                            onValueChange = { newDim ->
                                onUpdateSettings(settings.copy(wallpaperDimAlpha = newDim))
                            },
                            valueRange = 0f..0.70f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = glassStyle.primaryTint,
                                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .testTag("action_center_wallpaper_dim_slider")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${(settings.wallpaperDimAlpha * 100).roundToInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // System Resource & Background Process Monitor Card
                FluentGlassCard(glassStyle = glassStyle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tối ưu Pin & Ứng dụng chạy ngầm",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pin ${telemetry.batteryPct}% (${telemetry.batteryTempC}°C) • RAM trống ${telemetry.availableRamMb} MB",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 10.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(glassStyle.activeTileGlassBrush)
                                .clickable {
                                    val fresh = onOptimizeMemory()
                                    lastOptimizationFeedback =
                                        "Đã đóng băng ${fresh.lastStoppedAppsCount} app ngầm • +${fresh.lastFreedRamMb} MB RAM"
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("action_center_hibernate_button")
                        ) {
                            Text(
                                text = "Dọn ngay (${telemetry.backgroundAppsCount})",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (telemetry.ramUsagePct / 100f).coerceIn(0.05f, 1f) },
                        color = glassStyle.primaryTint,
                        trackColor = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    AnimatedVisibility(visible = lastOptimizationFeedback != null) {
                        Text(
                            text = lastOptimizationFeedback.orEmpty(),
                            color = Color(0xFF81C784),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom drag pill indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
        }
    }
}
