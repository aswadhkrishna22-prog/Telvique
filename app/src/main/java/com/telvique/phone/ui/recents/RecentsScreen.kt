package com.telvique.phone.ui.recents

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Voicemail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telvique.phone.data.CallLogItem
import com.telvique.phone.data.CallLogType
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.RecentsFilter
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.components.copyNumberToClipboard
import com.telvique.phone.ui.components.openAddContact
import com.telvique.phone.ui.components.openSmsApp
import com.telvique.phone.ui.theme.TelviqueColors

@Composable
fun RecentsScreen(
    callLogs: List<CallLogItem>,
    selectedFilter: RecentsFilter,
    searchQuery: String,
    blockedNumbers: Set<String>,
    onFilterChange: (RecentsFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCallNumber: (String) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onClearAll: () -> Unit,
    onToggleBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search recents") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            if (callLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Clear call history"
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RecentsFilter.entries) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent calls",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(callLogs, key = { it.id }) { item ->
                    val isExpanded = expandedId == item.id
                    val isBlocked = blockedNumbers.any {
                        it == item.number || PhoneFormatUtils.numbersMatch(it, item.number)
                    }

                    CallLogRow(
                        item = item,
                        isExpanded = isExpanded,
                        isBlocked = isBlocked,
                        onRowClick = {
                            expandedId = if (isExpanded) null else item.id
                        },
                        onCallClick = {
                            if (item.number.isNotBlank()) onCallNumber(item.number)
                        },
                        onMessageClick = {
                            if (item.number.isNotBlank()) openSmsApp(context, item.number)
                        },
                        onAddContactClick = {
                            if (item.number.isNotBlank()) openAddContact(context, item.number)
                        },
                        onCopyClick = {
                            if (item.number.isNotBlank()) copyNumberToClipboard(context, item.number)
                        },
                        onBlockToggle = {
                            if (item.number.isNotBlank()) onToggleBlock(item.number, !isBlocked)
                        },
                        onDeleteClick = { onDeleteEntry(item.id) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear call history?") },
            text = { Text("This removes all recent call entries from your device call log.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CallLogRow(
    item: CallLogItem,
    isExpanded: Boolean,
    isBlocked: Boolean,
    onRowClick: () -> Unit,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onCopyClick: () -> Unit,
    onBlockToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val typeColor = when (item.type) {
        CallLogType.MISSED, CallLogType.REJECTED -> TelviqueColors.MissedRed
        CallLogType.INCOMING -> TelviqueColors.IncomingBlue
        CallLogType.OUTGOING -> TelviqueColors.OutgoingGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val typeIcon: ImageVector = when (item.type) {
        CallLogType.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
        CallLogType.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
        CallLogType.MISSED, CallLogType.REJECTED -> Icons.AutoMirrored.Rounded.CallMissed
        CallLogType.BLOCKED -> Icons.Rounded.Block
        CallLogType.VOICEMAIL -> Icons.Rounded.Voicemail
        CallLogType.UNKNOWN -> Icons.Rounded.Call
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onRowClick),
        color = if (isExpanded) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactAvatar(
                    name = item.displayTitle,
                    photoUri = item.photoUri,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (item.type == CallLogType.MISSED) {
                            TelviqueColors.MissedRed
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val metaParts = buildList {
                            add(PhoneFormatUtils.formatRelativeTime(item.timestamp))
                            if (item.durationSeconds > 0L) {
                                add(PhoneFormatUtils.formatDuration(item.durationSeconds))
                            }
                            if (!item.simLabel.isNullOrBlank()) {
                                add(item.simLabel)
                            }
                        }
                        Text(
                            text = metaParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onCallClick) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Call ${item.displayTitle}",
                        tint = TelviqueColors.CallGreen
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionChipButton(Icons.Rounded.ChatBubbleOutline, "Message", onMessageClick)
                        if (item.cachedName.isNullOrBlank()) {
                            ActionChipButton(Icons.Rounded.PersonAdd, "Save", onAddContactClick)
                        }
                        ActionChipButton(Icons.Rounded.ContentCopy, "Copy", onCopyClick)
                        ActionChipButton(
                            Icons.Rounded.Block,
                            if (isBlocked) "Unblock" else "Block",
                            onBlockToggle
                        )
                        ActionChipButton(Icons.Rounded.DeleteOutline, "Delete", onDeleteClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChipButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
