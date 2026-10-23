package com.example.ui.theme

import android.content.res.Resources
import android.os.Build

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.DynamicAccentMode
import com.example.data.model.LauncherSettings
import com.example.data.model.WallpaperSource
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class LumiaAccent(
    val name: String,
    val hex: String,
    val color: Color
)

data class LumiaWallpaperPreset(
    val name: String,
    val stopColors: List<Color>,
    val isLinear: Boolean,
    val brush: Brush
)

/**
 * Material You + Windows 10 Mobile Tonal Palette extracted dynamically from a wallpaper.
 */
data class WallpaperTonalPalette(
    val wallpaperIndex: Int,
    val wallpaperName: String,
    val primarySeed: LumiaAccent,
    val tonalSecondary: LumiaAccent,
    val tertiaryGlow: LumiaAccent,
    val deepContrast: LumiaAccent,
    val tonalContainer: Color,
    val onTonalContainer: Color,
    val acrylicHighlight: Color
) {
    val swatches: List<LumiaAccent>
        get() = listOf(primarySeed, tonalSecondary, tertiaryGlow, deepContrast)

    fun getSwatch(index: Int): LumiaAccent {
        return swatches.getOrElse(index.coerceIn(0, swatches.lastIndex)) { primarySeed }
    }
}

/**
 * Dynamic Accent Color Engine that extracts primary and tonal colors from the user's wallpaper,
 * combining Windows 10 Mobile's "Automatically pick an accent color from my background" vibrancy
 * with modern Material You tonal harmonization.
 */
object DynamicAccentEngine {

    private data class HslColor(
        val h: Float, // 0..360
        val s: Float, // 0..1
        val l: Float  // 0..1
    )

    fun extractPaletteForWallpaper(
        wallpaperIndex: Int,
        mode: DynamicAccentMode = DynamicAccentMode.WP10_VIBRANT
    ): WallpaperTonalPalette {
        val preset = LumiaPalette.WallpaperPresets.getOrElse(wallpaperIndex) {
            LumiaPalette.WallpaperPresets.first()
        }
        return extractPaletteFromStops(
            wallpaperIndex = wallpaperIndex.coerceIn(0, LumiaPalette.WallpaperPresets.lastIndex),
            wallpaperName = preset.name,
            stops = preset.stopColors,
            mode = mode
        )
    }

