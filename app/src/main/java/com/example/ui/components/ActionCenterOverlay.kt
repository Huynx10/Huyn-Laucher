package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.LauncherSettings
import com.example.data.model.Wp10TileStyle
import com.example.ui.theme.LumiaPalette

/** Launcher-only shortcuts. System controls remain in Android's notification shade. */
@Composable
fun ActionCenterOverlay(
    settings: LauncherSettings,
    onCycleTileAppearance: () -> Unit,
    onToggleLayoutLock: () -> Unit,
    onOpenGlance: () -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    val accent = LumiaPalette.resolveEffectiveAccent(settings).color
    val styleLabel = when (settings.wp10TileStyle) {
        Wp10TileStyle.CRYSTAL_CLEAR -> "Trong suốt"
        Wp10TileStyle.FLUENT_ACRYLIC -> "Kính"
        Wp10TileStyle.TINTED_TRANSLUCENT -> "Pha màu"
        Wp10TileStyle.CLASSIC_METRO -> "Màu đặc"
    }

    Box(
        modifier = Modifier.fillMaxSize().zIndex(55f)
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .background(Color(0xFF131316))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("action_center_overlay")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thao tác nhanh",
                    color = Color(0xFFF7F7F8),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng thao tác nhanh", tint = Color.White)
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LauncherQuickAction(
                    title = "Kiểu ô", detail = styleLabel, icon = Icons.Default.BlurOn,
                    accent = accent, active = settings.wp10TileStyle != Wp10TileStyle.CLASSIC_METRO,
                    modifier = Modifier.weight(1f), tag = "action_center_tile_transparency",
                    onClick = onCycleTileAppearance
                )
                LauncherQuickAction(
                    title = "Khóa bố cục",
                    detail = if (settings.lockHomeLayout) "Đang khóa" else "Đang mở",
                    icon = if (settings.lockHomeLayout) Icons.Default.Lock else Icons.Default.LockOpen,
                    accent = accent, active = settings.lockHomeLayout,
                    modifier = Modifier.weight(1f), tag = "action_center_tile_lock_layout",
                    onClick = onToggleLayoutLock
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LauncherQuickAction(
                    title = "Xem nhanh", detail = "Giờ và pin", icon = Icons.Default.NightlightRound,
                    accent = accent, active = false,
                    modifier = Modifier.weight(1f), tag = "action_center_tile_glance",
                    onClick = { onDismiss(); onOpenGlance() }
                )
                LauncherQuickAction(
                    title = "Cài đặt", detail = "Tùy chỉnh Lumia", icon = Icons.Default.Settings,
                    accent = accent, active = false,
                    modifier = Modifier.weight(1f), tag = "action_center_tile_all_settings",
                    onClick = { onDismiss(); onOpenFullSettings() }
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LauncherQuickAction(
    title: String,
    detail: String,
    icon: ImageVector,
    accent: Color,
    active: Boolean,
    modifier: Modifier,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.heightIn(min = 68.dp).clip(RoundedCornerShape(14.dp))
            .background(if (active) accent.copy(alpha = 0.26f) else Color(0xFF222228))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White,
            modifier = Modifier.size(22.dp))
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(detail, color = Color(0xFFB4B4BC), fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
