package com.telvique.phone.ui.settings

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telvique.phone.BuildConfig
import com.telvique.phone.data.PhoneLayoutMode
import com.telvique.phone.data.SimAccountInfo
import com.telvique.phone.data.TelviqueSettings
import com.telvique.phone.ui.components.AppleLargeHeader
import com.telvique.phone.ui.glass.HeaderGlassBackdrop
import com.telvique.phone.ui.glass.LiquidCircularGlassButton
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.theme.LocalApplePalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsScreen(
    settings: TelviqueSettings,
    simAccounts: List<SimAccountInfo>,
    isDefaultDialer: Boolean,
    isCallScreeningAvailable: Boolean,
    isCallScreeningHeld: Boolean,
    bottomInsetPadding: Dp,
    onClose: (() -> Unit)? = null,
    onRequestDefaultDialer: () -> Unit,
    onRequestCallScreening: () -> Unit,
    onLayoutModeChange: (PhoneLayoutMode) -> Unit,
    onToggleDialpadTones: (Boolean) -> Unit,
    onToggleDialpadVibrate: (Boolean) -> Unit,
    onToggleVibrateOnConnect: (Boolean) -> Unit,
    onSelectPreferredSim: (String?) -> Unit,
    onBlockNumber: (String) -> Unit,
    onUnblockNumber: (String) -> Unit,
    onUpdateQuickResponse: (Int, String) -> Unit,
    onSetSpeedDial: (String, String?) -> Unit = { _, _ -> },
    onSetVoicemailNumber: (String) -> Unit = {},
    canAuthenticateDevice: Boolean = true,
    onRequestEnableAppLock: () -> Unit = {},
    onToggleAppLock: (Boolean) -> Unit = {},
    onLockGraceChange: (Int) -> Unit = {},
    onToggleHideInRecents: (Boolean) -> Unit = {},
    onToggleMaskNumbers: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val palette = LocalApplePalette.current
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var editingResponsePair by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var editingSpeedDialDigit by remember { mutableStateOf<String?>(null) }
    var showLockGraceDialog by remember { mutableStateOf(false) }
    var showNoDeviceLockDialog by remember { mutableStateOf(false) }
    val canFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
    } else {
        true
    }

    val headerBlurProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 90f).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.groupedBackground)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = PaddingValues(bottom = bottomInsetPadding + 28.dp)
        ) {
            item(key = "settings_header") {
                AppleLargeHeader(title = "Settings") {
                    if (onClose != null) {
                        LiquidCircularGlassButton(
                            icon = ApplePhoneIcons.XMark,
                            contentDescription = "Close Settings",
                            onClick = onClose,
                            size = 40.dp,
                            iconSize = 18.dp,
                            tint = palette.label,
                            isDark = palette.isDark,
                        )
                    }
                }
            }

            // Layout Mode switcher (iOS 26 Unified vs Classic)
            item(key = "layout_mode_section") {
                AppleSectionHeader("Phone View")
                AppleGroupedCard {
                    PhoneLayoutMode.entries.forEachIndexed { idx, mode ->
                        val selected = settings.layoutMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLayoutModeChange(mode) }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (mode == PhoneLayoutMode.UNIFIED) "Unified (iOS 26)" else "Classic",
                                    color = palette.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (mode == PhoneLayoutMode.UNIFIED) {
                                        "Calls, Contacts, Keypad + floating Search pill"
                                    } else {
                                        "Favorites, Recents, Contacts, Keypad, Settings"
                                    },
                                    color = palette.secondaryLabel,
                                    fontSize = 13.sp
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = ApplePhoneIcons.Checkmark,
                                    contentDescription = "Selected",
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (idx < PhoneLayoutMode.entries.lastIndex) {
                            AppleInsetDivider()
                        }
                    }
                }
            }

            // Default Dialer & Screening
            item(key = "dialer_status_section") {
                AppleSectionHeader("System Integration")
                AppleGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDefaultDialer) Icons.Rounded.CheckCircle else Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = if (isDefaultDialer) palette.callGreen else palette.accentBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDefaultDialer) "Default Phone App" else "Set as Default Phone",
                                color = palette.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isDefaultDialer) "Active for all calls" else "Required to handle calls",
                                color = palette.secondaryLabel,
                                fontSize = 13.sp
                            )
                        }
                        if (!isDefaultDialer) {
                            Button(
                                onClick = onRequestDefaultDialer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accentBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Enable", fontSize = 13.sp)
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !canFullScreen) {
                        AppleInsetDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Full-Screen Incoming Calls",
                                    color = palette.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Allow full-screen popup on lock screen",
                                    color = palette.secondaryLabel,
                                    fontSize = 13.sp
                                )
                            }
                            TextButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    runCatching { context.startActivity(intent) }
                                }
                            ) {
                                Text("Allow", color = palette.accentBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (isCallScreeningAvailable) {
                        AppleInsetDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Call Screening",
                                    color = palette.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isCallScreeningHeld) {
                                        "Auto-rejecting blocked callers"
                                    } else {
                                        "Enable to block calls automatically"
                                    },
                                    color = palette.secondaryLabel,
                                    fontSize = 13.sp
                                )
                            }
                            if (!isCallScreeningHeld) {
                                TextButton(onClick = onRequestCallScreening) {
                                    Text("Enable", color = palette.accentBlue, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Text(
                                    text = "Active",
                                    color = palette.callGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    AppleInsetDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val callSettingsIntent = Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                val opened = runCatching { context.startActivity(callSettingsIntent) }.isSuccess
                                if (!opened) {
                                    val fallbackIntent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    runCatching { context.startActivity(fallbackIntent) }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Carrier Call Settings",
                                color = palette.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Call forwarding, waiting, SIM & operator options",
                                color = palette.secondaryLabel,
                                fontSize = 13.sp
                            )
                        }
                        Icon(
                            imageVector = ApplePhoneIcons.ChevronRight,
                            contentDescription = null,
                            tint = palette.tertiaryLabel,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Privacy
            item(key = "privacy_section") {
                AppleSectionHeader("Privacy")
                AppleGroupedCard {
                    AppleSwitchRow(
                        title = "App Lock",
                        subtitle = "Require biometrics or device PIN to open Telvique",
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enable ->
                            if (!enable) {
                                onToggleAppLock(false)
                            } else if (!canAuthenticateDevice) {
                                showNoDeviceLockDialog = true
                            } else {
                                onRequestEnableAppLock()
                            }
                        }
                    )
                    if (settings.appLockEnabled && !canAuthenticateDevice) {
                        AppleInsetDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = palette.callRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Screen lock removed on this device. App Lock will not lock until you set a PIN or biometric, or turn App Lock off.",
                                color = palette.callRed,
                                fontSize = 12.sp
                            )
                        }
                    }
                    AppleInsetDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLockGraceDialog = true }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lock Delay",
                                color = palette.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Grace period after leaving Telvique",
                                color = palette.secondaryLabel,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = lockGraceLabel(settings.lockGraceSeconds),
                            color = palette.accentBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = ApplePhoneIcons.ChevronRight,
                            contentDescription = null,
                            tint = palette.tertiaryLabel,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    AppleInsetDivider()
                    AppleSwitchRow(
                        title = "Hide in App Switcher",
                        subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            "Hide recent-apps preview thumbnail"
                        } else {
                            "Hide preview thumbnail (also blocks screenshots)"
                        },
                        checked = settings.hideInRecents,
                        onCheckedChange = onToggleHideInRecents
                    )
                    AppleInsetDivider()
                    AppleSwitchRow(
                        title = "Hide Phone Numbers",
                        subtitle = "Mask numbers in Recents and Search; tap a number to reveal",
                        checked = settings.maskNumbers,
                        onCheckedChange = onToggleMaskNumbers
                    )
                }
            }

            // Sounds & Haptics
            item(key = "sounds_section") {
                AppleSectionHeader("Sounds & Haptics")
                AppleGroupedCard {
                    AppleSwitchRow(
                        title = "Dialpad Tones",
                        subtitle = "Play DTMF tones while dialing",
                        checked = settings.dialpadTonesEnabled,
                        onCheckedChange = onToggleDialpadTones
                    )
                    AppleInsetDivider()
                    AppleSwitchRow(
                        title = "Dialpad Vibration",
                        subtitle = "Tactile feedback on key press",
                        checked = settings.dialpadVibrateEnabled,
                        onCheckedChange = onToggleDialpadVibrate
                    )
                    AppleInsetDivider()
                    AppleSwitchRow(
                        title = "Vibrate on Connect",
                        subtitle = "Subtle pulse when call is answered",
                        checked = settings.vibrateOnConnectEnabled,
                        onCheckedChange = onToggleVibrateOnConnect
                    )
                }
            }

            // Dual SIM selector
            if (simAccounts.size > 1) {
                item(key = "sim_section") {
                    AppleSectionHeader("Calling Line")
                    AppleGroupedCard {
                        AppleSimRow(
                            label = "Ask Every Time",
                            selected = settings.preferredSimId == null,
                            onClick = { onSelectPreferredSim(null) }
                        )
                        simAccounts.forEachIndexed { idx, sim ->
                            AppleInsetDivider()
                            AppleSimRow(
                                label = "${sim.label} (SIM ${idx + 1})",
                                selected = settings.preferredSimId == sim.id,
                                onClick = { onSelectPreferredSim(sim.id) }
                            )
                        }
                    }
                }
            }

            // Blocked Contacts
            item(key = "blocked_section") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BLOCKED CONTACTS",
                        color = palette.secondaryLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showAddBlockDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Block Number",
                            tint = palette.accentBlue
                        )
                    }
                }

                AppleGroupedCard {
                    if (settings.blockedNumbers.isEmpty()) {
                        Text(
                            text = "No blocked numbers",
                            color = palette.secondaryLabel,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    } else {
                        val sorted = settings.blockedNumbers.sorted()
                        sorted.forEachIndexed { idx, number ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = palette.callRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = number,
                                    color = palette.label,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onUnblockNumber(number) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Unblock",
                                        tint = palette.secondaryLabel,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            if (idx < sorted.lastIndex) AppleInsetDivider()
                        }
                    }
                }
            }

            // Speed Dial (1 Voicemail, 2-9 long-press)
            item(key = "speed_dial_section") {
                AppleSectionHeader("Speed Dial (Long-Press Keypad)")
                AppleGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingSpeedDialDigit = "1" }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1 • Voicemail",
                            color = palette.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = settings.voicemailNumber,
                            color = palette.accentBlue,
                            fontSize = 14.sp
                        )
                    }
                    for (d in '2'..'9') {
                        val digit = d.toString()
                        val assigned = settings.speedDial[digit]
                        AppleInsetDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingSpeedDialDigit = digit }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Key $digit",
                                color = palette.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = assigned ?: "Not set",
                                color = if (assigned != null) palette.accentBlue else palette.tertiaryLabel,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Respond with Text
            item(key = "quick_responses_section") {
                AppleSectionHeader("Respond with Text")
                AppleGroupedCard {
                    settings.quickResponses.forEachIndexed { idx, text ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingResponsePair = idx to text }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = text,
                                color = palette.label,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = ApplePhoneIcons.ChevronRight,
                                contentDescription = null,
                                tint = palette.tertiaryLabel,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (idx < settings.quickResponses.lastIndex) AppleInsetDivider()
                    }
                }
            }

            item(key = "version_footer") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Telvique Phone v${BuildConfig.VERSION_NAME}",
                    color = palette.secondaryLabel,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        if (headerBlurProgress > 0.05f) {
            HeaderGlassBackdrop(
                progress = headerBlurProgress,
                surfaceColor = palette.groupedBackground,
                hazeState = hazeState,
                height = 56.dp,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    if (showAddBlockDialog) {
        var newNumber by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            containerColor = palette.cardSurface,
            titleContentColor = palette.label,
            title = { Text("Block Number", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = newNumber,
                    onValueChange = { newNumber = it },
                    placeholder = { Text("Enter phone number") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newNumber.isNotBlank()) onBlockNumber(newNumber)
                        showAddBlockDialog = false
                    }
                ) {
                    Text("Block", color = palette.callRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) {
                    Text("Cancel", color = palette.accentBlue)
                }
            }
        )
    }

    editingResponsePair?.let { (index, currentText) ->
        var draft by remember(index) { mutableStateOf(currentText) }
        AlertDialog(
            onDismissRequest = { editingResponsePair = null },
            containerColor = palette.cardSurface,
            titleContentColor = palette.label,
            title = { Text("Edit Quick Response", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    maxLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateQuickResponse(index, draft)
                        editingResponsePair = null
                    }
                ) {
                    Text("Save", color = palette.accentBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingResponsePair = null }) {
                    Text("Cancel", color = palette.secondaryLabel)
                }
            }
        )
    }

    editingSpeedDialDigit?.let { digit ->
        val initial = if (digit == "1") settings.voicemailNumber else settings.speedDial[digit].orEmpty()
        var draft by remember(digit) { mutableStateOf(initial) }
        AlertDialog(
            onDismissRequest = { editingSpeedDialDigit = null },
            containerColor = palette.cardSurface,
            titleContentColor = palette.label,
            title = {
                Text(
                    text = if (digit == "1") "Voicemail Number (Key 1)" else "Speed Dial Key $digit",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Enter phone number") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (digit == "1") onSetVoicemailNumber(draft) else onSetSpeedDial(digit, draft)
                        editingSpeedDialDigit = null
                    }
                ) {
                    Text("Save", color = palette.accentBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    if (digit != "1" && initial.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onSetSpeedDial(digit, null)
                                editingSpeedDialDigit = null
                            }
                        ) {
                            Text("Clear", color = palette.callRed)
                        }
                    }
                    TextButton(onClick = { editingSpeedDialDigit = null }) {
                        Text("Cancel", color = palette.secondaryLabel)
                    }
                }
            }
        )
    }

    if (showLockGraceDialog) {
        val options = listOf(0, 30, 60, 300)
        AlertDialog(
            onDismissRequest = { showLockGraceDialog = false },
            containerColor = palette.cardSurface,
            titleContentColor = palette.label,
            title = { Text("Lock Delay", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    options.forEachIndexed { idx, seconds ->
                        val selected = settings.lockGraceSeconds == seconds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLockGraceChange(seconds)
                                    showLockGraceDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lockGraceLabel(seconds),
                                color = palette.label,
                                fontSize = 16.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = ApplePhoneIcons.Checkmark,
                                    contentDescription = "Selected",
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (idx < options.lastIndex) AppleInsetDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLockGraceDialog = false }) {
                    Text("Close", color = palette.accentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showNoDeviceLockDialog) {
        AlertDialog(
            onDismissRequest = { showNoDeviceLockDialog = false },
            containerColor = palette.cardSurface,
            titleContentColor = palette.label,
            textContentColor = palette.secondaryLabel,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = palette.accentBlue
                )
            },
            title = { Text("Device Screen Lock Required", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Set up a PIN, pattern, password, or biometric unlock in Android Settings before enabling App Lock."
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoDeviceLockDialog = false }) {
                    Text("OK", color = palette.accentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

private fun lockGraceLabel(seconds: Int): String = when (seconds) {
    0 -> "Immediately"
    30 -> "30 seconds"
    60 -> "1 minute"
    300 -> "5 minutes"
    else -> "${seconds}s"
}

@Composable
private fun AppleSectionHeader(title: String) {
    val palette = LocalApplePalette.current
    Text(
        text = title.uppercase(),
        color = palette.secondaryLabel,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun AppleGroupedCard(content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalApplePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.cardSurface),
        content = content
    )
}

@Composable
private fun AppleInsetDivider() {
    val palette = LocalApplePalette.current
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = palette.separator.copy(alpha = 0.5f)
    )
}

@Composable
private fun AppleSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalApplePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = palette.secondaryLabel,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.callGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = palette.secondaryFill,
                uncheckedBorderColor = Color.Transparent,
            )
        )
    }
}

@Composable
private fun AppleSimRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalApplePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.SimCard,
            contentDescription = null,
            tint = palette.accentBlue,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = palette.label,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = ApplePhoneIcons.Checkmark,
                contentDescription = "Selected",
                tint = palette.accentBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