    fun extractPaletteFromStops(
        wallpaperIndex: Int,
        wallpaperName: String,
        stops: List<Color>,
        mode: DynamicAccentMode = DynamicAccentMode.WP10_VIBRANT
    ): WallpaperTonalPalette {
        val hslStops = stops.map { colorToHsl(it) }

        // Score stops by chromatic vibrancy and mid-tone presence (Windows 10 Mobile seed scoring)
        val scoredStops = hslStops.mapIndexed { idx, hsl ->
            val midToneWeight = 1f - (abs(hsl.l - 0.45f) * 1.35f).coerceIn(0f, 0.85f)
            val darkPenalty = if (hsl.l < 0.07f) 0.12f else 1f
            val brightPenalty = if (hsl.l > 0.92f) 0.2f else 1f
            val score = (hsl.s * 1.4f + 0.08f) * midToneWeight * darkPenalty * brightPenalty
            idx to score
        }.sortedByDescending { it.second }

        val dominantHsl = scoredStops.firstOrNull()?.let { hslStops[it.first] }
            ?: HslColor(206f, 0.9f, 0.45f)

        // Find a secondary chromatic stop with distinct hue/lightness if available
        val secondaryHsl = scoredStops.drop(1).firstOrNull()?.let { hslStops[it.first] }
            ?: dominantHsl.copy(h = (dominantHsl.h + 28f) % 360f)

        // If the wallpaper is pure AMOLED black (achromatic), use iconic Lumia Cyan / Material Ice seed
        val isAchromatic = dominantHsl.s < 0.08f && dominantHsl.l < 0.08f
        val baseHue = if (isAchromatic) 204f else dominantHsl.h
        val secondaryHue = if (isAchromatic) 188f else {
            val diff = abs(secondaryHsl.h - baseHue)
            if (diff < 12f) (baseHue + 32f) % 360f else secondaryHsl.h
        }

        // 1. Primary Seed shaped by the active DynamicAccentMode
        val primaryHsl = when (mode) {
            DynamicAccentMode.WP10_VIBRANT -> {
                // Windows 10 Mobile signature high-chroma vivid Live Tile accent
                HslColor(
                    h = baseHue,
                    s = dominantHsl.s.coerceIn(0.84f, 0.96f),
                    l = dominantHsl.l.coerceIn(0.44f, 0.52f)
                )
            }
            DynamicAccentMode.MATERIAL_YOU_TONAL -> {
                // Material You Tonal primary (balanced chroma & brighter perceptual tone)
                HslColor(
                    h = baseHue,
                    s = (dominantHsl.s * 0.82f).coerceIn(0.64f, 0.78f),
                    l = (dominantHsl.l + 0.14f).coerceIn(0.54f, 0.62f)
                )
            }
            DynamicAccentMode.HARMONIC_GLOW -> {
                // Harmonic complementary/analogous glow accent
                HslColor(
                    h = (secondaryHue + 18f) % 360f,
                    s = dominantHsl.s.coerceIn(0.80f, 0.92f),
                    l = dominantHsl.l.coerceIn(0.46f, 0.54f)
                )
            }
        }

        // 2. Material You Tonal Secondary Swatch
        val tonalSecondaryHsl = HslColor(
            h = (baseHue + 16f) % 360f,
            s = 0.70f,
            l = 0.58f
        )

        // 3. Tertiary Glow Swatch (Analogous / Accent highlight)
        val tertiaryGlowHsl = HslColor(
            h = (secondaryHue + 38f) % 360f,
            s = 0.84f,
            l = 0.50f
        )

        // 4. Deep Metro Contrast Swatch
        val deepContrastHsl = HslColor(
            h = (baseHue + 348f) % 360f,
            s = 0.88f,
            l = 0.39f
        )

        val primaryColor = hslToColor(primaryHsl)
        val secondaryColor = hslToColor(tonalSecondaryHsl)
        val tertiaryColor = hslToColor(tertiaryGlowHsl)
        val deepColor = hslToColor(deepContrastHsl)

        val tonalContainer = hslToColor(
            HslColor(
                h = primaryHsl.h,
                s = (primaryHsl.s * 0.55f).coerceIn(0.30f, 0.55f),
                l = 0.22f
            )
        )
        val onTonalContainer = hslToColor(
            HslColor(
                h = primaryHsl.h,
                s = 0.85f,
                l = 0.88f
            )
        )
        val acrylicHighlight = hslToColor(
            HslColor(
                h = primaryHsl.h,
                s = 0.90f,
                l = 0.72f
            )
        )

        val shortWallpaperTag = wallpaperName.substringBefore(" (").take(18)

        return WallpaperTonalPalette(
            wallpaperIndex = wallpaperIndex,
            wallpaperName = wallpaperName,
            primarySeed = LumiaAccent(
                name = "Động • $shortWallpaperTag",
                hex = colorToHex(primaryColor),
                color = primaryColor
            ),
            tonalSecondary = LumiaAccent(
                name = "Tonal • $shortWallpaperTag",
                hex = colorToHex(secondaryColor),
                color = secondaryColor
            ),
            tertiaryGlow = LumiaAccent(
                name = "Hòa sắc • $shortWallpaperTag",
                hex = colorToHex(tertiaryColor),
                color = tertiaryColor
            ),
            deepContrast = LumiaAccent(
                name = "Metro Đậm • $shortWallpaperTag",
                hex = colorToHex(deepColor),
                color = deepColor
            ),
            tonalContainer = tonalContainer,
            onTonalContainer = onTonalContainer,
            acrylicHighlight = acrylicHighlight
        )
    }

