package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TileSize(val spanX: Int, val spanY: Int) {
    SMALL(1, 1),
    MEDIUM(2, 2),
    WIDE(4, 2),
    LARGE(4, 4);

    fun next(): TileSize = when (this) {
        SMALL -> MEDIUM
        MEDIUM -> WIDE
        WIDE -> LARGE
        LARGE -> SMALL
    }

    fun stepUp(): TileSize = when (this) {
        SMALL -> MEDIUM
        MEDIUM -> WIDE
        WIDE -> LARGE
        LARGE -> LARGE
    }

    fun stepDown(): TileSize = when (this) {
        LARGE -> WIDE
        WIDE -> MEDIUM
        MEDIUM -> SMALL
        SMALL -> SMALL
    }

    fun effectiveSpanX(totalCols: Int): Int = spanX.coerceAtMost(totalCols)
    fun effectiveSpanY(): Int = spanY

    companion object {
        fun fromTargetSpans(targetSpanX: Int, targetSpanY: Int): TileSize {
            return when {
                targetSpanX <= 1 && targetSpanY <= 1 -> SMALL
                targetSpanY >= 3 && targetSpanX >= 2 -> LARGE
                targetSpanX >= 3 && targetSpanY >= 3 -> LARGE
                targetSpanX >= 3 -> WIDE
                else -> MEDIUM
            }
        }
    }
}

enum class TileType {
    APP,
    WIDGET,
    CLOCK_CALENDAR,
    WEATHER,
    BATTERY,
    PERFORMANCE,
    PHOTOS,
    NOTES,
    NEWS_QUOTE,
    SMART_FOLDER
}

enum class SmartFolderLayoutMode(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    HYPEROS_PEEK(
        label = "HyperOS Peek (3+1)",
        shortLabel = "HyperOS Peek",
        description = "Chạm trực tiếp các biểu tượng lớn để mở nhanh, chạm cụm góc Peek 2x2 để bung thư mục"
    ),
    CLASSIC_GRID(
        label = "Lưới 3x3 Cổ điển",
        shortLabel = "Lưới 3x3",
        description = "Hiển thị lưới 9 biểu tượng thu nhỏ kèm góc Peek, chạm để mở rộng thư mục"
    ),
    CATEGORY_STACK(
        label = "Thẻ Danh mục Động",
        shortLabel = "Thẻ Động",
        description = "Tự động luân chuyển tiêu điểm các Tile trong danh mục kèm cụm góc Peek"
    );

    fun next(): SmartFolderLayoutMode = when (this) {
        HYPEROS_PEEK -> CLASSIC_GRID
        CLASSIC_GRID -> CATEGORY_STACK
        CATEGORY_STACK -> HYPEROS_PEEK
    }
}

@Immutable
data class SmartFolderItem(
    val id: String,
    val title: String,
    val packageName: String = "",
    val tileType: TileType = TileType.APP,
    val customIconName: String = "",
    val category: AppCategory = AppCategory.TOOLS
)

