package com.example.ui.components

import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.AllAppsSortOrder
import com.example.data.model.AllAppsViewMode
import com.example.data.model.BatterySaverMode
import com.example.data.model.DynamicAccentMode
import com.example.data.model.LauncherSettings
import com.example.data.model.WallpaperSource
import com.example.data.model.PageTransitionEffect
import com.example.data.model.SystemTelemetry
import com.example.data.model.Wp10TileStyle
import com.example.data.remote.CloudBackup
import com.example.ui.theme.DynamicAccentEngine
import com.example.ui.theme.LumiaPalette
import com.example.ui.viewmodel.CloudAuthState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class SettingsCategoryTab(
    val title: String,
    val icon: ImageVector,
    val tag: String
) {
    WALLPAPER_COLOR("Hình nền", Icons.Default.Palette, "settings_tab_wallpaper"),
    TILES_LAYOUT("Màn hình chính", Icons.Default.GridView, "settings_tab_tiles"),
    APP_DRAWER("Ứng dụng", Icons.Default.Tune, "settings_tab_drawer"),
    BATTERY_SYSTEM("Hệ thống", Icons.Default.BatterySaver, "settings_tab_system")
}

private val LocalSettingsGlassStyle = compositionLocalOf<AdaptiveGlassStyle?> { null }

