package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileSize
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockLiveTileContent(size: TileSize) {
    var flipState by remember { mutableIntStateOf(0) }
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(7000)
            flipState = (flipState + 1) % 2
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d 'thg' M", Locale.getDefault())

    AnimatedContent(
        targetState = flipState,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
        },
        label = "clockFlip"
    ) { state ->
        if (state == 0 || size == TileSize.SMALL) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Lịch",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(if (size == TileSize.SMALL) 18.dp else 24.dp)
                    )
                    Text(
                        text = dayFormat.format(currentTime).uppercase(Locale.getDefault()),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = if (size == TileSize.SMALL) 9.sp else 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = timeFormat.format(currentTime),
                        color = Color.White,
                        fontSize = when (size) {
                            TileSize.SMALL -> 16.sp
                            TileSize.MEDIUM -> 28.sp
                            TileSize.WIDE -> 36.sp
                            TileSize.LARGE -> 48.sp
                        },
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = dateFormat.format(currentTime),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = if (size == TileSize.SMALL) 10.sp else 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LỊCH HÔM NAY",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "10:00 - Họp nhóm Lumia",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "14:30 - Thử nghiệm Windows 10",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = "Không còn sự kiện nào nữa",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun WeatherLiveTileContent(size: TileSize) {
    var flipState by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(8500)
            flipState = (flipState + 1) % 2
        }
    }

    AnimatedContent(
        targetState = flipState,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
        },
        label = "weatherFlip"
    ) { state ->
        if (state == 0 || size == TileSize.SMALL) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Weather",
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(if (size == TileSize.SMALL) 22.dp else 30.dp)
                    )
                    Text(
                        text = "Hà Nội",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = if (size == TileSize.SMALL) 10.sp else 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "28°",
                        color = Color.White,
                        fontSize = when (size) {
                            TileSize.SMALL -> 18.sp
                            TileSize.MEDIUM -> 30.sp
                            TileSize.WIDE -> 38.sp
                            TileSize.LARGE -> 48.sp
                        },
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = "Nắng đẹp",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = if (size == TileSize.SMALL) 9.sp else 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DỰ BÁO 3 NGÀY",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ForecastDay("T2", "30°", "Nắng")
                    ForecastDay("T3", "28°", "Mây")
                    ForecastDay("T4", "29°", "Nắng")
                }
                Text(
                    text = "Độ ẩm: 65% • Gió nhẹ 12km/h",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ForecastDay(day: String, temp: String, condition: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = day, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = temp, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(text = condition, color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp)
    }
}

