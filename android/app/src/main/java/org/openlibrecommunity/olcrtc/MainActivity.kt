package org.openlibrecommunity.olcrtc

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openlibrecommunity.olcrtc.routing.InstalledAppInfo
import org.openlibrecommunity.olcrtc.routing.RoutingMode
import org.openlibrecommunity.olcrtc.service.TunnelConfig
import org.openlibrecommunity.olcrtc.service.TunnelProvider
import org.openlibrecommunity.olcrtc.service.TunnelRepository
import org.openlibrecommunity.olcrtc.service.TunnelServiceController
import org.openlibrecommunity.olcrtc.service.TunnelStatus
import org.openlibrecommunity.olcrtc.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingConfig: TunnelConfig? = null

    private val vpnPermissionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        val config = pendingConfig ?: return@registerForActivityResult
        pendingConfig = null

        if (result.resultCode == Activity.RESULT_OK) {
            TunnelServiceController.start(this, config)
        } else {
            TunnelRepository.setError(config, getString(R.string.vpn_permission_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        state = uiState,
                        onProviderChange = viewModel::updateProvider,
                        onSessionIdChange = viewModel::updateSessionId,
                        onSecretKeyChange = viewModel::updateSecretKey,
                        onStartClick = {
                            viewModel.validatedConfigOrNull()?.let(::requestVpnPermission)
                        },
                        onStopClick = {
                            TunnelServiceController.stop(this)
                        },
                        onOpenRouting = {
                            if (uiState.routingSettings.onboardingCompleted) {
                                viewModel.openRoutingSettings()
                            } else {
                                viewModel.openOnboardingEditor()
                            }
                        },
                        onAcceptRecommendedRouting = viewModel::acceptRecommendedRouting,
                        onEditRecommendedRouting = viewModel::openOnboardingEditor,
                        onChooseAllTrafficRouting = viewModel::chooseAllTrafficRouting,
                        onDismissRoutingEditor = viewModel::closeRoutingSettings,
                        onDraftRoutingModeChange = viewModel::updateDraftRoutingMode,
                        onOpenAppPicker = viewModel::openAppPicker,
                        onDismissAppPicker = viewModel::closeAppPicker,
                        onAppSearchQueryChange = viewModel::updateAppSearchQuery,
                        onToggleDraftPackage = viewModel::toggleDraftPackage,
                        onSaveRouting = viewModel::saveRoutingSettings,
                    )
                }
            }
        }
    }

    private fun requestVpnPermission(config: TunnelConfig) {
        pendingConfig = config
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent == null) {
            pendingConfig = null
            TunnelServiceController.start(this, config)
            return
        }

        vpnPermissionLauncher.launch(prepareIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    state: MainUiState,
    onProviderChange: (TunnelProvider) -> Unit,
    onSessionIdChange: (String) -> Unit,
    onSecretKeyChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onOpenRouting: () -> Unit,
    onAcceptRecommendedRouting: () -> Unit,
    onEditRecommendedRouting: () -> Unit,
    onChooseAllTrafficRouting: () -> Unit,
    onDismissRoutingEditor: () -> Unit,
    onDraftRoutingModeChange: (RoutingMode) -> Unit,
    onOpenAppPicker: () -> Unit,
    onDismissAppPicker: () -> Unit,
    onAppSearchQueryChange: (String) -> Unit,
    onToggleDraftPackage: (String) -> Unit,
    onSaveRouting: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "olcrtc Android") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                StatusCard(state = state)
            }

            item {
                RoutingCard(
                    state = state,
                    onOpenRouting = onOpenRouting,
                )
            }

            item {
                ConfigCard(
                    state = state,
                    onProviderChange = onProviderChange,
                    onSessionIdChange = onSessionIdChange,
                    onSecretKeyChange = onSecretKeyChange,
                    onStartClick = onStartClick,
                    onStopClick = onStopClick,
                )
            }

            item {
                LogsHeader(logCount = state.serviceState.logs.size)
            }

            if (state.serviceState.logs.isEmpty()) {
                item {
                    Card {
                        Text(
                            text = stringResourceCompat(R.string.logs_empty_message),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = state.serviceState.logs,
                    key = { index, item -> "$index-$item" },
                ) { _, line ->
                    LogLine(line = line)
                }
            }
        }
    }

    if (state.isRoutingOnboardingVisible) {
        RoutingOnboardingDialog(
            state = state,
            onAcceptRecommendedRouting = onAcceptRecommendedRouting,
            onEditRecommendedRouting = onEditRecommendedRouting,
            onChooseAllTrafficRouting = onChooseAllTrafficRouting,
        )
    }

    if (state.isRoutingEditorVisible) {
        RoutingSettingsDialog(
            state = state,
            onDismiss = onDismissRoutingEditor,
            onDraftRoutingModeChange = onDraftRoutingModeChange,
            onOpenAppPicker = onOpenAppPicker,
            onSaveRouting = onSaveRouting,
        )
    }

    if (state.isAppPickerVisible) {
        AppPickerDialog(
            state = state,
            onDismiss = onDismissAppPicker,
            onSearchQueryChange = onAppSearchQueryChange,
            onTogglePackage = onToggleDraftPackage,
        )
    }
}

