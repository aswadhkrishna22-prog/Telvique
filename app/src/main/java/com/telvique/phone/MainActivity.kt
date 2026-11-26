package com.telvique.phone

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.telvique.phone.telecom.CallManager
import com.telvique.phone.telecom.CallPhase
import com.telvique.phone.ui.MainViewModel
import com.telvique.phone.ui.call.CallActivity
import com.telvique.phone.ui.components.SetupPromptBanner
import com.telvique.phone.ui.components.SimPickerDialog
import com.telvique.phone.ui.contacts.ContactsScreen
import com.telvique.phone.ui.dialer.DialerScreen
import com.telvique.phone.ui.recents.RecentsScreen
import com.telvique.phone.ui.settings.SettingsScreen
import com.telvique.phone.ui.theme.TelviqueColors
import com.telvique.phone.ui.theme.TelviqueTheme

private enum class MainTab(val label: String, val icon: ImageVector) {
    KEYPAD("Keypad", Icons.Rounded.Dialpad),
    RECENTS("Recents", Icons.Rounded.History),
    CONTACTS("Contacts", Icons.Rounded.Contacts),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDialIntent(intent)

        setContent {
            TelviqueTheme {
                TelviqueMainRoot(
                    viewModel = viewModel,
                    onReturnToCall = {
                        startActivity(Intent(this, CallActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
    }

    private fun handleDialIntent(intent: Intent?) {
        if (intent == null) return
        val data = intent.data
        if (data?.scheme == "tel") {
            val number = data.schemeSpecificPart.orEmpty()
            if (number.isNotBlank()) {
                viewModel.setDialerNumber(number)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelviqueMainRoot(
    viewModel: MainViewModel,
    onReturnToCall: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val telecomState by CallManager.uiState.collectAsStateWithLifecycle()
    var activeTab by rememberSaveable { mutableStateOf(MainTab.KEYPAD) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemStatus()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSystemStatus()
    }

    val requestDefaultDialer: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    context.packageName
                )
            }
            roleLauncher.launch(intent)
        }
    }

    val requestMissingPermissions = {
        if (uiState.missingPermissions.isNotEmpty()) {
            permissionsLauncher.launch(uiState.missingPermissions.toTypedArray())
        }
    }

    val ongoingCall = telecomState.primaryCall?.takeIf { it.phase != CallPhase.DISCONNECTED }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (activeTab == MainTab.KEYPAD) "Telvique" else activeTab.label,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
                if (ongoingCall != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TelviqueColors.CallGreen)
                            .clickable(onClick = onReturnToCall)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap to return to call • ${ongoingCall.displayName}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SetupPromptBanner(
                isDefaultDialer = uiState.isDefaultDialer,
                missingPermissionsCount = uiState.missingPermissions.size,
                onSetDefaultDialer = requestDefaultDialer,
                onRequestPermissions = requestMissingPermissions
            )

            when (activeTab) {
                MainTab.KEYPAD -> DialerScreen(
                    input = uiState.dialerInput,
                    t9Matches = uiState.t9Matches,
                    lastDialedNumber = uiState.callLogs.firstOrNull { it.number.isNotBlank() }?.number,
                    dialpadTonesEnabled = uiState.settings.dialpadTonesEnabled,
                    dialpadVibrateEnabled = uiState.settings.dialpadVibrateEnabled,
                    onDigitPress = viewModel::appendDialDigit,
                    onSetNumber = viewModel::setDialerNumber,
                    onBackspace = viewModel::backspaceDialDigit,
                    onClearAll = viewModel::clearDialInput,
                    onCallClick = { number, forceSim -> viewModel.callNumber(number, forceSim) }
                )

                MainTab.RECENTS -> RecentsScreen(
                    callLogs = uiState.filteredCallLogs,
                    selectedFilter = uiState.recentsFilter,
                    searchQuery = uiState.recentsQuery,
                    blockedNumbers = uiState.settings.blockedNumbers,
                    onFilterChange = viewModel::setRecentsFilter,
                    onSearchQueryChange = viewModel::setRecentsQuery,
                    onCallNumber = { viewModel.callNumber(it) },
                    onDeleteEntry = viewModel::deleteCallLog,
                    onClearAll = viewModel::clearCallLogs,
                    onToggleBlock = { number, block ->
                        if (block) viewModel.blockNumber(number) else viewModel.unblockNumber(number)
                    }
                )

                MainTab.CONTACTS -> ContactsScreen(
                    contacts = uiState.filteredContacts,
                    favorites = uiState.favoriteContacts,
                    searchQuery = uiState.contactsQuery,
                    blockedNumbers = uiState.settings.blockedNumbers,
                    onSearchQueryChange = viewModel::setContactsQuery,
                    onCallNumber = { viewModel.callNumber(it) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onToggleBlock = { number, block ->
                        if (block) viewModel.blockNumber(number) else viewModel.unblockNumber(number)
                    }
                )

                MainTab.SETTINGS -> SettingsScreen(
                    settings = uiState.settings,
                    simAccounts = uiState.simAccounts,
                    isDefaultDialer = uiState.isDefaultDialer,
                    onRequestDefaultDialer = requestDefaultDialer,
                    onToggleDialpadTones = viewModel::setDialpadTones,
                    onToggleDialpadVibrate = viewModel::setDialpadVibrate,
                    onToggleVibrateOnConnect = viewModel::setVibrateOnConnect,
                    onSelectPreferredSim = viewModel::setPreferredSimId,
                    onBlockNumber = viewModel::blockNumber,
                    onUnblockNumber = viewModel::unblockNumber,
                    onUpdateQuickResponse = viewModel::updateQuickResponse
                )
            }
        }
    }

    val simPromptNumber = uiState.pendingSimChoiceNumber
    if (simPromptNumber != null && uiState.simAccounts.size > 1) {
        SimPickerDialog(
            number = simPromptNumber,
            sims = uiState.simAccounts,
            onSelectSim = { sim -> viewModel.callWithSim(simPromptNumber, sim.handle) },
            onDismiss = viewModel::dismissSimChooser
        )
    }
}
