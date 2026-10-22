package com.example.ui.components

import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AllAppsSortOrder
import com.example.data.model.AllAppsViewMode
import com.example.data.model.DynamicAccentMode
import com.example.data.model.LauncherSettings
import com.example.data.model.PageTransitionEffect
import com.example.data.model.SystemTelemetry
import com.example.data.model.Wp10TileStyle
import com.example.ui.theme.DynamicAccentEngine
import com.example.ui.theme.LumiaPalette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: LauncherSettings,
    telemetry: SystemTelemetry,
    pagesCount: Int = 2,
    onSaveSettings: (LauncherSettings) -> Unit,
    onOpenPageManager: () -> Unit = {},
    onExportBackup: suspend () -> String,
    onRestoreBackup: suspend (String) -> Boolean,
    onResetDefaults: () -> Unit,
    onOptimizeMemory: () -> SystemTelemetry = { telemetry },
    onRefreshTelemetry: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedAccentHex by remember { mutableStateOf(settings.accentColorHex) }
    var autoWallpaperAccent by remember { mutableStateOf(settings.autoWallpaperAccentEnabled) }
    var dynamicAccentMode by remember { mutableStateOf(settings.dynamicAccentMode) }
    var dynamicSwatchIndex by remember { mutableIntStateOf(settings.dynamicSwatchIndex) }
    var isDarkTheme by remember { mutableStateOf(settings.isDarkTheme) }
    var tileOpacity by remember { mutableFloatStateOf(settings.tileOpacity) }
    var wp10TileStyle by remember { mutableStateOf(settings.wp10TileStyle) }
    var wp10RevealBorder by remember { mutableStateOf(settings.wp10RevealBorder) }
    var showMoreTiles by remember { mutableStateOf(settings.showMoreTiles) }
    var tileCornerRadius by remember { mutableIntStateOf(settings.tileCornerRadiusDp) }
    var tileSpacingDp by remember { mutableIntStateOf(settings.tileSpacingDp) }
    var useRealAppIcons by remember { mutableStateOf(settings.useRealAppIcons) }
    var miuiGlassBorder by remember { mutableStateOf(settings.miuiGlassBorder) }
    var pageTransition by remember { mutableStateOf(settings.pageTransitionEffect) }
    var doubleTapToGlance by remember { mutableStateOf(settings.doubleTapToGlance) }
    var lockHomeLayout by remember { mutableStateOf(settings.lockHomeLayout) }
    var wallpaperIndex by remember { mutableIntStateOf(settings.wallpaperGradientIndex) }
    var turnstileAnim by remember { mutableStateOf(settings.turnstileAnimationEnabled) }
    var glanceScreen by remember { mutableStateOf(settings.glanceScreenEnabled) }
    var showPageHeaderTitle by remember { mutableStateOf(settings.showPageHeaderTitle) }
    var allAppsViewMode by remember { mutableStateOf(settings.allAppsViewMode) }
    var allAppsGridColumns by remember { mutableIntStateOf(settings.allAppsGridColumns) }
    var allAppsSortOrder by remember { mutableStateOf(settings.allAppsSortOrder) }
    var allAppsShowSearchBar by remember { mutableStateOf(settings.allAppsShowSearchBar) }
    var allAppsShowLetterHeaders by remember { mutableStateOf(settings.allAppsShowLetterHeaders) }
    var allAppsCategorizedEnabled by remember { mutableStateOf(settings.allAppsCategorizedEnabled) }
    var quickNoteText by remember { mutableStateOf(settings.quickNotes) }

    var expandThemePalette by remember { mutableStateOf(false) }
    var expandAppDrawerOptions by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val dynamicPalette = remember(wallpaperIndex, dynamicAccentMode) {
        DynamicAccentEngine.extractPaletteForWallpaper(wallpaperIndex, dynamicAccentMode)
    }
    val effectiveAccentHex = if (autoWallpaperAccent) {
        dynamicPalette.getSwatch(dynamicSwatchIndex).hex
    } else {
        selectedAccentHex
    }
    val accent = LumiaPalette.getAccent(effectiveAccentHex)
    val currentWallpaper = LumiaPalette.Wallpapers.getOrNull(wallpaperIndex) ?: LumiaPalette.Wallpapers.first()

    val homeRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshTelemetry()
    }

    fun openSystemDefaultLauncherSettings() {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {}
            }
        }
    }

    fun requestSetDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                try {
                    val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    homeRoleLauncher.launch(roleIntent)
                    return
                } catch (_: Exception) {}
            }
        }
        openSystemDefaultLauncherSettings()
    }

    fun applyChanges() {
        val resolvedHex = if (autoWallpaperAccent) {
            DynamicAccentEngine.resolveAutoAccent(wallpaperIndex, dynamicAccentMode, dynamicSwatchIndex).hex
        } else {
            selectedAccentHex
        }
        onSaveSettings(
            settings.copy(
                accentColorHex = resolvedHex,
                autoWallpaperAccentEnabled = autoWallpaperAccent,
                dynamicAccentMode = dynamicAccentMode,
                dynamicSwatchIndex = dynamicSwatchIndex,
                isDarkTheme = isDarkTheme,
                tileOpacity = tileOpacity,
                wp10TileStyle = wp10TileStyle,
                wp10RevealBorder = wp10RevealBorder,
                showMoreTiles = showMoreTiles,
                tileCornerRadiusDp = tileCornerRadius,
                tileSpacingDp = tileSpacingDp,
                useRealAppIcons = useRealAppIcons,
                miuiGlassBorder = miuiGlassBorder,
                pageTransitionEffect = pageTransition,
                doubleTapToGlance = doubleTapToGlance,
                lockHomeLayout = lockHomeLayout,
                wallpaperGradientIndex = wallpaperIndex,
                turnstileAnimationEnabled = turnstileAnim,
                glanceScreenEnabled = glanceScreen,
                showPageHeaderTitle = showPageHeaderTitle,
                allAppsViewMode = allAppsViewMode,
                allAppsGridColumns = allAppsGridColumns,
                allAppsSortOrder = allAppsSortOrder,
                allAppsShowSearchBar = allAppsShowSearchBar,
                allAppsShowLetterHeaders = allAppsShowLetterHeaders,
                allAppsCategorizedEnabled = allAppsCategorizedEnabled,
                quickNotes = quickNoteText
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF000000),
        contentColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
        ) {
            // Samsung One UI Top App Bar ("Cài đặt Màn hình chờ")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            applyChanges()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Cài đặt Màn hình chờ",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Giao diện Tile Windows 10 Mobile • Tự động lưu mọi thay đổi",
                            color = Color(0xFF9E9EA8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // One UI Visual Preview Banner (Live Tile Home vs App Screen)
                item {
                    OneUiVisualPreviewCard(
                        accentColor = accent.color,
                        wallpaperBrush = currentWallpaper.second,
                        wp10TileStyle = wp10TileStyle,
                        tileOpacity = tileOpacity,
                        showMoreTiles = showMoreTiles,
                        tileCornerRadius = tileCornerRadius,
                        allAppsViewMode = allAppsViewMode
                    )
                }

                // Group 1: Default Home App (Ứng dụng Màn hình chờ mặc định)
                item {
                    OneUiSectionLabel("TRÌNH KHỞI CHẠY HỆ THỐNG")
                    Spacer(modifier = Modifier.height(6.dp))
                    OneUiGroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (telemetry.isDefaultLauncher) Color(0xFF107C41)
                                            else accent.color
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (telemetry.isDefaultLauncher) Icons.Default.CheckCircle else Icons.Default.Home,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Ứng dụng Màn hình chờ mặc định",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (telemetry.isDefaultLauncher)
                                            "Đang sử dụng Lumia Launcher làm mặc định"
                                        else
                                            "Hiện tại: ${telemetry.currentDefaultLauncherName}",
                                        color = if (telemetry.isDefaultLauncher) Color(0xFF4CAF50) else accent.color,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { requestSetDefaultLauncher() },
                                    modifier = Modifier.testTag("set_default_launcher_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (telemetry.isDefaultLauncher) Color(0xFF2B2B33) else accent.color
                                    )
                                ) {
                                    Text(
                                        text = if (telemetry.isDefaultLauncher) "Thay đổi" else "Đặt mặc định",
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(
                                    onClick = { openSystemDefaultLauncherSettings() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("open_home_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Mở cài đặt hệ thống",
                                        tint = Color(0xFF9E9EA8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Group 2: One UI Home Screen Layout & Grid (Bố cục và Lưới Màn hình chờ)
                item {
                    OneUiSectionLabel("BỐ CỤC & LƯỚI MÀN HÌNH CHỜ")
                    Spacer(modifier = Modifier.height(6.dp))
                    OneUiGroupCard {
                        // Row 1: Bố cục Màn hình chờ & Trang Tất cả ứng dụng
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandAppDrawerOptions = !expandAppDrawerOptions }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bố cục Màn hình chờ",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Trang Tile & ${allAppsViewMode.label}",
                                        color = accent.color,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = if (expandAppDrawerOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color(0xFF9E9EA8)
                                )
                            }

                            AnimatedVisibility(visible = expandAppDrawerOptions) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Kiểu hiển thị Trang Tất cả ứng dụng:",
                                        color = Color(0xFF9E9EA8),
                                        fontSize = 12.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AllAppsViewMode.entries.forEach { mode ->
                                            val selected = allAppsViewMode == mode
                                            OneUiChoicePill(
                                                label = mode.label,
                                                selected = selected,
                                                accentColor = accent.color,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    allAppsViewMode = mode
                                                    applyChanges()
                                                }
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Thứ tự sắp xếp danh sách ứng dụng:",
                                        color = Color(0xFF9E9EA8),
                                        fontSize = 12.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AllAppsSortOrder.entries.forEach { order ->
                                            val selected = allAppsSortOrder == order
                                            OneUiChoicePill(
                                                label = order.label,
                                                selected = selected,
                                                accentColor = accent.color,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    allAppsSortOrder = order
                                                    applyChanges()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OneUiDivider()

                        // Row 2: Lưới Màn hình chờ (4 cột vs 6 cột)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Lưới Màn hình chờ (Kích thước lưới Tile)",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (showMoreTiles) "6 cột (Mở rộng W10M / One UI)" else "4 cột (Tiêu chuẩn Lumia)",
                                        color = accent.color,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OneUiChoicePill(
                                    label = "4 cột (Chuẩn Lumia)",
                                    selected = !showMoreTiles,
                                    accentColor = accent.color,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showMoreTiles = false
                                        applyChanges()
                                    }
                                )
                                OneUiChoicePill(
                                    label = "6 cột (Nhiều ô hơn)",
                                    selected = showMoreTiles,
                                    accentColor = accent.color,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showMoreTiles = true
                                        applyChanges()
                                    }
                                )
                            }
                        }

                        OneUiDivider()

                        // Row 3: Lưới Màn hình ứng dụng (khi dùng chế độ Lưới biểu tượng One UI)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Lưới Màn hình ứng dụng",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${allAppsGridColumns}x5 (Áp dụng khi bật chế độ Lưới biểu tượng)",
                                        color = accent.color,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(4, 5).forEach { cols ->
                                        OneUiChoicePill(
                                            label = "${cols}x5",
                                            selected = allAppsGridColumns == cols,
                                            accentColor = accent.color,
                                            onClick = {
                                                allAppsGridColumns = cols
                                                applyChanges()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OneUiDivider()

                        // Row 4: Khóa bố cục Màn hình chờ
                        OneUiSwitchRow(
                            title = "Khóa bố cục Màn hình chờ",
                            subtitle = "Ngăn các ô Tile trên Màn hình chờ bị gỡ bỏ, đổi cỡ hoặc di chuyển.",
                            checked = lockHomeLayout,
                            accentColor = accent.color,
                            onCheckedChange = {
                                lockHomeLayout = it
                                applyChanges()
                            }
                        )
                    }
                }

                // Group 3: Giao diện Ô Tile Trong suốt Windows 10 Mobile & Chủ đề
                item {
                    OneUiSectionLabel("GIAO DIỆN Ô TILE TRONG SUỐT & CHỦ ĐỀ")
                    Spacer(modifier = Modifier.height(6.dp))
                    OneUiGroupCard {
                        // Windows 10 Mobile Tile Style selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Kiểu trong suốt ô Tile (Windows 10 Mobile)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = wp10TileStyle.label,
                                color = accent.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Wp10TileStyle.entries.forEach { style ->
                                    val selected = wp10TileStyle == style
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (selected) accent.color.copy(alpha = 0.2f)
                                                else Color(0xFF23232A)
                                            )
                                            .border(
                                                width = if (selected) 1.5.dp else 0.dp,
                                                color = if (selected) accent.color else Color.Transparent,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                wp10TileStyle = style
                                                if (style == Wp10TileStyle.CRYSTAL_CLEAR) {
                                                    tileOpacity = 0f
                                                } else if (style == Wp10TileStyle.CLASSIC_METRO) {
                                                    tileOpacity = 1f
                                                } else if (tileOpacity == 0f || tileOpacity == 1f) {
                                                    tileOpacity = 0.35f
                                                }
                                                applyChanges()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                            .testTag("wp10_tile_style_${style.name.lowercase()}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = style.label,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = style.description,
                                                color = Color(0xFF9E9EA8),
                                                fontSize = 11.sp
                                            )
                                        }
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = accent.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OneUiDivider()

                        // Transparency Slider (0%..100%)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            val transparencyPct = ((1f - tileOpacity) * 100).roundToInt().coerceIn(0, 100)
                            val opacityPct = (tileOpacity * 100).roundToInt().coerceIn(0, 100)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Độ trong suốt nền ô Tile",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$transparencyPct% trong suốt ($opacityPct% đậm)",
                                    color = accent.color,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Slider(
                                value = 1f - tileOpacity,
                                onValueChange = { transparencyVal ->
                                    tileOpacity = (1f - transparencyVal).coerceIn(0f, 1f)
                                    wp10TileStyle = when {
                                        tileOpacity <= 0.02f -> Wp10TileStyle.CRYSTAL_CLEAR
                                        tileOpacity >= 0.98f && wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR -> Wp10TileStyle.TINTED_TRANSLUCENT
                                        wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR -> Wp10TileStyle.TINTED_TRANSLUCENT
                                        wp10TileStyle == Wp10TileStyle.CLASSIC_METRO -> Wp10TileStyle.TINTED_TRANSLUCENT
                                        else -> wp10TileStyle
                                    }
                                    applyChanges()
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = accent.color,
                                    activeTrackColor = accent.color,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    1.0f to "0% Đặc",
                                    0.65f to "35% Vừa",
                                    0.35f to "65% W10M",
                                    0.15f to "85% Mỏng",
                                    0.0f to "100% Trong"
                                ).forEach { (opVal, chipLabel) ->
                                    val isChipSelected = abs(tileOpacity - opVal) < 0.04f
                                    OneUiChoicePill(
                                        label = chipLabel,
                                        selected = isChipSelected,
                                        accentColor = accent.color,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            tileOpacity = opVal
                                            wp10TileStyle = when {
                                                opVal <= 0.01f -> Wp10TileStyle.CRYSTAL_CLEAR
                                                opVal >= 0.99f -> Wp10TileStyle.CLASSIC_METRO
                                                wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR || wp10TileStyle == Wp10TileStyle.CLASSIC_METRO -> Wp10TileStyle.TINTED_TRANSLUCENT
                                                else -> wp10TileStyle
                                            }
                                            applyChanges()
                                        }
                                    )
                                }
                            }
                        }

                        OneUiDivider()

                        // Dynamic Accent Color Engine (Windows 10 Mobile + Material You)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tự động lấy màu chủ đạo từ hình nền",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (autoWallpaperAccent) {
                                            "Đang bật (${dynamicAccentMode.shortLabel} • ${accent.hex})"
                                        } else {
                                            "Đang chọn màu thủ công (${accent.name})"
                                        },
                                        color = accent.color,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Switch(
                                    checked = autoWallpaperAccent,
                                    onCheckedChange = { enabled ->
                                        autoWallpaperAccent = enabled
                                        if (enabled) {
                                            selectedAccentHex = dynamicPalette.getSwatch(dynamicSwatchIndex).hex
                                        }
                                        applyChanges()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = accent.color
                                    ),
                                    modifier = Modifier.testTag("auto_wallpaper_accent_switch")
                                )
                            }

                            AnimatedVisibility(visible = autoWallpaperAccent) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        DynamicAccentMode.entries.forEach { mode ->
                                            val isSelected = dynamicAccentMode == mode
                                            OneUiChoicePill(
                                                label = mode.shortLabel,
                                                selected = isSelected,
                                                accentColor = accent.color,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("dynamic_accent_mode_${mode.name.lowercase()}"),
                                                onClick = {
                                                    dynamicAccentMode = mode
                                                    val newPalette = DynamicAccentEngine.extractPaletteForWallpaper(wallpaperIndex, mode)
                                                    selectedAccentHex = newPalette.getSwatch(dynamicSwatchIndex).hex
                                                    applyChanges()
                                                }
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Bảng hạt màu Material You trích xuất từ \"${currentWallpaper.first}\":",
                                        color = Color(0xFF9E9EA8),
                                        fontSize = 11.sp
                                    )

                                    val swatchLabels = listOf("Chủ đạo W10M", "Tonal Dịu", "Hòa sắc", "Metro Đậm")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        dynamicPalette.swatches.forEachIndexed { sIdx, swatch ->
                                            val isPicked = dynamicSwatchIndex == sIdx
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isPicked) swatch.color.copy(alpha = 0.25f)
                                                        else Color(0xFF23232A)
                                                    )
                                                    .border(
                                                        width = if (isPicked) 1.5.dp else 0.8.dp,
                                                        color = if (isPicked) swatch.color else Color.White.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        dynamicSwatchIndex = sIdx
                                                        selectedAccentHex = swatch.hex
                                                        applyChanges()
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                                    .testTag("dynamic_wallpaper_swatch_$sIdx"),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(swatch.color)
                                                        .border(
                                                            width = if (isPicked) 2.dp else 1.dp,
                                                            color = if (isPicked) Color.White else Color.White.copy(alpha = 0.35f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isPicked) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = swatchLabels.getOrElse(sIdx) { "Tonal" },
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OneUiDivider()

                        // Bảng màu & Hình nền (Expandable One UI style)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandThemePalette = !expandThemePalette }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bảng màu chủ đề & Hình nền",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${accent.name} • ${currentWallpaper.first}",
                                        color = accent.color,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(accent.color)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = if (expandThemePalette) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF9E9EA8)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = expandThemePalette) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Chọn màu chủ đề thủ công (Accent):",
                                        color = Color(0xFF9E9EA8),
                                        fontSize = 12.sp
                                    )
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(6),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(108.dp)
                                    ) {
                                        items(LumiaPalette.Accents) { acc ->
                                            val isSelected = !autoWallpaperAccent && acc.hex.equals(selectedAccentHex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(acc.color)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 0.dp,
                                                        color = if (isSelected) Color.White else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        autoWallpaperAccent = false
                                                        selectedAccentHex = acc.hex
                                                        applyChanges()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Chọn hình nền Màn hình chờ (Tự động trích xuất màu khi đổi):",
                                        color = Color(0xFF9E9EA8),
                                        fontSize = 12.sp
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        LumiaPalette.Wallpapers.forEachIndexed { idx, pair ->
                                            val isSelected = idx == wallpaperIndex
                                            val previewPalette = remember(idx, dynamicAccentMode) {
                                                DynamicAccentEngine.extractPaletteForWallpaper(idx, dynamicAccentMode)
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isSelected) accent.color.copy(alpha = 0.2f)
                                                        else Color(0xFF23232A)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.dp,
                                                        color = if (isSelected) accent.color else Color.Transparent,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        wallpaperIndex = idx
                                                        if (autoWallpaperAccent) {
                                                            selectedAccentHex = previewPalette.getSwatch(dynamicSwatchIndex).hex
                                                        }
                                                        applyChanges()
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                                    .testTag("wallpaper_option_$idx"),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(pair.second)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = pair.first,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    previewPalette.swatches.take(3).forEach { sw ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(sw.color)
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = accent.color
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OneUiDivider()

                        // Corner Radius & Tile Spacing
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Độ bo góc & Khoảng cách ô Tile",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Bo ${tileCornerRadius}dp • Cách ${tileSpacingDp}dp",
                                    color = accent.color,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Slider(
                                value = tileCornerRadius.toFloat(),
                                onValueChange = {
                                    tileCornerRadius = it.toInt()
                                    applyChanges()
                                },
                                valueRange = 0f..18f,
                                steps = 17,
                                colors = SliderDefaults.colors(
                                    thumbColor = accent.color,
                                    activeTrackColor = accent.color,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    4 to "Sát cạnh (4dp)",
                                    6 to "Chuẩn W10M (6dp)",
                                    8 to "Lumia (8dp)",
                                    12 to "One UI (12dp)"
                                ).forEach { (dpVal, label) ->
                                    OneUiChoicePill(
                                        label = label,
                                        selected = tileSpacingDp == dpVal,
                                        accentColor = accent.color,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            tileSpacingDp = dpVal
                                            applyChanges()
                                        }
                                    )
                                }
                            }
                        }

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Viền sáng Fluent Reveal (Windows 10 Mobile)",
                            subtitle = "Hiển thị viền ánh sáng tinh tế cho các ô Tile trong suốt.",
                            checked = wp10RevealBorder,
                            accentColor = accent.color,
                            onCheckedChange = {
                                wp10RevealBorder = it
                                applyChanges()
                            }
                        )

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Hiển thị biểu tượng ứng dụng gốc",
                            subtitle = "Dùng biểu tượng thật của ứng dụng Android trên ô Tile.",
                            checked = useRealAppIcons,
                            accentColor = accent.color,
                            onCheckedChange = {
                                useRealAppIcons = it
                                applyChanges()
                            }
                        )

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Viền kính nổi One UI / HyperOS",
                            subtitle = "Tăng độ tương phản cạnh cho tất cả các ô Live Tile.",
                            checked = miuiGlassBorder,
                            accentColor = accent.color,
                            onCheckedChange = {
                                miuiGlassBorder = it
                                applyChanges()
                            }
                        )
                    }
                }

                // Group 4: Hiệu ứng chuyển cảnh & Cử chỉ thông minh (One UI Gestures & Transitions)
                item {
                    OneUiSectionLabel("CHUYỂN CẢNH & CỬ CHỈ MÀN HÌNH CHỜ")
                    Spacer(modifier = Modifier.height(6.dp))
                    OneUiGroupCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Hiệu ứng chuyển trang",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = pageTransition.label,
                                color = accent.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PageTransitionEffect.entries.forEach { effect ->
                                    OneUiChoicePill(
                                        label = effect.label,
                                        selected = pageTransition == effect,
                                        accentColor = accent.color,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            pageTransition = effect
                                            applyChanges()
                                        }
                                    )
                                }
                            }
                        }

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Hiệu ứng nhấn nghiêng 3D (Metro Tilt)",
                            subtitle = "Phản hồi lún đàn hồi chuẩn Windows Phone khi chạm vào ô Tile.",
                            checked = turnstileAnim,
                            accentColor = accent.color,
                            onCheckedChange = {
                                turnstileAnim = it
                                applyChanges()
                            }
                        )

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Chạm đúp vào vùng trống để bật Glance",
                            subtitle = "Chạm hai lần vào khoảng trống trên Màn hình chờ để mở đồng hồ Glance.",
                            checked = doubleTapToGlance,
                            accentColor = accent.color,
                            onCheckedChange = {
                                doubleTapToGlance = it
                                applyChanges()
                            }
                        )

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Màn hình chờ Nokia Glance Screen",
                            subtitle = "Hiển thị đồng hồ đêm AMOLED tiết kiệm pin và thông tin nhanh.",
                            checked = glanceScreen,
                            accentColor = accent.color,
                            onCheckedChange = {
                                glanceScreen = it
                                applyChanges()
                            }
                        )

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Hiển thị thanh tìm kiếm ở Trang ứng dụng",
                            subtitle = "Tìm nhanh ứng dụng trên đầu trang Tất cả ứng dụng.",
                            checked = allAppsShowSearchBar,
                            accentColor = accent.color,
                            onCheckedChange = {
                                allAppsShowSearchBar = it
                                applyChanges()
                            }
                        )

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Hiển thị chữ cái phân nhóm A-Z",
                            subtitle = "Chạm vào chữ cái để nhảy nhanh theo bảng chữ cái kiểu Lumia.",
                            checked = allAppsShowLetterHeaders,
                            accentColor = accent.color,
                            onCheckedChange = {
                                allAppsShowLetterHeaders = it
                                applyChanges()
                            }
                        )

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Thanh bộ lọc phân loại ứng dụng",
                            subtitle = "Hiển thị bộ lọc phân loại (Liên lạc, Giải trí, Trò chơi, Công cụ...) trên Trang ứng dụng.",
                            checked = allAppsCategorizedEnabled,
                            accentColor = accent.color,
                            onCheckedChange = {
                                allAppsCategorizedEnabled = it
                                applyChanges()
                            }
                        )

                        OneUiDivider()

                        OneUiSwitchRow(
                            title = "Hiển thị tiêu đề tên trang trên Màn hình chờ",
                            subtitle = "Trang trống luôn tự động ẩn tiêu đề để giữ nguyên hình nền.",
                            checked = showPageHeaderTitle,
                            accentColor = accent.color,
                            onCheckedChange = {
                                showPageHeaderTitle = it
                                applyChanges()
                            }
                        )
                    }
                }

                // Group 5: Ghi chú nhanh trên Live Tile & Tối ưu thiết bị (Device Care)
                item {
                    OneUiSectionLabel("TIỆN ÍCH GHI CHÚ & TỐI ƯU THIẾT BỊ")
                    Spacer(modifier = Modifier.height(6.dp))
                    OneUiGroupCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nội dung Ghi chú nhanh trên Live Tile",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedTextField(
                                value = quickNoteText,
                                onValueChange = {
                                    quickNoteText = it
                                    applyChanges()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "Nhập nội dung hiển thị trên ô Ghi chú...",
                                        color = Color(0xFF9E9EA8)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = accent.color,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        OneUiDivider()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Chăm sóc thiết bị & Bộ nhớ (Device Care)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            OneUiMetricRow(
                                label = "Bộ nhớ RAM",
                                value = "${telemetry.availableRamMb} MB trống / ${telemetry.totalRamMb} MB (${telemetry.ramUsagePct}%)"
                            )
                            OneUiMetricRow(
                                label = "Bộ nhớ lưu trữ",
                                value = "${telemetry.internalFreeGb} GB trống / ${telemetry.internalTotalGb} GB"
                            )
                            OneUiMetricRow(
                                label = "Trạng thái Pin",
                                value = "${telemetry.batteryPct}% • ${telemetry.batteryTempC}°C" + if (telemetry.isCharging) " (Đang sạc)" else ""
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val updated = onOptimizeMemory()
                                    Toast.makeText(
                                        context,
                                        "Đã tối ưu bộ nhớ! RAM trống: ${updated.availableRamMb} MB",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = accent.color)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tối ưu ngay", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Group 6: Sao lưu, Khôi phục & Đặt lại (Backup & Reset)
                item {
                    OneUiSectionLabel("SAO LƯU, KHÔI PHỤC & ĐẶT LẠI")
                    Spacer(modifier = Modifier.height(6.dp))
                    OneUiGroupCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Sao lưu và khôi phục bố cục Màn hình chờ",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = settings.cloudSyncState,
                                color = accent.color,
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val json = onExportBackup()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("LumiaLauncherBackup", json)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(
                                                context,
                                                "Đã sao chép bản sao lưu JSON vào bộ nhớ tạm!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = accent.color)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sao lưu", fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = { showRestoreDialog = !showRestoreDialog },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Khôi phục", color = Color.White, fontSize = 13.sp)
                                }
                            }

                            if (showRestoreDialog) {
                                OutlinedTextField(
                                    value = restoreJsonInput,
                                    onValueChange = { restoreJsonInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            text = "Dán dữ liệu JSON sao lưu vào đây...",
                                            color = Color(0xFF9E9EA8),
                                            fontSize = 12.sp
                                        )
                                    },
                                    maxLines = 4
                                )
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val success = onRestoreBackup(restoreJsonInput)
                                            if (success) {
                                                Toast.makeText(context, "Đã khôi phục bố cục thành công!", Toast.LENGTH_SHORT).show()
                                                showRestoreDialog = false
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, "Dữ liệu JSON không hợp lệ!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41))
                                ) {
                                    Text("Áp dụng bản khôi phục")
                                }
                            }
                        }

                        OneUiDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onResetDefaults()
                                    Toast.makeText(context, "Đã đặt lại bố cục Màn hình chờ mặc định!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Đặt lại bố cục Màn hình chờ mặc định",
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Khôi phục vị trí các ô Live Tile và trang mặc định ban đầu",
                                    color = Color(0xFF9E9EA8),
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B)
                            )
                        }
                    }
                }

                // Group 7: About Home Screen (Thông tin về Màn hình chờ)
                item {
                    OneUiGroupCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Thông tin về Màn hình chờ",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Lumia Start Home • Phiên bản One UI 9.0 Bridge (W10M 1709)",
                                color = accent.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Hỗ trợ chỉnh sửa riêng từng ô Tile khi nhấn giữ, sắp xếp tự do 2D trên lưới không tự động dồn ô, và quản lý trang chuẩn One UI.",
                                color = Color(0xFF9E9EA8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun OneUiVisualPreviewCard(
    accentColor: Color,
    wallpaperBrush: androidx.compose.ui.graphics.Brush,
    wp10TileStyle: Wp10TileStyle,
    tileOpacity: Float,
    showMoreTiles: Boolean,
    tileCornerRadius: Int,
    allAppsViewMode: AllAppsViewMode
) {
    val fillAlpha = when (wp10TileStyle) {
        Wp10TileStyle.CRYSTAL_CLEAR -> 0.05f
        Wp10TileStyle.CLASSIC_METRO -> 1f
        Wp10TileStyle.FLUENT_ACRYLIC -> (tileOpacity * 0.55f).coerceIn(0.12f, 0.72f)
        Wp10TileStyle.TINTED_TRANSLUCENT -> tileOpacity.coerceIn(0.05f, 0.95f)
    }

    OneUiGroupCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Mini Mockup: Home Screen Live Tiles
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(106.dp)
                        .height(142.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(wallpaperBrush)
                        .border(1.5.dp, accentColor.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(tileCornerRadius.coerceAtMost(8).dp))
                                .background(accentColor.copy(alpha = fillAlpha))
                                .border(
                                    0.6.dp,
                                    Color.White.copy(alpha = 0.35f),
                                    RoundedCornerShape(tileCornerRadius.coerceAtMost(8).dp)
                                )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(if (showMoreTiles) 3 else 2) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp)
                                        .clip(RoundedCornerShape(tileCornerRadius.coerceAtMost(8).dp))
                                        .background(accentColor.copy(alpha = fillAlpha))
                                        .border(
                                            0.6.dp,
                                            Color.White.copy(alpha = 0.35f),
                                            RoundedCornerShape(tileCornerRadius.coerceAtMost(8).dp)
                                        )
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clip(RoundedCornerShape(tileCornerRadius.coerceAtMost(8).dp))
                                .background(accentColor.copy(alpha = fillAlpha))
                                .border(
                                    0.6.dp,
                                    Color.White.copy(alpha = 0.35f),
                                    RoundedCornerShape(tileCornerRadius.coerceAtMost(8).dp)
                                )
                        )
                    }
                }
                Text(
                    text = if (showMoreTiles) "Màn hình chờ (6 cột)" else "Màn hình chờ (4 cột)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Right Mini Mockup: All Apps Page
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(106.dp)
                        .height(142.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(wallpaperBrush)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    if (allAppsViewMode == AllAppsViewMode.METRO_LIST) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(5) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(accentColor)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.White.copy(alpha = 0.65f))
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(4) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    repeat(4) {
                                        Box(
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(accentColor.copy(alpha = 0.85f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    text = if (allAppsViewMode == AllAppsViewMode.METRO_LIST) "Danh sách A-Z" else "Lưới One UI",
                    color = Color(0xFF9E9EA8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun OneUiGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF17171C)),
        content = content
    )
}

@Composable
private fun OneUiSectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF9E9EA8),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun OneUiDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        thickness = 0.5.dp,
        color = Color(0xFF2C2C34)
    )
}

@Composable
private fun OneUiSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFF9E9EA8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color(0xFF9E9EA8),
                uncheckedTrackColor = Color(0xFF2B2B33)
            )
        )
    }
}

@Composable
private fun OneUiChoicePill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor else Color(0xFF23232A))
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Color.White.copy(alpha = 0.35f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFFD0D0D8),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OneUiMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF9E9EA8), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
