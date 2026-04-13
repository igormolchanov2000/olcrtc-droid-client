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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import org.openlibrecommunity.olcrtc.service.TunnelConfig
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
                        onRoomIdChange = viewModel::updateRoomId,
                        onSecretKeyChange = viewModel::updateSecretKey,
                        onStartClick = {
                            viewModel.validatedConfigOrNull()?.let(::requestVpnPermission)
                        },
                        onStopClick = {
                            TunnelServiceController.stop(this)
                        },
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
    onRoomIdChange: (String) -> Unit,
    onSecretKeyChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
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
                ConfigCard(
                    state = state,
                    onRoomIdChange = onRoomIdChange,
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
                            text = "Logs will appear here after the service starts.",
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
}

@Composable
private fun StatusCard(state: MainUiState) {
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
private fun ConfigCard(
    state: MainUiState,
    onRoomIdChange: (String) -> Unit,
    onSecretKeyChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tunnel configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = state.roomId,
                onValueChange = onRoomIdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResourceCompat(R.string.room_id_label)) },
                supportingText = {
                    state.roomIdError?.let { Text(text = it) }
                },
                isError = state.roomIdError != null,
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
private fun LogsHeader(logCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Runtime logs",
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
private fun stringResourceCompat(id: Int): String {
    return androidx.compose.ui.res.stringResource(id)
}
