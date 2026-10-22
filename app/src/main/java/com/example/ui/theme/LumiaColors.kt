package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class LumiaAccent(
    val name: String,
    val hex: String,
    val color: Color
)

object LumiaPalette {
    val Accents = listOf(
        LumiaAccent("Lumia Cyan", "#0078D7", Color(0xFF0078D7)),
        LumiaAccent("Nokia Yellow", "#FFC700", Color(0xFFFFC700)),
        LumiaAccent("Crimson Red", "#E81123", Color(0xFFE81123)),
        LumiaAccent("Emerald Green", "#107C41", Color(0xFF107C41)),
        LumiaAccent("Mango Orange", "#F7630C", Color(0xFFF7630C)),
        LumiaAccent("Cobalt Blue", "#004E98", Color(0xFF004E98)),
        LumiaAccent("Plum Violet", "#881798", Color(0xFF881798)),
        LumiaAccent("Pure Magenta", "#D13438", Color(0xFFD13438)),
        LumiaAccent("Lumia Teal", "#008272", Color(0xFF008272)),
        LumiaAccent("One UI 9 Indigo", "#4361EE", Color(0xFF4361EE)),
        LumiaAccent("MIUI Hyper Orange", "#FF6900", Color(0xFFFF6900)),
        LumiaAccent("MIUI Mint Cyan", "#00B4D8", Color(0xFF00B4D8))
    )

    val Wallpapers = listOf(
        "Lumia 925 Deep Blue" to Brush.verticalGradient(listOf(Color(0xFF001122), Color(0xFF003366), Color(0xFF000814))),
        "Windows 10 Mobile Hero" to Brush.verticalGradient(listOf(Color(0xFF0B192C), Color(0xFF1E3E62), Color(0xFF000000))),
        "Pure Pitch Black (Lumia AMOLED)" to Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF050505), Color(0xFF000000))),
        "One UI 9.0 Dynamic Nebula" to Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF311042))),
        "Xiaomi MIUI / HyperOS Silk" to Brush.linearGradient(listOf(Color(0xFF0A192F), Color(0xFF112240), Color(0xFF1D3557))),
        "Lumia Crimson Twilight" to Brush.verticalGradient(listOf(Color(0xFF2C0B14), Color(0xFF5C1024), Color(0xFF0D0306))),
        "Emerald Nordic Aurora" to Brush.verticalGradient(listOf(Color(0xFF032212), Color(0xFF084824), Color(0xFF010E07)))
    )

    fun getAccent(hex: String): LumiaAccent {
        return Accents.find { it.hex.equals(hex, ignoreCase = true) } ?: Accents.first()
    }
}
