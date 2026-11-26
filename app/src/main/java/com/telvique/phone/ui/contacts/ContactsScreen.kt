package com.telvique.phone.ui.contacts

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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.components.copyNumberToClipboard
import com.telvique.phone.ui.components.openAddContact
import com.telvique.phone.ui.components.openSmsApp
import com.telvique.phone.ui.components.openSystemContact
import com.telvique.phone.ui.theme.TelviqueColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<ContactItem>,
    favorites: List<ContactItem>,
    searchQuery: String,
    blockedNumbers: Set<String>,
    onSearchQueryChange: (String) -> Unit,
    onCallNumber: (String) -> Unit,
    onToggleFavorite: (ContactItem) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedContact by remember { mutableStateOf<ContactItem?>(null) }

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
                placeholder = { Text("Search contacts") },
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
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(onClick = { openAddContact(context) }) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Contact")
            }
        }

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No contacts found" else "No matching contacts",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (favorites.isNotEmpty() && searchQuery.isBlank()) {
                    item(key = "favorites_header") {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item(key = "favorites_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favorites, key = { "fav_${it.id}" }) { fav ->
                                Column(
                                    modifier = Modifier
                                        .width(74.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (fav.primaryNumber.isNotBlank()) {
                                                onCallNumber(fav.primaryNumber)
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    ContactAvatar(
                                        name = fav.name,
                                        photoUri = fav.photoUri,
                                        size = 54.dp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = fav.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                items(contacts, key = { it.id }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedContact = contact }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactAvatar(
                            name = contact.name,
                            photoUri = contact.photoUri,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (contact.primaryNumber.isNotBlank()) {
                                Text(
                                    text = contact.primaryNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (contact.isStarred) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (contact.primaryNumber.isNotBlank()) {
                                    onCallNumber(contact.primaryNumber)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "Call ${contact.name}",
                                tint = TelviqueColors.CallGreen
                            )
                        }
                    }
                }
            }
        }
    }

    val activeContact = selectedContact?.let { current ->
        contacts.firstOrNull { it.id == current.id } ?: current
    }
    if (activeContact != null) {
        ModalBottomSheet(onDismissRequest = { selectedContact = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ContactAvatar(
                    name = activeContact.name,
                    photoUri = activeContact.photoUri,
                    size = 72.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = activeContact.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onToggleFavorite(activeContact) }) {
                        Icon(
                            imageVector = if (activeContact.isStarred) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (activeContact.isStarred) "Starred" else "Favorite")
                    }
                    OutlinedButton(
                        onClick = {
                            openSystemContact(context, activeContact.id, activeContact.lookupKey)
                            selectedContact = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                activeContact.numbers.forEach { entry ->
                    val isBlocked = blockedNumbers.any {
                        it == entry.number || PhoneFormatUtils.numbersMatch(it, entry.number)
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.number,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = entry.typeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { copyNumberToClipboard(context, entry.number) }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy")
                            }
                            IconButton(onClick = { openSmsApp(context, entry.number) }) {
                                Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "Message")
                            }
                            IconButton(onClick = { onToggleBlock(entry.number, !isBlocked) }) {
                                Icon(
                                    Icons.Rounded.Block,
                                    contentDescription = if (isBlocked) "Unblock" else "Block",
                                    tint = if (isBlocked) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedContact = null
                                    onCallNumber(entry.number)
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.Call,
                                    contentDescription = "Call",
                                    tint = TelviqueColors.CallGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