@Immutable
data class SmartFolderConfig(
    val category: AppCategory = AppCategory.TOOLS,
    val autoSyncCategory: Boolean = true,
    val layoutMode: SmartFolderLayoutMode = SmartFolderLayoutMode.HYPEROS_PEEK,
    val items: List<SmartFolderItem> = emptyList(),
    val excludedKeys: Set<String> = emptySet()
) {
    fun toJsonString(): String {
        val root = org.json.JSONObject()
        root.put("category", category.name)
        root.put("autoSyncCategory", autoSyncCategory)
        root.put("layoutMode", layoutMode.name)

        val itemsArr = org.json.JSONArray()
        items.forEach { item ->
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("packageName", item.packageName)
                put("tileType", item.tileType.name)
                put("customIconName", item.customIconName)
                put("category", item.category.name)
            }
            itemsArr.put(obj)
        }
        root.put("items", itemsArr)

        val exclArr = org.json.JSONArray()
        excludedKeys.forEach { exclArr.put(it) }
        root.put("excludedKeys", exclArr)
        return root.toString()
    }

    companion object {
        fun fromTile(tile: TileEntity): SmartFolderConfig {
            val raw = tile.packageName.trim()
            if (raw.startsWith("{")) {
                try {
                    val obj = org.json.JSONObject(raw)
                    val cat = try {
                        AppCategory.valueOf(obj.optString("category", AppCategory.TOOLS.name))
                    } catch (_: Exception) {
                        AppCategory.TOOLS
                    }
                    val autoSync = obj.optBoolean("autoSyncCategory", true)
                    val mode = try {
                        SmartFolderLayoutMode.valueOf(
                            obj.optString("layoutMode", SmartFolderLayoutMode.HYPEROS_PEEK.name)
                        )
                    } catch (_: Exception) {
                        SmartFolderLayoutMode.HYPEROS_PEEK
                    }
                    val itemsList = mutableListOf<SmartFolderItem>()
                    val arr = obj.optJSONArray("items")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val itemObj = arr.optJSONObject(i) ?: continue
                            val itemType = try {
                                TileType.valueOf(itemObj.optString("tileType", TileType.APP.name))
                            } catch (_: Exception) {
                                TileType.APP
                            }
                            val itemCat = try {
                                AppCategory.valueOf(itemObj.optString("category", cat.name))
                            } catch (_: Exception) {
                                cat
                            }
                            itemsList.add(
                                SmartFolderItem(
                                    id = itemObj.optString("id", "sf_item_$i"),
                                    title = itemObj.optString("title", "Ứng dụng"),
                                    packageName = itemObj.optString("packageName", ""),
                                    tileType = itemType,
                                    customIconName = itemObj.optString("customIconName", ""),
                                    category = itemCat
                                )
                            )
                        }
                    }
                    val excluded = mutableSetOf<String>()
                    val exclArr = obj.optJSONArray("excludedKeys")
                    if (exclArr != null) {
                        for (i in 0 until exclArr.length()) {
                            val k = exclArr.optString(i, "")
                            if (k.isNotBlank()) excluded.add(k)
                        }
                    }
                    return SmartFolderConfig(
                        category = cat,
                        autoSyncCategory = autoSync,
                        layoutMode = mode,
                        items = itemsList,
                        excludedKeys = excluded
                    )
                } catch (_: Exception) {
                    // Fallback below
                }
            }
            val fallbackCategory = try {
                AppCategory.valueOf(tile.customIconName.uppercase())
            } catch (_: Exception) {
                AppCategory.TOOLS
            }
            return SmartFolderConfig(
                category = fallbackCategory,
                autoSyncCategory = true,
                layoutMode = SmartFolderLayoutMode.HYPEROS_PEEK,
                items = defaultSeedItemsForCategory(fallbackCategory)
            )
        }

        fun classifyTileCategory(
            tile: TileEntity,
            customCategories: Map<String, String> = emptyMap()
        ): AppCategory {
            return when (tile.tileType) {
                TileType.SMART_FOLDER -> fromTile(tile).category
                TileType.CLOCK_CALENDAR, TileType.WEATHER -> AppCategory.TOOLS
                TileType.BATTERY, TileType.PERFORMANCE -> AppCategory.SYSTEM
                TileType.PHOTOS -> AppCategory.MEDIA
                TileType.NOTES -> AppCategory.PRODUCTIVITY
                TileType.NEWS_QUOTE -> AppCategory.SOCIAL
                TileType.WIDGET -> {
                    val pkg = tile.packageName.ifBlank {
                        tile.widgetProvider.substringBefore("/")
                    }
                    val overrideCat = customCategories[pkg]
                    if (!overrideCat.isNullOrBlank()) {
                        try {
                            return AppCategory.valueOf(overrideCat)
                        } catch (_: Exception) {}
                    }
                    AppCategory.classifyApp(
                        packageName = pkg,
                        label = tile.title
                    )
                }
                TileType.APP -> {
                    val overrideCat = customCategories[tile.packageName]
                    if (!overrideCat.isNullOrBlank()) {
                        try {
                            return AppCategory.valueOf(overrideCat)
                        } catch (_: Exception) {}
                    }
                    AppCategory.classifyApp(
                        packageName = tile.packageName,
                        label = tile.title
                    )
                }
            }
        }

        // Empty folders are intentional. Installed apps are resolved below; never invent content.
        @Suppress("UNUSED_PARAMETER")
        fun defaultSeedItemsForCategory(category: AppCategory): List<SmartFolderItem> = emptyList()

        fun resolveEffectiveItems(
            config: SmartFolderConfig,
            installedApps: List<AppItem>,
            customCategories: Map<String, String> = emptyMap()
        ): List<SmartFolderItem> {
            val result = mutableListOf<SmartFolderItem>()
            val seenKeys = mutableSetOf<String>()

            fun keyFor(pkg: String, type: TileType, title: String): String {
                return if (pkg.isNotBlank()) "pkg:$pkg" else "type:${type.name}:${title.lowercase()}"
            }

            // 1. Explicit items in config
            for (item in config.items) {
                val k = keyFor(item.packageName, item.tileType, item.title)
                if (k !in config.excludedKeys && item.id !in config.excludedKeys && seenKeys.add(k)) {
                    result.add(item)
                }
            }

            // 2. Auto-synced installed apps matching category
            if (config.autoSyncCategory && config.category != AppCategory.ALL) {
                for (app in installedApps) {
                    val effCat = app.effectiveCategory(customCategories)
                    if (effCat == config.category) {
                        val k = keyFor(app.packageName, TileType.APP, app.label)
                        if (k !in config.excludedKeys && app.packageName !in config.excludedKeys && seenKeys.add(k)) {
                            result.add(
                                SmartFolderItem(
                                    id = "auto_app_${app.packageName}",
                                    title = app.label,
                                    packageName = app.packageName,
                                    tileType = TileType.APP,
                                    customIconName = "",
                                    category = effCat
                                )
                            )
                        }
                    }
                }
            }

            return result
        }
    }
}

