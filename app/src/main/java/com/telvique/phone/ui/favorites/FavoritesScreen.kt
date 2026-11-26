package com.telvique.phone.ui.favorites

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telvique.phone.data.ContactItem
import com.telvique.phone.ui.components.AppleLargeHeader
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.contacts.AppleContactDetailSheet
import com.telvique.phone.ui.glass.HeaderGlassBackdrop
import com.telvique.phone.ui.glass.LiquidCircularGlassButton
import com.telvique.phone.ui.glass.LocalHazeState
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.theme.LocalApplePalette

@Composable
fun FavoritesScreen(
    favorites: List<ContactItem>,
    blockedNumbers: Set<String>,
    bottomInsetPadding: Dp,
    onCallNumber: (String) -> Unit,
    onToggleFavorite: (ContactItem) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit,
    onAddFavoritePrompt: () -> Unit,
) {
    val palette = LocalApplePalette.current
    val hazeState = LocalHazeState.current
    val listState = rememberLazyListState()
    var selectedContact by remember { mutableStateOf<ContactItem?>(null) }

    val headerBlurProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 90f).coerceIn(0f, 1f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (favorites.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppleLargeHeader(title = "Favorites") {
                    LiquidCircularGlassButton(
                        icon = ApplePhoneIcons.Plus,
                        contentDescription = "Add Favorite",
                        onClick = onAddFavoritePrompt,
                        size = 40.dp,
                        iconSize = 18.dp,
                        tint = palette.accentBlue,
                        isDark = palette.isDark,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomInsetPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = ApplePhoneIcons.StarOutline,
                            contentDescription = null,
                            tint = palette.tertiaryLabel,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Favorites",
                            color = palette.secondaryLabel,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Star contacts to pin them here for 1-tap calls.",
                            color = palette.tertiaryLabel,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomInsetPadding + 20.dp)
            ) {
                item(key = "fav_header") {
                    AppleLargeHeader(title = "Favorites") {
                        LiquidCircularGlassButton(
                            icon = ApplePhoneIcons.Plus,
                            contentDescription = "Add Favorite",
                            onClick = onAddFavoritePrompt,
                            size = 40.dp,
                            iconSize = 18.dp,
                            tint = palette.accentBlue,
                            isDark = palette.isDark,
                        )
                    }
                }

                itemsIndexed(favorites, key = { _, it -> it.id }) { index, contact ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (contact.primaryNumber.isNotBlank()) {
                                    onCallNumber(contact.primaryNumber)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(
                                name = contact.name,
                                photoUri = contact.photoUri,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    color = palette.label,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = ApplePhoneIcons.PhoneFill,
                                        contentDescription = null,
                                        tint = palette.secondaryLabel,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = contact.numbers.firstOrNull()?.typeLabel?.lowercase() ?: "mobile",
                                        color = palette.secondaryLabel,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { selectedContact = contact }) {
                                Icon(
                                    imageVector = ApplePhoneIcons.InfoCircle,
                                    contentDescription = "Info",
                                    tint = palette.accentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (index < favorites.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 74.dp),
                                thickness = 0.5.dp,
                                color = palette.separator.copy(alpha = 0.55f)
                            )
                        }
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
    }

    val activeContact = selectedContact?.let { c ->
        favorites.firstOrNull { it.id == c.id } ?: c
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
            onToggleBlock = onToggleBlock,
        )
    }
}
