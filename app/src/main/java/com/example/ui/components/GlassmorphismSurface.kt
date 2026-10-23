package com.example.ui.components

import android.app.WallpaperManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.LauncherSettings
import com.example.ui.theme.DynamicAccentEngine
import com.example.ui.theme.LumiaPalette
import com.example.ui.theme.WallpaperTonalPalette

/**
 * Holds dynamically computed Windows 11 / Windows 10 Mobile Fluent Acrylic & Mica
 * Glassmorphism tokens adapted to the active custom photo, system wallpaper, or preset gradient.
 */
@Immutable
data class AdaptiveGlassStyle(
    val palette: WallpaperTonalPalette,
    val primaryTint: Color,
    val secondaryTint: Color,
    val baseSurfaceColor: Color,
    val acrylicOverlayBrush: Brush,
    val specularBorderBrush: Brush,
    val cardBackgroundBrush: Brush,
    val cardBorderBrush: Brush,
    val activeTileGlassBrush: Brush,
    val wallpaperBrush: Brush,
    val sourceLabel: String
)

/**
 * Dynamically computes an [AdaptiveGlassStyle] that adapts to:
 * 1. Custom gallery photo wallpaper (if `customWallpaperBitmap` is active)
 * 2. System Android WallpaperColors via `WallpaperManager` (API 27+ when available)
 * 3. Active Lumia/Windows 10 Mobile Wallpaper Gradient preset
 */
@Composable
fun rememberAdaptiveGlassStyle(
    settings: LauncherSettings,
    customWallpaperBitmap: ImageBitmap? = null
): AdaptiveGlassStyle {
    val context = LocalContext.current

    return remember(
        settings.wallpaperGradientIndex,
        settings.dynamicAccentMode,
        settings.dynamicSwatchIndex,
        settings.accentColorHex,
        settings.autoWallpaperAccentEnabled,
        settings.isDarkTheme,
        settings.customWallpaperUri,
        customWallpaperBitmap
    ) {
        val preset = LumiaPalette.WallpaperPresets.getOrElse(settings.wallpaperGradientIndex) {
            LumiaPalette.WallpaperPresets.first()
        }

        // 1. Sample colors from custom photo bitmap if present, else system WallpaperColors if available, else preset
        var sampledStops: List<Color>? = null
        var sourceLabel = preset.name

        if (customWallpaperBitmap != null) {
            try {
                val androidBmp = customWallpaperBitmap.asAndroidBitmap()
                val w = androidBmp.width
                val h = androidBmp.height
                if (w > 0 && h > 0) {
                    val samplePoints = listOf(
                        w / 4 to h / 4,
                        (w * 3) / 4 to h / 4,
                        w / 2 to h / 2,
                        w / 4 to (h * 3) / 4,
                        (w * 3) / 4 to (h * 3) / 4
                    )
                    sampledStops = samplePoints.map { (px, py) ->
                        Color(androidBmp.getPixel(px.coerceIn(0, w - 1), py.coerceIn(0, h - 1)))
                    }
                    sourceLabel = "Ảnh nền cá nhân"
                }
            } catch (_: Exception) {
            }
        }

        if (sampledStops == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wm = WallpaperManager.getInstance(context)
                val sysColors = wm?.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (sysColors != null) {
                    val p = Color(sysColors.primaryColor.toArgb())
                    val s = sysColors.secondaryColor?.let { Color(it.toArgb()) } ?: preset.stopColors.getOrElse(1) { p }
                    val t = sysColors.tertiaryColor?.let { Color(it.toArgb()) } ?: preset.stopColors.last()
                    // Blend system wallpaper colors with preset stops for rich Acrylic depth
                    sampledStops = listOf(p, s, t) + preset.stopColors.take(2)
                    sourceLabel = "Hình nền Hệ thống • ${preset.name.take(14)}"
                }
            } catch (_: Exception) {
            }
        }

        val effectivePalette = if (!sampledStops.isNullOrEmpty()) {
            DynamicAccentEngine.extractPaletteFromStops(
                wallpaperIndex = settings.wallpaperGradientIndex,
                wallpaperName = sourceLabel,
                stops = sampledStops,
                mode = settings.dynamicAccentMode
            )
        } else {
            DynamicAccentEngine.extractPaletteForWallpaper(
                wallpaperIndex = settings.wallpaperGradientIndex,
                mode = settings.dynamicAccentMode
            )
        }

        val activeAccent = if (settings.autoWallpaperAccentEnabled) {
            effectivePalette.getSwatch(settings.dynamicSwatchIndex).color
        } else {
            LumiaPalette.getAccent(settings.accentColorHex).color
        }

        val primaryTint = activeAccent
        val secondaryTint = effectivePalette.tertiaryGlow.color
        val containerTone = effectivePalette.tonalContainer
        val highlightTone = effectivePalette.acrylicHighlight

        val baseSurfaceColor = if (settings.isDarkTheme) {
            Color(
                red = (containerTone.red * 0.38f + 0.04f).coerceIn(0f, 1f),
                green = (containerTone.green * 0.38f + 0.04f).coerceIn(0f, 1f),
                blue = (containerTone.blue * 0.42f + 0.06f).coerceIn(0f, 1f),
                alpha = 0.74f
            )
        } else {
            Color(
                red = (containerTone.red * 0.25f + 0.72f).coerceIn(0f, 1f),
                green = (containerTone.green * 0.25f + 0.72f).coerceIn(0f, 1f),
                blue = (containerTone.blue * 0.25f + 0.75f).coerceIn(0f, 1f),
                alpha = 0.78f
            )
        }

        val acrylicOverlayBrush = Brush.linearGradient(
            colors = listOf(
                primaryTint.copy(alpha = 0.22f),
                containerTone.copy(alpha = 0.48f),
                secondaryTint.copy(alpha = 0.16f),
                Color(0xFF090B10).copy(alpha = 0.64f)
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 1200f)
        )

        val specularBorderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.34f),
                highlightTone.copy(alpha = 0.28f),
                primaryTint.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.08f)
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 800f)
        )

        val cardBackgroundBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.09f),
                primaryTint.copy(alpha = 0.08f),
                containerTone.copy(alpha = 0.28f)
            ),
            start = Offset(0f, 0f),
            end = Offset(600f, 400f)
        )

        val cardBorderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                highlightTone.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.05f)
            )
        )

        val activeTileGlassBrush = Brush.linearGradient(
            colors = listOf(
                primaryTint.copy(alpha = 0.88f),
                highlightTone.copy(alpha = 0.72f)
            )
        )

        AdaptiveGlassStyle(
            palette = effectivePalette,
            primaryTint = primaryTint,
            secondaryTint = secondaryTint,
            baseSurfaceColor = baseSurfaceColor,
            acrylicOverlayBrush = acrylicOverlayBrush,
            specularBorderBrush = specularBorderBrush,
            cardBackgroundBrush = cardBackgroundBrush,
            cardBorderBrush = cardBorderBrush,
            activeTileGlassBrush = activeTileGlassBrush,
            wallpaperBrush = preset.brush,
            sourceLabel = sourceLabel
        )
    }
}