enum class PageType {
    HOME,
    ALL_APPS
}

@Immutable
data class LauncherPage(
    val id: String,
    val title: String,
    val pageType: PageType = PageType.HOME,
    val orderIndex: Int = 0
)

enum class AllAppsViewMode(val label: String) {
    METRO_LIST("Danh sách dọc A-Z (Lumia)"),
    ONE_UI_GRID("Lưới biểu tượng (One UI / MIUI)")
}

enum class AllAppsSortOrder(val label: String) {
    NAME_ASC("Tên (A → Z)"),
    NAME_DESC("Tên (Z → A)"),
    USER_FIRST("Ứng dụng cá nhân trước")
}

enum class PageTransitionEffect(val label: String) {
    CLASSIC_SLIDE("Trượt phẳng chuẩn"),
    TURNSTILE_3D("Lật 3D Lumia"),
    MIUI_CUBE("Khối 3D MIUI"),
    ONE_UI_DEPTH("Chiều sâu One UI 9.0")
}

enum class Wp10TileStyle(val label: String, val description: String) {
    TINTED_TRANSLUCENT(
        "Trong suốt pha màu (W10M)",
        "Ô Tile trong suốt pha màu chủ đề xuyên thấu hình nền chuẩn Windows 10 Mobile"
    ),
    CRYSTAL_CLEAR(
        "Trong suốt hoàn toàn (Clear)",
        "Trong suốt 100% không màu nền, nổi bật icon và thông tin trực tiếp trên hình nền"
    ),
    FLUENT_ACRYLIC(
        "Kính mờ Fluent (W10M 1709)",
        "Hiệu ứng kính Acrylic đa lớp kèm viền sáng Reveal của bản cập nhật Fall Creators"
    ),
    CLASSIC_METRO(
        "Màu đặc Metro (Lumia 925)",
        "Ô Tile màu đặc truyền thống sắc nét của Windows Phone 8.1"
    );

    fun next(): Wp10TileStyle = when (this) {
        TINTED_TRANSLUCENT -> CRYSTAL_CLEAR
        CRYSTAL_CLEAR -> FLUENT_ACRYLIC
        FLUENT_ACRYLIC -> CLASSIC_METRO
        CLASSIC_METRO -> TINTED_TRANSLUCENT
    }
}

@Immutable
data class WidgetProviderItem(
    val providerComponent: String, // ComponentName.flattenToString() -> "pkg/class"
    val packageName: String,
    val className: String,
    val label: String,
    val appName: String,
    val description: String = "",
    val minWidthDp: Int = 110,
    val minHeightDp: Int = 110,
    val minResizeWidthDp: Int = 70,
    val minResizeHeightDp: Int = 70,
    val targetCellWidth: Int = 2,
    val targetCellHeight: Int = 2,
    val resizeMode: Int = 3, // AppWidgetProviderInfo.RESIZE_BOTH (1 = HORIZONTAL, 2 = VERTICAL, 3 = BOTH, 0 = NONE)
    val recommendedTileSize: TileSize = TileSize.MEDIUM,
    val allowedTileSizes: List<TileSize> = listOf(TileSize.SMALL, TileSize.MEDIUM, TileSize.WIDE, TileSize.LARGE),
    val hasConfigurationActivity: Boolean = false,
    val configureComponent: String = "",
    val updatePeriodMillis: Int = 0
)

