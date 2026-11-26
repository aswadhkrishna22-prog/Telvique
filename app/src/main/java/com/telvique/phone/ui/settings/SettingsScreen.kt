package com.telvique.phone.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.telvique.phone.BuildConfig
import com.telvique.phone.data.SimAccountInfo
import com.telvique.phone.data.TelviqueSettings

@Composable
fun SettingsScreen(
    settings: TelviqueSettings,
    simAccounts: List<SimAccountInfo>,
    isDefaultDialer: Boolean,
    isCallScreeningAvailable: Boolean,
    isCallScreeningHeld: Boolean,
    onRequestDefaultDialer: () -> Unit,
    onRequestCallScreening: () -> Unit,
    onToggleDialpadTones: (Boolean) -> Unit,
    onToggleDialpadVibrate: (Boolean) -> Unit,
    onToggleVibrateOnConnect: (Boolean) -> Unit,
    onSelectPreferredSim: (String?) -> Unit,
    onBlockNumber: (String) -> Unit,
    onUnblockNumber: (String) -> Unit,
    onUpdateQuickResponse: (Int, String) -> Unit
) {
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var editingResponsePair by remember { mutableStateOf<Pair<Int, String>?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDefaultDialer) Icons.Rounded.CheckCircle else Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDefaultDialer) "Telvique is your default phone app" else "Not default phone app",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isDefaultDialer) "Handling incoming and outgoing calls" else "Tap to set as default dialer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isDefaultDialer) {
                        Button(onClick = onRequestDefaultDialer) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("Sounds & Haptics")
            SettingSwitchRow(
                title = "Dialpad tones",
                subtitle = "Play DTMF tones while typing numbers",
                checked = settings.dialpadTonesEnabled,
                onCheckedChange = onToggleDialpadTones
            )
            SettingSwitchRow(
                title = "Dialpad vibration",
                subtitle = "Subtle haptic feedback on key press",
                checked = settings.dialpadVibrateEnabled,
                onCheckedChange = onToggleDialpadVibrate
            )
            SettingSwitchRow(
                title = "Vibrate when call connects",
                subtitle = "Short pulse when outgoing call is answered",
                checked = settings.vibrateOnConnectEnabled,
                onCheckedChange = onToggleVibrateOnConnect
            )
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }

        if (simAccounts.size > 1) {
            item {
                SectionTitle("Calling SIM")
                SimOptionRow(
                    label = "Ask every time",
                    selected = settings.preferredSimId == null,
                    onClick = { onSelectPreferredSim(null) }
                )
                simAccounts.forEachIndexed { idx, sim ->
                    SimOptionRow(
                        label = "${sim.label} (SIM ${idx + 1})",
                        selected = settings.preferredSimId == sim.id,
                        onClick = { onSelectPreferredSim(sim.id) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Blocked Numbers", modifier = Modifier.weight(1f))
                IconButton(onClick = { showAddBlockDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Block a number")
                }
            }
            if (isCallScreeningAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCallScreeningHeld) {
                                    "Call screening active"
                                } else {
                                    "Enable call screening"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isCallScreeningHeld) {
                                    "Blocked numbers are screened automatically."
                                } else {
                                    "Optional — required by Android to auto-reject blocked numbers."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isCallScreeningHeld) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onRequestCallScreening) {
                                Text("Enable")
                            }
                        }
                    }
                }
            }
            if (settings.blockedNumbers.isEmpty()) {
                Text(
                    text = "No blocked numbers yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                settings.blockedNumbers.sorted().forEach { number ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onUnblockNumber(number) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Unblock $number")
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }

        item {
            SectionTitle("Quick Reject Messages")
        }

        itemsIndexed(settings.quickResponses) { index, text ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editingResponsePair = index to text }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text(
                text = "Telvique Phone v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }

    if (showAddBlockDialog) {
        var newNumber by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Block phone number") },
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
                        if (newNumber.isNotBlank()) {
                            onBlockNumber(newNumber)
                        }
                        showAddBlockDialog = false
                    }
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    editingResponsePair?.let { (index, currentText) ->
        var draft by remember(index) { mutableStateOf(currentText) }
        AlertDialog(
            onDismissRequest = { editingResponsePair = null },
            title = { Text("Edit quick response") },
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
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingResponsePair = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SimOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.SimCard,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        RadioButton(selected = selected, onClick = onClick)
    }
}
