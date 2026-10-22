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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LauncherSettings
import com.example.data.model.PageTransitionEffect
import com.example.data.model.SystemTelemetry
import com.example.ui.theme.LumiaPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: LauncherSettings,
    telemetry: SystemTelemetry,
    onSaveSettings: (LauncherSettings) -> Unit,
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
    var isDarkTheme by remember { mutableStateOf(settings.isDarkTheme) }
    var tileOpacity by remember { mutableFloatStateOf(settings.tileOpacity) }
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
    var quickNoteText by remember { mutableStateOf(settings.quickNotes) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val accent = LumiaPalette.getAccent(selectedAccentHex)

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
        onSaveSettings(
            settings.copy(
                accentColorHex = selectedAccentHex,
                isDarkTheme = isDarkTheme,
                tileOpacity = tileOpacity,
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
                quickNotes = quickNoteText
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        contentColor = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LUMIA • ONE UI 9.0 • MIUI",
                            color = accent.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Cài đặt Launcher",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            applyChanges()
                            Toast.makeText(context, "Đã lưu cài đặt!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent.color)
                    ) {
                        Text("Xong")
                    }
                }
            }

            // Section 0: Set as Default System Launcher (Đặt làm Launcher mặc định)
            item {
                SettingsSectionHeader(icon = Icons.Default.Home, title = "Ứng dụng Màn hình chính mặc định")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (telemetry.isDefaultLauncher) Color(0xFF107C41) else accent.color.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (telemetry.isDefaultLauncher)
                            Color(0xFF107C41).copy(alpha = 0.16f)
                        else
                            accent.color.copy(alpha = 0.14f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (telemetry.isDefaultLauncher) Color(0xFF107C41) else accent.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (telemetry.isDefaultLauncher) Icons.Default.CheckCircle else Icons.Default.Home,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (telemetry.isDefaultLauncher)
                                        "Lumia Launcher đang là mặc định"
                                    else
                                        "Chưa đặt làm Launcher mặc định",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Trình khởi chạy hiện tại: ${telemetry.currentDefaultLauncherName}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { requestSetDefaultLauncher() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("set_default_launcher_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (telemetry.isDefaultLauncher) Color(0xFF107C41) else accent.color
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (telemetry.isDefaultLauncher) "Đổi Launcher mặc định" else "Đặt làm mặc định",
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { openSystemDefaultLauncherSettings() },
                                modifier = Modifier.testTag("open_home_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Cài đặt hệ thống",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Color & Accent
            item {
                SettingsSectionHeader(icon = Icons.Default.ColorLens, title = "Bảng màu Lumia • One UI • MIUI")
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    items(LumiaPalette.Accents) { acc ->
                        val isSelected = acc.hex.equals(selectedAccentHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(acc.color)
                                .clickable {
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Wallpaper Background
            item {
                SettingsSectionHeader(icon = Icons.Default.Dashboard, title = "Hình nền Lumia • One UI 9.0 • HyperOS")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LumiaPalette.Wallpapers.forEachIndexed { idx, pair ->
                        val isSelected = idx == wallpaperIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accent.color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) accent.color else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    wallpaperIndex = idx
                                    applyChanges()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(pair.second)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = pair.first,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = accent.color)
                            }
                        }
                    }
                }
            }

            // Section 3: Page Transitions & Motion (MIUI / One UI 9.0 / Lumia)
            item {
                SettingsSectionHeader(icon = Icons.Default.AutoAwesomeMotion, title = "Hiệu ứng chuyển trang (Lumia • MIUI • One UI)")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PageTransitionEffect.entries.forEach { effect ->
                        val selected = pageTransition == effect
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) accent.color.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.06f))
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) accent.color else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    pageTransition = effect
                                    applyChanges()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = effect.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Section 4: Tile Appearance & Grid Spacing
            item {
                SettingsSectionHeader(icon = Icons.Default.Dashboard, title = "Kiểu dáng ô Tile & Bố cục lưới")
                Spacer(modifier = Modifier.height(8.dp))

                // Opacity slider
                Text(
                    text = "Độ trong suốt ô Tile: ${(tileOpacity * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
                Slider(
                    value = tileOpacity,
                    onValueChange = {
                        tileOpacity = it
                        applyChanges()
                    },
                    valueRange = 0.2f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent.color,
                        activeTrackColor = accent.color,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                // Corner radius slider
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Góc bo ô Tile: ${tileCornerRadius}dp (${if (tileCornerRadius == 0) "Vuông sắc bén Lumia WP" else if (tileCornerRadius <= 6) "Lumia 925" else "Bo tròn One UI / MIUI"})",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
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
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                // Tile Spacing selector (4dp WP8.1, 8dp Lumia 925, 12dp One UI / MIUI)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Khoảng cách giữa các ô Tile:",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        4 to "Sát cạnh WP (4dp)",
                        8 to "Lumia 925 (8dp)",
                        12 to "One UI / MIUI (12dp)"
                    ).forEach { (dpVal, label) ->
                        val selected = tileSpacingDp == dpVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) accent.color else Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    tileSpacingDp = dpVal
                                    applyChanges()
                                }
                                .padding(vertical = 9.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real App Icons toggle
                SettingToggleRow(
                    title = "Hiển thị biểu tượng ứng dụng gốc",
                    subtitle = "Kết hợp icon thật của Android trên nền ô Tile (Tắt = Icon phẳng Lumia)",
                    checked = useRealAppIcons,
                    accentColor = accent.color,
                    onCheckedChange = {
                        useRealAppIcons = it
                        applyChanges()
                    }
                )

                // MIUI / HyperOS Glass Border toggle
                SettingToggleRow(
                    title = "Viền kính nổi MIUI / One UI 9.0",
                    subtitle = "Hiệu ứng viền ánh sáng tinh tế quanh các ô Live Tile",
                    checked = miuiGlassBorder,
                    accentColor = accent.color,
                    onCheckedChange = {
                        miuiGlassBorder = it
                        applyChanges()
                    }
                )

                // Show more tiles (4 cols vs 6 cols)
                SettingToggleRow(
                    title = "Hiển thị nhiều ô hơn (6 cột)",
                    subtitle = if (showMoreTiles) "Chế độ 6 cột (Phablet / One UI)" else "Chế độ 4 cột (Lumia 925 chuẩn)",
                    checked = showMoreTiles,
                    accentColor = accent.color,
                    onCheckedChange = {
                        showMoreTiles = it
                        applyChanges()
                    }
                )
            }

            // Section 5: Gestures & Smart Controls (Lumia / One UI / MIUI)
            item {
                SettingsSectionHeader(icon = Icons.Default.TouchApp, title = "Cử chỉ & Bảo vệ bố cục")
                Spacer(modifier = Modifier.height(8.dp))

                SettingToggleRow(
                    title = "Hiệu ứng nghiêng 3D Metro Tilt",
                    subtitle = "Phản hồi lún đàn hồi chuẩn Windows Phone khi chạm vào ô Tile",
                    checked = turnstileAnim,
                    accentColor = accent.color,
                    onCheckedChange = {
                        turnstileAnim = it
                        applyChanges()
                    }
                )

                SettingToggleRow(
                    title = "Màn hình chờ Nokia Glance Screen",
                    subtitle = "Đồng hồ AMOLED tiết kiệm pin đặc trưng dòng Lumia",
                    checked = glanceScreen,
                    accentColor = accent.color,
                    onCheckedChange = {
                        glanceScreen = it
                        applyChanges()
                    }
                )

                SettingToggleRow(
                    title = "Chạm đúp vùng trống để mở Glance",
                    subtitle = "Cử chỉ nhanh kiểu Lumia & Xiaomi MIUI / One UI",
                    checked = doubleTapToGlance,
                    accentColor = accent.color,
                    onCheckedChange = {
                        doubleTapToGlance = it
                        applyChanges()
                    }
                )

                SettingToggleRow(
                    title = "Khóa bố cục Màn hình chính",
                    subtitle = "Ngăn vô tình xóa, đổi cỡ hoặc di chuyển ô Tile (MIUI / One UI)",
                    checked = lockHomeLayout,
                    accentColor = accent.color,
                    onCheckedChange = {
                        lockHomeLayout = it
                        applyChanges()
                    }
                )
            }

            // Section 6: Quick Notes
            item {
                SettingsSectionHeader(icon = Icons.Default.Edit, title = "Nội dung ghi chú trên Live Tile")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quickNoteText,
                    onValueChange = {
                        quickNoteText = it
                        applyChanges()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nhập ghi chú nhanh...", color = Color.White.copy(alpha = 0.4f)) }
                )
            }

            // Section 7: Backup & Restore
            item {
                SettingsSectionHeader(icon = Icons.Default.Backup, title = "Sao lưu & Đồng bộ cấu hình")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF69F0AE))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Trạng thái cấu hình", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = settings.cloudSyncState, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                Toast.makeText(context, "Đã sao chép cấu hình sao lưu vào bộ nhớ tạm!", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.color)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xuất sao lưu")
                    }

                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Khôi phục")
                    }
                }

                if (showRestoreDialog) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Dán mã JSON sao lưu vào đây...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val success = onRestoreBackup(restoreJsonInput)
                                if (success) {
                                    Toast.makeText(context, "Khôi phục thành công!", Toast.LENGTH_SHORT).show()
                                    showRestoreDialog = false
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Định dạng JSON không hợp lệ!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41))
                    ) {
                        Text("Xác nhận khôi phục")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onResetDefaults()
                        Toast.makeText(context, "Đã đặt lại ô Tile mặc định!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đặt lại màn hình bắt đầu mặc định")
                }
            }

            // Section 8: System Performance Analytics & MIUI/One UI Memory Booster
            item {
                SettingsSectionHeader(icon = Icons.Default.Speed, title = "Báo cáo hiệu năng & Tối ưu RAM")
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PerformanceMetricRow("Bộ nhớ RAM", "${telemetry.availableRamMb} MB trống / ${telemetry.totalRamMb} MB (${telemetry.ramUsagePct}% đang dùng)")
                        PerformanceMetricRow("Bộ nhớ trong", "${telemetry.internalFreeGb} GB trống / ${telemetry.internalTotalGb} GB")
                        PerformanceMetricRow("Tình trạng Pin", "${telemetry.batteryPct}% • ${telemetry.batteryTempC}°C" + if (telemetry.isCharging) " (Đang sạc)" else "")
                        PerformanceMetricRow("Thời gian hoạt động", "${telemetry.uptimeHours} giờ liên tục")
                        PerformanceMetricRow("Kiến trúc giao diện", telemetry.deviceModel)

                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val updated = onOptimizeMemory()
                                Toast.makeText(
                                    context,
                                    "Đã dọn dẹp bộ nhớ đệm! RAM trống: ${updated.availableRamMb} MB",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accent.color.copy(alpha = 0.85f))
                        ) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Giải phóng RAM & Tối ưu mượt mà (MIUI / One UI Boost)", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 9: About
            item {
                SettingsSectionHeader(icon = Icons.Default.Info, title = "Thông tin Lumia Launcher")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lumia Launcher • Kết hợp tinh hoa Live Tiles của Nokia Lumia 925 (Windows 10 Mobile), bố cục thích ứng của Samsung One UI 9.0 và hiệu ứng chuyển cảnh 3D mượt mà của Xiaomi MIUI / HyperOS.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Color.White.copy(alpha = 0.58f), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor
            )
        )
    }
}

@Composable
private fun SettingsSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PerformanceMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