@Immutable
@Entity(tableName = "tiles")
data class TileEntity(
    @PrimaryKey val id: String,
    val title: String,
    val packageName: String = "",
    val tileType: TileType = TileType.APP,
    val size: TileSize = TileSize.MEDIUM,
    val orderIndex: Int = 0,
    val pageId: String = "page_home_1",
    val gridX: Int = -1,
    val gridY: Int = -1,
    val colorOverrideHex: String? = null, // null = theme accent, "TRANSPARENT" = 100% clear tile, "#HEX" = custom color
    val customIconName: String = "",
    val notificationCount: Int = 0,
    val isLiveEnabled: Boolean = true,
    val appWidgetId: Int = -1,
    val widgetProvider: String = "",
    val widgetMinSpanX: Int = 1,
    val widgetMinSpanY: Int = 1,
    val widgetResizeMode: Int = 3
) {
    fun hasBoundOrLinkedWidget(): Boolean {
        return tileType == TileType.WIDGET || widgetProvider.isNotBlank() || appWidgetId > 0
    }

    fun allowedSizesForTile(): List<TileSize> {
        if (!hasBoundOrLinkedWidget()) {
            return TileSize.entries
        }
        val minX = widgetMinSpanX.coerceIn(1, 4)
        val minY = widgetMinSpanY.coerceIn(1, 4)
        val canResizeH = (widgetResizeMode and 1) != 0
        val canResizeV = (widgetResizeMode and 2) != 0

        if (!canResizeH && !canResizeV) {
            val fixed = TileSize.fromTargetSpans(minX, minY)
            return listOf(fixed, size).distinct()
        }

        val matching = TileSize.entries.filter { candidate ->
            val okX = if (canResizeH) candidate.spanX >= minX else candidate.spanX == minX.coerceAtLeast(2)
            val okY = if (canResizeV) candidate.spanY >= minY else candidate.spanY == minY.coerceAtLeast(2)
            okX && okY
        }
        return if (matching.isNotEmpty()) {
            (matching + size).distinct().sortedBy { it.ordinal }
        } else {
            listOf(TileSize.MEDIUM, TileSize.WIDE, TileSize.LARGE)
        }
    }
}

