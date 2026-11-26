package com.telvique.phone

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.telvique.phone.data.PhoneLayoutMode
import com.telvique.phone.telecom.CallManager
import com.telvique.phone.telecom.CallPhase
import com.telvique.phone.ui.MainViewModel
import com.telvique.phone.ui.call.CallActivity
import com.telvique.phone.ui.components.SetupPromptBanner
import com.telvique.phone.ui.components.SimPickerDialog
import com.telvique.phone.ui.contacts.ContactsScreen
import com.telvique.phone.ui.dialer.DialerScreen
import com.telvique.phone.ui.favorites.FavoritesScreen
import com.telvique.phone.ui.glass.LiquidCircularGlassButton
import com.telvique.phone.ui.glass.LiquidPhoneTabBar
import com.telvique.phone.ui.glass.LiquidPhoneTabItem
import com.telvique.phone.ui.glass.LocalGlassBackdrop
import com.telvique.phone.ui.glass.isBackdropGlassCapable
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.recents.RecentsScreen
import com.telvique.phone.ui.search.UnifiedSearchOverlay
import com.telvique.phone.ui.settings.SettingsScreen
import com.telvique.phone.ui.theme.LocalApplePalette
import com.telvique.phone.ui.theme.TelviqueTheme
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState

private enum class UnifiedTab { CALLS, CONTACTS, KEYPAD }
private enum class ClassicTab { FAVORITES, RECENTS, CONTACTS, KEYPAD, SETTINGS }

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