@Composable
private fun StatusCard(state: MainUiState) {
    val statusProvider = if (state.serviceState.status == TunnelStatus.IDLE) {
        state.provider
    } else {
        state.serviceState.provider
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (state.serviceState.status) {
                TunnelStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                TunnelStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.serviceState.status.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = state.serviceState.message,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = statusProvider.displayName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.serviceState.sessionId.isNotBlank()) {
                Text(
                    text = state.serviceState.sessionId,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.serviceState.errorMessage != null) {
                Text(
                    text = state.serviceState.errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun RoutingCard(
    state: MainUiState,
    onOpenRouting: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResourceCompat(R.string.routing_card_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = state.routingSummary)
            if (state.routingValidationError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = state.routingValidationError,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Button(onClick = onOpenRouting) {
                Text(text = stringResourceCompat(R.string.routing_change_button))
            }
        }
    }
}

@Composable
private fun ConfigCard(
    state: MainUiState,
    onProviderChange: (TunnelProvider) -> Unit,
    onSessionIdChange: (String) -> Unit,
    onSecretKeyChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    val sessionLabel = when (state.provider) {
        TunnelProvider.TELEMOST -> stringResourceCompat(R.string.room_id_label)
        TunnelProvider.SALUTE_JAZZ -> stringResourceCompat(R.string.salutejazz_session_label)
    }
    val sessionHint = when (state.provider) {
        TunnelProvider.TELEMOST -> stringResourceCompat(R.string.telemost_session_hint)
        TunnelProvider.SALUTE_JAZZ -> stringResourceCompat(R.string.salutejazz_session_hint)
    }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResourceCompat(R.string.tunnel_configuration_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResourceCompat(R.string.provider_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TunnelProvider.values().forEach { provider ->
                    FilterChip(
                        selected = state.provider == provider,
                        onClick = { onProviderChange(provider) },
                        label = { Text(text = provider.displayName) },
                    )
                }
            }
            if (state.providerNotice != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = state.providerNotice,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            OutlinedTextField(
                value = state.sessionId,
                onValueChange = onSessionIdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = sessionLabel) },
                supportingText = {
                    Text(text = state.sessionIdError ?: sessionHint)
                },
                isError = state.sessionIdError != null,
                enabled = state.provider == TunnelProvider.SALUTE_JAZZ,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text,
                ),
            )
            OutlinedTextField(
                value = state.secretKey,
                onValueChange = onSecretKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResourceCompat(R.string.secret_key_label)) },
                supportingText = {
                    state.secretKeyError?.let { Text(text = it) }
                },
                isError = state.secretKeyError != null,
                enabled = state.provider == TunnelProvider.SALUTE_JAZZ,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Password,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onStartClick,
                    enabled = state.canStart,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResourceCompat(R.string.connect_button))
                }
                Button(
                    onClick = onStopClick,
                    enabled = state.canStop,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResourceCompat(R.string.disconnect_button))
                }
            }
        }
    }
}

