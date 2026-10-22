package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.LumiaDatabase
import com.example.data.model.AppItem
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.PageType
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.repository.LauncherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LumiaDatabase.getDatabase(application)
    private val repository = LauncherRepository(application, database.tileDao())

    val tiles: StateFlow<List<TileEntity>> = repository.tilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pages = MutableStateFlow(repository.loadPages())
    val pages: StateFlow<List<LauncherPage>> = _pages.asStateFlow()

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _telemetry = MutableStateFlow(repository.getSystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _selectedTileId = MutableStateFlow<String?>(null)
    val selectedTileId: StateFlow<String?> = _selectedTileId.asStateFlow()

    private val _isPageManagerOpen = MutableStateFlow(false)
    val isPageManagerOpen: StateFlow<Boolean> = _isPageManagerOpen.asStateFlow()

    private val _isGlanceScreenOpen = MutableStateFlow(false)
    val isGlanceScreenOpen: StateFlow<Boolean> = _isGlanceScreenOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isAddTileSheetOpen = MutableStateFlow(false)
    val isAddTileSheetOpen: StateFlow<Boolean> = _isAddTileSheetOpen.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultTiles()
            refreshApps()
        }

        viewModelScope.launch {
            while (true) {
                delay(10000)
                _telemetry.value = repository.getSystemTelemetry()
            }
        }
    }

    /**
     * Long-pressing an empty area on the Home screen enters Home Screen Edit Mode (One UI style).
     */
    fun enterEditMode() {
        _selectedTileId.value = null
        _isEditMode.value = true
    }

    fun toggleEditMode() {
        val next = !_isEditMode.value
        _isEditMode.value = next
        if (next) {
            _selectedTileId.value = null
        }
    }

    fun closeEditMode() {
        _isEditMode.value = false
        _selectedTileId.value = null
    }

    /**
     * Long-pressing a single Tile selects ONLY that single tile for editing (resize, move freely, customize, unpin)
     * without entering global Home Screen Edit Mode or showing the bottom edit bar.
     */
    fun selectTileForEdit(tileId: String?) {
        _selectedTileId.value = tileId
        if (tileId != null) {
            _isEditMode.value = false
        }
    }

    fun clearSelectedTile() {
        _selectedTileId.value = null
    }

    fun openAddTileSheet() {
        _isAddTileSheetOpen.value = true
    }

    fun closeAddTileSheet() {
        _isAddTileSheetOpen.value = false
    }

    fun openPageManager() {
        _isPageManagerOpen.value = true
    }

    fun closePageManager() {
        _isPageManagerOpen.value = false
    }

    fun addHomePage(title: String) {
        val current = _pages.value.toMutableList()
        val newPage = LauncherPage(
            id = "page_home_${System.currentTimeMillis()}",
            title = title,
            pageType = PageType.HOME,
            orderIndex = current.size
        )
        // Insert new Home page right before All Apps if All Apps is at the end, else at the end
        val allAppsIdx = current.indexOfLast { it.pageType == PageType.ALL_APPS }
        if (allAppsIdx != -1 && allAppsIdx == current.size - 1) {
            current.add(allAppsIdx, newPage)
        } else {
            current.add(newPage)
        }
        val reindexed = current.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
        _pages.value = reindexed
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePages(reindexed)
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val current = _pages.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            val reindexed = current.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
            _pages.value = reindexed
            viewModelScope.launch(Dispatchers.IO) {
                repository.savePages(reindexed)
            }
        }
    }

    fun renamePage(pageId: String, newTitle: String) {
        val updated = _pages.value.map {
            if (it.id == pageId) it.copy(title = newTitle) else it
        }
        _pages.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePages(updated)
        }
    }

    fun deleteHomePage(pageId: String) {
        val homePages = _pages.value.filter { it.pageType == PageType.HOME }
        if (homePages.size <= 1) return
        val remaining = _pages.value.filter { it.id != pageId }
            .mapIndexed { idx, p -> p.copy(orderIndex = idx) }
        val fallbackHomeId = remaining.firstOrNull { it.pageType == PageType.HOME }?.id ?: "page_home_1"

        _pages.value = remaining
        if (_settings.value.defaultHomePageId == pageId) {
            updateSettings(_settings.value.copy(defaultHomePageId = fallbackHomeId))
        }

        viewModelScope.launch {
            repository.savePages(remaining)
            repository.reassignTilesFromDeletedPage(pageId, fallbackHomeId)
        }
    }

    fun setDefaultHomePage(pageId: String) {
        updateSettings(_settings.value.copy(defaultHomePageId = pageId))
    }

    fun resizeTile(tileId: String) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            repository.resizeTile(tileId)
        }
    }

    fun stepTileSize(tileId: String, increase: Boolean) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            repository.stepTileSize(tileId, increase)
        }
    }

    fun setTileSize(tileId: String, newSize: TileSize) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            repository.setTileSize(tileId, newSize)
        }
    }

    fun updateTileCustomization(
        tileId: String,
        colorOverrideHex: String?,
        customTitle: String,
        isLiveEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.updateTileCustomization(tileId, colorOverrideHex, customTitle, isLiveEnabled)
        }
    }

    fun unpinTile(tileId: String) {
        if (_settings.value.lockHomeLayout) return
        if (_selectedTileId.value == tileId) {
            _selectedTileId.value = null
        }
        viewModelScope.launch {
            repository.unpinTile(tileId)
            refreshApps()
        }
    }

    fun moveTileInPage(tileId: String, pageId: String, moveUp: Boolean) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            repository.moveTileInPage(tileId, pageId, moveUp)
        }
    }

    fun moveTileToGridPosition(tileId: String, gridX: Int, gridY: Int, totalCols: Int = 4) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            repository.moveTileToGridPosition(tileId, gridX, gridY, totalCols)
        }
    }

    fun nudgeTileByDelta(tileId: String, deltaX: Int, deltaY: Int, totalCols: Int = 4) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            repository.nudgeTileByDelta(tileId, deltaX, deltaY, totalCols)
        }
    }

    fun reorderTilesInPage(pageId: String, fromIndex: Int, toIndex: Int) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            repository.reorderTilesInPage(pageId, fromIndex, toIndex)
        }
    }

    fun moveTileToPage(tileId: String, targetPageId: String) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            repository.moveTileToPage(tileId, targetPageId)
        }
    }

    fun pinAppToPage(app: AppItem, pageId: String = _settings.value.defaultHomePageId) {
        viewModelScope.launch {
            repository.pinApp(app, pageId)
            refreshApps()
        }
    }

    fun pinTile(tile: TileEntity) {
        viewModelScope.launch {
            repository.pinTile(tile)
        }
    }

    fun launchApp(packageName: String) {
        repository.launchApp(packageName)
    }

    fun optimizeMemory(): SystemTelemetry {
        val fresh = repository.optimizeMemory()
        _telemetry.value = fresh
        return fresh
    }

    fun refreshTelemetry() {
        _telemetry.value = repository.getSystemTelemetry()
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pinned = tiles.value.map { it.packageName }.filter { it.isNotEmpty() }.toSet()
            _apps.value = repository.getInstalledApps(pinned)
            _telemetry.value = repository.getSystemTelemetry()
        }
    }

    fun openGlanceScreen() {
        _isGlanceScreenOpen.value = true
    }

    fun closeGlanceScreen() {
        _isGlanceScreenOpen.value = false
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun updateSettings(newSettings: LauncherSettings) {
        val prevCustom = _settings.value.customAppCategories
        val syncedSettings = if (newSettings.autoWallpaperAccentEnabled) {
            val autoHex = com.example.ui.theme.DynamicAccentEngine.resolveAutoAccent(
                wallpaperIndex = newSettings.wallpaperGradientIndex,
                mode = newSettings.dynamicAccentMode,
                swatchIndex = newSettings.dynamicSwatchIndex
            ).hex
            newSettings.copy(accentColorHex = autoHex)
        } else {
            newSettings
        }
        _settings.value = syncedSettings
        if (prevCustom != syncedSettings.customAppCategories) {
            _apps.value = _apps.value.map { app ->
                val overrideCat = syncedSettings.customAppCategories[app.packageName]
                if (!overrideCat.isNullOrBlank()) {
                    val parsed = try {
                        com.example.data.model.AppCategory.valueOf(overrideCat)
                    } catch (_: Exception) {
                        app.category
                    }
                    app.copy(category = parsed, isCustomCategory = true)
                } else {
                    app.copy(
                        category = com.example.data.model.AppCategory.classifyApp(
                            packageName = app.packageName,
                            label = app.label,
                            isSystemApp = app.isSystemApp
                        ),
                        isCustomCategory = false
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(syncedSettings)
        }
    }

    suspend fun exportBackup(): String {
        return repository.exportBackupJson()
    }

    suspend fun restoreBackup(json: String): Boolean {
        val success = repository.restoreFromJson(json)
        if (success) {
            _settings.value = repository.loadSettings()
            _pages.value = repository.loadPages()
            refreshApps()
        }
        return success
    }

    fun resetDefaults() {
        viewModelScope.launch {
            repository.resetToDefaultTiles()
            _pages.value = repository.loadPages()
            refreshApps()
        }
    }
}
