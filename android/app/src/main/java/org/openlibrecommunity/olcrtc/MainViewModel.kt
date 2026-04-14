package org.openlibrecommunity.olcrtc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openlibrecommunity.olcrtc.routing.InstalledAppInfo
import org.openlibrecommunity.olcrtc.routing.InstalledAppsRepository
import org.openlibrecommunity.olcrtc.routing.RecommendedAppsLoader
import org.openlibrecommunity.olcrtc.routing.RoutingMode
import org.openlibrecommunity.olcrtc.routing.RoutingOnboardingState
import org.openlibrecommunity.olcrtc.routing.RoutingSettings
import org.openlibrecommunity.olcrtc.routing.RoutingSettingsRepository
import org.openlibrecommunity.olcrtc.service.TunnelConfig
import org.openlibrecommunity.olcrtc.service.TunnelProvider
import org.openlibrecommunity.olcrtc.service.TunnelRepository
import org.openlibrecommunity.olcrtc.service.TunnelServiceState
import org.openlibrecommunity.olcrtc.service.TunnelStatus

enum class RoutingEditorContext {
    SETTINGS,
    ONBOARDING,
}

data class MainUiState(
    val provider: TunnelProvider = TunnelProvider.TELEMOST,
    val sessionId: String = "",
    val secretKey: String = "",
    val providerNotice: String? = null,
    val sessionIdError: String? = null,
    val secretKeyError: String? = null,
    val serviceState: TunnelServiceState = TunnelServiceState(),
    val routingSettings: RoutingSettings = RoutingSettings(),
    val routingSummary: String = "",
    val routingDraftSummary: String = "",
    val routingValidationError: String? = null,
    val onboardingState: RoutingOnboardingState = RoutingOnboardingState(),
    val isRoutingOnboardingVisible: Boolean = false,
    val isRoutingEditorVisible: Boolean = false,
    val isAppPickerVisible: Boolean = false,
    val routingEditorContext: RoutingEditorContext = RoutingEditorContext.SETTINGS,
    val routingDraftMode: RoutingMode = RoutingMode.SELECTED_APPS,
    val routingDraftSelectedPackages: Set<String> = emptySet(),
    val routingDraftValidationError: String? = null,
    val routingDialogTitle: String = "",
    val routingDialogConfirmLabel: String = "",
    val appSearchQuery: String = "",
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingInstalledApps: Boolean = true,
    val canStart: Boolean = false,
    val canStop: Boolean = false,
)

private data class RoutingUiTransientState(
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingInstalledApps: Boolean = true,
    val recommendedPackages: Set<String> = emptySet(),
    val showOnboarding: Boolean = false,
    val showRoutingEditor: Boolean = false,
    val showAppPicker: Boolean = false,
    val editorContext: RoutingEditorContext = RoutingEditorContext.SETTINGS,
    val draftMode: RoutingMode = RoutingMode.SELECTED_APPS,
    val draftSelectedPackages: Set<String> = emptySet(),
    val appSearchQuery: String = "",
)

