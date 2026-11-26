package com.telvique.phone.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.ui.components.AppleLargeHeader
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.components.copyNumberToClipboard
import com.telvique.phone.ui.components.openAddContact
import com.telvique.phone.ui.components.openSmsApp
import com.telvique.phone.ui.components.openSystemContact
import com.telvique.phone.ui.components.posterColorsForName
import com.telvique.phone.ui.glass.HeaderGlassBackdrop
import com.telvique.phone.ui.glass.LiquidCircularGlassButton
import com.telvique.phone.ui.glass.clearLiquidGlassPill
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.recents.AppleSheetActionCircle
import com.telvique.phone.ui.theme.LocalApplePalette
import com.telvique.phone.ui.theme.TelviqueColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    contacts: List<ContactItem>,
    searchQuery: String,
    blockedNumbers: Set<String>,
    showTopSettingsButton: Boolean,
    bottomInsetPadding: Dp,
    onOpenSettings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCallNumber: (String) -> Unit,
    onToggleFavorite: (ContactItem) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalApplePalette.current
    val hazeState = remember { HazeState() }
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedContact by remember { mutableStateOf<ContactItem?>(null) }

    val grouped = remember(contacts) {
        contacts.groupBy { contact ->
            val first = contact.name.trim().firstOrNull()?.uppercaseChar()
            if (first != null && first in 'A'..'Z') first.toString() else "#"
        }.toSortedMap(compareBy<String> { if (it == "#") 1 else 0 }.thenBy { it })
    }

    val sectionLetters = remember(grouped) { grouped.keys.toList() }
    val sectionScrollIndices = remember(grouped) {
        val map = mutableMapOf<String, Int>()
        var idx = 2 // 0 = header, 1 = search bar
        grouped.forEach { (letter, list) ->
            map[letter] = idx
            idx += 1 + list.size
        }
        map
    }

    val headerBlurProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 90f).coerceIn(0f, 1f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = PaddingValues(bottom = bottomInsetPadding + 24.dp)
        ) {
            item(key = "contacts_header") {
                AppleLargeHeader(title = "Contacts") {
                    LiquidCircularGlassButton(
                        icon = ApplePhoneIcons.Plus,
                        contentDescription = "Add Contact",
                        onClick = { openAddContact(context) },
                        size = 40.dp,
                        iconSize = 18.dp,
                        tint = palette.accentBlue,
                        isDark = palette.isDark,
                    )
                    if (showTopSettingsButton) {
                        LiquidCircularGlassButton(
                            icon = ApplePhoneIcons.GearShapeFill,
                            contentDescription = "Settings",
                            onClick = onOpenSettings,
                            size = 40.dp,
                            iconSize = 18.dp,
                            tint = palette.label,
                            isDark = palette.isDark,
                        )
                    }
                }
            }

            item(key = "contacts_search") {
                AppleSearchField(
                    query = searchQuery,
                    placeholder = "Search",
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 24.dp, top = 2.dp, bottom = 10.dp)
                )
            }

            if (contacts.isEmpty()) {
                item(key = "contacts_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No Contacts" else "No Results",
                            color = palette.secondaryLabel,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                grouped.forEach { (letter, sectionContacts) ->
                    item(key = "hdr_$letter") {
                        Text(
                            text = letter,
                            color = palette.secondaryLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 28.dp, top = 10.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(sectionContacts, key = { _, c -> c.id }) { idx, contact ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedContact = contact }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 28.dp, top = 9.dp, bottom = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        color = palette.label,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (contact.primaryNumber.isNotBlank()) {
                                        Text(
                                            text = contact.primaryNumber,
                                            color = palette.secondaryLabel,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (contact.isStarred) {
                                    Icon(
                                        imageVector = ApplePhoneIcons.StarFill,
                                        contentDescription = "Favorite",
                                        tint = TelviqueColors.FavoriteGold,
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                            if (idx < sectionContacts.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp, end = 24.dp),
                                    thickness = 0.5.dp,
                                    color = palette.separator.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Apple A-Z right-edge drag index scrubber
        if (sectionLetters.size > 3 && searchQuery.isBlank()) {
            var indexHeight by remember { mutableIntStateOf(1) }
            var lastLetterIdx by remember { mutableIntStateOf(-1) }

            fun jumpToY(y: Float) {
                val fraction = (y / indexHeight.coerceAtLeast(1)).coerceIn(0f, 0.999f)
                val targetIdx = (fraction * sectionLetters.size).toInt().coerceIn(0, sectionLetters.lastIndex)
                if (targetIdx != lastLetterIdx) {
                    lastLetterIdx = targetIdx
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val scrollTo = sectionScrollIndices[sectionLetters[targetIdx]] ?: 0
                    scope.launch { listState.scrollToItem(scrollTo) }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 120.dp, bottom = bottomInsetPadding + 32.dp)
                    .fillMaxHeight(0.65f)
                    .width(20.dp)
                    .onSizeChanged { indexHeight = it.height }
                    .pointerInput(sectionLetters) {
                        detectVerticalDragGestures(
                            onDragStart = { offset -> jumpToY(offset.y) },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                jumpToY(change.position.y)
                            },
                            onDragEnd = { lastLetterIdx = -1 },
                            onDragCancel = { lastLetterIdx = -1 },
                        )
                    },
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                sectionLetters.forEach { letter ->
                    Text(
                        text = letter,
                        color = palette.accentBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val scrollTo = sectionScrollIndices[letter] ?: 0
                            scope.launch { listState.scrollToItem(scrollTo) }
                        }
                    )
                }
            }
        }

        if (headerBlurProgress > 0.05f) {
            HeaderGlassBackdrop(
                progress = headerBlurProgress,
                surfaceColor = palette.background,
                hazeState = hazeState,
                height = 56.dp,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    val activeContact = selectedContact?.let { current ->
        contacts.firstOrNull { it.id == current.id } ?: current
    }
    if (activeContact != null) {
        AppleContactDetailSheet(
            contact = activeContact,
            blockedNumbers = blockedNumbers,
            onDismiss = { selectedContact = null },
            onCallNumber = { number ->
                selectedContact = null
                onCallNumber(number)
            },
            onToggleFavorite = onToggleFavorite,
            onToggleBlock = onToggleBlock
        )
    }
}

@Composable
fun AppleSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalApplePalette.current

    Row(
        modifier = modifier
            .height(42.dp)
            .clearLiquidGlassPill(
                shape = CircleShape,
                hazeState = null,
                isDark = palette.isDark,
                blurRadius = 8.dp,
                shadowElevation = 2.dp,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ApplePhoneIcons.MagnifyingGlass,
            contentDescription = null,
            tint = palette.secondaryLabel,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    color = palette.secondaryLabel,
                    fontSize = 16.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.label,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(palette.accentBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = ApplePhoneIcons.XMark,
                    contentDescription = "Clear",
                    tint = palette.secondaryLabel,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleContactDetailSheet(
    contact: ContactItem,
    blockedNumbers: Set<String>,
    onDismiss: () -> Unit,
    onCallNumber: (String) -> Unit,
    onToggleFavorite: (ContactItem) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalApplePalette.current
    val (posterTop, posterBottom) = posterColorsForName(contact.name)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.cardSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contact Poster ambient header band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                posterTop.copy(alpha = 0.28f),
                                posterBottom.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ContactAvatar(
                        name = contact.name,
                        photoUri = contact.photoUri,
                        size = 86.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = contact.name,
                        color = palette.label,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppleSheetActionCircle(
                    icon = ApplePhoneIcons.MessageFill,
                    label = "message",
                    tint = palette.accentBlue,
                    onClick = {
                        if (contact.primaryNumber.isNotBlank()) openSmsApp(context, contact.primaryNumber)
                    }
                )
                AppleSheetActionCircle(
                    icon = ApplePhoneIcons.PhoneFill,
                    label = "call",
                    tint = palette.callGreen,
                    onClick = {
                        if (contact.primaryNumber.isNotBlank()) onCallNumber(contact.primaryNumber)
                    }
                )
                AppleSheetActionCircle(
                    icon = if (contact.isStarred) ApplePhoneIcons.StarFill else ApplePhoneIcons.StarOutline,
                    label = if (contact.isStarred) "starred" else "favorite",
                    tint = if (contact.isStarred) TelviqueColors.FavoriteGold else palette.accentBlue,
                    onClick = { onToggleFavorite(contact) }
                )
                AppleSheetActionCircle(
                    icon = Icons.Rounded.Edit,
                    label = "edit",
                    tint = palette.accentBlue,
                    onClick = {
                        openSystemContact(context, contact.id, contact.lookupKey)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.tertiaryFill)
            ) {
                contact.numbers.forEachIndexed { idx, entry ->
                    val isBlocked = blockedNumbers.any {
                        it == entry.number || PhoneFormatUtils.numbersMatch(it, entry.number)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCallNumber(entry.number) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.typeLabel.lowercase(),
                                color = palette.secondaryLabel,
                                fontSize = 12.sp
                            )
                            Text(
                                text = entry.number,
                                color = palette.accentBlue,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { copyNumberToClipboard(context, entry.number) }) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                                tint = palette.secondaryLabel,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(onClick = { onToggleBlock(entry.number, !isBlocked) }) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = if (isBlocked) "Unblock" else "Block",
                                tint = if (isBlocked) palette.callRed else palette.secondaryLabel,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    if (idx < contact.numbers.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = palette.separator.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