    /**
     * Extracts a primary seed color from raw ARGB pixels (e.g. from a custom bitmap wallpaper).
     */
    fun extractSeedFromArgbPixels(
        pixels: IntArray,
        mode: DynamicAccentMode = DynamicAccentMode.WP10_VIBRANT
    ): LumiaAccent {
        if (pixels.isEmpty()) return LumiaPalette.Accents.first()
        val step = (pixels.size / 64).coerceAtLeast(1)
        val sampled = mutableListOf<Color>()
        var i = 0
        while (i < pixels.size) {
            sampled.add(Color(pixels[i]))
            i += step
        }
        return extractPaletteFromStops(
            wallpaperIndex = 0,
            wallpaperName = "Hình nền",
            stops = sampled,
            mode = mode
        ).primarySeed
    }

    fun resolveAutoAccent(
        wallpaperIndex: Int,
        mode: DynamicAccentMode = DynamicAccentMode.WP10_VIBRANT,
        swatchIndex: Int = 0
    ): LumiaAccent {
        val palette = extractPaletteForWallpaper(wallpaperIndex, mode)
        return palette.getSwatch(swatchIndex)
    }

    fun colorToHex(color: Color): String {
        val argb = color.toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
    }

    fun parseHexToColorOrNull(hex: String): Color? {
        val cleaned = hex.trim()
        if (!cleaned.startsWith("#") || (cleaned.length != 7 && cleaned.length != 9)) return null
        return try {
            val intVal = cleaned.substring(1).toLong(16)
            if (cleaned.length == 7) {
                val r = ((intVal shr 16) and 0xFF).toInt()
                val g = ((intVal shr 8) and 0xFF).toInt()
                val b = (intVal and 0xFF).toInt()
                Color(r, g, b)
            } else {
                val a = ((intVal shr 24) and 0xFF).toInt()
                val r = ((intVal shr 16) and 0xFF).toInt()
                val g = ((intVal shr 8) and 0xFF).toInt()
                val b = (intVal and 0xFF).toInt()
                Color(r, g, b, a)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun describeHueName(color: Color): String {
        val hsl = colorToHsl(color)
        if (hsl.s < 0.12f) return "Đơn sắc Tonal"
        val h = hsl.h
        return when {
            h < 18f || h >= 345f -> "Đỏ Hồng Ngọc"
            h < 42f -> "Cam Hổ Phách"
            h < 68f -> "Vàng Ánh Kim"
            h < 155f -> "Xanh Ngọc Lục"
            h < 195f -> "Xanh Lam Ngọc"
            h < 250f -> "Xanh Dương W10M"
            h < 295f -> "Tím Tinh Vân"
            else -> "Hồng Magenta"
        }
    }

    private fun colorToHsl(color: Color): HslColor {
        val r = color.red.coerceIn(0f, 1f)
        val g = color.green.coerceIn(0f, 1f)
        val b = color.blue.coerceIn(0f, 1f)

        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC

        val l = (maxC + minC) / 2f
        if (delta < 0.0001f) {
            return HslColor(0f, 0f, l)
        }

        val s = if (l > 0.5f) {
            delta / (2f - maxC - minC)
        } else {
            delta / (maxC + minC)
        }

        var h = when (maxC) {
            r -> ((g - b) / delta) + (if (g < b) 6f else 0f)
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        }
        h *= 60f
        if (h < 0f) h += 360f
        return HslColor(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    private fun hslToColor(hsl: HslColor): Color {
        val h = hsl.h.coerceIn(0f, 360f)
        val s = hsl.s.coerceIn(0f, 1f)
        val l = hsl.l.coerceIn(0f, 1f)

        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
        val m = l - c / 2f

        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return Color(r, g, b)
    }
}

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

    val WallpaperPresets: List<LumiaWallpaperPreset> = listOf(
        LumiaWallpaperPreset(
            name = "Windows 10 Mobile Hero",
            stopColors = listOf(Color(0xFF020B18), Color(0xFF003B73), Color(0xFF0074D9), Color(0xFF01162E)),
            isLinear = true,
            brush = Brush.linearGradient(
                listOf(Color(0xFF020B18), Color(0xFF003B73), Color(0xFF0074D9), Color(0xFF01162E))
            )
        ),
        LumiaWallpaperPreset(
            name = "Lumia 950 XL Continuum",
            stopColors = listOf(Color(0xFF051937), Color(0xFF004D7A), Color(0xFF008793), Color(0xFF041020)),
            isLinear = false,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF051937), Color(0xFF004D7A), Color(0xFF008793), Color(0xFF041020))
            )
        ),
        LumiaWallpaperPreset(
            name = "Lumia 925 Deep Blue",
            stopColors = listOf(Color(0xFF001122), Color(0xFF003366), Color(0xFF000814)),
            isLinear = false,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF001122), Color(0xFF003366), Color(0xFF000814))
            )
        ),
        LumiaWallpaperPreset(
            name = "Pure Pitch Black (AMOLED)",
            stopColors = listOf(Color(0xFF000000), Color(0xFF050505), Color(0xFF000000)),
            isLinear = false,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF000000), Color(0xFF050505), Color(0xFF000000))
            )
        ),
        LumiaWallpaperPreset(
            name = "One UI 9.0 Dynamic Nebula",
            stopColors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF3B0764), Color(0xFF090D16)),
            isLinear = true,
            brush = Brush.linearGradient(
                listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF3B0764), Color(0xFF090D16))
            )
        ),
        LumiaWallpaperPreset(
            name = "Xiaomi MIUI / HyperOS Silk",
            stopColors = listOf(Color(0xFF0A192F), Color(0xFF0F3460), Color(0xFF16213E), Color(0xFF050B14)),
            isLinear = true,
            brush = Brush.linearGradient(
                listOf(Color(0xFF0A192F), Color(0xFF0F3460), Color(0xFF16213E), Color(0xFF050B14))
            )
        ),
        LumiaWallpaperPreset(
            name = "Lumia Crimson Sunset",
            stopColors = listOf(Color(0xFF2C0B14), Color(0xFF6B112B), Color(0xFF1A050B)),
            isLinear = false,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF2C0B14), Color(0xFF6B112B), Color(0xFF1A050B))
            )
        ),
        LumiaWallpaperPreset(
            name = "Emerald Nordic Aurora",
            stopColors = listOf(Color(0xFF032212), Color(0xFF0A5C36), Color(0xFF010E07)),
            isLinear = false,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF032212), Color(0xFF0A5C36), Color(0xFF010E07))
            )
        )
    )

    val Wallpapers: List<Pair<String, Brush>> = WallpaperPresets.map { it.name to it.brush }

    /**
     * Resolves any static or dynamically extracted hex color into a LumiaAccent.
     */
    fun getAccent(hex: String): LumiaAccent {
        Accents.find { it.hex.equals(hex, ignoreCase = true) }?.let { return it }
        val parsed = DynamicAccentEngine.parseHexToColorOrNull(hex)
        if (parsed != null) {
            val normalizedHex = DynamicAccentEngine.colorToHex(parsed)
            val hueLabel = DynamicAccentEngine.describeHueName(parsed)
            return LumiaAccent(
                name = "Động • $hueLabel",
                hex = normalizedHex,
                color = parsed
            )
        }
        return Accents.first()
    }

    /**
     * Resolves the effective LumiaAccent for the given LauncherSettings, automatically
     * picking the wallpaper's dynamic tonal color when autoWallpaperAccentEnabled is true.
     */
    fun resolveEffectiveAccent(settings: LauncherSettings): LumiaAccent {
        if (settings.autoWallpaperAccentEnabled && settings.wallpaperSource == WallpaperSource.SYSTEM &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            // Android 12+ supplies a palette derived from the user's current system wallpaper.
            val systemAccent = runCatching {
                Color(Resources.getSystem().getColor(android.R.color.system_accent1_500, null))
            }.getOrNull()
            if (systemAccent != null) return getAccent(DynamicAccentEngine.colorToHex(systemAccent))
        }
        return if (settings.autoWallpaperAccentEnabled) {
            DynamicAccentEngine.resolveAutoAccent(
                wallpaperIndex = settings.wallpaperGradientIndex,
                mode = settings.dynamicAccentMode,
                swatchIndex = settings.dynamicSwatchIndex
            )
        } else {
            getAccent(settings.accentColorHex)
        }
    }
}
