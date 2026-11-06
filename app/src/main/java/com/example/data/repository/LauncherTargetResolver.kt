package com.example.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.example.data.model.AppItem
import java.util.Locale

/** Exact launchable activity and Android-local profile serial. No new permission or polling. */
internal class LauncherTargetResolver(private val context: Context) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

    private fun accessibleProfiles(manager: LauncherApps): List<UserHandle> {
        val profiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { manager.profiles }.getOrDefault(emptyList())
        } else {
            runCatching { userManager?.userProfiles.orEmpty() }.getOrDefault(emptyList())
        }
        return profiles.ifEmpty { listOf(Process.myUserHandle()) }
    }

    fun discover(): List<AppItem> {
        val manager = launcherApps ?: return discoverCurrentUserFallback()
        val profiles = accessibleProfiles(manager)
        val pm = context.packageManager
        val seen = HashSet<String>()
        val result = ArrayList<AppItem>()
        for (user in profiles) {
            val serial = userManager?.getSerialNumberForUser(user) ?: -1L
            if (serial < 0) continue
            val activities = runCatching { manager.getActivityList(null, user) }.getOrDefault(emptyList())
            for (activity in activities) {
                val component = activity.componentName
                val pkg = component.packageName
                if (pkg == context.packageName || !seen.add("${component.flattenToString()}:$serial")) continue
                val appInfo = activity.applicationInfo
                val rawLabel = activity.label?.toString().orEmpty().ifBlank { pkg }
                val badgedLabel = runCatching { pm.getUserBadgedLabel(rawLabel, user).toString() }
                    .getOrDefault(rawLabel)
                result += AppItem(
                    label = badgedLabel, packageName = pkg,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    launcherComponent = component.flattenToString(), launcherUserSerial = serial
                )
            }
        }
        return result.sortedWith(compareBy<AppItem> { it.label.lowercase(Locale.getDefault()) }
            .thenBy { it.launcherComponent }.thenBy { it.launcherUserSerial })
    }

    private fun discoverCurrentUserFallback(): List<AppItem> {
        val serial = userManager?.getSerialNumberForUser(Process.myUserHandle()) ?: -1L
        if (serial < 0) return emptyList()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0).mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            if (activity.packageName == context.packageName) return@mapNotNull null
            AppItem(
                label = info.loadLabel(pm)?.toString() ?: activity.packageName,
                packageName = activity.packageName,
                isSystemApp = activity.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0,
                launcherComponent = ComponentName(activity.packageName, activity.name).flattenToString(),
                launcherUserSerial = serial
            )
        }.distinctBy { it.drawerKey }
            .sortedWith(compareBy<AppItem> { it.label.lowercase(Locale.getDefault()) }
                .thenBy { it.launcherComponent })
    }

    /** Same-device lookup only; imported profile serials must be UNRESOLVED. */
    fun resolve(componentFlat: String?, serial: Long?, expectedPackage: String): Pair<ComponentName, UserHandle>? {
        if (componentFlat.isNullOrBlank() || serial == null || serial < 0) return null
        val component = ComponentName.unflattenFromString(componentFlat) ?: return null
        if (component.packageName != expectedPackage) return null
        val user = userManager?.getUserForSerialNumber(serial) ?: return null
        val manager = launcherApps ?: return null
        val profiles = accessibleProfiles(manager)
        if (user !in profiles) return null
        val matches = runCatching { manager.getActivityList(component.packageName, user) }.getOrDefault(emptyList())
        if (matches.none { it.componentName == component }) return null
        return component to user
    }

    fun launch(componentFlat: String?, serial: Long?, expectedPackage: String): Boolean {
        val resolved = resolve(componentFlat, serial, expectedPackage) ?: return false
        return runCatching {
            launcherApps!!.startMainActivity(resolved.first, resolved.second, null, null)
            true
        }.getOrDefault(false)
    }

    /** Existing package slot launches only if ONE activity in current profile; never silently pick one. */
    fun launchLegacy(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val currentSerial = userManager?.getSerialNumberForUser(Process.myUserHandle()) ?: -1L
        val matches = discover().filter { it.packageName == packageName && it.launcherUserSerial == currentSerial }
        if (matches.size != 1) return false
        val only = matches.single()
        return launch(only.launcherComponent, only.launcherUserSerial, packageName)
    }
}

internal object LauncherIdentityPolicy {
    /** Legacy folder membership is one package slot, never a random exact activity. */
    fun legacyFolderSlot(pkg: String, discovered: List<AppItem>): AppItem? {
        val entries = discovered.filter { it.packageName == pkg }
        if (entries.isEmpty()) return null
        return AppItem(label = entries.first().label, packageName = pkg,
            isSystemApp = entries.all { it.isSystemApp }, category = entries.first().category)
    }
}
