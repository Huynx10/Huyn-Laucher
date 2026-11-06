package com.example.ui.components

import android.content.Context
import android.content.ComponentName
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserManager
import com.example.data.model.AppItem
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconMemoryCache {
    private val cache = object : LruCache<String, ImageBitmap>(160) {}

    fun get(packageName: String): ImageBitmap? = synchronized(cache) {
        cache.get(packageName)
    }

    fun put(packageName: String, bitmap: ImageBitmap) {
        synchronized(cache) {
            cache.put(packageName, bitmap)
        }
    }

    fun clearCache() {
        synchronized(cache) {
            cache.evictAll()
        }
    }

    fun loadAppIcon(context: Context, packageName: String): ImageBitmap? {
        if (packageName.isBlank() || packageName.startsWith("SYSTEM_")) return null
        get(packageName)?.let { return it }
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable, 128)
            val imageBitmap = bitmap.asImageBitmap()
            put(packageName, imageBitmap)
            imageBitmap
        } catch (_: Exception) {
            null
        }
    }

    /** Do not fetch a different activity/profile icon if this exact target disappeared. */
    fun loadExactAppIcon(context: Context, app: AppItem): ImageBitmap? {
        val key = app.drawerKey
        get(key)?.let { return it }
        val component = ComponentName.unflattenFromString(app.launcherComponent ?: "") ?: return null
        if (component.packageName != app.packageName) return null
        val serial = app.launcherUserSerial ?: return null
        val manager = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return null
        val users = context.getSystemService(Context.USER_SERVICE) as? UserManager ?: return null
        val user = users.getUserForSerialNumber(serial) ?: return null
        val profiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            runCatching { manager.profiles }.getOrDefault(emptyList())
        else runCatching { users.userProfiles }.getOrDefault(emptyList())
        if (user !in profiles.ifEmpty { listOf(Process.myUserHandle()) }) return null
        val activity = runCatching { manager.getActivityList(component.packageName, user) }
            .getOrDefault(emptyList()).firstOrNull { it.componentName == component } ?: return null
        return runCatching {
            activity.getBadgedIcon(0)?.let { drawable ->
                drawableToBitmap(drawable, 128).asImageBitmap().also { put(key, it) }
            }
        }.getOrNull()
    }

    private fun drawableToBitmap(drawable: Drawable, maxSizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width <= maxSizePx && src.height <= maxSizePx) {
                return src
            }
            return Bitmap.createScaledBitmap(src, maxSizePx, maxSizePx, true)
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceAtMost(maxSizePx) else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceAtMost(maxSizePx) else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) {
        mutableStateOf(AppIconMemoryCache.get(packageName))
    }

    LaunchedEffect(packageName) {
        if (iconBitmap == null && packageName.isNotBlank() && !packageName.startsWith("SYSTEM_")) {
            iconBitmap = withContext(Dispatchers.IO) {
                AppIconMemoryCache.loadAppIcon(context, packageName)
            }
        }
    }
    return iconBitmap
}

/** Activity-specific icon including Android work-profile badging; no package fallback for EXACT. */
@Composable
fun rememberAppIcon(app: AppItem): ImageBitmap? {
    val context = LocalContext.current
    val key = app.drawerKey
    var bitmap by remember(key) { mutableStateOf(AppIconMemoryCache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) {
                if (app.launcherComponent != null && app.launcherUserSerial != null) {
                    AppIconMemoryCache.loadExactAppIcon(context, app)
                } else {
                    AppIconMemoryCache.loadAppIcon(context, app.packageName)
                }
            }
        }
    }
    return bitmap
}
