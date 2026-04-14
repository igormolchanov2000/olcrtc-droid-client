package org.openlibrecommunity.olcrtc.routing

import android.content.Context

class RecommendedAppsLoader private constructor(
    private val appContext: Context,
) {
    fun recommendedPackages(installedApps: List<InstalledAppInfo>): Set<String> {
        val recommendedFromAsset = readBundledPackageList()
        return installedApps
            .asSequence()
            .map { it.packageName }
            .filter { packageName ->
                packageName in recommendedFromAsset || isHeuristicallyRecommended(packageName)
            }
            .toSet()
    }

    private fun readBundledPackageList(): Set<String> {
        return appContext.assets
            .open(assetName)
            .bufferedReader()
            .useLines { lines ->
                lines.mapNotNull { line ->
                    line.trim()
                        .takeIf { it.isNotEmpty() && !it.startsWith("#") }
                }.toSet()
            }
    }

    private fun isHeuristicallyRecommended(packageName: String): Boolean {
        if (packageName == "com.google.android.webview") {
            return false
        }
        return packageName.startsWith("com.google")
    }

    companion object {
        private const val assetName = "recommended_proxy_packages.txt"

        @Volatile
        private var instance: RecommendedAppsLoader? = null

        fun getInstance(context: Context): RecommendedAppsLoader {
            return instance ?: synchronized(this) {
                instance ?: RecommendedAppsLoader(context.applicationContext).also { instance = it }
            }
        }
    }
}
