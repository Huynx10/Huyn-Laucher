package com.example.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.model.TileEntity
import com.example.data.model.TileType
import com.example.data.repository.LauncherRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30, 31])
class WidgetReconfigurationTest {
    private lateinit var context: Context
    private lateinit var manager: LumiaWidgetHostManager
    private lateinit var widgetManager: AppWidgetManager
    private val provider = ComponentName("example.widgets", "example.widgets.ClockWidget")
    private val configActivity = ComponentName("example.widgets", "example.widgets.ClockSettings")
    private val widgetId = 201

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        widgetManager = AppWidgetManager.getInstance(context)
        // Fresh manager per test avoids state leakage from the production process singleton.
        val ctor = LumiaWidgetHostManager::class.java.getDeclaredConstructor(Context::class.java)
        ctor.isAccessible = true
        manager = ctor.newInstance(context)
    }

    private fun boundTile(
        configured: Boolean = true,
        flags: Int = AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE
    ): TileEntity {
        val info = AppWidgetProviderInfo().apply {
            provider = this@WidgetReconfigurationTest.provider
            configure = if (configured) configActivity else null
            widgetFeatures = flags
        }
        shadowOf(widgetManager).addBoundWidget(widgetId, info)
        return TileEntity(
            "saved_widget", "Clock", tileType = TileType.WIDGET,
            appWidgetId = widgetId, widgetProvider = provider.flattenToShortString(),
            pageId = "page_home_2", gridX = 2, gridY = 5, orderIndex = 7
        )
    }

    @Test fun onlyMatchingLiveBindingLaunchesExpectedId() {
        val tile = boundTile()
        // Legacy backups may contain the canonical (not shortened) component name.
        assertTrue(manager.canReconfigureTile(tile.copy(widgetProvider = provider.flattenToString())))
        var launched = -1
        assertEquals(
            WidgetReconfigureResult.STARTED,
            manager.startWidgetReconfiguration(tile) { launched = it }
        )
        assertEquals(widgetId, launched)
    }

    @Test fun invalidIdUnboundProviderMismatchAndAppTileNeverLaunch() {
        val tile = boundTile()
        val rejected = listOf(
            tile.copy(appWidgetId = -1), tile.copy(appWidgetId = 999),
            tile.copy(widgetProvider = "example.other/.ClockWidget"),
            tile.copy(widgetProvider = "not-a-component"), tile.copy(tileType = TileType.APP)
        )
        var launches = 0
        rejected.forEach {
            assertFalse(manager.canReconfigureTile(it))
            assertEquals(WidgetReconfigureResult.UNSUPPORTED, manager.startWidgetReconfiguration(it) { launches++ })
        }
        assertEquals(0, launches)
    }

    @Test fun missingConfigureActivityHidesActionAndDoesNotLaunch() {
        val tile = boundTile(configured = false)
        assertFalse(manager.canReconfigureTile(tile))
        assertEquals(
            WidgetReconfigureResult.UNSUPPORTED,
            manager.startWidgetReconfiguration(tile) { error("Unsupported widget launched") }
        )
    }

    @Test fun android12RequiresFeatureFlagWhileAndroid11AcceptsConfiguredProvider() {
        val tile = boundTile(flags = 0)
        val shouldLaunch = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        assertEquals(shouldLaunch, manager.canReconfigureTile(tile))
        var launches = 0
        assertEquals(
            if (shouldLaunch) WidgetReconfigureResult.STARTED else WidgetReconfigureResult.UNSUPPORTED,
            manager.startWidgetReconfiguration(tile) { launches++ }
        )
        assertEquals(if (shouldLaunch) 1 else 0, launches)
    }

    @Test fun cancelAndHostFailuresDoNotChangeExistingWidgetPlacementOrBackup() = runBlocking {
        val tile = boundTile()
        val db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val repo = LauncherRepository(context, db.tileDao(), db)
            db.tileDao().insertOrUpdate(tile)
            val tilesBefore = db.tileDao().getAllTiles()
            val pagesBefore = repo.loadPages()
            val backupBefore = JSONObject(repo.exportBackupJson())

            // Provider UI may return RESULT_CANCELED; this host flow has no follow-up write
            // or deletion (unlike the separate new-widget binding/configuration flow).
            assertEquals(WidgetReconfigureResult.STARTED, manager.startWidgetReconfiguration(tile) {
                assertEquals(widgetId, it)
            })
            assertEquals(
                WidgetReconfigureResult.ACTIVITY_MISSING,
                manager.startWidgetReconfiguration(tile) { throw ActivityNotFoundException("missing") }
            )
            assertEquals(
                WidgetReconfigureResult.ACCESS_DENIED,
                manager.startWidgetReconfiguration(tile) { throw SecurityException("blocked") }
            )
            assertEquals(
                WidgetReconfigureResult.FAILED,
                manager.startWidgetReconfiguration(tile) { throw IllegalStateException("broken") }
            )
            assertEquals(tilesBefore, db.tileDao().getAllTiles())
            assertEquals(pagesBefore, repo.loadPages())
            val backupAfter = JSONObject(repo.exportBackupJson())
            assertEquals(backupBefore.getJSONArray("tiles").toString(), backupAfter.getJSONArray("tiles").toString())
            assertEquals(backupBefore.getJSONArray("pages").toString(), backupAfter.getJSONArray("pages").toString())
            assertNotNull(widgetManager.getAppWidgetInfo(widgetId))
        } finally {
            db.close()
        }
    }
}
