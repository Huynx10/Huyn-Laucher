package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.model.TileType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Open a seeded REAL version-5 SQLite file through the new v6 Room schema; no destructive fallback. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class P1BRoomMigrationTest {
    @Test fun v5AppWidgetFolderRowsKeepIdsPositionsAndWidgetBindingsAfterReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "p1b_identity_migration_test.db"
        context.deleteDatabase(dbName)
        val file = context.getDatabasePath(dbName).also { it.parentFile?.mkdirs() }
        val sqlite = SQLiteDatabase.openOrCreateDatabase(file, null)
        sqlite.execSQL("""CREATE TABLE IF NOT EXISTS tiles (
            id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, packageName TEXT NOT NULL,
            tileType TEXT NOT NULL, size TEXT NOT NULL, orderIndex INTEGER NOT NULL,
            pageId TEXT NOT NULL, gridX INTEGER NOT NULL, gridY INTEGER NOT NULL,
            colorOverrideHex TEXT, customIconName TEXT NOT NULL,
            notificationCount INTEGER NOT NULL, isLiveEnabled INTEGER NOT NULL,
            appWidgetId INTEGER NOT NULL, widgetProvider TEXT NOT NULL,
            widgetMinSpanX INTEGER NOT NULL, widgetMinSpanY INTEGER NOT NULL,
            widgetResizeMode INTEGER NOT NULL,
            customSpanX INTEGER NOT NULL DEFAULT 0, customSpanY INTEGER NOT NULL DEFAULT 0
        )""".trimIndent())
        fun seed(id: String, type: String, pkg: String, widgetId: Int, provider: String,
                 gx: Int, gy: Int, order: Int) {
            sqlite.execSQL("""INSERT INTO tiles
                (id,title,packageName,tileType,size,orderIndex,pageId,gridX,gridY,
                 colorOverrideHex,customIconName,notificationCount,isLiveEnabled,appWidgetId,
                 widgetProvider,widgetMinSpanX,widgetMinSpanY,widgetResizeMode,customSpanX,customSpanY)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""".trimIndent(),
                arrayOf(id, id, pkg, type, "MEDIUM", order, "page_home_2", gx, gy,
                    null, "", 0, 1, widgetId, provider, 1, 1, 3, 0, 0))
        }
        seed("legacy_app", "APP", "com.acme.demo", -1, "", 0, 3, 10)
        seed("bound_widget", "WIDGET", "com.acme.widget", 501, "com.acme.widget/.Provider", 2, 4, 20)
        seed("legacy_folder", "SMART_FOLDER", "{}", -1, "", 0, 6, 30)
        sqlite.version = 5
        sqlite.close()

        fun open(): LumiaDatabase = Room.databaseBuilder(context, LumiaDatabase::class.java, dbName)
            .addMigrations(LumiaDatabase.MIGRATION_1_4, LumiaDatabase.MIGRATION_2_4,
                LumiaDatabase.MIGRATION_3_4, LumiaDatabase.MIGRATION_4_5,
                LumiaDatabase.MIGRATION_5_6)
            .allowMainThreadQueries().build()

        val migrated = open()
        val rows = migrated.tileDao().getAllTiles()
        assertEquals(3, rows.size)
        assertEquals(setOf("legacy_app", "bound_widget", "legacy_folder"), rows.map { it.id }.toSet())
        rows.forEach {
            assertEquals("LEGACY", it.launcherIdentityState)
            assertNull(it.launcherComponent)
            assertNull(it.launcherUserSerial)
            assertEquals("page_home_2", it.pageId)
        }
        assertEquals(TileType.APP, rows.single { it.id == "legacy_app" }.tileType)
        val widget = rows.single { it.id == "bound_widget" }
        assertEquals(501, widget.appWidgetId)
        assertEquals("com.acme.widget/.Provider", widget.widgetProvider)
        assertEquals(2, widget.gridX)
        assertEquals(4, widget.gridY)
        assertEquals(20, widget.orderIndex)
        migrated.close()

        val reopened = open()
        assertEquals(rows, reopened.tileDao().getAllTiles())
        reopened.close()
        context.deleteDatabase(dbName)
        Unit // JUnit4 @Test methods must return void, not deleteDatabase's Boolean.
    }
}
