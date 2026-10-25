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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AllAppsViewMode
import com.example.data.model.LauncherSettings
import com.example.data.remote.CloudBackup
import com.example.ui.viewmodel.CloudAuthState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class CleanSection(val label: String) {
    HOME("Màn hình chính"), APPS("Danh sách ứng dụng"),
    WIDGETS("Widget"), BACKUP("Sao lưu và khôi phục")
}
private val bg = Color(0xFF131316)
private val primary = Color(0xFFF7F7F8)
private val secondary = Color(0xFFB4B4BC)
private val accent = Color(0xFF90B6FF)

@Composable
fun CleanSettingsScreen(
    settings: LauncherSettings,
    cloudAuthState: CloudAuthState,
    cloudBackups: List<CloudBackup>,
    isCloudSyncing: Boolean,
    isSavingSettings: Boolean,
    onSaveSettings: (LauncherSettings) -> Unit,
    onPickCustomWallpaper: (Uri) -> Unit,
    onExportBackup: suspend () -> String,
    onRestoreBackup: suspend (String) -> Boolean,
    onResetDefaults: suspend () -> Unit,
    onSignInWithGoogle: () -> Unit,
    onSignOutCloud: () -> Unit,
    onSyncToCloudNow: (String) -> Unit,
    onRestoreCloudBackup: (CloudBackup) -> Unit,
    onOpenAdvanced: () -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf<CleanSection?>(null) }
    var busy by remember { mutableStateOf(false) }
    var resetConfirmation by remember { mutableStateOf(false) }
    var restoreShown by remember { mutableStateOf(false) }
    var restoreData by remember { mutableStateOf("") }
    var cloudConfirmation by remember { mutableStateOf<CloudBackup?>(null) }
    var opacity by remember(settings.tileOpacity) { mutableFloatStateOf(settings.tileOpacity) }

    fun back() { if (!busy) { if (section == null) onDismiss() else section = null } }
    BackHandler { back() }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    fun chooseHome() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val role = ctx.getSystemService(RoleManager::class.java)
                if (role != null && role.isRoleAvailable(RoleManager.ROLE_HOME) && !role.isRoleHeld(RoleManager.ROLE_HOME)) {
                    roleLauncher.launch(role.createRequestRoleIntent(RoleManager.ROLE_HOME))
                } else {
                    ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                }
            } else {
                ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        } catch (_: Exception) {
            Toast.makeText(ctx, "Không mở được cài đặt mặc định", Toast.LENGTH_SHORT).show()
        }
    }
    fun runSafe(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                Toast.makeText(ctx, "Không thực hiện được. Hãy thử lại.", Toast.LENGTH_SHORT).show()
            } finally { busy = false }
        }
    }

    Box(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()
        .testTag("clean_settings_screen")) {
        Column {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = ::back, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = primary)
                }
                Text(section?.label ?: "Cài đặt", fontSize = 23.sp, fontWeight = FontWeight.SemiBold,
                    color = primary, modifier = Modifier.weight(1f).padding(start = 8.dp))
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, "Đóng cài đặt", tint = primary)
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 50.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (section == null) {
                    item { Hint("Chọn cách hiển thị màn hình và ứng dụng.") }
                    CleanSection.entries.forEach { group ->
                        item(key = group.name) {
                            SettingAction(group.label, when (group) {
                                CleanSection.HOME -> "Bố cục, Tile và chuyển trang"
                                CleanSection.APPS -> "A–Z, tìm kiếm và nhóm"
                                CleanSection.WIDGETS -> "Nội dung thật từ ứng dụng"
                                CleanSection.BACKUP -> "Sao lưu và khôi phục bố cục"
                            }, { section = group }, card = true)
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                    item { SettingAction("Tùy chỉnh nâng cao", "Hiệu ứng và các tùy chọn khác", onOpenAdvanced) }
                }
                when (section) {
                    CleanSection.HOME -> {
                        item { Heading("Bố cục") }
                        item { SettingToggle("Tự dồn ô", "Tắt để tự kéo thả và giữ khoảng trống", settings.autoArrangeTiles) {
                            onSaveSettings(settings.copy(autoArrangeTiles = it))
                        } }
                        item { SettingToggle("Khóa bố cục", "Tránh vô tình di chuyển hoặc xóa ô", settings.lockHomeLayout) {
                            onSaveSettings(settings.copy(lockHomeLayout = it))
                        } }
                        item { SettingToggle("Hiển thị nhiều ô hơn", "Dùng lưới 6 cột thay vì 4 cột", settings.showMoreTiles) {
                            onSaveSettings(settings.copy(showMoreTiles = it))
                        } }
                        item { Heading("Giao diện ô") }
                        item { SettingToggle("Viền sáng", null, settings.wp10RevealBorder) {
                            onSaveSettings(settings.copy(wp10RevealBorder = it))
                        } }
                        item { SettingSlider("Độ đậm nền ô", opacity, 0f..1f,
                            { opacity = it }, { onSaveSettings(settings.copy(tileOpacity = opacity)) }) }
                    }
                    CleanSection.APPS -> {
                        item { Heading("Kiểu hiển thị") }
                        item { SettingAction("Danh sách A–Z",
                            if (settings.allAppsViewMode == AllAppsViewMode.METRO_LIST) "Đang dùng" else "Chuyển sang danh sách",
                            { onSaveSettings(settings.copy(allAppsViewMode = AllAppsViewMode.METRO_LIST)) }) }
                        item { SettingAction("Lưới ứng dụng",
                            if (settings.allAppsViewMode == AllAppsViewMode.ONE_UI_GRID) "Đang dùng" else "Chuyển sang lưới",
                            { onSaveSettings(settings.copy(allAppsViewMode = AllAppsViewMode.ONE_UI_GRID)) }) }
                        item { Heading("Tùy chọn") }
                        item { SettingToggle("Lọc theo nhóm", null, settings.allAppsCategorizedEnabled) {
                            onSaveSettings(settings.copy(allAppsCategorizedEnabled = it))
                        } }
                        item { SettingToggle("Chữ cái A–Z", null, settings.allAppsShowLetterHeaders) {
                            onSaveSettings(settings.copy(allAppsShowLetterHeaders = it))
                        } }
                        item { SettingToggle("Dùng biểu tượng ứng dụng gốc", null, settings.useRealAppIcons) {
                            onSaveSettings(settings.copy(useRealAppIcons = it))
                        } }
                    }
                    CleanSection.WIDGETS -> {
                        item { Heading("Widget thật từ ứng dụng") }
                        item { Hint("Lumia sử dụng widget từ các ứng dụng đã cài. Android sẽ yêu cầu xác nhận nếu cần.") }
                        item { SettingAction("Thêm widget", "Trên Home, nhấn giữ vùng trống rồi chọn Thêm ô", onDismiss) }
                        item { SettingAction("Đặt làm màn hình chính", "Dùng Lumia khi nhấn nút Home", ::chooseHome) }
                    }
                    CleanSection.BACKUP -> {
                        item { Heading("Sao lưu trên máy") }
                        item { SettingAction("Sao chép bản sao lưu", "Lưu nội dung bố cục vào bộ nhớ tạm", {
                            runSafe {
                                val data = onExportBackup()
                                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("LumiaBackup", data))
                                Toast.makeText(ctx, "Đã sao chép bản sao lưu", Toast.LENGTH_SHORT).show()
                            }
                        }) }
                        item { SettingAction("Khôi phục", "Dán bản sao lưu đã tạo", { restoreShown = !restoreShown }) }
                        if (restoreShown) {
                            item {
                                OutlinedTextField(
                                    value = restoreData, onValueChange = { restoreData = it },
                                    label = { Text("Dữ liệu sao lưu") }, minLines = 3, maxLines = 5,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                Button(
                                    onClick = { runSafe {
                                        if (onRestoreBackup(restoreData)) {
                                            Toast.makeText(ctx, "Đã khôi phục bố cục", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else Toast.makeText(ctx, "Bản sao lưu không hợp lệ", Toast.LENGTH_SHORT).show()
                                    } },
                                    enabled = restoreData.isNotBlank() && !busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Khôi phục bố cục") }
                            }
                        }
                        item { Heading("Sao lưu đám mây") }
                        when (cloudAuthState) {
                            is CloudAuthState.Authenticated -> {
                                item { SettingAction("Sao lưu lên Google",
                                    if (isCloudSyncing) "Đang lưu…" else cloudAuthState.displayName,
                                    { if (!isCloudSyncing) onSyncToCloudNow("Bố cục Lumia") }) }
                                cloudBackups.take(5).forEach { backup ->
                                    item(key = backup.id) {
                                        SettingAction(backup.title, backup.tileCount.toString() + " ô · " + backup.deviceName,
                                            { cloudConfirmation = backup })
                                    }
                                }
                                item { SettingAction("Đăng xuất Google", null, onSignOutCloud) }
                            }
                            CloudAuthState.Authenticating -> item { Hint("Đang kết nối Google…") }
                            else -> item { SettingAction("Kết nối Google", "Lưu bố cục lên đám mây", onSignInWithGoogle) }
                        }
                        item { Heading("Đặt lại") }
                        item { SettingAction("Đặt lại bố cục", "Thay các ô hiện tại bằng bố cục mặc định",
                            { resetConfirmation = true }, destructive = true) }
                    }
                    null -> {}
                }
            }
        }
        if (busy || isSavingSettings) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                    .testTag("settings_loading"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accent)
                    Spacer(Modifier.height(12.dp))
                    Text("Đang lưu thay đổi…", color = primary)
                }
            }
        }
    }
    if (resetConfirmation) {
        AlertDialog(
            onDismissRequest = { resetConfirmation = false },
            title = { Text("Đặt lại bố cục?") },
            text = { Text("Các ô hiện tại sẽ được thay bằng bố cục mặc định. Hãy sao lưu nếu muốn giữ lại.") },
            confirmButton = { TextButton(onClick = {
                resetConfirmation = false
                runSafe {
                    onResetDefaults()
                    Toast.makeText(ctx, "Đã đặt lại bố cục", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            }) { Text("Đặt lại") } },
            dismissButton = { TextButton(onClick = { resetConfirmation = false }) { Text("Hủy") } }
        )
    }
    cloudConfirmation?.let { backup ->
        AlertDialog(
            onDismissRequest = { cloudConfirmation = null },
            title = { Text("Khôi phục bản sao lưu?") },
            text = { Text("Bố cục hiện tại sẽ được thay bằng bản “" + backup.title + "”.") },
            confirmButton = { TextButton(onClick = {
                cloudConfirmation = null
                onRestoreCloudBackup(backup)
            }) { Text("Khôi phục") } },
            dismissButton = { TextButton(onClick = { cloudConfirmation = null }) { Text("Hủy") } }
        )
    }
}

@Composable private fun Heading(text: String) {
    Text(text, color = secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 6.dp))
}
@Composable private fun Hint(text: String) {
    Text(text, color = secondary, fontSize = 14.sp, lineHeight = 21.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
}
@Composable private fun SettingAction(
    title: String, subtitle: String?, onClick: () -> Unit,
    card: Boolean = false, destructive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .then(if (card) Modifier.background(Color(0xFF222228)) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (destructive) Color(0xFFFF9999) else primary,
                fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = secondary, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = secondary, modifier = Modifier.size(20.dp))
    }
    if (!card) HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
}
@Composable private fun SettingToggle(title: String, description: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!value) }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = primary, fontSize = 15.sp)
            if (!description.isNullOrBlank()) Text(description, color = secondary,
                fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}
@Composable private fun SettingSlider(
    title: String, value: Float, range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit, onFinish: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row {
            Text(title, color = primary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text((value * 100).toInt().toString() + "%", color = secondary, fontSize = 13.sp)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), valueRange = range,
            onValueChange = onChange, onValueChangeFinished = onFinish)
    }
}
