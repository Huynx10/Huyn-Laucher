package com.example.data.model

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
}

enum class TileType {
    APP,
    CLOCK_CALENDAR,
    WEATHER,
    BATTERY,
    PERFORMANCE,
    PHOTOS,
    NOTES,
    NEWS_QUOTE
}

enum class PageType {
    HOME,
    ALL_APPS
}

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

@Entity(tableName = "tiles")
data class TileEntity(
    @PrimaryKey val id: String,
    val title: String,
    val packageName: String = "",
    val tileType: TileType = TileType.APP,
    val size: TileSize = TileSize.MEDIUM,
    val orderIndex: Int = 0,
    val pageId: String = "page_home_1",
    val colorOverrideHex: String? = null,
    val customIconName: String = "",
    val notificationCount: Int = 0,
    val isLiveEnabled: Boolean = true
)

data class AppItem(
    val label: String,
    val packageName: String,
    val versionName: String = "",
    val isSystemApp: Boolean = false,
    val isPinned: Boolean = false
)

data class SystemTelemetry(
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val batteryTempC: Float = 28.5f,
    val availableRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val ramUsagePct: Int = 0,
    val internalFreeGb: Float = 0f,
    val internalTotalGb: Float = 0f,
    val uptimeHours: Long = 0,
    val deviceModel: String = "Lumia 925 Special Edition",
    val isDefaultLauncher: Boolean = false,
    val currentDefaultLauncherName: String = "Hệ thống"
)

data class LauncherSettings(
    val accentColorHex: String = "#0078D7", // Lumia Cyan
    val isDarkTheme: Boolean = true,
    val tileOpacity: Float = 0.85f,
    val showMoreTiles: Boolean = false, // false = 4 columns (Lumia 925 default), true = 6 columns (Phablet/One UI style)
    val tileCornerRadiusDp: Int = 4, // 0 = Pure Sharp WP8/10, 4 = Lumia subtle, 12 = One UI / MIUI squircle
    val tileSpacingDp: Int = 8, // 4 = Sát cạnh WP8.1, 8 = Lumia 925, 12 = Thoáng One UI / MIUI
    val useRealAppIcons: Boolean = true,
    val miuiGlassBorder: Boolean = true,
    val pageTransitionEffect: PageTransitionEffect = PageTransitionEffect.TURNSTILE_3D,
    val doubleTapToGlance: Boolean = true,
    val lockHomeLayout: Boolean = false,
    val fullWallpaperEnabled: Boolean = true,
    val wallpaperGradientIndex: Int = 0,
    val turnstileAnimationEnabled: Boolean = true,
    val glanceScreenEnabled: Boolean = true,
    val quickNotes: String = "Chào mừng trở lại Lumia 925! Chạm đúp khoảng trống để mở Glance, nhấn giữ để tùy chỉnh.",
    val cloudSyncState: String = "Đã đồng bộ cục bộ",
    val defaultHomePageId: String = "page_home_1",
    val showPageHeaderTitle: Boolean = false,
    val allAppsViewMode: AllAppsViewMode = AllAppsViewMode.METRO_LIST,
    val allAppsGridColumns: Int = 4,
    val allAppsSortOrder: AllAppsSortOrder = AllAppsSortOrder.NAME_ASC,
    val allAppsShowSearchBar: Boolean = true,
    val allAppsShowLetterHeaders: Boolean = true
)
