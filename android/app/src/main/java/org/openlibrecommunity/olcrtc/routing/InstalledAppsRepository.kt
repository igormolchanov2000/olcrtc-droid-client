package org.openlibrecommunity.olcrtc.routing

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.Collator

class InstalledAppsRepository private constructor(
    private val appContext: Context,
) {
    private val cacheMutex = Mutex()
    private var cachedApps: List<InstalledAppInfo>? = null

    suspend fun loadInstalledApps(): List<InstalledAppInfo> {
        cachedApps?.let { return it }

        return cacheMutex.withLock {
            cachedApps?.let { return@withLock it }

            val apps = withContext(Dispatchers.IO) {
                val packageManager = appContext.packageManager
                val applications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getInstalledApplications(
                        PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                }

                val collator = Collator.getInstance()
                applications
                    .asSequence()
                    .filter { it.packageName != appContext.packageName }
                    .map { info -> info.toInstalledAppInfo(packageManager) }
                    .sortedWith(compareBy<InstalledAppInfo>({ it.isSystemApp }, { collator.getCollationKey(it.label) }, { it.packageName }))
                    .toList()
            }

            cachedApps = apps
            apps
        }
    }

    companion object {
        @Volatile
        private var instance: InstalledAppsRepository? = null

        fun getInstance(context: Context): InstalledAppsRepository {
            return instance ?: synchronized(this) {
                instance ?: InstalledAppsRepository(context.applicationContext).also { instance = it }
            }
        }

        private fun ApplicationInfo.toInstalledAppInfo(packageManager: PackageManager): InstalledAppInfo {
            return InstalledAppInfo(
                label = loadLabel(packageManager).toString(),
                packageName = packageName,
                isSystemApp = flags and ApplicationInfo.FLAG_SYSTEM != 0,
            )
        }
    }
}