enum class AppCategory(
    val label: String,
    val shortLabel: String,
    val description: String,
    val badgeColorHex: String
) {
    ALL(
        label = "Tất cả",
        shortLabel = "Tất cả",
        description = "Toàn bộ ứng dụng đã cài đặt trên thiết bị",
        badgeColorHex = "#0078D7"
    ),
    COMMUNICATION(
        label = "Liên lạc",
        shortLabel = "Liên lạc",
        description = "Điện thoại, tin nhắn, danh bạ và trò chuyện",
        badgeColorHex = "#00B294"
    ),
    SOCIAL(
        label = "Giải trí & MXH",
        shortLabel = "Giải trí",
        description = "Mạng xã hội, video trực tuyến và giải trí",
        badgeColorHex = "#E81123"
    ),
    GAMES(
        label = "Trò chơi",
        shortLabel = "Trò chơi",
        description = "Game giải trí và dịch vụ trò chơi",
        badgeColorHex = "#FF8C00"
    ),
    MEDIA(
        label = "Đa phương tiện",
        shortLabel = "Ảnh & Nhạc",
        description = "Máy ảnh, thư viện ảnh, âm nhạc và video",
        badgeColorHex = "#881798"
    ),
    TOOLS(
        label = "Công cụ",
        shortLabel = "Công cụ",
        description = "Trình duyệt, lịch, máy tính, bản đồ và tiện ích",
        badgeColorHex = "#107C10"
    ),
    PRODUCTIVITY(
        label = "Tài chính & Việc",
        shortLabel = "Công việc",
        description = "Văn phòng, ghi chú, tài chính và mua sắm",
        badgeColorHex = "#0063B1"
    ),
    SYSTEM(
        label = "Hệ thống",
        shortLabel = "Hệ thống",
        description = "Cài đặt và dịch vụ cốt lõi của hệ điều hành",
        badgeColorHex = "#68768A"
    );

    companion object {
        val assignableCategories: List<AppCategory> = entries.filter { it != ALL }

        private val commKeywords = listOf(
            "dialer", "contacts", "mms", "messaging", "telecom", "phone", "call",
            "zalo", "telegram", "whatsapp", "messenger", "viber", "skype", "wechat",
            "discord", "signal", "email", "gmail", "outlook", "thunderbird",
            "điện thoại", "tin nhắn", "danh bạ", "liên lạc", "gọi điện", "thư"
        )

        private val gameKeywords = listOf(
            "game", "play.games", "roblox", "minecraft", "genshin", "pubg",
            "freefire", "arcade", "puzzle", "chess", "solitaire", "asphalt",
            "candycrush", "clash", "unity", "trò chơi", "cờ vua", "cờ tướng"
        )

        private val socialKeywords = listOf(
            "facebook", "instagram", "tiktok", "musically", "twitter", "threads",
            "reddit", "snapchat", "pinterest", "youtube", "netflix", "twitch",
            "bilibili", "spotify", "soundcloud", "xã hội", "giải trí"
        )

        private val mediaKeywords = listOf(
            "camera", "gallery", "photos", "music", "video", "audio", "podcast",
            "vlc", "player", "recorder", "soundrecorder", "mediaprovider",
            "máy ảnh", "thư viện", "ảnh", "nhạc", "video", "ghi âm"
        )

        private val prodKeywords = listOf(
            "docs", "sheets", "slides", "drive", "office", "word", "excel",
            "powerpoint", "keep", "note", "notion", "evernote", "pdf", "wallet",
            "bank", "pay", "finance", "shop", "store", "vending", "lazada",
            "shopee", "tiki", "news", "linkedin", "ghi chú", "văn phòng",
            "cửa hàng", "ch play", "play store", "tài chính", "ngân hàng"
        )

        private val toolKeywords = listOf(
            "browser", "chrome", "firefox", "edge", "opera", "webview",
            "calculator", "deskclock", "clock", "alarm", "calendar", "weather",
            "files", "documentsui", "filemanager", "explorer", "maps", "translate",
            "compass", "scanner", "flashlight", "keyboard", "inputmethod",
            "search", "googlequicksearchbox", "downloads", "download",
            "trình duyệt", "máy tính", "đồng hồ", "lịch", "thời tiết",
            "tệp", "quản lý tệp", "bản đồ", "dịch", "la bàn", "công cụ"
        )

        private val sysKeywords = listOf(
            "settings", "systemui", "packageinstaller", "permissioncontroller",
            "stk", "bluetooth", "nfc", "print", "telephony", "certinstaller",
            "vpndialogs", "shell", "cài đặt", "hệ thống", "bảo mật"
        )

        fun classifyApp(
            packageName: String,
            label: String,
            androidCategory: Int = -1,
            isSystemApp: Boolean = false
        ): AppCategory {
            val pkg = packageName.lowercase()
            val name = label.lowercase()

            // 1. Check Communication first (high priority in MIUI)
            if (commKeywords.any { pkg.contains(it) || name.contains(it) }) {
                return COMMUNICATION
            }

            // 2. Check Android ApplicationInfo.category (API 26+)
            when (androidCategory) {
                0 -> return GAMES // CATEGORY_GAME
                1, 2, 3 -> return MEDIA // CATEGORY_AUDIO, CATEGORY_VIDEO, CATEGORY_IMAGE
                4 -> return SOCIAL // CATEGORY_SOCIAL
                5, 6 -> return PRODUCTIVITY // CATEGORY_NEWS, CATEGORY_PRODUCTIVITY
                7, 8 -> return TOOLS // CATEGORY_MAPS, CATEGORY_ACCESSIBILITY
            }

            // 3. Keyword heuristics for Games
            if (gameKeywords.any { pkg.contains(it) || name.contains(it) }) {
                return GAMES
            }

            // 4. Keyword heuristics for Social & Entertainment
            if (socialKeywords.any { pkg.contains(it) || name.contains(it) }) {
                return SOCIAL
            }

            // 5. Keyword heuristics for Media & Photography
            if (mediaKeywords.any { pkg.contains(it) || name.contains(it) }) {
                return MEDIA
            }

            // 6. Keyword heuristics for Productivity, Finance & Shopping
            if (prodKeywords.any { pkg.contains(it) || name.contains(it) }) {
                return PRODUCTIVITY
            }

            // 7. Keyword heuristics for Tools & Utilities
            if (toolKeywords.any { pkg.contains(it) || name.contains(it) }) {
                return TOOLS
            }

            // 8. System apps & Settings
            if (sysKeywords.any { pkg.contains(it) || name.contains(it) } || isSystemApp) {
                return SYSTEM
            }

            return TOOLS
        }
    }
}