@Composable
fun BatteryLiveTileContent(telemetry: SystemTelemetry, size: TileSize) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                contentDescription = "Pin",
                tint = if (telemetry.batteryPct <= 20) Color(0xFFFF5252) else Color.White,
                modifier = Modifier.size(if (size == TileSize.SMALL) 20.dp else 26.dp)
            )
            if (telemetry.isCharging) {
                Text(
                    text = "Đang sạc",
                    color = Color(0xFF69F0AE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column {
            Text(
                text = "${telemetry.batteryPct}%",
                color = Color.White,
                fontSize = when (size) {
                    TileSize.SMALL -> 18.sp
                    TileSize.MEDIUM -> 30.sp
                    TileSize.WIDE -> 36.sp
                    TileSize.LARGE -> 46.sp
                },
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { (telemetry.batteryPct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (telemetry.batteryPct <= 20) Color(0xFFFF5252) else Color.White,
                trackColor = Color.White.copy(alpha = 0.2f),
            )
            if (size != TileSize.SMALL) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nhiệt độ: ${telemetry.batteryTempC}°C • Tốt",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun PerformanceLiveTileContent(telemetry: SystemTelemetry, size: TileSize) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Hiệu năng",
                tint = Color.White,
                modifier = Modifier.size(if (size == TileSize.SMALL) 18.dp else 24.dp)
            )
            Text(
                text = "RAM: ${telemetry.ramUsagePct}%",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = if (size == TileSize.SMALL) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LinearProgressIndicator(
                progress = { (telemetry.ramUsagePct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (telemetry.ramUsagePct > 80) Color(0xFFFF5252) else Color(0xFF00E5FF),
                trackColor = Color.White.copy(alpha = 0.2f),
            )
            if (size != TileSize.SMALL) {
                Text(
                    text = "Trống: ${telemetry.availableRamMb} MB / ${telemetry.totalRamMb} MB",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )
                Text(
                    text = "Bộ nhớ trống: ${telemetry.internalFreeGb} GB",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun PhotosLiveTileContent(size: TileSize) {
    val photoLabels = listOf("Lumia PureView Moments", "Hoàng hôn Hà Nội", "Nhiếp ảnh di động", "Kỷ niệm Lumia 925")
    var currentPhotoIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            currentPhotoIndex = (currentPhotoIndex + 1) % photoLabels.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
    ) {
        AnimatedContent(
            targetState = currentPhotoIndex,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "photoFade",
            modifier = Modifier.fillMaxSize()
        ) { index ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when (index % 4) {
                            0 -> Color(0xFF0D47A1)
                            1 -> Color(0xFF880E4F)
                            2 -> Color(0xFF1B5E20)
                            else -> Color(0xFFE65100)
                        }
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(if (size == TileSize.LARGE) 64.dp else 40.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = photoLabels[currentPhotoIndex],
                color = Color.White,
                fontSize = if (size == TileSize.SMALL) 9.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NotesLiveTileContent(quickNote: String, size: TileSize) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notes,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "GHI CHÚ LUMIA",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = quickNote.ifEmpty { "Chạm để thêm ghi chú nhanh..." },
            color = Color.White,
            fontSize = if (size == TileSize.SMALL) 10.sp else 12.sp,
            maxLines = when (size) {
                TileSize.SMALL -> 2
                TileSize.MEDIUM -> 3
                TileSize.WIDE -> 2
                TileSize.LARGE -> 6
            },
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
        Text(
            text = "Đã lưu an toàn trên máy",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp
        )
    }
}

@Composable
fun QuickActionsLiveTileContent(
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    size: TileSize
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "TRUNG TÂM HÀNH ĐỘNG",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionButton(
                icon = Icons.Default.FlashlightOn,
                label = "Đèn pin",
                isActive = isTorchOn,
                onClick = onToggleTorch
            )
            QuickActionButton(
                icon = Icons.Default.Wifi,
                label = "Wi-Fi",
                isActive = true,
                onClick = { /* status */ }
            )
            if (size != TileSize.SMALL) {
                QuickActionButton(
                    icon = Icons.Default.Bluetooth,
                    label = "Bluetooth",
                    isActive = false,
                    onClick = { /* status */ }
                )
                QuickActionButton(
                    icon = Icons.Default.VolumeUp,
                    label = "Âm lượng",
                    isActive = true,
                    onClick = { /* status */ }
                )
            }
        }

        Text(
            text = "Chạm nhanh để kích hoạt",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 9.sp
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF0078D7) else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 9.sp
        )
    }
}

@Composable
fun NewsQuoteLiveTileContent(size: TileSize) {
    val quotes = listOf(
        "\"Sáng tạo không ngừng, biến đổi tương lai.\"" to "Lumia PureView",
        "\"Đơn giản là đỉnh cao của sự tinh tế.\"" to "Metro Design",
        "\"Mỗi khoảnh khắc là một câu chuyện đẹp.\"" to "Nokia Lumia 925",
        "\"Hiệu năng mượt mà, trải nghiệm đỉnh cao.\"" to "Windows 10 Mobile"
    )
    var quoteIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(9000)
            quoteIndex = (quoteIndex + 1) % quotes.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.Default.FormatQuote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = quotes[quoteIndex].first,
            color = Color.White,
            fontSize = if (size == TileSize.SMALL) 10.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "— " + quotes[quoteIndex].second,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
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