/**
 * Windows 11 / Windows 10 Mobile Fluent Acrylic Glassmorphism container.
 * Renders a blurred wallpaper layer underneath an adaptive tonal tint, frosted noise luminosity,
 * and a directional specular glass border.
 */
@Composable
fun FluentGlassPanel(
    glassStyle: AdaptiveGlassStyle,
    customWallpaperBitmap: ImageBitmap? = null,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    blurRadius: Dp = 28.dp,
    borderWidth: Dp = 1.2.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(glassStyle.baseSurfaceColor)
    ) {
        // Layer 1: Blurred Wallpaper Backdrop (mimics Windows Fluent Acrylic sampling the desktop/home wallpaper)
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(blurRadius)
                .background(glassStyle.wallpaperBrush)
        ) {
            if (customWallpaperBitmap != null) {
                Image(
                    bitmap = customWallpaperBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.55f,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Layer 2: Adaptive Tonal Acrylic Diffusion Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(glassStyle.acrylicOverlayBrush)
        )

        // Layer 3: Frosted Mica Micro-Luminosity Layer + Directional Specular Glass Border
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.035f))
                .border(
                    width = borderWidth,
                    brush = glassStyle.specularBorderBrush,
                    shape = shape
                )
        )

        // Foreground Content
        content()
    }
}

/**
 * Frosted glass card for settings groups and Action Center modules that dynamically harmonizes
 * with the current wallpaper's tonal palette.
 */
@Composable
fun FluentGlassCard(
    glassStyle: AdaptiveGlassStyle,
    shape: Shape = RoundedCornerShape(18.dp),
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glassStyle.cardBackgroundBrush)
            .border(width = 1.dp, brush = glassStyle.cardBorderBrush, shape = shape)
            .padding(14.dp),
        content = content
    )
}