private data class MainPersistentState(
    val provider: TunnelProvider,
    val sessionId: String,
    val secretKey: String,
    val serviceState: TunnelServiceState,
    val routingSettings: RoutingSettings,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val provider = MutableStateFlow(TunnelProvider.TELEMOST)
    private val sessionId = MutableStateFlow("")
    private val secretKey = MutableStateFlow("")
    private val routingUi = MutableStateFlow(RoutingUiTransientState())

    private val routingRepository = RoutingSettingsRepository.getInstance(application)
    private val installedAppsRepository = InstalledAppsRepository.getInstance(application)
    private val recommendedAppsLoader = RecommendedAppsLoader.getInstance(application)

    private val persistentState = combine(
        provider,
        sessionId,
        secretKey,
        TunnelRepository.state,
        routingRepository.settings,
    ) { selectedProvider, session, key, serviceState, routingSettings ->
        MainPersistentState(
            provider = selectedProvider,
            sessionId = session,
            secretKey = key,
            serviceState = serviceState,
            routingSettings = routingSettings,
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        persistentState,
        routingUi,
    ) { persistent, routingState ->
        val providerNotice = when (persistent.provider) {
            TunnelProvider.TELEMOST -> app.getString(R.string.provider_notice_telemost_unavailable)
            TunnelProvider.SALUTE_JAZZ -> null
        }
        val sessionError = when {
            persistent.sessionId.isBlank() && persistent.provider == TunnelProvider.SALUTE_JAZZ -> app.getString(R.string.error_salutejazz_session_required)
            else -> null
        }
        val keyError = when {
            persistent.secretKey.isBlank() -> app.getString(R.string.error_secret_key_required)
            !hexKey.matches(persistent.secretKey) -> app.getString(R.string.error_secret_key_invalid)
            else -> null
        }
        val routingValidationError = when {
            !persistent.routingSettings.onboardingCompleted -> app.getString(R.string.routing_setup_required_message)
            persistent.routingSettings.mode == RoutingMode.SELECTED_APPS && persistent.routingSettings.selectedPackages.isEmpty() -> app.getString(R.string.routing_selection_required_message)
            else -> null
        }
        val draftValidationError = when {
            routingState.draftMode == RoutingMode.SELECTED_APPS && routingState.draftSelectedPackages.isEmpty() -> app.getString(R.string.routing_selection_required_message)
            else -> null
        }
        val routingSummary = when (persistent.routingSettings.mode) {
            RoutingMode.SELECTED_APPS -> app.getString(R.string.routing_summary_selected_apps, persistent.routingSettings.selectedPackages.size)
            RoutingMode.ALL_TRAFFIC -> app.getString(R.string.routing_summary_all_traffic)
        }
        val routingDraftSummary = when (routingState.draftMode) {
            RoutingMode.SELECTED_APPS -> app.getString(R.string.routing_summary_selected_apps, routingState.draftSelectedPackages.size)
            RoutingMode.ALL_TRAFFIC -> app.getString(R.string.routing_summary_all_traffic)
        }
        val statusSessionId = if (persistent.serviceState.status == TunnelStatus.IDLE) persistent.sessionId else persistent.serviceState.sessionId
        val canStart = providerNotice == null &&
            sessionError == null &&
            keyError == null &&
            routingValidationError == null &&
            persistent.serviceState.status in setOf(TunnelStatus.IDLE, TunnelStatus.ERROR)

        MainUiState(
            provider = persistent.provider,
            sessionId = persistent.sessionId,
            secretKey = persistent.secretKey,
            providerNotice = providerNotice,
            sessionIdError = sessionError,
            secretKeyError = keyError,
            serviceState = persistent.serviceState.copy(sessionId = statusSessionId),
            routingSettings = persistent.routingSettings,
            routingSummary = routingSummary,
            routingDraftSummary = routingDraftSummary,
            routingValidationError = routingValidationError,
            onboardingState = RoutingOnboardingState(
                isRequired = !persistent.routingSettings.onboardingCompleted,
                isLoadingRecommendations = routingState.isLoadingInstalledApps,
                recommendedPackages = routingState.recommendedPackages,
            ),
            isRoutingOnboardingVisible = routingState.showOnboarding,
            isRoutingEditorVisible = routingState.showRoutingEditor,
            isAppPickerVisible = routingState.showAppPicker,
            routingEditorContext = routingState.editorContext,
            routingDraftMode = routingState.draftMode,
            routingDraftSelectedPackages = routingState.draftSelectedPackages,
            routingDraftValidationError = draftValidationError,
            routingDialogTitle = if (routingState.editorContext == RoutingEditorContext.ONBOARDING) {
                app.getString(R.string.routing_setup_title)
            } else {
                app.getString(R.string.routing_settings_title)
            },
            routingDialogConfirmLabel = if (routingState.editorContext == RoutingEditorContext.ONBOARDING) {
                app.getString(R.string.routing_setup_confirm_button)
            } else {
                app.getString(R.string.routing_settings_save_button)
            },
            appSearchQuery = routingState.appSearchQuery,
            installedApps = filterAndSortApps(
                apps = routingState.installedApps,
                selectedPackages = routingState.draftSelectedPackages,
                query = routingState.appSearchQuery,
            ),
            isLoadingInstalledApps = routingState.isLoadingInstalledApps,
            canStart = canStart,
            canStop = persistent.serviceState.status == TunnelStatus.CONNECTING || persistent.serviceState.status == TunnelStatus.CONNECTED,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        observeRoutingState()
        loadInstalledApps()
    }

    fun updateProvider(value: TunnelProvider) {
        provider.update { value }
    }

    fun updateSessionId(value: String) {
        sessionId.update { value.trim() }
    }

    fun updateSecretKey(value: String) {
        secretKey.update { value.trim() }
    }

    fun openRoutingSettings() {
        val settings = uiState.value.routingSettings
        routingUi.update {
            it.copy(
                showRoutingEditor = true,
                showOnboarding = false,
                showAppPicker = false,
                editorContext = RoutingEditorContext.SETTINGS,
                draftMode = settings.mode,
                draftSelectedPackages = settings.selectedPackages,
                appSearchQuery = "",
            )
        }
    }

    fun closeRoutingSettings() {
        val isOnboardingContext = routingUi.value.editorContext == RoutingEditorContext.ONBOARDING
        routingUi.update {
            it.copy(
                showRoutingEditor = false,
                showAppPicker = false,
                showOnboarding = isOnboardingContext,
                appSearchQuery = "",
            )
        }
    }

    fun openAppPicker() {
        routingUi.update {
            it.copy(showAppPicker = true)
        }
    }

    fun closeAppPicker() {
        routingUi.update {
            it.copy(showAppPicker = false, appSearchQuery = "")
        }
    }

    fun updateAppSearchQuery(value: String) {
        routingUi.update { it.copy(appSearchQuery = value) }
    }

    fun updateDraftRoutingMode(value: RoutingMode) {
        routingUi.update { it.copy(draftMode = value) }
    }

    fun toggleDraftPackage(packageName: String) {
        routingUi.update { current ->
            val updated = current.draftSelectedPackages.toMutableSet()
            if (!updated.add(packageName)) {
                updated.remove(packageName)
            }
            current.copy(draftSelectedPackages = updated)
        }
    }

    fun acceptRecommendedRouting() {
        val recommendedPackages = routingUi.value.recommendedPackages.ifEmpty { uiState.value.routingSettings.selectedPackages }
        if (recommendedPackages.isEmpty()) {
            openOnboardingEditor()
            return
        }

        viewModelScope.launch {
            routingRepository.completeOnboarding(
                mode = RoutingMode.SELECTED_APPS,
                selectedPackages = recommendedPackages,
            )
            routingUi.update {
                it.copy(
                    showOnboarding = false,
                    showRoutingEditor = false,
                    showAppPicker = false,
                    draftMode = RoutingMode.SELECTED_APPS,
                    draftSelectedPackages = recommendedPackages,
                    appSearchQuery = "",
                )
            }
        }
    }

    fun chooseAllTrafficRouting() {
        val preservedSelection = routingUi.value.recommendedPackages.ifEmpty { uiState.value.routingSettings.selectedPackages }
        viewModelScope.launch {
            routingRepository.completeOnboarding(
                mode = RoutingMode.ALL_TRAFFIC,
                selectedPackages = preservedSelection,
            )
            routingUi.update {
                it.copy(
                    showOnboarding = false,
                    showRoutingEditor = false,
                    showAppPicker = false,
                    draftMode = RoutingMode.ALL_TRAFFIC,
                    draftSelectedPackages = preservedSelection,
                    appSearchQuery = "",
                )
            }
        }
    }

    fun openOnboardingEditor() {
        val settings = uiState.value.routingSettings
        routingUi.update {
            it.copy(
                showOnboarding = false,
                showRoutingEditor = true,
                showAppPicker = false,
                editorContext = RoutingEditorContext.ONBOARDING,
                draftMode = settings.mode,
                draftSelectedPackages = settings.selectedPackages.ifEmpty { it.recommendedPackages },
                appSearchQuery = "",
            )
        }
    }

    fun saveRoutingSettings() {
        val draft = routingUi.value
        val packages = draft.draftSelectedPackages
        if (draft.draftMode == RoutingMode.SELECTED_APPS && packages.isEmpty()) {
            return
        }

        viewModelScope.launch {
            if (draft.editorContext == RoutingEditorContext.ONBOARDING) {
                routingRepository.completeOnboarding(draft.draftMode, packages)
            } else {
                routingRepository.save(draft.draftMode, packages)
            }
            routingUi.update {
                it.copy(
                    showOnboarding = false,
                    showRoutingEditor = false,
                    showAppPicker = false,
                    appSearchQuery = "",
                )
            }
        }
    }

    fun validatedConfigOrNull(): TunnelConfig? {
        val state = uiState.value
        if (!state.canStart) {
            return null
        }

        return TunnelConfig(
            provider = state.provider,
            sessionId = state.sessionId,
            secretKey = state.secretKey,
            routingMode = state.routingSettings.mode,
            selectedPackages = state.routingSettings.selectedPackages,
        )
    }

    private fun observeRoutingState() {
        viewModelScope.launch {
            combine(
                routingRepository.settings,
                routingUi,
            ) { settings, ui -> settings to ui }
                .collect { (settings, ui) ->
                    if (ui.isLoadingInstalledApps) {
                        return@collect
                    }

                    val recommendedPackages = recommendedAppsLoader.recommendedPackages(ui.installedApps)
                    if (recommendedPackages != ui.recommendedPackages) {
                        routingUi.update { current -> current.copy(recommendedPackages = recommendedPackages) }
                    }

                    if (!settings.recommendationSeeded) {
                        routingRepository.seedRecommendationsIfNeeded(recommendedPackages)
                        return@collect
                    }

                    if (!settings.onboardingCompleted && !ui.showRoutingEditor) {
                        routingUi.update { current -> current.copy(showOnboarding = true) }
                    }
                }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = installedAppsRepository.loadInstalledApps()
            routingUi.update {
                it.copy(
                    installedApps = apps,
                    isLoadingInstalledApps = false,
                )
            }
        }
    }

    private val app: Application
        get() = getApplication()

    companion object {
        private val hexKey = Regex("^[0-9a-fA-F]{64}$")

        private fun filterAndSortApps(
            apps: List<InstalledAppInfo>,
            selectedPackages: Set<String>,
            query: String,
        ): List<InstalledAppInfo> {
            val normalizedQuery = query.trim().lowercase()
            return apps
                .asSequence()
                .filter { app ->
                    normalizedQuery.isEmpty() ||
                        app.label.lowercase().contains(normalizedQuery) ||
                        app.packageName.lowercase().contains(normalizedQuery)
                }
                .sortedWith(
                    compareByDescending<InstalledAppInfo> { selectedPackages.contains(it.packageName) }
                        .thenBy { it.isSystemApp }
                        .thenBy { it.label.lowercase() }
                        .thenBy { it.packageName },
                )
                .toList()
        }
    }
}