@Composable
fun SettingsSheet(
    settings: LauncherSettings,
    telemetry: SystemTelemetry,
    customWallpaperBitmap: ImageBitmap? = null,
    pagesCount: Int = 2,
    cloudAuthState: CloudAuthState = CloudAuthState.Unauthenticated,
    cloudBackups: List<CloudBackup> = emptyList(),
    isCloudSyncing: Boolean = false,
    onSignInWithGoogle: () -> Unit = {},
    onSignOutCloud: () -> Unit = {},
    onSyncToCloudNow: (String) -> Unit = {},
    onRestoreCloudBackup: (CloudBackup) -> Unit = {},
    onDeleteCloudBackup: (String) -> Unit = {},
    onSaveSettings: (LauncherSettings) -> Unit,
    onPickCustomWallpaper: (Uri) -> Unit = {},
    onClearCustomWallpaper: () -> Unit = {},
    onOpenPageManager: () -> Unit = {},
    onExportBackup: suspend () -> String,
    onRestoreBackup: suspend (String) -> Boolean,
    onResetDefaults: () -> Unit,
    onOptimizeMemory: () -> SystemTelemetry = { telemetry },
    onRefreshTelemetry: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf<SettingsCategoryTab?>(null) }
    BackHandler { if (selectedTab != null) selectedTab = null else onDismiss() }
    var isExpandedHeight by remember { mutableStateOf(false) }

    var selectedAccentHex by remember(settings.accentColorHex) { mutableStateOf(settings.accentColorHex) }
    var autoWallpaperAccent by remember(settings.autoWallpaperAccentEnabled) { mutableStateOf(settings.autoWallpaperAccentEnabled) }
    var dynamicAccentMode by remember(settings.dynamicAccentMode) { mutableStateOf(settings.dynamicAccentMode) }
    var dynamicSwatchIndex by remember(settings.dynamicSwatchIndex) { mutableIntStateOf(settings.dynamicSwatchIndex) }
    var isDarkTheme by remember(settings.isDarkTheme) { mutableStateOf(settings.isDarkTheme) }
    var tileOpacity by remember(settings.tileOpacity) { mutableFloatStateOf(settings.tileOpacity) }
    var wp10TileStyle by remember(settings.wp10TileStyle) { mutableStateOf(settings.wp10TileStyle) }
    var wp10RevealBorder by remember(settings.wp10RevealBorder) { mutableStateOf(settings.wp10RevealBorder) }
    var showMoreTiles by remember(settings.showMoreTiles) { mutableStateOf(settings.showMoreTiles) }
    var tileCornerRadius by remember(settings.tileCornerRadiusDp) { mutableIntStateOf(settings.tileCornerRadiusDp) }
    var tileSpacingDp by remember(settings.tileSpacingDp) { mutableIntStateOf(settings.tileSpacingDp) }
    var useRealAppIcons by remember(settings.useRealAppIcons) { mutableStateOf(settings.useRealAppIcons) }
    var miuiGlassBorder by remember(settings.miuiGlassBorder) { mutableStateOf(settings.miuiGlassBorder) }
    var pageTransition by remember(settings.pageTransitionEffect) { mutableStateOf(settings.pageTransitionEffect) }
    var doubleTapToGlance by remember(settings.doubleTapToGlance) { mutableStateOf(settings.doubleTapToGlance) }
    var lockHomeLayout by remember(settings.lockHomeLayout) { mutableStateOf(settings.lockHomeLayout) }
    var autoArrangeTiles by remember(settings.autoArrangeTiles) { mutableStateOf(settings.autoArrangeTiles) }
    var wallpaperSource by remember(settings.wallpaperSource) { mutableStateOf(settings.wallpaperSource) }
    var wallpaperIndex by remember(settings.wallpaperGradientIndex) { mutableIntStateOf(settings.wallpaperGradientIndex) }
    var wallpaperDimAlpha by remember(settings.wallpaperDimAlpha) { mutableFloatStateOf(settings.wallpaperDimAlpha) }
    var turnstileAnim by remember(settings.turnstileAnimationEnabled) { mutableStateOf(settings.turnstileAnimationEnabled) }
    var glanceScreen by remember(settings.glanceScreenEnabled) { mutableStateOf(settings.glanceScreenEnabled) }
    var batterySaverMode by remember(settings.batterySaverMode) { mutableStateOf(settings.batterySaverMode) }
    var batterySaverThreshold by remember(settings.batterySaverThresholdPct) { mutableIntStateOf(settings.batterySaverThresholdPct) }
    var autoOptimizeBgApps by remember(settings.autoOptimizeBackgroundApps) { mutableStateOf(settings.autoOptimizeBackgroundApps) }
    var showPageHeaderTitle by remember(settings.showPageHeaderTitle) { mutableStateOf(settings.showPageHeaderTitle) }
    var allAppsViewMode by remember(settings.allAppsViewMode) { mutableStateOf(settings.allAppsViewMode) }
    var allAppsGridColumns by remember(settings.allAppsGridColumns) { mutableIntStateOf(settings.allAppsGridColumns) }
    var allAppsSortOrder by remember(settings.allAppsSortOrder) { mutableStateOf(settings.allAppsSortOrder) }
    var allAppsShowSearchBar by remember(settings.allAppsShowSearchBar) { mutableStateOf(settings.allAppsShowSearchBar) }
    var allAppsShowLetterHeaders by remember(settings.allAppsShowLetterHeaders) { mutableStateOf(settings.allAppsShowLetterHeaders) }
    var allAppsCategorizedEnabled by remember(settings.allAppsCategorizedEnabled) { mutableStateOf(settings.allAppsCategorizedEnabled) }
    var quickNoteText by remember(settings.quickNotes) { mutableStateOf(settings.quickNotes) }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var lastOptimizeBanner by remember { mutableStateOf<String?>(null) }

    val dynamicPalette = remember(wallpaperIndex, dynamicAccentMode) {
        DynamicAccentEngine.extractPaletteForWallpaper(wallpaperIndex, dynamicAccentMode)
    }
    val effectiveAccentHex = if (autoWallpaperAccent) {
        dynamicPalette.getSwatch(dynamicSwatchIndex).hex
    } else {
        selectedAccentHex
    }
    val accent = LumiaPalette.getAccent(effectiveAccentHex)
    val glassStyle = rememberAdaptiveGlassStyle(
        settings = settings.copy(
            wallpaperGradientIndex = wallpaperIndex,
            dynamicAccentMode = dynamicAccentMode,
            dynamicSwatchIndex = dynamicSwatchIndex,
            accentColorHex = effectiveAccentHex,
            autoWallpaperAccentEnabled = autoWallpaperAccent,
            isDarkTheme = isDarkTheme
        ),
        customWallpaperBitmap = customWallpaperBitmap
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            wallpaperSource = WallpaperSource.CUSTOM
            onPickCustomWallpaper(uri)
        }
    }

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

    fun openSystemWallpaperChooser() {
        try {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Chọn hình nền hệ thống"))
        } catch (_: Exception) {
            Toast.makeText(context, "Không thể mở trình chọn hình nền hệ thống", Toast.LENGTH_SHORT).show()
        }
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
                autoArrangeTiles = autoArrangeTiles,
                wallpaperSource = wallpaperSource,
                wallpaperGradientIndex = wallpaperIndex,
                wallpaperDimAlpha = wallpaperDimAlpha,
                turnstileAnimationEnabled = turnstileAnim,
                glanceScreenEnabled = glanceScreen,
                batterySaverMode = batterySaverMode,
                batterySaverThresholdPct = batterySaverThreshold,
                autoOptimizeBackgroundApps = autoOptimizeBgApps,
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

    val targetHeightFraction by animateFloatAsState(
        targetValue = if (selectedTab == null || isExpandedHeight) 0.84f else 0.64f,
        animationSpec = tween(durationMillis = 220),
        label = "settingsDockHeight"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(50f)
    ) {
        // Upper Live-Preview Zone: completely transparent (no dark scrim!) so user sees real Home Screen changes immediately
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = accent.color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chạm bên ngoài để đóng",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Docked Minimalist Settings Panel with Dynamic Wallpaper-Adaptive Fluent Acrylic Glassmorphism
        CompositionLocalProvider(LocalSettingsGlassStyle provides glassStyle) {
            FluentGlassPanel(
                glassStyle = glassStyle,
                customWallpaperBitmap = customWallpaperBitmap,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                blurRadius = 28.dp,
                borderWidth = 1.2.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(targetHeightFraction)
                    .shadow(28.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume clicks inside dock */ }
                    .testTag("settings_fluent_glass_dock")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    // Compact Top Header + Height Toggle + Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedTab != null) {
                                IconButton(onClick = { selectedTab = null }, modifier = Modifier.size(44.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại cài đặt", tint = Color.White)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accent.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedTab?.title ?: "Cài đặt",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isExpandedHeight) "Mở rộng" else "Xem trước",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(glassStyle.activeTileGlassBrush)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Thay đổi được áp dụng ngay",
                                    color = Color.White.copy(alpha = 0.68f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.testTag("settings_glass_source_label")
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isExpandedHeight = !isExpandedHeight },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("toggle_settings_height_button")
                            ) {
                        Icon(
                            imageVector = if (isExpandedHeight) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isExpandedHeight) "Thu gọn để xem trực tiếp" else "Mở rộng bảng cài đặt",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng cài đặt",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Active Tab Content (Focused, clean, minimal scrolling)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    null -> {
                        item(key = "settings_home") {
                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                SettingsCategoryTab.entries.forEachIndexed { index, category ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedTab = category }
                                            .padding(horizontal = 16.dp, vertical = 17.dp)
                                            .testTag(category.tag),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(category.icon, contentDescription = null, tint = accent.color, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(category.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                            val subtitle = when (category) {
                                                SettingsCategoryTab.WALLPAPER_COLOR -> "Theo hệ thống, màu sắc, giao diện tối"
                                                SettingsCategoryTab.TILES_LAYOUT -> "Sắp xếp ô, kích thước và hiệu ứng"
                                                SettingsCategoryTab.APP_DRAWER -> "Danh sách A–Z, tìm kiếm và nhóm"
                                                SettingsCategoryTab.BATTERY_SYSTEM -> "Sao lưu, pin và cài đặt mặc định"
                                            }
                                            Text(subtitle, color = Color.White.copy(alpha = 0.64f), fontSize = 12.sp)
                                        }
                                        Text("›", color = Color.White.copy(alpha = 0.54f), fontSize = 25.sp)
                                    }
                                    if (index < SettingsCategoryTab.entries.lastIndex) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                                    }
                                }
                            }
                        }
                    }
                    SettingsCategoryTab.WALLPAPER_COLOR -> {
                        item {
                            MinimalGroupCard(title = "HÌNH NỀN") {
                                // Custom Photo Picker & System Wallpaper Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accent.color),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pick_custom_wallpaper_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Chọn ảnh",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            wallpaperSource = WallpaperSource.SYSTEM
                                            applyChanges()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_system_wallpaper_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wallpaper,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Theo hệ thống",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                OutlinedButton(onClick = { openSystemWallpaperChooser() }) {
                                    Text("Đổi hình nền điện thoại", fontSize = 12.sp, color = Color.White)
                                }

                                if (settings.wallpaperSource == WallpaperSource.CUSTOM && settings.customWallpaperUri.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accent.color.copy(alpha = 0.15f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Đang dùng ảnh của bạn",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Gỡ ảnh",
                                            color = accent.color,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { onClearCustomWallpaper() }
                                                .padding(4.dp)
                                                .testTag("clear_custom_wallpaper_button")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Live Wallpaper Dim Slider
                                LiveSliderRow(
                                    label = "Làm tối hình nền",
                                    valueText = "${(wallpaperDimAlpha * 100).roundToInt()}%",
                                    value = wallpaperDimAlpha,
                                    valueRange = 0f..0.70f,
                                    accentColor = accent.color,
                                    onValueChange = {
                                        wallpaperDimAlpha = it
                                        applyChanges()
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Hình nền Lumia",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(LumiaPalette.Wallpapers) { idx, pair ->
                                        val isSelected = wallpaperSource == WallpaperSource.LUMIA && wallpaperIndex == idx
                                        Column(
                                            modifier = Modifier
                                                .width(78.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) accent.color else Color.White.copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    wallpaperIndex = idx
                                                    wallpaperSource = WallpaperSource.LUMIA
                                                    applyChanges()
                                                }
                                                .padding(5.dp)
                                                .testTag("wallpaper_option_$idx"),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(pair.second),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = pair.first,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            MinimalGroupCard(title = "MÀU SẮC & GIAO DIỆN") {
                                MinimalSwitchRow(
                                    title = "Lấy màu theo hình nền",
                                    subtitle = "Tự chọn màu phù hợp với hình nền",
                                    checked = autoWallpaperAccent,
                                    accentColor = accent.color,
                                    switchTag = "auto_wallpaper_accent_switch",
                                    onCheckedChange = {
                                        autoWallpaperAccent = it
                                        applyChanges()
                                    }
                                )

                                AnimatedVisibility(visible = autoWallpaperAccent) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            DynamicAccentMode.entries.forEach { mode ->
                                                val isModeSelected = dynamicAccentMode == mode
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(
                                                            if (isModeSelected) accent.color.copy(alpha = 0.25f)
                                                            else Color.White.copy(alpha = 0.05f)
                                                        )
                                                        .border(
                                                            width = if (isModeSelected) 1.5.dp else 1.dp,
                                                            color = if (isModeSelected) accent.color else Color.White.copy(alpha = 0.1f),
                                                            shape = RoundedCornerShape(9.dp)
                                                        )
                                                        .clickable {
                                                            dynamicAccentMode = mode
                                                            applyChanges()
                                                        }
                                                        .padding(vertical = 7.dp)
                                                        .testTag("dynamic_accent_mode_${mode.name.lowercase()}"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = mode.shortLabel,
                                                        color = if (isModeSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            dynamicPalette.swatches.forEachIndexed { sIdx, swatch ->
                                                val isSwatchSelected = dynamicSwatchIndex == sIdx
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(swatch.color.copy(alpha = 0.2f))
                                                        .border(
                                                            width = if (isSwatchSelected) 1.5.dp else 1.dp,
                                                            color = if (isSwatchSelected) Color.White else swatch.color.copy(alpha = 0.5f),
                                                            shape = RoundedCornerShape(9.dp)
                                                        )
                                                        .clickable {
                                                            dynamicSwatchIndex = sIdx
                                                            selectedAccentHex = swatch.hex
                                                            applyChanges()
                                                        }
                                                        .padding(vertical = 7.dp, horizontal = 6.dp)
                                                        .testTag("dynamic_wallpaper_swatch_$sIdx"),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(swatch.color)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = swatch.name,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Bảng màu Lumia thủ công",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(LumiaPalette.Accents) { _, item ->
                                        val isSelected = !autoWallpaperAccent &&
                                            item.hex.equals(selectedAccentHex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(item.color)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    autoWallpaperAccent = false
                                                    selectedAccentHex = item.hex
                                                    applyChanges()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = item.name,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                MinimalSwitchRow(
                                    title = "Giao diện tối",
                                    subtitle = "Sử dụng nền tối cho launcher",
                                    checked = isDarkTheme,
                                    accentColor = accent.color,
                                    onCheckedChange = {
                                        isDarkTheme = it
                                        applyChanges()
                                    }
                                )
                            }
                        }
                    }

                    SettingsCategoryTab.TILES_LAYOUT -> {
                        item {
                            MinimalGroupCard(title = "GIAO DIỆN Ô") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Wp10TileStyle.entries.forEach { style ->
                                        val isSelected = wp10TileStyle == style
                                        val shortTitle = when (style) {
                                            Wp10TileStyle.CRYSTAL_CLEAR -> "Trong suốt"
                                            Wp10TileStyle.FLUENT_ACRYLIC -> "Kính mờ"
                                            Wp10TileStyle.TINTED_TRANSLUCENT -> "Nhuộm màu"
                                            Wp10TileStyle.CLASSIC_METRO -> "Đặc 100%"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) accent.color
                                                    else Color.White.copy(alpha = 0.07f)
                                                )
                                                .clickable {
                                                    wp10TileStyle = style
                                                    when (style) {
                                                        Wp10TileStyle.CRYSTAL_CLEAR -> tileOpacity = 0f
                                                        Wp10TileStyle.CLASSIC_METRO -> tileOpacity = 1f
                                                        Wp10TileStyle.FLUENT_ACRYLIC -> if (tileOpacity > 0.7f || tileOpacity < 0.1f) tileOpacity = 0.38f
                                                        Wp10TileStyle.TINTED_TRANSLUCENT -> if (tileOpacity > 0.75f || tileOpacity < 0.1f) tileOpacity = 0.42f
                                                    }
                                                    applyChanges()
                                                }
                                                .padding(vertical = 8.dp)
                                                .testTag("wp10_tile_style_${style.name.lowercase()}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shortTitle,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LiveSliderRow(
                                    label = "Độ đậm nền ô",
                                    valueText = "${(tileOpacity * 100).roundToInt()}%",
                                    value = tileOpacity,
                                    valueRange = 0f..1f,
                                    accentColor = accent.color,
                                    onValueChange = {
                                        tileOpacity = it
                                        if (it <= 0.03f && wp10TileStyle == Wp10TileStyle.CLASSIC_METRO) {
                                            wp10TileStyle = Wp10TileStyle.CRYSTAL_CLEAR
                                        } else if (it < 0.98f && wp10TileStyle == Wp10TileStyle.CLASSIC_METRO) {
                                            wp10TileStyle = Wp10TileStyle.TINTED_TRANSLUCENT
                                        }
                                        applyChanges()
                                    }
                                )

                                LiveSliderRow(
                                    label = "Bo góc ô",
                                    valueText = "${tileCornerRadius}dp",
                                    value = tileCornerRadius.toFloat(),
                                    valueRange = 0f..24f,
                                    accentColor = accent.color,
                                    onValueChange = {
                                        tileCornerRadius = it.roundToInt()
                                        applyChanges()
                                    }
                                )

                                LiveSliderRow(
                                    label = "Khoảng cách giữa các ô",
                                    valueText = "${tileSpacingDp}dp",
                                    value = tileSpacingDp.toFloat(),
                                    valueRange = 2f..14f,
                                    accentColor = accent.color,
                                    onValueChange = {
                                        tileSpacingDp = it.roundToInt()
                                        applyChanges()
                                    }
                                )
                            }
                        }

                        item {
                            MinimalGroupCard(title = "BỐ CỤC & HIỆU ỨNG") {
                                MinimalSwitchRow(
                                    title = "Hiển thị nhiều ô hơn",
                                    subtitle = "Dùng lưới 6 cột thay vì 4 cột",
                                    checked = showMoreTiles,
                                    accentColor = accent.color,
                                    onCheckedChange = {
                                        showMoreTiles = it
                                        applyChanges()
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
                                MinimalSwitchRow(
                                    title = "Viền sáng quanh ô",
                                    subtitle = "Làm nổi bật đường viền ô",
                                    checked = wp10RevealBorder,
                                    accentColor = accent.color,
                                    onCheckedChange = {
                                        wp10RevealBorder = it
                                        applyChanges()
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
                                MinimalSwitchRow(
                                    title = "Tự dồn ô",
                                    subtitle = "Tự lấp khoảng trống khi thêm, di chuyển hoặc xóa ô",
                                    checked = autoArrangeTiles,
                                    accentColor = accent.color,
                                    onCheckedChange = {
                                        autoArrangeTiles = it
                                        applyChanges()
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
                                MinimalSwitchRow(
                                    title = "Khóa bố cục",
                                    subtitle = "Ngăn di chuyển, thay đổi kích thước và xóa ô",
                                    checked = lockHomeLayout,
                                    accentColor = accent.color,
                                    onCheckedChange = {
                                        lockHomeLayout = it
                                        applyChanges()
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Hiệu ứng chuyển trang",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PageTransitionEffect.entries.forEach { effect ->
                                        val isSelected = pageTransition == effect
                                        val label = when (effect) {
                                            PageTransitionEffect.TURNSTILE_3D -> "Turnstile"
                                            PageTransitionEffect.CLASSIC_SLIDE -> "Trượt phẳng"
                                            PageTransitionEffect.MIUI_CUBE -> "Khối 3D"
                                            PageTransitionEffect.ONE_UI_DEPTH -> "Chiều sâu"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(
                                                    if (isSelected) accent.color
                                                    else Color.White.copy(alpha = 0.07f)
                                                )
                                                .clickable {
                                                    pageTransition = effect
                                                    applyChanges()
                                                }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsCategoryTab.APP_DRAWER -> {
                        item {
                            MinimalGroupCard(title = "DANH SÁCH ỨNG DỤNG") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AllAppsViewMode.entries.forEach { mode ->
                                        val isSelected = allAppsViewMode == mode
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) accent.color
                                                    else Color.White.copy(alpha = 0.07f)
                                                )
                                                .clickable {
                                                    allAppsViewMode = mode
                                                    applyChanges()
                                                }
                                                .padding(vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mode.label,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = allAppsViewMode == AllAppsViewMode.ONE_UI_GRID) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Số cột lưới:",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        listOf(3, 4, 5).forEach { cols ->
                                            val isSelected = allAppsGridColumns == cols
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) accent.color
                                                        else Color.White.copy(alpha = 0.08f)
                                                    )
                                                    .clickable {
                                                        allAppsGridColumns = cols
                                                        applyChanges()
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "$cols cột",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                MinimalSwitchRow(
                                    title = "Hiện chữ cái A–Z",
                                    subtitle = "Chia ứng dụng theo chữ cái",
                                    checked = allAppsShowLetterHeaders,
                                    accentColor = accent.color,
                                    onCheckedChange = {
                                        allAppsShowLetterHeaders = it
                                        applyChanges()
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
                                MinimalSwitchRow(
                                    title = "Lọc theo nhóm",
                                    subtitle = "Tìm nhanh ứng dụng theo nhóm",
                                    checked = allAppsCategorizedEnabled,
                                    accentColor = accent.color,
                                    onCheckedChange = {
                                        allAppsCategorizedEnabled = it
                                        applyChanges()
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
                                MinimalSwitchRow(
                                    title = "Hiện nút tìm kiếm",
                                    subtitle = "Tìm ứng dụng trong danh sách",
                                    checked = allAppsShowSearchBar,
                                    accentColor = accent.color,
                                    onCheckedChange = {
                                        allAppsShowSearchBar = it
                                        applyChanges()
                                    }
                                )
                            }
                        }
                    }

                    SettingsCategoryTab.BATTERY_SYSTEM -> {
                        // 1. Background App Hibernation & RAM Optimizer
                        item {
                            MinimalGroupCard(title = "BỘ NHỚ LAUNCHER & PIN") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatMiniColumn(
                                        title = "Tiến trình hiển thị",
                                        value = "${telemetry.backgroundAppsCount} app",
                                        valueColor = if (telemetry.backgroundAppsCount > 2) Color(0xFFFFB300) else Color(0xFF4CAF50)
                                    )
                                    StatMiniColumn(
                                        title = "Bộ nhớ RAM",
                                        value = "${telemetry.ramUsagePct}% (${telemetry.availableRamMb}MB trống)",
                                        valueColor = Color.White
                                    )
                                    StatMiniColumn(
                                        title = "Pin & Nhiệt độ",
                                        value = "${telemetry.batteryPct}% • ${telemetry.batteryTempC}°C",
                                        valueColor = if (telemetry.batteryPct <= 25) Color(0xFFFF5252) else Color(0xFF4CAF50)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        val fresh = onOptimizeMemory()
                                        lastOptimizeBanner =
                                            "Đã xóa bộ nhớ đệm biểu tượng của Launcher"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent.color),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("optimize_background_apps_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Xóa bộ nhớ đệm Launcher",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                lastOptimizeBanner?.let { banner ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = banner,
                                        color = Color(0xFF81C784),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Android quản lý ứng dụng chạy nền. Launcher chỉ quản lý bộ nhớ đệm của chính mình.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }

                        // 2. Battery Saver Mode
                        item {
                            val isSaverActiveNow = settings.copy(
                                batterySaverMode = batterySaverMode,
                                batterySaverThresholdPct = batterySaverThreshold
                            ).isBatterySaverActive(telemetry)

                            MinimalGroupCard(title = "CHẾ ĐỘ TIẾT KIỆM PIN LAUNCHER") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Giảm tần suất cập nhật Tile 3x & tắt 3D khi pin thấp",
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (isSaverActiveNow) Color(0xFF2E7D32).copy(alpha = 0.32f)
                                                else Color.White.copy(alpha = 0.08f)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .testTag("battery_saver_status_badge")
                                    ) {
                                        Text(
                                            text = if (isSaverActiveNow) "ĐANG BẬT" else "CHỜ",
                                            color = if (isSaverActiveNow) Color(0xFF81C784) else Color.White.copy(alpha = 0.65f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    BatterySaverMode.entries.forEach { mode ->
                                        val isSelected = batterySaverMode == mode
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(
                                                    if (isSelected) accent.color
                                                    else Color.White.copy(alpha = 0.07f)
                                                )
                                                .clickable {
                                                    batterySaverMode = mode
                                                    applyChanges()
                                                }
                                                .padding(vertical = 8.dp)
                                                .testTag("battery_saver_mode_${mode.name.lowercase()}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mode.shortLabel,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = batterySaverMode == BatterySaverMode.AUTO) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Ngưỡng kích hoạt:",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                        listOf(15, 20, 25, 30, 35).forEach { pct ->
                                            val isSelected = batterySaverThreshold == pct
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) accent.color
                                                        else Color.White.copy(alpha = 0.07f)
                                                    )
                                                    .clickable {
                                                        batterySaverThreshold = pct
                                                        applyChanges()
                                                    }
                                                    .padding(vertical = 5.dp)
                                                    .testTag("battery_saver_threshold_$pct"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$pct%",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Default Launcher & Cloud Sync / Backup
                        item {
                            MinimalGroupCard(title = "MÀN HÌNH CHÍNH MẶC ĐỊNH & ĐÁM MÂY") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { requestSetDefaultLauncher() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (telemetry.isDefaultLauncher) Color(0xFF2E7D32) else accent.color
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("set_default_launcher_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (telemetry.isDefaultLauncher) "Đã đặt Mặc định" else "Đặt làm Mặc định",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { openSystemDefaultLauncherSettings() },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("open_home_settings_button")
                                    ) {
                                        Text(
                                            text = "Cài đặt Home",
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Cloud Sync Section
                                when (cloudAuthState) {
                                    is CloudAuthState.Authenticated -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDone,
                                                    contentDescription = null,
                                                    tint = accent.color,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = cloudAuthState.displayName,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = cloudAuthState.email ?: "Đã kết nối Firebase Cloud",
                                                        color = Color.White.copy(alpha = 0.6f),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = onSignOutCloud,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("google_sign_out_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Logout,
                                                    contentDescription = "Đăng xuất",
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val stamp = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                                                onSyncToCloudNow("Bản sao lưu $stamp")
                                            },
                                            enabled = !isCloudSyncing,
                                            colors = ButtonDefaults.buttonColors(containerColor = accent.color),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("sync_to_cloud_button")
                                        ) {
                                            if (isCloudSyncing) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.CloudUpload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Đồng bộ lên Đám mây ngay",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (cloudBackups.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            cloudBackups.take(3).forEach { backup ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.White.copy(alpha = 0.05f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                        .testTag("cloud_backup_item_${backup.id}"),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = backup.title,
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = "${backup.tileCount} ô • ${backup.deviceName}",
                                                            color = Color.White.copy(alpha = 0.55f),
                                                            fontSize = 10.sp,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    Row {
                                                        Text(
                                                            text = "Khôi phục",
                                                            color = accent.color,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier
                                                                .clickable { onRestoreCloudBackup(backup) }
                                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                                .testTag("restore_cloud_backup_${backup.id}")
                                                        )
                                                        IconButton(
                                                            onClick = { onDeleteCloudBackup(backup.id) },
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .testTag("delete_cloud_backup_${backup.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Xóa bản sao lưu",
                                                                tint = Color.White.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    else -> {
                                        Button(
                                            onClick = onSignInWithGoogle,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("google_sign_in_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = accent.color,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Đăng nhập Google để Sao lưu Đám mây",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val json = onExportBackup()
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("LumiaBackup", json))
                                                Toast.makeText(context, "Đã sao chép JSON sao lưu vào bộ nhớ tạm", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sao lưu bố cục", color = Color.White, fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { showRestoreDialog = !showRestoreDialog },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Khôi phục hoặc đặt lại", color = Color.White, fontSize = 11.sp)
                                    }
                                }

                                AnimatedVisibility(visible = showRestoreDialog) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        OutlinedTextField(
                                            value = restoreJsonInput,
                                            onValueChange = { restoreJsonInput = it },
                                            placeholder = { Text("Dán dữ liệu sao lưu", fontSize = 11.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = accent.color,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 3
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val ok = onRestoreBackup(restoreJsonInput)
                                                        if (ok) {
                                                            Toast.makeText(context, "Đã khôi phục bố cục", Toast.LENGTH_SHORT).show()
                                                            onDismiss()
                                                        } else {
                                                            Toast.makeText(context, "JSON không hợp lệ", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = accent.color),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Khôi phục", fontSize = 11.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    onResetDefaults()
                                                    Toast.makeText(context, "Đã đặt lại bố cục", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Đặt lại bố cục", color = Color(0xFFFF8A80), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

}

@Composable
private fun MinimalGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassStyle = LocalSettingsGlassStyle.current
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .then(
                if (glassStyle != null) {
                    Modifier
                        .background(glassStyle.cardBackgroundBrush)
                        .border(1.dp, glassStyle.cardBorderBrush, cardShape)
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.045f))
                        .border(1.dp, Color.White.copy(alpha = 0.07f), cardShape)
                }
            )
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun LiveSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
private fun MinimalSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    switchTag: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor
            ),
            modifier = if (switchTag != null) Modifier.testTag(switchTag) else Modifier
        )
    }
}

@Composable
private fun StatMiniColumn(
    title: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
