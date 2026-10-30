package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LumiaPalette

private val jumpLetters = listOf('#') + ('A'..'Z').toList()

@Composable
fun JumpListModal(
    availableLetters: Set<Char>,
    accentHex: String,
    onLetterSelected: (Char) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LumiaPalette.getAccent(accentHex)
    val accentText = if (accent.color.luminance() > 0.179f) Color.Black else Color.White
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val minTileSize = maxOf(64.dp, (32f * fontScale).dp)
    var closed by remember { mutableStateOf(false) }

    fun dismissOnce() {
        if (!closed) {
            closed = true
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::dismissOnce,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalDensity provides density) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Đóng bảng chọn chữ cái" }
                        .clickable(role = Role.Button) { dismissOnce() }
                        .testTag("jump_list_scrim")
                )
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .pointerInput(Unit) { detectTapGestures(onTap = {}) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CHỌN CHỮ CÁI",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minTileSize),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("jump_list_grid")
                    ) {
                        items(jumpLetters, key = { it }) { char ->
                            val isAvailable = char in availableLetters
                            val shape = RoundedCornerShape(4.dp)
                            val label = if (char == '#') "Khác A đến Z" else "Chữ $char"
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(shape)
                                    .background(
                                        if (isAvailable) accent.color
                                        else Color.White.copy(alpha = 0.12f)
                                    )
                                    .then(
                                        if (isAvailable) Modifier.border(
                                            1.dp, Color.White.copy(alpha = 0.4f), shape
                                        ) else Modifier
                                    )
                                    .semantics {
                                        contentDescription = label
                                        if (!isAvailable) stateDescription = "Không có ứng dụng"
                                    }
                                    .clickable(enabled = isAvailable, role = Role.Button) {
                                        if (!closed) {
                                            closed = true
                                            onLetterSelected(char)
                                            onDismiss()
                                        }
                                    }
                                    .testTag("jump_letter_$char"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char.toString(),
                                    color = if (isAvailable) accentText
                                    else Color.White.copy(alpha = 0.55f),
                                    fontSize = 22.sp,
                                    fontWeight = if (isAvailable) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
