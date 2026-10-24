package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.db.LumiaDatabase
import com.example.data.model.AppCategory
import com.example.data.model.AppItem
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.PageType
import com.example.data.model.SmartFolderConfig
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.WidgetProviderItem
import com.example.data.remote.CloudBackup
import com.example.data.remote.UserProfile
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.LauncherRepository
import com.example.ui.auth.GoogleSignInHelper
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface CloudAuthState {
    data object Unauthenticated : CloudAuthState
    data object Authenticating : CloudAuthState
    data class Authenticated(
        val uid: String,
        val displayName: String,
        val email: String?,
        val photoUrl: String?
    ) : CloudAuthState
    data class Error(val message: String) : CloudAuthState
}

data class SmartFolderOpenRequest(
    val tileId: String,
    val pivotFractionX: Float = 0.5f,
    val pivotFractionY: Float = 0.5f
)

class LauncherViewModel(
    private val repository: LauncherRepository,
    private val cloudSyncRepository: CloudSyncRepository?,
    private val pagePersistenceDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val tiles: StateFlow<List<TileEntity>> = repository.tilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pages = MutableStateFlow(repository.loadPages())
    val pages: StateFlow<List<LauncherPage>> = _pages.asStateFlow()
    private val pendingPages = Channel<List<LauncherPage>>(Channel.CONFLATED)

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val _installedWidgets = MutableStateFlow<List<WidgetProviderItem>>(emptyList())
    val installedWidgets: StateFlow<List<WidgetProviderItem>> = _installedWidgets.asStateFlow()

    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()
    private val pendingSettings = Channel<LauncherSettings>(Channel.CONFLATED)
    private val settingsMutationMutex = Mutex()
    private val _isSettingsBusy = MutableStateFlow(false)
    val isSettingsBusy: StateFlow<Boolean> = _isSettingsBusy.asStateFlow()

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

    private val _activeSmartFolderRequest = MutableStateFlow<SmartFolderOpenRequest?>(null)
    val activeSmartFolderRequest: StateFlow<SmartFolderOpenRequest?> = _activeSmartFolderRequest.asStateFlow()

    // --- Firebase Auth & Firestore Cloud Backup State ---
    private val _cloudAuthState = MutableStateFlow<CloudAuthState>(CloudAuthState.Unauthenticated)
    val cloudAuthState: StateFlow<CloudAuthState> = _cloudAuthState.asStateFlow()

    private val _cloudBackups = MutableStateFlow<List<CloudBackup>>(emptyList())
    val cloudBackups: StateFlow<List<CloudBackup>> = _cloudBackups.asStateFlow()

    private val _cloudUserProfile = MutableStateFlow<UserProfile?>(null)
    val cloudUserProfile: StateFlow<UserProfile?> = _cloudUserProfile.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _cloudSyncMessage = MutableStateFlow<String?>(null)
    val cloudSyncMessage: StateFlow<String?> = _cloudSyncMessage.asStateFlow()

    private var activeCloudUserId: String? = null
    private var cloudBackupsJob: Job? = null
    private var cloudProfileJob: Job? = null
    private var appRefreshJob: Job? = null
    private var lastAppRefreshAtMs: Long = -1L

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            val displayName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Người dùng Lumia"
            _cloudAuthState.value = CloudAuthState.Authenticated(
                uid = user.uid,
                displayName = displayName,
                email = user.email,
                photoUrl = user.photoUrl?.toString()
            )
            startObservingCloudData(user.uid, displayName)
        } else {
            stopObservingCloudData()
            _cloudAuthState.value = CloudAuthState.Unauthenticated
        }
    }

    init {
        viewModelScope.launch(pagePersistenceDispatcher) {
            for (snapshot in pendingPages) {
                settingsMutationMutex.withLock {
                    // A restore/reset may have replaced the layout while this write was queued.
                    if (snapshot != _pages.value) return@withLock
                    try {
                        repository.savePages(snapshot)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.e(TAG, "Page save failed", error)
                        _cloudSyncMessage.value = "Không lưu được danh sách trang. Hãy thử lại."
                    }
                }
            }
        }

        // One writer per launcher: rapid slider/toggle changes cannot start concurrent Room reflows.
        viewModelScope.launch(Dispatchers.IO) {
            for (candidate in pendingSettings) {
                settingsMutationMutex.withLock {
                    if (candidate != _settings.value) return@withLock
                    var heavy = false
                    try {
                        val saved = repository.loadSettings()
                        heavy = candidate.showMoreTiles != saved.showMoreTiles ||
                            (candidate.autoArrangeTiles && !saved.autoArrangeTiles)
                        if (heavy) _isSettingsBusy.value = true
                        repository.saveSettingsWithReflow(candidate)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.e(TAG, "Settings save failed", error)
                        _cloudSyncMessage.value = "Không lưu được cài đặt. Hãy thử lại."
                    } finally {
                        if (heavy) _isSettingsBusy.value = false
                    }
                }
            }
        }
        viewModelScope.launch {
            // A corrupt provider/database must not take down the default HOME process.
            runLauncherBackgroundRead(onError = { error ->
                Log.e(TAG, "Home initialization failed", error)
                _cloudSyncMessage.value = "Không tải được dữ liệu Home. Hãy thử mở lại ứng dụng."
            }) {
                repository.ensureDefaultTiles()
                Unit
            }
            refreshApps()
        }


        if (cloudSyncRepository != null) {
            try {
                Firebase.auth.addAuthStateListener(authStateListener)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth not available; launcher remains usable offline", e)
            }
        } else {
            Log.i(TAG, "Firebase is not configured; local launcher features remain available")
        }
    }

    private fun startObservingCloudData(userId: String, displayName: String) {
        val cloud = cloudSyncRepository ?: return
        if (activeCloudUserId == userId && cloudBackupsJob?.isActive == true) return
        stopObservingCloudData()
        activeCloudUserId = userId

        cloudProfileJob = viewModelScope.launch {
            cloud.observeUserProfile(userId)
                .catch { error ->
                    Log.w(TAG, "Error observing cloud user profile", error)
                }
                .collect { profile ->
                    _cloudUserProfile.value = profile
                }
        }

        cloudBackupsJob = viewModelScope.launch {
            cloud.observeCloudBackups(userId)
                .catch { error ->
                    Log.w(TAG, "Error observing cloud backups", error)
                    _cloudSyncMessage.value = "Không thể tải danh sách sao lưu đám mây: ${error.localizedMessage ?: "Lỗi quyền truy cập"}"
                }
                .collect { backups ->
                    _cloudBackups.value = backups
                }
        }

        // Ensure user profile metadata exists on Firestore
        viewModelScope.launch(Dispatchers.IO) {
            val currentTilesCount = repository.tilesFlow.stateIn(viewModelScope).value.size
            val homePagesCount = _pages.value.count { it.pageType == PageType.HOME }.coerceAtLeast(1)
            cloud.upsertUserProfile(
                userId = userId,
                displayName = displayName,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                tileCount = currentTilesCount,
                pageCount = homePagesCount,
                accentColorHex = _settings.value.accentColorHex
            )
        }
    }

    private fun stopObservingCloudData() {
        cloudBackupsJob?.cancel()
        cloudBackupsJob = null
        cloudProfileJob?.cancel()
        cloudProfileJob = null
        activeCloudUserId = null
        _cloudBackups.value = emptyList()
        _cloudUserProfile.value = null
    }

    fun signInWithGoogleIdToken(idToken: String) {
        if (cloudSyncRepository == null) {
            onGoogleSignInError("Chưa cấu hình Firebase; các chức năng launcher vẫn hoạt động offline.")
            return
        }
        try {
            val auth = Firebase.auth
            if (auth.currentUser == null) {
                _cloudAuthState.value = CloudAuthState.Authenticating
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    _cloudSyncMessage.value = "Đã kết nối tài khoản Google với Firebase Cloud!"
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Firebase signInWithCredential failed", exception)
                    _cloudAuthState.value = CloudAuthState.Error(
                        exception.localizedMessage ?: "Xác thực Firebase thất bại"
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error during signInWithGoogleIdToken", e)
            _cloudAuthState.value = CloudAuthState.Error(
                e.localizedMessage ?: "Không thể kết nối Firebase Auth"
            )
        }
    }

    fun onGoogleSignInError(message: String) {
        _cloudAuthState.value = CloudAuthState.Error(message)
        _cloudSyncMessage.value = message
    }

    fun signOutFromCloud(context: Context) {
        viewModelScope.launch {
            GoogleSignInHelper.clearCredentialSession(context)
            try {
                Firebase.auth.signOut()
                _cloudSyncMessage.value = "Đã đăng xuất khỏi tài khoản đám mây."
            } catch (e: Exception) {
                Log.w(TAG, "Error signing out of Firebase", e)
            }
        }
    }

    fun syncToCloudNow(customTitle: String = "") {
        val cloud = cloudSyncRepository ?: run {
            _cloudSyncMessage.value = "Chưa cấu hình Firebase; hãy sử dụng sao lưu cục bộ."
            return
        }
        val currentState = _cloudAuthState.value
        if (currentState !is CloudAuthState.Authenticated) {
            _cloudSyncMessage.value = "Vui lòng đăng nhập Google trước khi đồng bộ lên đám mây."
            return
        }
        if (_isCloudSyncing.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isCloudSyncing.value = true
            try {
                val backupJson = repository.exportBackupJson()
                val timeFmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android Lumia" }
                val backupTitle = customTitle.trim().ifBlank { "Sao lưu Lumia ($timeFmt)" }
                val tileCount = tiles.value.size
                val pageCount = _pages.value.count { it.pageType == PageType.HOME }.coerceAtLeast(1)
                val accentHex = _settings.value.accentColorHex

                val createRes = cloud.createCloudBackup(
                    userId = currentState.uid,
                    title = backupTitle,
                    deviceName = deviceName,
                    backupPayloadJson = backupJson,
                    tileCount = tileCount,
                    pageCount = pageCount,
                    accentColorHex = accentHex
                )

                createRes.onSuccess { backupId ->
                    cloud.upsertUserProfile(
                        userId = currentState.uid,
                        displayName = currentState.displayName,
                        deviceModel = deviceName,
                        activeBackupId = backupId,
                        tileCount = tileCount,
                        pageCount = pageCount,
                        accentColorHex = accentHex
                    )
                    val updatedSettings = _settings.value.copy(
                        cloudSyncState = "Đã đồng bộ Firebase Cloud lúc $timeFmt (${currentState.email ?: currentState.displayName})"
                    )
                    _settings.value = updatedSettings
                    repository.saveSettingsWithReflow(updatedSettings)
                    _cloudSyncMessage.value = "Đã sao lưu bố cục lên Firebase Cloud thành công!"
                }.onFailure { err ->
                    _cloudSyncMessage.value = "Lỗi sao lưu đám mây: ${err.localizedMessage ?: "Không xác định"}"
                }
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun restoreFromCloudBackup(backup: CloudBackup, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                val ok = restoreBackup(backup.backupPayloadJson)
                if (ok) {
                    _cloudSyncMessage.value = "Đã khôi phục bố cục \"${backup.title}\" từ Firebase Cloud!"
                } else {
                    _cloudSyncMessage.value = "Bản sao lưu đám mây không hợp lệ hoặc bị hỏng."
                }
                onResult(ok)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun deleteCloudBackup(backupId: String) {
        val cloud = cloudSyncRepository ?: return
        val currentState = _cloudAuthState.value
        if (currentState !is CloudAuthState.Authenticated) return
        viewModelScope.launch(Dispatchers.IO) {
            cloud.deleteCloudBackup(currentState.uid, backupId)
                .onSuccess {
                    _cloudSyncMessage.value = "Đã xóa bản sao lưu trên đám mây."
                }
                .onFailure { err ->
                    _cloudSyncMessage.value = "Không thể xóa bản sao lưu: ${err.localizedMessage}"
                }
        }
    }

    fun clearCloudSyncMessage() {
        _cloudSyncMessage.value = null
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

    private fun persistPages(snapshot: List<LauncherPage>) {
        pendingPages.trySend(snapshot.toList())
    }

    fun addHomePage(title: String) {
        val current = _pages.value.toMutableList()
        val newPage = LauncherPage(
            id = "page_home_${UUID.randomUUID()}",
            title = title,
            pageType = PageType.HOME,
            orderIndex = current.size
        )
        val allAppsIdx = current.indexOfLast { it.pageType == PageType.ALL_APPS }
        if (allAppsIdx != -1 && allAppsIdx == current.size - 1) {
            current.add(allAppsIdx, newPage)
        } else {
            current.add(newPage)
        }
        val reindexed = current.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
        _pages.value = reindexed
        persistPages(reindexed)
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val current = _pages.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            val reindexed = current.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
            _pages.value = reindexed
            persistPages(reindexed)
        }
    }

    fun renamePage(pageId: String, newTitle: String) {
        val updated = _pages.value.map {
            if (it.id == pageId) it.copy(title = newTitle) else it
        }
        _pages.value = updated
        persistPages(updated)
    }

    fun deleteHomePage(pageId: String) {
        if (_pages.value.none { it.id == pageId && it.pageType == PageType.HOME }) return
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
            persistPages(remaining)
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

    fun pinWidgetTile(
        widgetItem: WidgetProviderItem,
        appWidgetId: Int,
        pageId: String = _settings.value.defaultHomePageId,
        preferredSize: TileSize? = null,
        onPinned: (TileEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val created = repository.pinWidgetTile(
                provider = widgetItem,
                appWidgetId = appWidgetId,
                pageId = pageId,
                preferredSize = preferredSize
            )
            onPinned(created)
        }
    }

    fun linkWidgetToExistingTile(
        tileId: String,
        widgetItem: WidgetProviderItem,
        appWidgetId: Int
    ) {
        viewModelScope.launch {
            repository.linkWidgetToExistingTile(
                tileId = tileId,
                provider = widgetItem,
                appWidgetId = appWidgetId
            )
        }
    }

    fun unlinkWidgetFromTile(tileId: String) {
        viewModelScope.launch {
            repository.unlinkWidgetFromTile(tileId)
        }
    }

    fun openSmartFolder(
        tileId: String,
        pivotFractionX: Float = 0.5f,
        pivotFractionY: Float = 0.5f
    ) {
        _selectedTileId.value = null
        _isEditMode.value = false
        _activeSmartFolderRequest.value = SmartFolderOpenRequest(
            tileId = tileId,
            pivotFractionX = pivotFractionX.coerceIn(0.1f, 0.9f),
            pivotFractionY = pivotFractionY.coerceIn(0.1f, 0.9f)
        )
    }

    fun closeSmartFolder() {
        _activeSmartFolderRequest.value = null
    }

    fun createSmartFolderByCategory(
        category: AppCategory,
        pageId: String = _settings.value.defaultHomePageId,
        size: TileSize = TileSize.MEDIUM,
        absorbMatchingPageTiles: Boolean = false
    ) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            repository.createSmartFolderByCategory(
                category = category,
                pageId = pageId,
                size = size,
                absorbMatchingPageTiles = absorbMatchingPageTiles
            )
            refreshApps()
        }
    }

    fun groupPageTilesByCategory(pageId: String, onDone: (Int) -> Unit = {}) {
        if (_settings.value.lockHomeLayout) return
        viewModelScope.launch {
            val count = repository.groupPageTilesByCategoryIntoSmartFolders(pageId)
            refreshApps()
            onDone(count)
        }
    }

    fun convertTileIntoSmartFolder(
        tileId: String,
        absorbSameCategoryOnPage: Boolean = true
    ) {
        if (_settings.value.lockHomeLayout) return
        _selectedTileId.value = null
        viewModelScope.launch {
            repository.convertTileIntoSmartFolder(tileId, absorbSameCategoryOnPage)
            refreshApps()
        }
    }

    fun updateSmartFolderConfig(
        tileId: String,
        newTitle: String,
        newConfig: SmartFolderConfig
    ) {
        viewModelScope.launch {
            repository.updateSmartFolderConfig(tileId, newTitle, newConfig)
        }
    }

    fun cycleSmartFolderLayoutMode(tileId: String) {
        val target = tiles.value.find { it.id == tileId } ?: return
        val currentConfig = SmartFolderConfig.fromTile(target)
        val nextConfig = currentConfig.copy(layoutMode = currentConfig.layoutMode.next())
        updateSmartFolderConfig(tileId, target.title, nextConfig)
    }

    fun ungroupSmartFolderToTiles(folderTileId: String) {
        if (_settings.value.lockHomeLayout) return
        if (_activeSmartFolderRequest.value?.tileId == folderTileId) {
            _activeSmartFolderRequest.value = null
        }
        if (_selectedTileId.value == folderTileId) {
            _selectedTileId.value = null
        }
        viewModelScope.launch {
            repository.ungroupSmartFolderToTiles(folderTileId)
            refreshApps()
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

    fun refreshApps(fromResume: Boolean = false) {
        // Avoid repeated package/widget scans from rapid HOME/resume events on One UI.
        // Explicit data changes still call refreshApps() without throttling.
        if (fromResume) {
            val now = SystemClock.elapsedRealtime()
            if (appRefreshJob?.isActive == true ||
                (lastAppRefreshAtMs >= 0L && now - lastAppRefreshAtMs in 0L..5_000L)
            ) return
        }
        lastAppRefreshAtMs = SystemClock.elapsedRealtime()
        // Explicit refresh replaces any stale in-flight scan instead of racing to publish old data.
        appRefreshJob?.cancel()
        appRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            val pinned = tiles.value.map { it.packageName }.filter { it.isNotEmpty() }.toSet()
            runLauncherBackgroundRead(onError = { Log.e(TAG, "App catalog scan failed", it) }) {
                repository.getInstalledApps(pinned)
            }?.let { _apps.value = it }
            runLauncherBackgroundRead(onError = { Log.e(TAG, "Widget provider scan failed", it) }) {
                repository.discoverInstalledWidgets()
            }?.let { _installedWidgets.value = it }
            runLauncherBackgroundRead(onError = { Log.e(TAG, "System telemetry read failed", it) }) {
                repository.getSystemTelemetry()
            }?.let { _telemetry.value = it }
        }
    }

    fun refreshWidgets() {
        viewModelScope.launch(Dispatchers.IO) {
            runLauncherBackgroundRead(onError = { Log.e(TAG, "Widget refresh failed", it) }) {
                repository.discoverInstalledWidgets()
            }?.let { _installedWidgets.value = it }
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

    fun setCustomWallpaper(uri: Uri) {
        viewModelScope.launch {
            val savedPath = repository.saveCustomWallpaperFromUri(uri)
            if (!savedPath.isNullOrBlank()) {
                updateSettings(
                    _settings.value.copy(
                        customWallpaperUri = savedPath,
                        wallpaperSource = com.example.data.model.WallpaperSource.CUSTOM,
                        fullWallpaperEnabled = true
                    )
                )
            }
        }
    }

    fun clearCustomWallpaper() {
        viewModelScope.launch {
            repository.clearCustomWallpaper()
            updateSettings(_settings.value.copy(customWallpaperUri = "", wallpaperSource = com.example.data.model.WallpaperSource.SYSTEM))
        }
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
        pendingSettings.trySend(syncedSettings)
    }

    suspend fun exportBackup(): String {
        return repository.exportBackupJson()
    }

    suspend fun restoreBackup(json: String): Boolean {
        return settingsMutationMutex.withLock {
            val success = repository.restoreFromJson(json)
            if (success) {
                _settings.value = repository.loadSettings()
                _pages.value = repository.loadPages()
                refreshApps()
            }
            success
        }
    }

    suspend fun resetDefaultsAwait() {
        settingsMutationMutex.withLock {
            repository.resetToDefaultTiles()
            _settings.value = repository.loadSettings()
            _pages.value = repository.loadPages()
            refreshApps()
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            try {
                resetDefaultsAwait()
            } catch (e: Exception) {
                Log.e(TAG, "Could not reset launcher layout", e)
                _cloudSyncMessage.value = "Không đặt lại được bố cục. Hãy thử lại."
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            Firebase.auth.removeAuthStateListener(authStateListener)
        } catch (_: Exception) {}
        stopObservingCloudData()
    }

    companion object {
        private const val TAG = "LauncherViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val database = LumiaDatabase.getDatabase(app)
                // The CI debug APK may intentionally lack google-services.json.
                // Firebase must never block the local Home screen from opening.
                val cloud = if (FirebaseApp.getApps(app).isEmpty()) {
                    null
                } else {
                    runCatching {
                        CloudSyncRepository(FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id)))
                    }.onFailure {
                        Log.w(TAG, "Cloud sync unavailable; starting local-only launcher", it)
                    }.getOrNull()
                }
                LauncherViewModel(
                    repository = LauncherRepository(app, database.tileDao()),
                    cloudSyncRepository = cloud
                )
            }
        }
    }
}
