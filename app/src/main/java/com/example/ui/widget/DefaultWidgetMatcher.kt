package com.example.ui.widget

import com.example.data.model.TileEntity
import com.example.data.model.TileType
import com.example.data.model.WidgetProviderItem
import java.util.Locale

/**
 * Matches a factory Lumia tile to an *installed* Android widget provider.
 *
 * Never guess a provider component: packages and localized widget labels are
 * both checked. No generic/system/third-party tile is silently converted.
 * A user must opt in, and Android's binding/configuration screens still apply.
 */
object DefaultWidgetMatcher {
    private data class Rule(
        val packages: List<String>,
        val names: List<String>
    )

    private val clock = Rule(
        packages = listOf(
            "com.sec.android.app.clockpackage",
            "com.google.android.deskclock"
        ),
        names = listOf("clock", "đồng hồ", "digital", "analog", "time")
    )

    private val photos = Rule(
        packages = listOf(
            "com.sec.android.gallery3d",
            "com.google.android.apps.photos"
        ),
        names = listOf("gallery", "photo", "album", "story", "stories", "bộ sưu tập", "ảnh", "câu chuyện")
    )

    private val weather = Rule(
        packages = listOf(
            "com.sec.android.daemonapp",
            "com.samsung.android.weather",
            "com.google.android.googlequicksearchbox"
        ),
        names = listOf("weather", "forecast", "thời tiết", "dự báo")
    )

    private val notes = Rule(
        packages = listOf(
            "com.samsung.android.app.notes",
            "com.google.android.keep"
        ),
        names = listOf("note", "ghi chú", "keep")
    )

    private fun ruleFor(tile: TileEntity): Rule? = when {
        tile.id == "tile_clock_calendar" && tile.tileType == TileType.CLOCK_CALENDAR -> clock
        tile.id == "tile_photos" && tile.tileType == TileType.APP -> photos
        tile.id == "tile_weather" && tile.tileType == TileType.WEATHER -> weather
        tile.id == "tile_notes" && tile.tileType == TileType.NOTES -> notes
        // Older layouts might contain these native placeholder tiles with custom IDs.
        tile.tileType == TileType.WEATHER -> weather
        tile.tileType == TileType.PHOTOS -> photos
        tile.tileType == TileType.NOTES -> notes
        else -> null
    }

    fun recommend(tile: TileEntity, installed: List<WidgetProviderItem>): WidgetProviderItem? {
        if (tile.hasBoundOrLinkedWidget()) return null
        val rule = ruleFor(tile) ?: return null
        return installed
            .mapNotNull { candidate ->
                val pkgRank = rule.packages.indexOfFirst {
                    it.equals(candidate.packageName, ignoreCase = true)
                }
                if (pkgRank < 0) return@mapNotNull null
                val text = listOf(candidate.label, candidate.className, candidate.description)
                    .joinToString(" ").lowercase(Locale.ROOT)
                if (rule.names.none { text.contains(it) }) return@mapNotNull null
                val score = (rule.packages.size - pkgRank) * 100 +
                    rule.names.count { text.contains(it) } * 5 -
                    (candidate.targetCellWidth * candidate.targetCellHeight).coerceAtMost(16)
                score to candidate
            }
            .maxByOrNull { it.first }
            ?.second
    }

    fun suggestions(
        tiles: List<TileEntity>,
        installed: List<WidgetProviderItem>
    ): List<Pair<TileEntity, WidgetProviderItem>> {
        val defaults = listOf("tile_clock_calendar", "tile_photos", "tile_weather", "tile_notes")
        return tiles
            .filter { it.id in defaults }
            .sortedBy { defaults.indexOf(it.id) }
            .mapNotNull { tile -> recommend(tile, installed)?.let { tile to it } }
    }
}
