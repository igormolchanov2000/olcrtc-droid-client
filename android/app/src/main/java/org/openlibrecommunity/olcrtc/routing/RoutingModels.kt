package org.openlibrecommunity.olcrtc.routing

enum class RoutingMode(
    val storageValue: String,
) {
    SELECTED_APPS("selected_apps"),
    ALL_TRAFFIC("all_traffic"),
    ;

    companion object {
        fun fromStorageValue(value: String?): RoutingMode {
            return entries.firstOrNull { it.storageValue == value } ?: SELECTED_APPS
        }
    }
}

data class RoutingSettings(
    val mode: RoutingMode = RoutingMode.SELECTED_APPS,
    val selectedPackages: Set<String> = emptySet(),
    val onboardingCompleted: Boolean = false,
    val recommendationSeeded: Boolean = false,
)

data class RoutingOnboardingState(
    val isRequired: Boolean = true,
    val isLoadingRecommendations: Boolean = true,
    val recommendedPackages: Set<String> = emptySet(),
)

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val isSystemApp: Boolean,
)