@Composable
private fun TelviqueMainRoot(
    viewModel: MainViewModel,
    onReturnToCall: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalApplePalette.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val telecomState by CallManager.uiState.collectAsStateWithLifecycle()

    var unifiedTab by rememberSaveable { mutableStateOf(UnifiedTab.KEYPAD) }
    var classicTab by rememberSaveable { mutableStateOf(ClassicTab.KEYPAD) }
    var showUnifiedSettings by rememberSaveable { mutableStateOf(false) }
    var showUnifiedSearch by rememberSaveable { mutableStateOf(false) }
    var showRestrictedSettingsHelp by remember { mutableStateOf(false) }

    // Switch tab cleanly when user toggles layout mode
    LaunchedEffect(uiState.settings.layoutMode) {
        if (uiState.settings.layoutMode == PhoneLayoutMode.CLASSIC) {
            showUnifiedSettings = false
            showUnifiedSearch = false
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.refreshSystemStatus()
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            context.getSystemService(TelecomManager::class.java)
                ?.defaultDialerPackage == context.packageName
        }
        if (!granted && result.resultCode != Activity.RESULT_OK) {
            showRestrictedSettingsHelp = true
        }
    }

    val screeningRoleLauncher = rememberLauncherForActivityResult(
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
                if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    viewModel.refreshSystemStatus()
                } else {
                    roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                }
            }
        } else {
            val telecom = context.getSystemService(TelecomManager::class.java)
            if (telecom?.defaultDialerPackage == context.packageName) {
                viewModel.refreshSystemStatus()
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
    }

    val requestCallScreening: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    viewModel.refreshSystemStatus()
                } else {
                    screeningRoleLauncher.launch(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    )
                }
            }
        }
    }

    val requestMissingPermissions = {
        if (uiState.missingPermissions.isNotEmpty()) {
            permissionsLauncher.launch(uiState.missingPermissions.toTypedArray())
        }
    }

    BackHandler(enabled = showUnifiedSearch || showUnifiedSettings) {
        if (showUnifiedSearch) showUnifiedSearch = false
        else if (showUnifiedSettings) showUnifiedSettings = false
    }

    val ongoingCall = telecomState.primaryCall?.takeIf { it.phase != CallPhase.DISCONNECTED }
    val glassCapable = isBackdropGlassCapable()
    val appBackground = palette.background
    val glassBackdrop = if (glassCapable) {
        rememberLayerBackdrop {
            drawRect(appBackground)
            drawContent()
        }
    } else null
    val useLiquidNav = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val navLiquidState = if (useLiquidNav) rememberLiquidState() else null

    val navBarBottomDp = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val bottomBarReservedHeight = navBarBottomDp + 86.dp
    val isUnified = uiState.settings.layoutMode == PhoneLayoutMode.UNIFIED

    CompositionLocalProvider(
        LocalGlassBackdrop provides glassBackdrop,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackground)
        ) {
            // Childless background layer captured by Kyant Backdrop (strictly acyclic!)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appBackground)
                    .then(if (glassBackdrop != null) Modifier.layerBackdrop(glassBackdrop) else Modifier)
            )

            // Content Plane (captured by Fletchmckee Liquid for the sibling bottom tab bar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (navLiquidState != null) Modifier.liquefiable(navLiquidState) else Modifier)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (ongoingCall != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.callGreen)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .clickable(onClick = onReturnToCall)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ApplePhoneIcons.PhoneFill,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Return to Call • ${ongoingCall.displayName}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    SetupPromptBanner(
                        isDefaultDialer = uiState.isDefaultDialer,
                        missingPermissionsCount = uiState.missingPermissions.size,
                        onSetDefaultDialer = requestDefaultDialer,
                        onRequestPermissions = requestMissingPermissions
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (isUnified) {
                            when (unifiedTab) {
                                UnifiedTab.CALLS -> RecentsScreen(
                                    callLogs = uiState.filteredCallLogs,
                                    favorites = uiState.favoriteContacts,
                                    layoutMode = PhoneLayoutMode.UNIFIED,
                                    selectedFilter = uiState.recentsFilter,
                                    blockedNumbers = uiState.settings.blockedNumbers,
                                    bottomInsetPadding = bottomBarReservedHeight,
                                    onFilterChange = viewModel::setRecentsFilter,
                                    onLayoutModeChange = viewModel::setLayoutMode,
                                    onOpenSettings = { showUnifiedSettings = true },
                                    onCallNumber = { viewModel.callNumber(it) },
                                    onDeleteEntry = viewModel::deleteCallLog,
                                    onClearAll = viewModel::clearCallLogs,
                                    onToggleBlock = { number, block ->
                                        if (block) viewModel.blockNumber(number) else viewModel.unblockNumber(number)
                                    }
                                )

                                UnifiedTab.CONTACTS -> ContactsScreen(
                                    contacts = uiState.filteredContacts,
                                    searchQuery = uiState.contactsQuery,
                                    blockedNumbers = uiState.settings.blockedNumbers,
                                    showTopSettingsButton = true,
                                    bottomInsetPadding = bottomBarReservedHeight,
                                    onOpenSettings = { showUnifiedSettings = true },
                                    onSearchQueryChange = viewModel::setContactsQuery,
                                    onCallNumber = { viewModel.callNumber(it) },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onToggleBlock = { number, block ->
                                        if (block) viewModel.blockNumber(number) else viewModel.unblockNumber(number)
                                    }
                                )

                                UnifiedTab.KEYPAD -> DialerScreen(
                                    input = uiState.dialerInput,
                                    t9Matches = uiState.t9Matches,
                                    lastDialedNumber = uiState.callLogs.firstOrNull { it.number.isNotBlank() }?.number,
                                    dialpadTonesEnabled = uiState.settings.dialpadTonesEnabled,
                                    dialpadVibrateEnabled = uiState.settings.dialpadVibrateEnabled,
                                    showTopSettingsButton = true,
                                    bottomInsetPadding = bottomBarReservedHeight,
                                    onOpenSettings = { showUnifiedSettings = true },
                                    onDigitPress = viewModel::appendDialDigit,
                                    onSetNumber = viewModel::setDialerNumber,
                                    onBackspace = viewModel::backspaceDialDigit,
                                    onClearAll = viewModel::clearDialInput,
                                    onCallClick = { number, forceSim -> viewModel.callNumber(number, forceSim) }
                                )
                            }
                        } else {
                            when (classicTab) {
                                ClassicTab.FAVORITES -> FavoritesScreen(
                                    favorites = uiState.favoriteContacts,
                                    blockedNumbers = uiState.settings.blockedNumbers,
                                    bottomInsetPadding = bottomBarReservedHeight,
                                    onCallNumber = { viewModel.callNumber(it) },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onToggleBlock = { number, block ->
                                        if (block) viewModel.blockNumber(number) else viewModel.unblockNumber(number)
                                    },
                                    onAddFavoritePrompt = { classicTab = ClassicTab.CONTACTS }
                                )

                                ClassicTab.RECENTS -> RecentsScreen(
                                    callLogs = uiState.filteredCallLogs,
                                    favorites = uiState.favoriteContacts,
                                    layoutMode = PhoneLayoutMode.CLASSIC,
                                    selectedFilter = uiState.recentsFilter,
                                    blockedNumbers = uiState.settings.blockedNumbers,
                                    bottomInsetPadding = bottomBarReservedHeight,
                                    onFilterChange = viewModel::setRecentsFilter,
                                    onLayoutModeChange = viewModel::setLayoutMode,
                                    onOpenSettings = { classicTab = ClassicTab.SETTINGS },
                                    onCallNumber = { viewModel.callNumber(it) },
                                    onDeleteEntry = viewModel::deleteCallLog,
                                    onClearAll = viewModel::clearCallLogs,
                                    onToggleBlock = { number, block ->
                                        if (block) viewModel.blockNumber(number) else viewModel.unblockNumber(number)
                                    }
                                )

                                ClassicTab.CONTACTS -> ContactsScreen(
                                    contacts = uiState.filteredContacts,
                                    searchQuery = uiState.contactsQuery,
                                    blockedNumbers = uiState.settings.blockedNumbers,
                                    showTopSettingsButton = false,
                                    bottomInsetPadding = bottomBarReservedHeight,
                                    onOpenSettings = { classicTab = ClassicTab.SETTINGS },
                                    onSearchQueryChange = viewModel::setContactsQuery,
                                    onCallNumber = { viewModel.callNumber(it) },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onToggleBlock = { number, block ->
                                        if (block) viewModel.blockNumber(number) else viewModel.unblockNumber(number)
                                    }
                                )

                                ClassicTab.KEYPAD -> DialerScreen(
                                    input = uiState.dialerInput,
                                    t9Matches = uiState.t9Matches,
                                    lastDialedNumber = uiState.callLogs.firstOrNull { it.number.isNotBlank() }?.number,
                                    dialpadTonesEnabled = uiState.settings.dialpadTonesEnabled,
                                    dialpadVibrateEnabled = uiState.settings.dialpadVibrateEnabled,
                                    showTopSettingsButton = false,
                                    bottomInsetPadding = bottomBarReservedHeight,
                                    onOpenSettings = { classicTab = ClassicTab.SETTINGS },
                                    onDigitPress = viewModel::appendDialDigit,
                                    onSetNumber = viewModel::setDialerNumber,
                                    onBackspace = viewModel::backspaceDialDigit,
                                    onClearAll = viewModel::clearDialInput,
                                    onCallClick = { number, forceSim -> viewModel.callNumber(number, forceSim) }
                                )

                                ClassicTab.SETTINGS -> SettingsScreen(
                                    settings = uiState.settings,
                                    simAccounts = uiState.simAccounts,
                                    isDefaultDialer = uiState.isDefaultDialer,
                                    isCallScreeningAvailable = uiState.isCallScreeningAvailable,
                                    isCallScreeningHeld = uiState.isCallScreeningHeld,
                                    bottomInsetPadding = bottomBarReservedHeight,
                                    onRequestDefaultDialer = requestDefaultDialer,
                                    onRequestCallScreening = requestCallScreening,
                                    onLayoutModeChange = viewModel::setLayoutMode,
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
                }
            }

            // Floating iOS 26 Liquid Glass Bottom Navigation Bar (sibling to content plane!)
            if (!showUnifiedSettings && !showUnifiedSearch) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUnified) {
                        val unifiedItems = remember {
                            listOf(
                                LiquidPhoneTabItem("Calls", ApplePhoneIcons.ClockFill, ApplePhoneIcons.ClockFill),
                                LiquidPhoneTabItem("Contacts", ApplePhoneIcons.PersonCropCircleFill, ApplePhoneIcons.PersonCropCircleFill),
                                LiquidPhoneTabItem("Keypad", ApplePhoneIcons.KeypadDots, ApplePhoneIcons.KeypadDots),
                            )
                        }
                        LiquidPhoneTabBar(
                            liquidState = navLiquidState,
                            items = unifiedItems,
                            selectedIndex = unifiedTab.ordinal,
                            onSelect = { idx ->
                                unifiedTab = UnifiedTab.entries.getOrElse(idx) { UnifiedTab.KEYPAD }
                            },
                            isDark = palette.isDark,
                            maxWidthDp = 282.dp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        LiquidCircularGlassButton(
                            icon = ApplePhoneIcons.MagnifyingGlass,
                            contentDescription = "Search",
                            onClick = { showUnifiedSearch = true },
                            size = 60.dp,
                            iconSize = 22.dp,
                            tint = palette.label,
                            isDark = palette.isDark,
                        )
                    } else {
                        val classicItems = remember {
                            listOf(
                                LiquidPhoneTabItem("Favorites", ApplePhoneIcons.StarFill, ApplePhoneIcons.StarFill),
                                LiquidPhoneTabItem("Recents", ApplePhoneIcons.ClockFill, ApplePhoneIcons.ClockFill),
                                LiquidPhoneTabItem("Contacts", ApplePhoneIcons.PersonCropCircleFill, ApplePhoneIcons.PersonCropCircleFill),
                                LiquidPhoneTabItem("Keypad", ApplePhoneIcons.KeypadDots, ApplePhoneIcons.KeypadDots),
                                LiquidPhoneTabItem("Settings", ApplePhoneIcons.GearShapeFill, ApplePhoneIcons.GearShapeFill),
                            )
                        }
                        LiquidPhoneTabBar(
                            liquidState = navLiquidState,
                            items = classicItems,
                            selectedIndex = classicTab.ordinal,
                            onSelect = { idx ->
                                classicTab = ClassicTab.entries.getOrElse(idx) { ClassicTab.KEYPAD }
                            },
                            isDark = palette.isDark,
                            maxWidthDp = 368.dp,
                        )
                    }
                }
            }

            // Unified Settings Full Overlay
            AnimatedVisibility(
                visible = showUnifiedSettings && isUnified,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SettingsScreen(
                    settings = uiState.settings,
                    simAccounts = uiState.simAccounts,
                    isDefaultDialer = uiState.isDefaultDialer,
                    isCallScreeningAvailable = uiState.isCallScreeningAvailable,
                    isCallScreeningHeld = uiState.isCallScreeningHeld,
                    bottomInsetPadding = navBarBottomDp,
                    onClose = { showUnifiedSettings = false },
                    onRequestDefaultDialer = requestDefaultDialer,
                    onRequestCallScreening = requestCallScreening,
                    onLayoutModeChange = viewModel::setLayoutMode,
                    onToggleDialpadTones = viewModel::setDialpadTones,
                    onToggleDialpadVibrate = viewModel::setDialpadVibrate,
                    onToggleVibrateOnConnect = viewModel::setVibrateOnConnect,
                    onSelectPreferredSim = viewModel::setPreferredSimId,
                    onBlockNumber = viewModel::blockNumber,
                    onUnblockNumber = viewModel::unblockNumber,
                    onUpdateQuickResponse = viewModel::updateQuickResponse
                )
            }

            // Unified Search Full Overlay
            AnimatedVisibility(
                visible = showUnifiedSearch && isUnified,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                UnifiedSearchOverlay(
                    contacts = uiState.contacts,
                    callLogs = uiState.callLogs,
                    onCallNumber = { viewModel.callNumber(it) },
                    onDismiss = { showUnifiedSearch = false }
                )
            }
        }
    }

    if (showRestrictedSettingsHelp) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsHelp = false },
            containerColor = palette.cardSurface,
            titleContentColor = palette.label,
            textContentColor = palette.secondaryLabel,
            title = { Text("Allow Default Phone Access", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "If Android showed \"App was denied access\" (Restricted settings), " +
                        "Android 13–16 restricts the default phone role for apps installed outside Google Play.\n\n" +
                        "To allow it:\n" +
                        "1. Open Settings > Apps > See all apps > Telvique\n" +
                        "2. Tap the \u22ee menu in the top-right corner\n" +
                        "3. Tap \"Allow restricted settings\" and verify\n" +
                        "4. Return here and tap Set Default again"
                )
            },
            confirmButton = {
                TextButton(onClick = { showRestrictedSettingsHelp = false }) {
                    Text("Got it", color = palette.accentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
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