@Composable
private fun RoutingOnboardingDialog(
    state: MainUiState,
    onAcceptRecommendedRouting: () -> Unit,
    onEditRecommendedRouting: () -> Unit,
    onChooseAllTrafficRouting: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = stringResourceCompat(R.string.routing_setup_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResourceCompat(R.string.routing_setup_message))
                if (state.onboardingState.isLoadingRecommendations) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        Text(text = stringResourceCompat(R.string.routing_setup_loading))
                    }
                } else {
                    Text(
                        text = stringResourceCompat(
                            R.string.routing_setup_recommended_ready,
                            state.onboardingState.recommendedPackages.size,
                        ),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onAcceptRecommendedRouting,
                enabled = !state.onboardingState.isLoadingRecommendations && state.onboardingState.recommendedPackages.isNotEmpty(),
            ) {
                Text(text = stringResourceCompat(R.string.routing_accept_recommended_button))
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick = onEditRecommendedRouting,
                    enabled = !state.onboardingState.isLoadingRecommendations,
                ) {
                    Text(text = stringResourceCompat(R.string.routing_edit_recommended_button))
                }
                TextButton(
                    onClick = onChooseAllTrafficRouting,
                    enabled = !state.onboardingState.isLoadingRecommendations,
                ) {
                    Text(text = stringResourceCompat(R.string.routing_choose_all_traffic_button))
                }
            }
        },
    )
}

@Composable
private fun RoutingSettingsDialog(
    state: MainUiState,
    onDismiss: () -> Unit,
    onDraftRoutingModeChange: (RoutingMode) -> Unit,
    onOpenAppPicker: () -> Unit,
    onSaveRouting: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = state.routingDialogTitle)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChipRow(
                    selectedMode = state.routingDraftMode,
                    onModeSelected = onDraftRoutingModeChange,
                )
                Text(
                    text = state.routingDraftSummary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.routingDraftMode == RoutingMode.SELECTED_APPS) {
                    Text(
                        text = stringResourceCompat(
                            R.string.routing_selected_apps_count,
                            state.routingDraftSelectedPackages.size,
                        ),
                        fontWeight = FontWeight.Medium,
                    )
                    Button(onClick = onOpenAppPicker) {
                        Text(text = stringResourceCompat(R.string.routing_edit_apps_button))
                    }
                }
                if (state.routingDraftValidationError != null) {
                    Text(
                        text = state.routingDraftValidationError,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSaveRouting,
                enabled = state.routingDraftValidationError == null,
            ) {
                Text(text = state.routingDialogConfirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResourceCompat(R.string.cancel_button))
            }
        },
    )
}

@Composable
private fun FilterChipRow(
    selectedMode: RoutingMode,
    onModeSelected: (RoutingMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilterChip(
            selected = selectedMode == RoutingMode.SELECTED_APPS,
            onClick = { onModeSelected(RoutingMode.SELECTED_APPS) },
            label = { Text(text = stringResourceCompat(R.string.routing_mode_selected_apps)) },
        )
        FilterChip(
            selected = selectedMode == RoutingMode.ALL_TRAFFIC,
            onClick = { onModeSelected(RoutingMode.ALL_TRAFFIC) },
            label = { Text(text = stringResourceCompat(R.string.routing_mode_all_traffic)) },
        )
    }
}

@Composable
private fun AppPickerDialog(
    state: MainUiState,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTogglePackage: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResourceCompat(R.string.app_picker_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.appSearchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = stringResourceCompat(R.string.app_picker_search_label)) },
                )
                if (state.isLoadingInstalledApps) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.installedApps.isEmpty()) {
                    Text(text = stringResourceCompat(R.string.app_picker_empty_message))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(
                            items = state.installedApps,
                            key = { it.packageName },
                        ) { app ->
                            AppPickerRow(
                                app = app,
                                selected = state.routingDraftSelectedPackages.contains(app.packageName),
                                recommended = state.onboardingState.recommendedPackages.contains(app.packageName),
                                onToggle = { onTogglePackage(app.packageName) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResourceCompat(R.string.done_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResourceCompat(R.string.cancel_button))
            }
        },
    )
}

@Composable
private fun AppPickerRow(
    app: InstalledAppInfo,
    selected: Boolean,
    recommended: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = app.label,
                        fontWeight = FontWeight.Medium,
                    )
                    if (recommended) {
                        Text(
                            text = stringResourceCompat(R.string.routing_recommended_badge),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LogsHeader(logCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResourceCompat(R.string.runtime_logs_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$logCount lines",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LogLine(line: String) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        ) {
            Text(
                text = line,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Visible,
            )
        }
    }
}

@Composable
private fun stringResourceCompat(id: Int, vararg formatArgs: Any): String {
    return androidx.compose.ui.res.stringResource(id, *formatArgs)
}
