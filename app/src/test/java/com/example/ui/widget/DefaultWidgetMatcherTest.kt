package com.example.ui.widget

import com.example.data.model.TileEntity
import com.example.data.model.TileType
import com.example.data.model.WidgetProviderItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultWidgetMatcherTest {
    private fun widget(pkg: String, label: String, cls: String = ".Widget") =
        WidgetProviderItem(
            providerComponent = "$pkg/$cls",
            packageName = pkg,
            className = cls,
            label = label,
            appName = label
        )

    @Test fun clockUsesInstalledSamsungProviderNotAnUnrelatedWidget() {
        val tile = TileEntity("tile_clock_calendar", "Đồng hồ", tileType = TileType.CLOCK_CALENDAR)
        val gallery = widget("com.sec.android.gallery3d", "Ảnh và Câu chuyện")
        val clock = widget("com.sec.android.app.clockpackage", "Digital Clock")
        assertEquals(clock, DefaultWidgetMatcher.recommend(tile, listOf(gallery, clock)))
    }

    @Test fun analogClockIsNotOfferedAsDigitalDefault() {
        val tile = TileEntity("tile_clock_calendar", "Đồng hồ", tileType = TileType.CLOCK_CALENDAR)
        val analog = widget("com.sec.android.app.clockpackage", "Analog Clock", ".AnalogWidget")
        assertNull(DefaultWidgetMatcher.recommend(tile, listOf(analog)))
    }

    @Test fun gallerySupportsVietnameseWidgetNameAndPrioritizesSamsungOnS24() {
        val tile = TileEntity("tile_photos", "Ảnh", tileType = TileType.APP)
        val google = widget("com.google.android.apps.photos", "Photos")
        val samsung = widget("com.sec.android.gallery3d", "Ảnh và Câu chuyện")
        assertEquals(samsung, DefaultWidgetMatcher.recommend(tile, listOf(google, samsung)))
    }

    @Test fun neverSuggestArbitraryChatWidgetForPerformanceTile() {
        val tile = TileEntity("tile_performance", "Hiệu năng máy", tileType = TileType.PERFORMANCE)
        val chat = widget("com.openai.chatgpt", "ChatGPT")
        assertNull(DefaultWidgetMatcher.recommend(tile, listOf(chat)))
    }

    @Test fun neverReplaceAlreadyBoundWidgetOrUserTile() {
        val samsung = widget("com.sec.android.gallery3d", "Gallery Stories")
        val linked = TileEntity(
            "tile_photos", "Ảnh", tileType = TileType.WIDGET,
            appWidgetId = 42, widgetProvider = samsung.providerComponent
        )
        assertNull(DefaultWidgetMatcher.recommend(linked, listOf(samsung)))
        assertNull(DefaultWidgetMatcher.recommend(
            TileEntity("user_custom", "Ảnh", tileType = TileType.APP),
            listOf(samsung)
        ))
    }

    @Test fun suggestionsOnlyIncludeMatchingDefaultTiles() {
        val widgets = listOf(
            widget("com.sec.android.app.clockpackage", "Đồng hồ số"),
            widget("com.sec.android.gallery3d", "Ảnh và Câu chuyện"),
            widget("com.openai.chatgpt", "ChatGPT")
        )
        val tiles = listOf(
            TileEntity("tile_performance", "Hiệu năng máy", tileType = TileType.PERFORMANCE),
            TileEntity("tile_photos", "Ảnh", tileType = TileType.APP),
            TileEntity("tile_clock_calendar", "Đồng hồ", tileType = TileType.CLOCK_CALENDAR)
        )
        val suggestions = DefaultWidgetMatcher.suggestions(tiles, widgets)
        assertEquals(listOf("tile_clock_calendar", "tile_photos"), suggestions.map { it.first.id })
        assertTrue(suggestions.none { it.second.packageName == "com.openai.chatgpt" })
    }
}
