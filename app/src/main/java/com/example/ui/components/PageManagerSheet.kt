package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.PageType
import com.example.ui.theme.LumiaPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerSheet(
    pages: List<LauncherPage>,
    tileCountsByPage: Map<String, Int>,
    settings: LauncherSettings,
    onAddHomePage: (String) -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onRenamePage: (String, String) -> Unit,
    onDeleteHomePage: (String) -> Unit,
    onSetDefaultHomePage: (String) -> Unit,
    onToggleShowHeaderTitle: (Boolean) -> Unit,
    onJumpToPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = LumiaPalette.getAccent(settings.accentColorHex)

    var newPageName by remember { mutableStateOf("") }
    var editingPageId by remember { mutableStateOf<String?>(null) }
    var editingPageTitle by remember { mutableStateOf("") }

    val homePagesCount = pages.count { it.pageType == PageType.HOME }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161616),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "QUẢN LÝ TRANG MÀN HÌNH",
                        color = accent.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Thêm, sắp xếp vị trí trang Home & Ứng dụng",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add new Home Page input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newPageName,
                    onValueChange = { newPageName = it },
                    placeholder = {
                        Text(
                            text = "Tên trang Home mới (VD: Công việc, Game...)",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accent.color,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_home_page_input")
                )
                Button(
                    onClick = {
                        val title = newPageName.trim().ifEmpty { "Trang Home ${homePagesCount + 1}" }
                        onAddHomePage(title)
                        newPageName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent.color),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    modifier = Modifier.testTag("add_home_page_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm trang", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Toggle whether to show subtle page title on Home pages
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hiển thị tiêu đề tên trang trên Home",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Tắt để màn hình bắt đầu hoàn toàn tràn viền không có thanh tiêu đề",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = settings.showPageHeaderTitle,
                    onCheckedChange = onToggleShowHeaderTitle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent.color
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "DANH SÁCH TRANG (Dùng mũi tên để đổi vị trí trang)",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                    val isHome = page.pageType == PageType.HOME
                    val isDefaultHome = isHome && page.id == settings.defaultHomePageId
                    val tileCount = tileCountsByPage[page.id] ?: 0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(
                                width = if (isDefaultHome) 1.5.dp else 1.dp,
                                color = if (isDefaultHome) accent.color else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isHome) accent.color else Color.White.copy(alpha = 0.15f))
                                    .clickable {
                                        onJumpToPage(index)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isHome) Icons.Default.Home else Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onJumpToPage(index)
                                        onDismiss()
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}. ${page.title}",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isDefaultHome) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Mặc định",
                                            color = accent.color,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(accent.color.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isHome) "$tileCount ô Live Tile • Chạm để mở trang" else "Trang danh sách tất cả ứng dụng • Có thể đổi vị trí",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 11.sp
                                )
                            }

                            // Action controls: Default Star, Move Up (Left), Move Down (Right), Edit Name, Delete
                            if (isHome) {
                                IconButton(
                                    onClick = { onSetDefaultHomePage(page.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDefaultHome) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Đặt làm trang chính mặc định",
                                        tint = if (isDefaultHome) Color(0xFFFFC700) else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { if (index > 0) onMovePage(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Di chuyển sang trái / lên trước",
                                    tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { if (index < pages.size - 1) onMovePage(index, index + 1) },
                                enabled = index < pages.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Di chuyển sang phải / xuống sau",
                                    tint = if (index < pages.size - 1) Color.White else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (editingPageId == page.id) {
                                        editingPageId = null
                                    } else {
                                        editingPageId = page.id
                                        editingPageTitle = page.title
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Đổi tên trang",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            if (isHome && homePagesCount > 1) {
                                IconButton(
                                    onClick = { onDeleteHomePage(page.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa trang Home",
                                        tint = Color(0xFFE81123),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        if (editingPageId == page.id) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editingPageTitle,
                                    onValueChange = { editingPageTitle = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (editingPageTitle.isNotBlank()) {
                                            onRenamePage(page.id, editingPageTitle.trim())
                                        }
                                        editingPageId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent.color)
                                ) {
                                    Text("Lưu")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