@Immutable
data class AppItem(
    val label: String,
    val packageName: String,
    val versionName: String = "",
    val isSystemApp: Boolean = false,
    val isPinned: Boolean = false,
    val category: AppCategory = AppCategory.classifyApp(packageName, label, -1, isSystemApp),
    val isCustomCategory: Boolean = false
) {
    fun effectiveCategory(customCategories: Map<String, String>): AppCategory {
        val overrideName = customCategories[packageName]
        if (!overrideName.isNullOrBlank()) {
            return try {
                AppCategory.valueOf(overrideName)
            } catch (_: Exception) {
                category
            }
        }
        return category
    }
}

@Immutable
data class SystemTelemetry(
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val isSystemPowerSaveMode: Boolean = false,
    val batteryTempC: Float = 28.5f,
    val availableRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val ramUsagePct: Int = 0,
    val backgroundAppsCount: Int = 0,
    val lastStoppedAppsCount: Int = 0,
    val lastFreedRamMb: Long = 0,
    val internalFreeGb: Float = 0f,
    val internalTotalGb: Float = 0f,
    val uptimeHours: Long = 0,
    val deviceModel: String = "Lumia 925 Special Edition",
    val isDefaultLauncher: Boolean = false,
    val currentDefaultLauncherName: String = "Hệ thống"
)

enum class BatterySaverMode(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    AUTO(
        label = "Tự động khi pin yếu",
        shortLabel = "Tự động",
        description = "Tự động giảm tần suất Live Tile và tối giản hiệu ứng 3D khi pin thấp hoặc bật Tiết kiệm pin hệ thống"
    ),
    ALWAYS_ON(
        label = "Luôn bật (Tối giản mượt mà)",
        shortLabel = "Luôn bật",
        description = "Luôn tối giản hiệu ứng chuyển động và giảm tần suất cập nhật ngầm để đạt độ mượt tối đa"
    ),
    OFF(
        label = "Tắt",
        shortLabel = "Tắt",
        description = "Luôn giữ đầy đủ hiệu ứng chuyển động 3D và tần suất cập nhật Live Tile tiêu chuẩn"
    )
}

enum class DynamicAccentMode(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    WP10_VIBRANT(
        label = "Chuẩn Windows 10 Mobile Rực rỡ",
        shortLabel = "W10M Rực rỡ",
        description = "Trích xuất màu chủ đạo từ hình nền và tăng độ bão hòa chuẩn Live Tile Windows 10 Mobile"
    ),
    MATERIAL_YOU_TONAL(
        label = "Material You Tonal Dịu mắt",
        shortLabel = "Material You",
        description = "Pha trộn sắc thái Tonal Palette dịu mắt chuẩn Android 12+ và One UI Color Palette"
    ),
    HARMONIC_GLOW(
        label = "Tương phản Ánh sáng (Harmonic)",
        shortLabel = "Tương phản",
        description = "Tự động chọn sắc thái bổ trợ từ dải chuyển sắc hình nền để ô Tile nổi bật có chiều sâu"
    )
}

@Immutable
data class AppFolder(
    val id: String,
    val title: String,
    val appPackages: List<String>,
    val category: AppCategory = AppCategory.ALL,
    val orderIndex: Int = 0
)

enum class WallpaperSource { SYSTEM, CUSTOM, LUMIA }

