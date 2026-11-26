package com.telvique.phone.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telvique.phone.data.CallLogItem
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.T9Matcher
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.contacts.AppleSearchField
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.theme.LocalApplePalette

/**
 * Full-screen iOS 26 Unified Search overlay triggered from the floating Search Liquid Glass pill.
 */
@Composable
fun UnifiedSearchOverlay(
    contacts: List<ContactItem>,
    callLogs: List<CallLogItem>,
    onCallNumber: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalApplePalette.current
    var query by remember { mutableStateOf("") }

    val matchingContacts = remember(contacts, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) contacts.take(8) else contacts.filter { T9Matcher.matches(it, trimmed) }
    }
    val matchingLogs = remember(callLogs, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) callLogs.take(6)
        else callLogs.filter {
            it.displayTitle.contains(trimmed, ignoreCase = true) ||
                it.number.contains(trimmed, ignoreCase = true)
        }.take(15)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background.copy(alpha = 0.96f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppleSearchField(
                query = query,
                placeholder = "Contacts, numbers & calls",
                onQueryChange = { query = it },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Cancel",
                color = palette.accentBlue,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (matchingContacts.isNotEmpty()) {
                item(key = "sec_contacts") {
                    Text(
                        text = if (query.isBlank()) "Suggested Contacts" else "Contacts",
                        color = palette.secondaryLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(matchingContacts, key = { "sc_${it.id}" }) { contact ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (contact.primaryNumber.isNotBlank()) {
                                    onDismiss()
                                    onCallNumber(contact.primaryNumber)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 38.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    color = palette.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = contact.primaryNumber,
                                    color = palette.secondaryLabel,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = ApplePhoneIcons.PhoneFill,
                                contentDescription = "Call",
                                tint = palette.callGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 70.dp),
                            thickness = 0.5.dp,
                            color = palette.separator.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (matchingLogs.isNotEmpty()) {
                item(key = "sec_recents") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recent Calls",
                        color = palette.secondaryLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(matchingLogs, key = { "sl_${it.id}" }) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (log.number.isNotBlank()) {
                                    onDismiss()
                                    onCallNumber(log.number)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(name = log.displayTitle, photoUri = log.photoUri, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.displayTitle,
                                    color = palette.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = PhoneFormatUtils.formatAppleRecentTime(log.timestamp),
                                    color = palette.secondaryLabel,
                                    fontSize = 13.sp
                                )
                            }
                            Icon(
                                imageVector = ApplePhoneIcons.PhoneFill,
                                contentDescription = "Call",
                                tint = palette.callGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp),
                            thickness = 0.5.dp,
                            color = palette.separator.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (matchingContacts.isEmpty() && matchingLogs.isEmpty()) {
                item(key = "search_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results for \"$query\"",
                            color = palette.secondaryLabel,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
