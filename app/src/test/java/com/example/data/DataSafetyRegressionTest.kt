package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.db.TileDao
import com.example.data.model.*
import com.example.data.repository.LauncherRepository
import com.example.data.repository.validateCloudBackupJson
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DataSafetyRegressionTest {
    private lateinit var db: LumiaDatabase
    private lateinit var repo: LauncherRepository
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java).allowMainThreadQueries().build()
        repo = LauncherRepository(context, db.tileDao(), db)
    }
    @After fun close() = db.close()

    @Test fun legacyTileStyleTypesRecoverWithoutDroppingTilesOrWidgetLinks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val existing = TileEntity(
            "existing_widget", "Existing widget", tileType = TileType.WIDGET,
            appWidgetId = 127, widgetProvider = "example/.Provider", gridX = 0, gridY = 0
        )
        db.tileDao().insertOrUpdate(existing)
        assertTrue(prefs.edit().putInt("wp10_tile_style", 7)
            .putString("tile_opacity", "0.72").commit())

        val recovered = repo.loadSettings()
        assertEquals(Wp10TileStyle.TINTED_TRANSLUCENT, recovered.wp10TileStyle)
        assertEquals(0.72f, recovered.tileOpacity, 0.001f)
        assertEquals(listOf(existing), db.tileDao().getAllTiles())
        // A read is non-destructive: the user can still recover their original data.
        assertEquals(7, prefs.getInt("wp10_tile_style", -1))

        repo.saveSettingsWithReflow(recovered.copy(
            wp10TileStyle = Wp10TileStyle.CLASSIC_METRO, tileOpacity = 1f
        ))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(1f, repo.loadSettings().tileOpacity, 0f)
        assertEquals(listOf(existing), db.tileDao().getAllTiles())
    }

    @Test fun malformedOrNonFiniteOpacityNeverReachesTileRenderingSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.edit().putString("tile_opacity", "not-a-number").commit())
        assertEquals(0.45f, repo.loadSettings().tileOpacity, 0f)

        assertTrue(prefs.edit().putFloat("tile_opacity", Float.NaN).commit())
        assertEquals(0.45f, repo.loadSettings().tileOpacity, 0f)

        assertTrue(prefs.edit().putFloat("tile_opacity", Float.NEGATIVE_INFINITY).commit())
        assertEquals(0.45f, repo.loadSettings().tileOpacity, 0f)

        repo.saveSettings(repo.loadSettings().copy(tileOpacity = Float.POSITIVE_INFINITY))
        assertEquals(0.45f, prefs.getFloat("tile_opacity", -1f), 0f)
        assertTrue(repo.loadSettings().tileOpacity.isFinite())
    }

    @Test fun wrongTypeHomeGridFlagsCannotAbortTileStyleSaveOrDeleteWidgets() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val widget = TileEntity(
            "existing_widget", "Saved widget", tileType = TileType.WIDGET,
            appWidgetId = 127, widgetProvider = "example/.Provider", gridX = 0, gridY = 0
        )
        db.tileDao().insertOrUpdate(widget)
        assertTrue(prefs.edit().putString("show_more_tiles", "true")
            .putInt("auto_arrange_tiles", 1).commit())

        val recovered = repo.loadSettings()
        assertFalse(recovered.showMoreTiles)
        assertFalse(recovered.autoArrangeTiles)
        assertEquals("true", prefs.getString("show_more_tiles", null))
        assertEquals(1, prefs.getInt("auto_arrange_tiles", -1))

        repo.saveSettingsWithReflow(recovered.copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }

    @Test fun wrongTypeTileDimensionsDoNotAbortStyleSaveOrChangeWidgetRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val widget = TileEntity(
            "existing_widget", "Saved widget", tileType = TileType.WIDGET,
            appWidgetId = 127, widgetProvider = "example/.Provider", gridX = 0, gridY = 0
        )
        db.tileDao().insertOrUpdate(widget)
        assertTrue(prefs.edit().putString("tile_corner_radius", "8")
            .putBoolean("tile_spacing_dp", true).commit())

        val recovered = repo.loadSettings()
        assertEquals(2, recovered.tileCornerRadiusDp)
        assertEquals(6, recovered.tileSpacingDp)
        assertEquals("8", prefs.getString("tile_corner_radius", null))
        assertTrue(prefs.getBoolean("tile_spacing_dp", false))
        assertEquals(listOf(widget), db.tileDao().getAllTiles())

        repo.saveSettingsWithReflow(recovered.copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }

    @Test fun wrongTypeTileAppearanceFlagsDoNotAbortStyleSaveOrChangeWidgetRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val widget = TileEntity(
            "appearance_widget", "Saved widget", tileType = TileType.WIDGET,
            appWidgetId = 128, widgetProvider = "example/.Provider", gridX = 0, gridY = 0
        )
        db.tileDao().insertOrUpdate(widget)
        assertTrue(prefs.edit().putString("wp10_reveal_border", "false")
            .putInt("use_real_app_icons", 0)
            .putString("miui_glass_border", "false").commit())

        val recovered = repo.loadSettings()
        assertTrue(recovered.wp10RevealBorder)
        assertTrue(recovered.useRealAppIcons)
        assertTrue(recovered.miuiGlassBorder)
        assertEquals("false", prefs.getString("wp10_reveal_border", null))
        assertEquals(0, prefs.getInt("use_real_app_icons", -1))
        assertEquals("false", prefs.getString("miui_glass_border", null))

        repo.saveSettingsWithReflow(recovered.copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }

    @Test fun wrongTypeHomePageNavigationSettingsDoNotAbortTileStyleSave() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val widget = TileEntity(
            "navigation_widget", "Saved widget", tileType = TileType.WIDGET,
            appWidgetId = 129, widgetProvider = "example/.Provider", gridX = 0, gridY = 0
        )
        db.tileDao().insertOrUpdate(widget)
        assertTrue(prefs.edit().putInt("page_transition_effect", 3)
            .putString("lock_home_layout", "false")
            .putInt("show_page_header_title", 1).commit())

        val recovered = repo.loadSettings()
        assertEquals(PageTransitionEffect.TURNSTILE_3D, recovered.pageTransitionEffect)
        assertFalse(recovered.lockHomeLayout)
        assertFalse(recovered.showPageHeaderTitle)
        // A read must not rewrite legacy data or Room widget bindings.
        assertEquals(3, prefs.getInt("page_transition_effect", -1))
        assertEquals("false", prefs.getString("lock_home_layout", null))
        assertEquals(1, prefs.getInt("show_page_header_title", -1))
        assertEquals(listOf(widget), db.tileDao().getAllTiles())

        repo.saveSettingsWithReflow(recovered.copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }

    @Test fun wrongTypeHomeGestureAndAnimationFlagsCannotAbortTileStyleSave() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val widget = TileEntity(
            "gesture_widget", "Existing widget", tileType = TileType.WIDGET,
            appWidgetId = 138, widgetProvider = "example/.Provider", gridX = 0, gridY = 0
        )
        db.tileDao().insertOrUpdate(widget)
        assertTrue(prefs.edit().putString("double_tap_to_glance", "false")
            .putInt("turnstile_animation", 0)
            .putString("glance_screen_enabled", "false").commit())

        val recovered = repo.loadSettings()
        assertTrue(recovered.doubleTapToGlance)
        assertTrue(recovered.turnstileAnimationEnabled)
        assertTrue(recovered.glanceScreenEnabled)
        // Reading a legacy value must not overwrite it or alter Room widget links.
        assertEquals("false", prefs.getString("double_tap_to_glance", null))
        assertEquals(0, prefs.getInt("turnstile_animation", -1))
        assertEquals("false", prefs.getString("glance_screen_enabled", null))
        assertEquals(listOf(widget), db.tileDao().getAllTiles())

        repo.saveSettingsWithReflow(recovered.copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }

    @Test fun malformedHomeAccentPreferencesDoNotAbortTileStyleSaveOrLoseWidgets() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val widget = TileEntity(
            "accent_widget", "Existing widget", tileType = TileType.WIDGET,
            appWidgetId = 139, widgetProvider = "example/.Provider", gridX = 0, gridY = 0
        )
        db.tileDao().insertOrUpdate(widget)
        assertTrue(prefs.edit().putString("auto_wallpaper_accent_enabled", "true")
            .putString("dynamic_swatch_index", "2")
            .putBoolean("wallpaper_gradient_index", true)
            .putString("is_dark_theme", "false").commit())

        val recovered = repo.loadSettings()
        assertTrue(recovered.autoWallpaperAccentEnabled)
        assertEquals(0, recovered.dynamicSwatchIndex)
        assertEquals(0, recovered.wallpaperGradientIndex)
        assertTrue(recovered.isDarkTheme)
        // Loading settings must not rewrite malformed values or widget bindings.
        assertEquals("true", prefs.getString("auto_wallpaper_accent_enabled", null))
        assertEquals("2", prefs.getString("dynamic_swatch_index", null))
        assertTrue(prefs.getBoolean("wallpaper_gradient_index", false))
        assertEquals("false", prefs.getString("is_dark_theme", null))
        assertEquals(listOf(widget), db.tileDao().getAllTiles())

        repo.saveSettingsWithReflow(recovered.copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }

    @Test fun wrongTypeDefaultHomePageIdFallsBackWithoutRewritingOriginalValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.edit().putInt("default_home_page_id", 7).commit())

        val recovered = repo.loadSettings()
        assertEquals("page_home_1", recovered.defaultHomePageId)
        assertEquals(7, prefs.getInt("default_home_page_id", -1))

        repo.saveSettings(recovered.copy(defaultHomePageId = "page_home_2"))
        assertEquals("page_home_2", repo.loadSettings().defaultHomePageId)
    }

    @Test fun malformedWallpaperPreferencesCannotAbortTileStyleSaveOrChangeWidgetRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val widget = TileEntity(
            "wallpaper_widget", "Saved widget", tileType = TileType.WIDGET,
            appWidgetId = 130, widgetProvider = "example/.Provider", gridX = 0, gridY = 0
        )
        db.tileDao().insertOrUpdate(widget)
        assertTrue(prefs.edit().putString("wallpaper_dim_alpha", "bad")
            .putString("full_wallpaper_enabled", "true").commit())

        val recovered = repo.loadSettings()
        assertEquals(0.18f, recovered.wallpaperDimAlpha, 0f)
        assertTrue(recovered.fullWallpaperEnabled)
        assertEquals("bad", prefs.getString("wallpaper_dim_alpha", null))
        assertEquals("true", prefs.getString("full_wallpaper_enabled", null))
        assertEquals(listOf(widget), db.tileDao().getAllTiles())

        repo.saveSettingsWithReflow(recovered.copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(0.18f, prefs.getFloat("wallpaper_dim_alpha", -1f), 0f)
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }

    @Test fun nonFiniteWallpaperDimAlphaNeverReachesRenderingOrPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.edit().putFloat("wallpaper_dim_alpha", Float.NaN).commit())
        assertEquals(0.18f, repo.loadSettings().wallpaperDimAlpha, 0f)

        repo.saveSettings(repo.loadSettings().copy(wallpaperDimAlpha = Float.POSITIVE_INFINITY))
        assertEquals(0.18f, prefs.getFloat("wallpaper_dim_alpha", -1f), 0f)
        assertTrue(repo.loadSettings().wallpaperDimAlpha.isFinite())
    }

    @Test fun malformedBackupWallpaperAlphaCannotOverwriteHomeOrWidgetBindings() = runBlocking {
        repo.ensureDefaultTiles()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val widget = TileEntity(
            "backup_widget", "Existing widget", tileType = TileType.WIDGET,
            appWidgetId = 137, widgetProvider = "example/.Provider", gridX = 0, gridY = 16
        )
        db.tileDao().insertOrUpdate(widget)
        val beforeTiles = db.tileDao().getAllTiles()
        val beforePages = repo.loadPages()
        val beforeSettings = repo.loadSettings()
        val originalBackup = repo.exportBackupJson()

        for (invalid in listOf<Any>("NaN", "Infinity", JSONObject().put("invalid", true))) {
            val backup = JSONObject(originalBackup)
            backup.getJSONObject("settings").put("wallpaperDimAlpha", invalid)
            assertFalse(repo.restoreFromJson(backup.toString()))
            assertEquals(beforeTiles, db.tileDao().getAllTiles())
            assertEquals(beforePages, repo.loadPages())
            assertEquals(beforeSettings, repo.loadSettings())
            assertNull(prefs.getString("pending_restore_json", null))
        }
    }

    @Test fun deletingHomePageMovesWidgetRowsWithStableIdsAndOrder() = runBlocking {
        val pinned = TileEntity("pinned_app", "Pinned", pageId = "page_home_1",
            orderIndex = 0, gridX = 0, gridY = 0)
        val widgetA = TileEntity("widget_a", "Widget A", tileType = TileType.WIDGET,
            appWidgetId = 141, widgetProvider = "example/.ProviderA",
            pageId = "page_home_2", orderIndex = 0, gridX = 0, gridY = 0)
        val widgetB = TileEntity("widget_b", "Widget B", tileType = TileType.WIDGET,
            appWidgetId = 142, widgetProvider = "example/.ProviderB",
            pageId = "page_home_2", orderIndex = 1, gridX = 2, gridY = 0)
        db.tileDao().insertAll(listOf(pinned, widgetA, widgetB))

        repo.reassignTilesFromDeletedPage("page_home_2", "page_home_1")

        val rows = db.tileDao().getAllTiles()
        assertEquals(3, rows.size)
        assertEquals(setOf(pinned.id, widgetA.id, widgetB.id), rows.map { it.id }.toSet())
        assertTrue(rows.all { it.pageId == "page_home_1" })
        val moved = rows.filter { it.id != pinned.id }.sortedBy { it.orderIndex }
        assertEquals(listOf(1, 2), moved.map { it.orderIndex })
        assertEquals(widgetA.appWidgetId, rows.single { it.id == widgetA.id }.appWidgetId)
        assertEquals(widgetA.widgetProvider, rows.single { it.id == widgetA.id }.widgetProvider)
        assertEquals(widgetB.appWidgetId, rows.single { it.id == widgetB.id }.appWidgetId)
        assertEquals(widgetB.widgetProvider, rows.single { it.id == widgetB.id }.widgetProvider)
        assertEquals(3, rows.map { it.gridX to it.gridY }.toSet().size)
    }

    @Test fun movingWidgetToPageAppendsAfterSparseOrderWithoutLosingBinding() = runBlocking {
        val pinned = TileEntity("pinned", "Pinned", pageId = "page_home_1",
            orderIndex = 0, gridX = 0, gridY = 0)
        val existing = TileEntity("existing", "Existing", pageId = "page_home_1",
            orderIndex = 5, gridX = 2, gridY = 0)
        val widget = TileEntity("moving_widget", "Moving widget", tileType = TileType.WIDGET,
            appWidgetId = 161, widgetProvider = "example/.Provider",
            pageId = "page_home_2", orderIndex = 0, gridX = 0, gridY = 0)
        db.tileDao().insertAll(listOf(pinned, existing, widget))

        repo.moveTileToPage(widget.id, "page_home_1")

        val rows = db.tileDao().getAllTiles()
        assertEquals(3, rows.size)
        assertEquals(pinned, rows.single { it.id == pinned.id })
        assertEquals(existing, rows.single { it.id == existing.id })
        val moved = rows.single { it.id == widget.id }
        assertEquals("page_home_1", moved.pageId)
        assertEquals(6, moved.orderIndex)
        assertEquals(widget.appWidgetId, moved.appWidgetId)
        assertEquals(widget.widgetProvider, moved.widgetProvider)
        assertEquals(3, rows.map { it.gridX to it.gridY }.toSet().size)
    }

    @Test fun samePageReassignmentFailsFastWithoutChangingWidgetRows() = runBlocking {
        val widget = TileEntity("same_page_widget", "Widget", tileType = TileType.WIDGET,
            appWidgetId = 151, widgetProvider = "example/.Provider",
            pageId = "page_home_1", orderIndex = 0, gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(widget)

        try {
            repo.reassignTilesFromDeletedPage("page_home_1", "page_home_1")
            fail("Expected invalid same-page reassignment to fail")
        } catch (_: IllegalArgumentException) {
            // Invalid page pair is rejected, not silently rewritten.
        }
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }

    @Test fun failedSecondWidgetMoveRollsBackAllReassignments() = runBlocking {
        val pinned = TileEntity("pinned_app", "Pinned", pageId = "page_home_1",
            orderIndex = 0, gridX = 0, gridY = 0)
        val widgetA = TileEntity("widget_a", "Widget A", tileType = TileType.WIDGET,
            appWidgetId = 152, widgetProvider = "example/.ProviderA",
            pageId = "page_home_2", orderIndex = 0, gridX = 0, gridY = 0)
        val widgetB = TileEntity("widget_b", "Widget B", tileType = TileType.WIDGET,
            appWidgetId = 153, widgetProvider = "example/.ProviderB",
            pageId = "page_home_2", orderIndex = 1, gridX = 2, gridY = 0)
        db.tileDao().insertAll(listOf(pinned, widgetA, widgetB))
        val before = db.tileDao().getAllTiles()

        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_second_widget_move
            BEFORE INSERT ON tiles
            WHEN NEW.id = 'widget_b' AND NEW.pageId = 'page_home_1'
            BEGIN
                SELECT RAISE(ABORT, 'injected second widget move failure');
            END
            """.trimIndent()
        )

        try {
            repo.reassignTilesFromDeletedPage("page_home_2", "page_home_1")
            fail("Expected injected database failure")
        } catch (_: android.database.sqlite.SQLiteException) {
            // Room must roll back the whole batch, not just the rejected row.
        }
        assertEquals(before, db.tileDao().getAllTiles())
    }

    @Test fun nativeWidgetPinAfterSparseOrderPreservesBindingsAndTileStyle() = runBlocking {
        val pinned = TileEntity("pinned_app", "Pinned", pageId = "page_home_2",
            orderIndex = 0, gridX = 0, gridY = 0)
        val existing = TileEntity("existing_widget", "Existing", tileType = TileType.WIDGET,
            appWidgetId = 187, widgetProvider = "example/.Existing",
            pageId = "page_home_2", orderIndex = 5, gridX = 2, gridY = 0)
        db.tileDao().insertAll(listOf(pinned, existing))
        val provider = WidgetProviderItem("example/.New", "example", "New", "New widget", "Example")
        val created = repo.pinWidgetTile(provider, 188, "page_home_2")
        val rows = db.tileDao().getAllTiles()
        assertEquals(3, rows.size)
        assertEquals(pinned, rows.single { it.id == pinned.id })
        assertEquals(existing, rows.single { it.id == existing.id })
        assertEquals(6, created.orderIndex)
        assertEquals(TileType.WIDGET, created.tileType)
        assertEquals(188, created.appWidgetId)
        assertEquals(provider.providerComponent, created.widgetProvider)
        assertEquals(created, rows.single { it.id == created.id })
        assertEquals(3, rows.map { it.gridX to it.gridY }.toSet().size)
        repo.saveSettingsWithReflow(repo.loadSettings().copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(rows.toSet(), db.tileDao().getAllTiles().toSet())
    }

    @Test fun failedNativeWidgetPinLeavesExistingWidgetBindingsUntouched() = runBlocking {
        val existing = TileEntity("existing_widget", "Existing", tileType = TileType.WIDGET,
            appWidgetId = 187, widgetProvider = "example/.Existing",
            pageId = "page_home_2", orderIndex = 5, gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(existing)
        val before = db.tileDao().getAllTiles()
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_widget_pin
            BEFORE INSERT ON tiles
            WHEN NEW.appWidgetId = 188
            BEGIN
                SELECT RAISE(ABORT, 'injected widget pin failure');
            END
            """.trimIndent()
        )
        val provider = WidgetProviderItem("example/.New", "example", "New", "New widget", "Example")
        try {
            repo.pinWidgetTile(provider, 188, "page_home_2")
            fail("Expected injected Room insert failure")
        } catch (_: android.database.sqlite.SQLiteException) {
            // Failure propagates; existing Room/widget bindings must remain intact.
        }
        assertEquals(before, db.tileDao().getAllTiles())
    }

    @Test fun genericTilePinAfterSparseOrderKeepsNativeWidgetBindings() = runBlocking {
        val pinned = TileEntity("pinned_app", "Pinned", pageId = "page_home_1",
            orderIndex = 0, gridX = 0, gridY = 0)
        val widget = TileEntity("native_widget", "Widget", tileType = TileType.WIDGET,
            appWidgetId = 197, widgetProvider = "example/.Provider",
            pageId = "page_home_1", orderIndex = 5, gridX = 2, gridY = 0)
        db.tileDao().insertAll(listOf(pinned, widget))
        val added = TileEntity("new_app", "New", pageId = "page_home_1",
            gridX = -1, gridY = -1)

        repo.pinTile(added)

        val rows = db.tileDao().getAllTiles()
        assertEquals(3, rows.size)
        assertEquals(pinned, rows.single { it.id == pinned.id })
        assertEquals(widget, rows.single { it.id == widget.id })
        assertEquals(6, rows.single { it.id == added.id }.orderIndex)
        assertEquals(3, rows.map { it.gridX to it.gridY }.toSet().size)
        repo.saveSettingsWithReflow(repo.loadSettings().copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(rows.toSet(), db.tileDao().getAllTiles().toSet())
    }

    @Test fun failedGenericTilePinDoesNotChangeExistingWidgetBindings() = runBlocking {
        val widget = TileEntity("native_widget", "Widget", tileType = TileType.WIDGET,
            appWidgetId = 197, widgetProvider = "example/.Provider",
            pageId = "page_home_1", orderIndex = 5, gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(widget)
        val before = db.tileDao().getAllTiles()
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_generic_tile_pin
            BEFORE INSERT ON tiles
            WHEN NEW.id = 'new_app'
            BEGIN
                SELECT RAISE(ABORT, 'injected generic Tile pin failure');
            END
            """.trimIndent()
        )
        try {
            repo.pinTile(TileEntity("new_app", "New", pageId = "page_home_1",
                gridX = -1, gridY = -1))
            fail("Expected injected database failure")
        } catch (_: android.database.sqlite.SQLiteException) {
            // The caller receives the write failure; existing widget data stays intact.
        }
        assertEquals(before, db.tileDao().getAllTiles())
    }

    @Test fun malformedRestoreDoesNotWritePagesSettingsOrTiles() = runBlocking {
        repo.ensureDefaultTiles()
        val before = db.tileDao().getAllTiles()
        val pages = repo.loadPages()
        val settings = repo.loadSettings()
        val backup = JSONObject(repo.exportBackupJson())
        backup.getJSONArray("pages").getJSONObject(0).put("title", "Changed")
        backup.getJSONObject("settings").put("quickNotes", "Changed")
        backup.getJSONArray("tiles").getJSONObject(0).put("tileType", "INVALID")
        assertFalse(repo.restoreFromJson(backup.toString()))
        assertEquals(before, db.tileDao().getAllTiles())
        assertEquals(pages, repo.loadPages())
        assertEquals(settings, repo.loadSettings())
    }

    @Test fun invalidPendingRestoreJournalDoesNotCrashOrOverwriteExistingTiles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val saved = TileEntity("saved", "My existing tile", gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(saved)
        assertTrue(prefs.edit().putBoolean("home_initialized", true)
            .putString("pending_restore_json", "{invalid").commit())

        repo.ensureDefaultTiles()

        assertEquals(listOf(saved), db.tileDao().getAllTiles())
        assertEquals("{invalid", prefs.getString("pending_restore_json", null))
    }

    @Test fun validPendingRestoreJournalStillReplaysAndClearsMarker() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        repo.ensureDefaultTiles()
        val expected = db.tileDao().getAllTiles().toSet()
        val journal = repo.exportBackupJson()
        db.tileDao().clearAll()
        db.tileDao().insertOrUpdate(TileEntity("interrupted", "Interrupted", gridX = 0, gridY = 0))
        assertTrue(prefs.edit().putString("pending_restore_json", journal).commit())

        repo.ensureDefaultTiles()

        assertEquals(expected, db.tileDao().getAllTiles().toSet())
        assertNull(prefs.getString("pending_restore_json", null))
    }

    @Test fun emptyRestoreStaysEmptyAfterInitialization() = runBlocking {
        repo.ensureDefaultTiles()
        val backup = JSONObject(repo.exportBackupJson()).put("tiles", JSONArray())
        assertTrue(repo.restoreFromJson(backup.toString()))
        repo.ensureDefaultTiles()
        assertTrue(db.tileDao().getAllTiles().isEmpty())
    }

    @Test fun lastTileRemovalDoesNotReseed() = runBlocking {
        repo.ensureDefaultTiles()
        db.tileDao().getAllTiles().forEach { repo.unpinTile(it.id) }
        repo.ensureDefaultTiles()
        assertTrue(db.tileDao().getAllTiles().isEmpty())
    }

    @Test fun freePositionDoesNotWrapToOccupiedOriginAfterRow80() {
        val occupied = (0..90).map { TileEntity("$it", "$it", size = TileSize.WIDE, gridX = 0, gridY = it * 2) }
        assertEquals(0 to 182, repo.findFirstFreeGridPosition(occupied, 4, 2))
    }

    @Test fun gridShrinkKeepsTilesInBoundsAndNonOverlapping() = runBlocking {
        val settings = repo.loadSettings().copy(showMoreTiles = true)
        repo.saveSettings(settings)
        db.tileDao().insertAll(listOf(
            TileEntity("a", "a", gridX = 4, gridY = 0),
            TileEntity("b", "b", gridX = 0, gridY = 0)))
        repo.saveSettingsWithReflow(settings.copy(showMoreTiles = false))
        val tiles = db.tileDao().getAllTiles()
        assertEquals(2, tiles.size)
        assertTrue(tiles.all { it.gridX in 0..2 })
        assertTrue(tiles[0].gridX != tiles[1].gridX || tiles[0].gridY != tiles[1].gridY)
    }

    @Test fun widgetRestoreNeverTrustsSerializedHostIds() = runBlocking {
        repo.ensureDefaultTiles()
        val backup = JSONObject(repo.exportBackupJson())
        backup.getJSONArray("tiles").getJSONObject(0)
            .put("tileType", "WIDGET").put("appWidgetId", 12345).put("widgetProvider", "example/.Provider")
        assertTrue(repo.restoreFromJson(backup.toString()))
        val tile = db.tileDao().getAllTiles().first { it.tileType == TileType.WIDGET }
        assertEquals(-1, tile.appWidgetId)
        assertEquals("example/.Provider", tile.widgetProvider)
    }
    @Test fun cloudBackupPayloadRejectsOversizedJsonInsteadOfSilentlyTruncating() {
        val small = """{"app":"LumiaLauncher","tiles":[]}"""
        assertEquals(small, validateCloudBackupJson(small))
        val tooLarge = JSONObject().put("tiles", "x".repeat(200_001)).toString()
        try {
            validateCloudBackupJson(tooLarge)
            fail("Oversized backup must not be accepted")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.contains("200 KB") == true)
        }
        try {
            validateCloudBackupJson("""{"app":""")
            fail("Malformed JSON must not be accepted")
        } catch (expected: org.json.JSONException) {
            // Reject before uploading.
        }
    }

    @Test fun manualLayoutPreservesGapsWhenAutoArrangeIsOff() = runBlocking {
        val before = listOf(
            TileEntity("a", "A", gridX = 0, gridY = 8),
            TileEntity("b", "B", gridX = 2, gridY = 12)
        )
        db.tileDao().insertAll(before)
        repo.saveSettings(repo.loadSettings().copy(autoArrangeTiles = false))
        assertEquals(before.toSet(), db.tileDao().getAllTiles().toSet())
    }

    @Test fun autoArrangeCompactsTilesWithoutLosingWidgetBindings() = runBlocking {
        db.tileDao().insertAll(listOf(
            TileEntity("a", "A", gridX = 0, gridY = 8, size = TileSize.MEDIUM),
            TileEntity("widget", "Widget", gridX = 2, gridY = 12, size = TileSize.MEDIUM,
                tileType = TileType.WIDGET, appWidgetId = 102, widgetProvider = "example/.Widget")
        ))
        repo.saveSettingsWithReflow(repo.loadSettings().copy(autoArrangeTiles = true))
        val after = db.tileDao().getAllTiles().associateBy { it.id }
        assertEquals(0, after.getValue("a").gridY)
        assertEquals(0, after.getValue("widget").gridY)
        assertEquals(2, after.getValue("widget").gridX)
        assertEquals(102, after.getValue("widget").appWidgetId)
    }

    @Test fun wallpaperAndArrangeOptionsSurviveBackupRestore() = runBlocking {
        repo.ensureDefaultTiles()
        repo.saveSettings(repo.loadSettings().copy(
            wallpaperSource = WallpaperSource.LUMIA,
            autoArrangeTiles = true
        ))
        val backup = repo.exportBackupJson()
        repo.saveSettings(repo.loadSettings().copy(
            wallpaperSource = WallpaperSource.SYSTEM,
            autoArrangeTiles = false
        ))
        assertTrue(repo.restoreFromJson(backup))
        assertEquals(WallpaperSource.LUMIA, repo.loadSettings().wallpaperSource)
        assertTrue(repo.loadSettings().autoArrangeTiles)
    }

    @Test fun existingHomeLayoutIsNotModifiedByLegacySmartFolderSeeding() = runBlocking {
        val original = TileEntity("user_tile", "My tile", packageName = "example.app",
            gridX = 2, gridY = 8, pageId = "page_home_1")
        db.tileDao().insertAll(listOf(original))

        repo.ensureDefaultTiles()
        assertEquals(listOf(original), db.tileDao().getAllTiles())
        // Repeated launcher startup must remain idempotent.
        repo.ensureDefaultTiles()
        assertEquals(listOf(original), db.tileDao().getAllTiles())
    }

    @Test fun deliberatelyEmptyInitializedHomeStaysEmptyAfterLegacyUpgrade() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("home_initialized", true).commit()

        repo.ensureDefaultTiles()
        assertTrue(db.tileDao().getAllTiles().isEmpty())
    }

    @Test fun version2MigrationPreservesTileAndAddsUnboundWidgetFields() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration_data_safety.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE tiles (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, packageName TEXT NOT NULL, tileType TEXT NOT NULL, size TEXT NOT NULL, orderIndex INTEGER NOT NULL, pageId TEXT NOT NULL, gridX INTEGER NOT NULL, gridY INTEGER NOT NULL, colorOverrideHex TEXT, customIconName TEXT NOT NULL, notificationCount INTEGER NOT NULL, isLiveEnabled INTEGER NOT NULL)")
            old.execSQL("INSERT INTO tiles VALUES ('saved', 'Saved title', 'example.app', 'APP', 'MEDIUM', 0, 'page_home_1', 2, 6, NULL, '', 0, 1)")
            old.version = 2
        }
        val migrated = Room.databaseBuilder(context, LumiaDatabase::class.java, name)
            .addMigrations(LumiaDatabase.MIGRATION_2_4).allowMainThreadQueries().build()
        try {
            val tile = migrated.tileDao().getAllTiles().single()
            assertEquals("Saved title", tile.title)
            assertEquals(2, tile.gridX)
            assertEquals(6, tile.gridY)
            assertEquals(-1, tile.appWidgetId)
            assertEquals("", tile.widgetProvider)
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun ungroupRestoresAllItemsBeyondPreviewLimit() = runBlocking {
        val items = (1..12).map { SmartFolderItem("app_$it", "App $it", "example.app$it") }
        db.tileDao().insertOrUpdate(TileEntity("folder", "Folder", tileType = TileType.SMART_FOLDER,
            packageName = SmartFolderConfig(items = items).toJsonString(), gridX = 0, gridY = 0))
        repo.ungroupSmartFolderToTiles("folder")
        val after = db.tileDao().getAllTiles()
        assertEquals(items.map { it.packageName }.toSet(), after.map { it.packageName }.toSet())
        assertEquals(12, after.size)
        assertEquals(12, after.map { it.gridX to it.gridY }.distinct().size)
    }

    @Test fun allFolderAbsorptionPathsPreserveNativeWidgets() = runBlocking {
        val widgets = listOf(
            TileEntity("widget", "Widget", tileType = TileType.WIDGET, appWidgetId = 102,
                widgetProvider = "example/.Provider", gridX = 0, gridY = 0),
            TileEntity("linked", "Linked app", tileType = TileType.APP, appWidgetId = 103,
                widgetProvider = "example/.Provider", gridX = 2, gridY = 0),
            TileEntity("pending", "Needs rebind", tileType = TileType.APP,
                widgetProvider = "example/.Provider", gridX = 0, gridY = 2)
        )
        val app = TileEntity("app", "App", packageName = "example.app", gridX = 2, gridY = 2)
        for (operation in 0..2) {
            db.tileDao().clearAll()
            db.tileDao().insertAll(widgets + app)
            when (operation) {
                0 -> repo.groupPageTilesByCategoryIntoSmartFolders("page_home_1")
                1 -> repo.createSmartFolderByCategory(AppCategory.ALL, absorbMatchingPageTiles = true)
                2 -> repo.convertTileIntoSmartFolder("app", absorbSameCategoryOnPage = true)
            }
            val after = db.tileDao().getAllTiles()
            widgets.forEach { expected -> assertEquals(expected, after.single { it.id == expected.id }) }
            val folder = after.single { it.tileType == TileType.SMART_FOLDER }
            assertEquals(listOf("example.app"), SmartFolderConfig.fromTile(folder).items.map { it.packageName })
            widgets.forEach { widget ->
                assertFalse(folder.gridX < widget.gridX + 2 && folder.gridX + 2 > widget.gridX &&
                    folder.gridY < widget.gridY + 2 && folder.gridY + 2 > widget.gridY)
            }
        }
        widgets.forEach { assertNull(repo.convertTileIntoSmartFolder(it.id)) }
    }

    @Test fun customizedTilesRoundTripThroughAllFolderGroupingPaths() = runBlocking {
        // Two distinct Home tiles for the same app must not be collapsed by package.
        val original = listOf(
            TileEntity("wide", "Wide", packageName = "example.same", size = TileSize.WIDE,
                orderIndex = 4, gridX = 0, gridY = 0, colorOverrideHex = "#123456",
                customIconName = "star", notificationCount = 7, isLiveEnabled = false),
            TileEntity("small", "Small", packageName = "example.same", size = TileSize.SMALL,
                orderIndex = 5, gridX = 0, gridY = 3, colorOverrideHex = "TRANSPARENT",
                customIconName = "heart", notificationCount = 2, isLiveEnabled = true)
        )
        for (operation in 0..2) {
            db.tileDao().clearAll()
            db.tileDao().insertAll(original)
            val folder = when (operation) {
                0 -> {
                    repo.groupPageTilesByCategoryIntoSmartFolders("page_home_1")
                    db.tileDao().getAllTiles().single { it.tileType == TileType.SMART_FOLDER }
                }
                1 -> repo.createSmartFolderByCategory(AppCategory.ALL,
                    absorbMatchingPageTiles = true)
                else -> requireNotNull(repo.convertTileIntoSmartFolder("wide",
                    absorbSameCategoryOnPage = true))
            }
            val persistedFolder = db.tileDao().getAllTiles().single { it.id == folder.id }
            val items = SmartFolderConfig.fromTile(persistedFolder).items
            assertEquals(2, items.size)
            assertEquals(original.map { it.id }.toSet(),
                items.mapNotNull { it.sourceTile?.id }.toSet())
            // Folder UI must show both pinned tiles, but not add an auto-synced third.
            val visible = SmartFolderConfig.resolveEffectiveItems(
                SmartFolderConfig.fromTile(persistedFolder),
                listOf(AppItem(packageName = "example.same", label = "Same app"))
            )
            assertEquals(2, visible.size)
            repo.ungroupSmartFolderToTiles(folder.id)
            assertEquals(original.toSet(), db.tileDao().getAllTiles().toSet())
        }
    }

    @Test fun ungroupNeverOverwritesAnExistingTileWithSnapshotId() = runBlocking {
        val source = TileEntity("original", "Original", packageName = "example.app",
            size = TileSize.WIDE, colorOverrideHex = "#AABBCC", isLiveEnabled = false)
        val item = SmartFolderItem("item_original", source.title, source.packageName,
            source.tileType, source.customIconName, sourceTile = source)
        val folder = TileEntity("folder", "Folder", tileType = TileType.SMART_FOLDER,
            packageName = SmartFolderConfig(items = listOf(item)).toJsonString(),
            gridX = 0, gridY = 0)
        val occupied = TileEntity("original", "Other page", pageId = "page_home_2",
            packageName = "example.other", gridX = 0, gridY = 0)
        db.tileDao().insertAll(listOf(folder, occupied))
        repo.ungroupSmartFolderToTiles(folder.id)
        val after = db.tileDao().getAllTiles()
        assertEquals(occupied, after.single { it.id == occupied.id })
        val unpacked = after.single { it.pageId == folder.pageId }
        assertEquals(source.size, unpacked.size)
        assertEquals(source.colorOverrideHex, unpacked.colorOverrideHex)
        assertFalse(unpacked.isLiveEnabled)
        assertNotEquals(occupied.id, unpacked.id)
    }

    @Test fun coordinateMigrationReservesLaterPositionedTiles() = runBlocking {
        val fixed = TileEntity("fixed", "Fixed", orderIndex = 1, gridX = 0, gridY = 0)
        db.tileDao().insertAll(listOf(TileEntity("legacy", "Legacy", orderIndex = 0), fixed))
        repo.ensureDefaultTiles()
        val after = db.tileDao().getAllTiles()
        assertEquals(fixed, after.single { it.id == "fixed" })
        val migrated = after.single { it.id == "legacy" }
        assertTrue(migrated.gridX >= 2 || migrated.gridY >= 2)
        repo.ensureDefaultTiles()
        assertEquals(after.toSet(), db.tileDao().getAllTiles().toSet())
    }


    @Test fun failedUnpinDoesNotReleaseLiveWidgetBinding() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widget = TileEntity("widget", "Widget", tileType = TileType.WIDGET,
            appWidgetId = 101, widgetProvider = "example/.Provider", gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(widget)
        val released = mutableListOf<Int>()
        val failingDao = object : TileDao by db.tileDao() {
            override suspend fun deleteById(tileId: String) {
                throw IllegalStateException("simulated Room delete failure")
            }
        }
        val guarded = LauncherRepository(context, failingDao, db) { id -> released.add(id) }
        try {
            guarded.unpinTile(widget.id)
            fail("Expected the Room delete to fail")
        } catch (expected: IllegalStateException) {
            assertEquals("simulated Room delete failure", expected.message)
        }
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
        assertTrue(released.isEmpty())
    }

    @Test fun failedUnlinkDoesNotReleaseLiveWidgetBinding() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widget = TileEntity("widget", "Widget", tileType = TileType.WIDGET,
            appWidgetId = 102, widgetProvider = "example/.Provider", gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(widget)
        val released = mutableListOf<Int>()
        val failingDao = object : TileDao by db.tileDao() {
            override suspend fun update(tile: TileEntity) {
                throw IllegalStateException("simulated Room update failure")
            }
        }
        val guarded = LauncherRepository(context, failingDao, db) { id -> released.add(id) }
        try {
            guarded.unlinkWidgetFromTile(widget.id)
            fail("Expected the Room update to fail")
        } catch (expected: IllegalStateException) {
            assertEquals("simulated Room update failure", expected.message)
        }
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
        assertTrue(released.isEmpty())
    }

    @Test fun failedWidgetReplacementKeepsPreviousHostBinding() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widget = TileEntity("widget", "Widget", tileType = TileType.WIDGET,
            appWidgetId = 103, widgetProvider = "example/.Provider", gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(widget)
        val released = mutableListOf<Int>()
        val failingDao = object : TileDao by db.tileDao() {
            override suspend fun insertOrUpdate(tile: TileEntity) {
                throw IllegalStateException("simulated Room replace failure")
            }
        }
        val guarded = LauncherRepository(context, failingDao, db) { id -> released.add(id) }
        val replacement = WidgetProviderItem(
            providerComponent = "example/.Replacement", packageName = "example",
            className = "Replacement", label = "Replacement", appName = "Example"
        )
        try {
            guarded.linkWidgetToExistingTile(widget.id, replacement, 104)
            fail("Expected the Room replace to fail")
        } catch (expected: IllegalStateException) {
            assertEquals("simulated Room replace failure", expected.message)
        }
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
        assertTrue(released.isEmpty())
    }

    @Test fun successfulUnpinReleasesBindingAfterRemovingTile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widget = TileEntity("widget", "Widget", tileType = TileType.WIDGET,
            appWidgetId = 105, widgetProvider = "example/.Provider", gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(widget)
        val released = mutableListOf<Int>()
        val guarded = LauncherRepository(context, db.tileDao(), db) { id -> released.add(id) }
        guarded.unpinTile(widget.id)
        assertTrue(db.tileDao().getAllTiles().isEmpty())
        assertEquals(listOf(105), released)
    }

    @Test fun emptySmartFolderDoesNotInventAppsOrLiveContent() = runBlocking {
        val folder = repo.createSmartFolderByCategory(AppCategory.MEDIA)
        val config = SmartFolderConfig.fromTile(folder)
        assertTrue(config.items.isEmpty())
        assertTrue(SmartFolderConfig.resolveEffectiveItems(config, emptyList()).isEmpty())
        AppCategory.entries.forEach { assertTrue(SmartFolderConfig.defaultSeedItemsForCategory(it).isEmpty()) }
    }

    @Test fun pinAppRetriesCollidingIdWithoutReplacingExistingTile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original = TileEntity("taken", "Keep me", packageName = "example.existing",
            gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(original)
        val candidates = java.util.ArrayDeque(listOf("taken", "app_unique"))
        val guarded = LauncherRepository(context, db.tileDao(), db,
            newTileIdCandidate = { candidates.removeFirst() })

        guarded.pinApp(AppItem(label = "New app", packageName = "example.new"))

        val after = db.tileDao().getAllTiles()
        assertEquals(2, after.size)
        assertEquals(original, after.single { it.id == original.id })
        assertEquals("example.new", after.single { it.id == "app_unique" }.packageName)
    }

    @Test fun pinNativeWidgetRetriesCollidingIdWithoutLosingPreviousBinding() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original = TileEntity("taken", "Existing widget", tileType = TileType.WIDGET,
            appWidgetId = 201, widgetProvider = "example/.OldProvider", gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(original)
        val candidates = java.util.ArrayDeque(listOf("taken", "widget_unique"))
        val guarded = LauncherRepository(context, db.tileDao(), db,
            newTileIdCandidate = { candidates.removeFirst() })
        val provider = WidgetProviderItem(
            providerComponent = "example/.NewProvider", packageName = "example",
            className = "NewProvider", label = "New widget", appName = "Example"
        )

        val pinned = guarded.pinWidgetTile(provider, 202)
        val after = db.tileDao().getAllTiles()
        assertEquals("widget_unique", pinned.id)
        assertEquals(2, after.size)
        assertEquals(original, after.single { it.id == original.id })
        assertEquals(202, after.single { it.id == pinned.id }.appWidgetId)
    }

    @Test fun exhaustedIdCandidatesFailWithoutReplacingAnyTile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original = TileEntity("taken", "Keep me", gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(original)
        val guarded = LauncherRepository(context, db.tileDao(), db,
            newTileIdCandidate = { "taken" })

        try {
            guarded.pinApp(AppItem(label = "New app", packageName = "example.new"))
            fail("Expected duplicate IDs to be rejected")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message?.contains("unique tile ID") == true)
        }
        assertEquals(listOf(original), db.tileDao().getAllTiles())
    }


    @Test fun appPinAfterSparseOrderPreservesNativeWidgetAndTileStyle() = runBlocking {
        val pinned = TileEntity("pinned", "Pinned", pageId = "page_home_1",
            orderIndex = 0, gridX = 0, gridY = 0)
        val widget = TileEntity("native_widget", "Widget", tileType = TileType.WIDGET,
            appWidgetId = 217, widgetProvider = "example/.Provider",
            pageId = "page_home_1", orderIndex = 5, gridX = 2, gridY = 0)
        db.tileDao().insertAll(listOf(pinned, widget))

        repo.pinApp(AppItem(label = "New app", packageName = "example.new"), "page_home_1")

        val rows = db.tileDao().getAllTiles()
        assertEquals(3, rows.size)
        assertEquals(pinned, rows.single { it.id == pinned.id })
        assertEquals(widget, rows.single { it.id == widget.id })
        val added = rows.single { it.packageName == "example.new" }
        assertEquals(6, added.orderIndex)
        assertEquals("page_home_1", added.pageId)
        assertEquals(3, rows.map { it.gridX to it.gridY }.toSet().size)

        repo.saveSettingsWithReflow(repo.loadSettings().copy(wp10TileStyle = Wp10TileStyle.CLASSIC_METRO))
        assertEquals(Wp10TileStyle.CLASSIC_METRO, repo.loadSettings().wp10TileStyle)
        assertEquals(rows.toSet(), db.tileDao().getAllTiles().toSet())
    }

    @Test fun failedAppPinPropagatesRoomErrorWithoutChangingWidgetBindings() = runBlocking {
        val widget = TileEntity("native_widget", "Widget", tileType = TileType.WIDGET,
            appWidgetId = 218, widgetProvider = "example/.Provider",
            pageId = "page_home_1", orderIndex = 5, gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(widget)
        val before = db.tileDao().getAllTiles()
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_app_pin
            BEFORE INSERT ON tiles
            WHEN NEW.packageName = 'example.new'
            BEGIN
                SELECT RAISE(ABORT, 'injected app pin failure');
            END
            """.trimIndent()
        )
        try {
            repo.pinApp(AppItem(label = "New app", packageName = "example.new"), "page_home_1")
            fail("Expected injected Room insert failure")
        } catch (_: android.database.sqlite.SQLiteException) {
            // The exception must reach the caller; no widget binding may be lost.
        }
        assertEquals(before, db.tileDao().getAllTiles())
    }

}