@Immutable
data class LauncherSettings(
    val accentColorHex: String = "#0078D7", // Lumia Cyan
    val autoWallpaperAccentEnabled: Boolean = true, // Windows 10 Mobile "Automatically pick an accent color from my background"
    val dynamicAccentMode: DynamicAccentMode = DynamicAccentMode.WP10_VIBRANT,
    val dynamicSwatchIndex: Int = 0, // 0..3 Material You tonal swatch index extracted from wallpaper
    val isDarkTheme: Boolean = true,
    val wp10TileStyle: Wp10TileStyle = Wp10TileStyle.TINTED_TRANSLUCENT,
    val tileOpacity: Float = 0.45f, // 0.0f = 100% Transparent (Clear), 1.0f = 0% Transparent (Solid)
    val wp10RevealBorder: Boolean = true, // Viền mảnh Fluent Reveal trên ô Tile trong suốt
    val showMoreTiles: Boolean = false, // false = 4 columns (Lumia 925 default), true = 6 columns (Phablet/One UI style)
    val tileCornerRadiusDp: Int = 2, // 0..2 = Crisp W10M, 4 = Lumia subtle, 12 = One UI / MIUI squircle
    val tileSpacingDp: Int = 6, // 4 = Sát cạnh WP, 6 = Chuẩn W10M, 8 = Lumia 925, 12 = One UI / MIUI
    val useRealAppIcons: Boolean = true,
    val miuiGlassBorder: Boolean = true,
    val pageTransitionEffect: PageTransitionEffect = PageTransitionEffect.TURNSTILE_3D,
    val doubleTapToGlance: Boolean = true,
    val lockHomeLayout: Boolean = false,
    val autoArrangeTiles: Boolean = false,
    val fullWallpaperEnabled: Boolean = true,
    val wallpaperSource: WallpaperSource = WallpaperSource.SYSTEM,
    val wallpaperGradientIndex: Int = 1, // Windows 10 Mobile Hero default
    val customWallpaperUri: String = "", // User-selected custom photo wallpaper path/URI
    val wallpaperDimAlpha: Float = 0.18f, // Minimalist dark overlay over wallpaper for legibility
    val turnstileAnimationEnabled: Boolean = true,
    val glanceScreenEnabled: Boolean = true,
    val batterySaverMode: BatterySaverMode = BatterySaverMode.AUTO,
    val batterySaverThresholdPct: Int = 25,
    val autoOptimizeBackgroundApps: Boolean = true,
    val quickNotes: String = "Chào mừng trở lại Windows 10 Mobile! Ô Tile hỗ trợ tuỳ chỉnh trong suốt xuyên thấu từ 0% đến 100%.",
    val cloudSyncState: String = "Đã đồng bộ cục bộ",
    val defaultHomePageId: String = "page_home_1",
    val showPageHeaderTitle: Boolean = false,
    val allAppsViewMode: AllAppsViewMode = AllAppsViewMode.METRO_LIST,
    val allAppsGridColumns: Int = 4,
    val allAppsSortOrder: AllAppsSortOrder = AllAppsSortOrder.NAME_ASC,
    val allAppsShowSearchBar: Boolean = true,
    val allAppsShowLetterHeaders: Boolean = true,
    val allAppsCategorizedEnabled: Boolean = true,
    val allAppsGroupByCategory: Boolean = false,
    val customAppCategories: Map<String, String> = emptyMap(),
    val appDrawerFolders: List<AppFolder> = emptyList(),
    val appDrawerCustomOrder: List<String> = emptyList()
) {
    fun isBatterySaverActive(telemetry: SystemTelemetry): Boolean {
        return when (batterySaverMode) {
            BatterySaverMode.ALWAYS_ON -> true
            BatterySaverMode.OFF -> false
            BatterySaverMode.AUTO -> {
                telemetry.isSystemPowerSaveMode ||
                    (!telemetry.isCharging && telemetry.batteryPct <= batterySaverThresholdPct.coerceIn(10, 50))
            }
        }
    }

    fun effectiveLiveTileIntervalMs(telemetry: SystemTelemetry, baseIntervalMs: Long): Long {
        return if (isBatterySaverActive(telemetry)) {
            baseIntervalMs * 3L
        } else {
            baseIntervalMs
        }
    }

    fun effectiveTelemetryPollIntervalMs(telemetry: SystemTelemetry): Long {
        return if (isBatterySaverActive(telemetry)) {
            30_000L
        } else {
            12_000L
        }
    }

    fun effectivePageTransitionEffect(telemetry: SystemTelemetry): PageTransitionEffect {
        return if (isBatterySaverActive(telemetry)) {
            PageTransitionEffect.CLASSIC_SLIDE
        } else {
            pageTransitionEffect
        }
    }
}

