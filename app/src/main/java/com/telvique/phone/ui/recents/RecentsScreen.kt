package com.telvique.phone.ui.recents

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telvique.phone.data.CallLogItem
import com.telvique.phone.data.CallLogType
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.GroupedCallLogItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.PhoneLayoutMode
import com.telvique.phone.data.RecentsFilter
import com.telvique.phone.data.groupConsecutiveCallLogs
import com.telvique.phone.telecom.CallManager
import com.telvique.phone.ui.components.AppleLargeHeader
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.components.copyNumberToClipboard
import com.telvique.phone.ui.components.openAddContact
import com.telvique.phone.ui.components.openSmsApp
import com.telvique.phone.ui.glass.HeaderGlassBackdrop
import com.telvique.phone.ui.glass.LiquidCircularGlassButton
import com.telvique.phone.ui.glass.clearLiquidGlassPill
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.theme.LocalApplePalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    callLogs: List<CallLogItem>,
    favorites: List<ContactItem>,
    layoutMode: PhoneLayoutMode,
    selectedFilter: RecentsFilter,
    blockedNumbers: Set<String>,
    maskNumbers: Boolean = false,
    maskRevealResetTrigger: Int = 0,
    bottomInsetPadding: Dp,
    onFilterChange: (RecentsFilter) -> Unit,
    onLayoutModeChange: (PhoneLayoutMode) -> Unit,
    onOpenSettings: () -> Unit,
    onCallNumber: (String) -> Unit,
    onDeleteEntries: (List<Long>) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalApplePalette.current
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    var showFilterMenu by remember { mutableStateOf(false) }
    var infoTarget by remember { mutableStateOf<GroupedCallLogItem?>(null) }
    var revealedGroupIds by remember { mutableStateOf(emptySet<Long>()) }
    var detailRevealed by remember { mutableStateOf(false) }
    val groupedLogs = remember(callLogs) { groupConsecutiveCallLogs(callLogs) }

    LaunchedEffect(maskNumbers, maskRevealResetTrigger) {
        revealedGroupIds = emptySet()
        detailRevealed = false
    }

    LaunchedEffect(infoTarget?.primary?.id) {
        detailRevealed = false
    }

    DisposableEffect(Unit) {
        onDispose {
            revealedGroupIds = emptySet()
            detailRevealed = false
        }
    }

    val headerBlurProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 90f).coerceIn(0f, 1f)
        }
    }

    val isUnified = layoutMode == PhoneLayoutMode.UNIFIED

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = PaddingValues(bottom = bottomInsetPadding + 24.dp)
        ) {
            item(key = "recents_header") {
                AppleLargeHeader(title = if (isUnified) "Calls" else "Recents") {
                    Box {
                        LiquidCircularGlassButton(
                            icon = ApplePhoneIcons.FilterLines,
                            contentDescription = "Filter & Layout",
                            onClick = { showFilterMenu = true },
                            size = 40.dp,
                            iconSize = 18.dp,
                            tint = if (selectedFilter != RecentsFilter.ALL) palette.accentBlue else palette.label,
                            isDark = palette.isDark,
                        )
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            containerColor = palette.cardSurface,
                        ) {
                            RecentsFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = filter.label,
                                            color = palette.label,
                                            fontWeight = if (selectedFilter == filter) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    trailingIcon = if (selectedFilter == filter) {
                                        {
                                            Icon(
                                                imageVector = ApplePhoneIcons.Checkmark,
                                                contentDescription = null,
                                                tint = palette.accentBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        onFilterChange(filter)
                                        showFilterMenu = false
                                    }
                                )
                            }
                            HorizontalDivider(color = palette.separator.copy(alpha = 0.5f))
                            PhoneLayoutMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${mode.label} Layout",
                                            color = palette.label,
                                            fontWeight = if (layoutMode == mode) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    trailingIcon = if (layoutMode == mode) {
                                        {
                                            Icon(
                                                imageVector = ApplePhoneIcons.Checkmark,
                                                contentDescription = null,
                                                tint = palette.accentBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        onLayoutModeChange(mode)
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (isUnified) {
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

            // Classic mode shows iOS Segmented Control (All | Missed) right under header
            if (!isUnified) {
                item(key = "classic_segmented") {
                    AppleAllMissedSegmentedControl(
                        selectedFilter = selectedFilter,
                        onSelect = onFilterChange,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Unified mode pins Favorites strip at the top of Calls
            if (isUnified && favorites.isNotEmpty()) {
                item(key = "unified_favorites") {
                    Column(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
                        Text(
                            text = "Favorites",
                            color = palette.secondaryLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(favorites, key = { "ufav_${it.id}" }) { fav ->
                                Column(
                                    modifier = Modifier
                                        .width(86.dp)
                                        .clearLiquidGlassPill(
                                            shape = RoundedCornerShape(20.dp),
                                            hazeState = null,
                                            isDark = palette.isDark,
                                            blurRadius = 8.dp,
                                            shadowElevation = 3.dp,
                                        )
                                        .clickable {
                                            if (fav.primaryNumber.isNotBlank()) {
                                                onCallNumber(fav.primaryNumber)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    ContactAvatar(
                                        name = fav.name,
                                        photoUri = fav.photoUri,
                                        size = 48.dp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = fav.name.substringBefore(' '),
                                        color = palette.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (callLogs.isEmpty()) {
                item(key = "empty_recents") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = ApplePhoneIcons.ClockFill,
                                contentDescription = null,
                                tint = palette.tertiaryLabel,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Recents",
                                color = palette.secondaryLabel,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(groupedLogs, key = { _, g -> g.primary.id }) { index, group ->
                    val item = group.primary
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            onDeleteEntries(group.entryIds)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(palette.callRed)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    ) {
                        AppleRecentCallRow(
                            item = item,
                            count = group.count,
                            showDivider = index < groupedLogs.lastIndex,
                            maskNumbers = maskNumbers,
                            isRevealed = item.id in revealedGroupIds,
                            onToggleReveal = {
                                revealedGroupIds = if (item.id in revealedGroupIds) {
                                    revealedGroupIds - item.id
                                } else {
                                    revealedGroupIds + item.id
                                }
                            },
                            onRowClick = {
                                if (item.number.isNotBlank()) onCallNumber(item.number)
                            },
                            onInfoClick = { infoTarget = group }
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

    val activeGroup = infoTarget
    val activeItem = activeGroup?.primary
    if (activeItem != null) {
        val cnapName = if (activeItem.cachedName.isNullOrBlank()) {
            CallManager.lookupCachedCnapName(activeItem.number)
        } else null
        val effectiveTitle = cnapName ?: activeItem.displayTitle
        val isUnsavedSheetTitle = cnapName == null && activeItem.cachedName.isNullOrBlank() && activeItem.number.isNotBlank()
        val sheetTitleMasked = maskNumbers && isUnsavedSheetTitle && !detailRevealed
        val sheetTitleText = when {
            sheetTitleMasked -> PhoneFormatUtils.maskPhoneNumber(activeItem.number)
            isUnsavedSheetTitle -> PhoneFormatUtils.formatDisplayNumber(activeItem.number)
            else -> effectiveTitle
        }
        val isBlocked = blockedNumbers.any {
            it == activeItem.number || PhoneFormatUtils.numbersMatch(it, activeItem.number)
        }
        ModalBottomSheet(
            onDismissRequest = { infoTarget = null },
            containerColor = palette.cardSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ContactAvatar(
                    name = if (sheetTitleMasked) sheetTitleText else effectiveTitle,
                    photoUri = activeItem.photoUri,
                    size = 76.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (maskNumbers && isUnsavedSheetTitle) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { detailRevealed = !detailRevealed }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sheetTitleText,
                            color = palette.label,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.semantics { contentDescription = sheetTitleText }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (detailRevealed) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (detailRevealed) "Hide number" else "Reveal number",
                            tint = palette.secondaryLabel,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Text(
                        text = sheetTitleText,
                        color = palette.label,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (cnapName != null) {
                    Text(
                        text = "Carrier ID (CNAP)",
                        color = palette.accentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (activeItem.number.isNotBlank() && activeItem.number != effectiveTitle) {
                    val subNumberText = if (maskNumbers && !detailRevealed) {
                        PhoneFormatUtils.maskPhoneNumber(activeItem.number)
                    } else {
                        PhoneFormatUtils.formatDisplayNumber(activeItem.number)
                    }
                    if (maskNumbers) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { detailRevealed = !detailRevealed }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subNumberText,
                                color = palette.secondaryLabel,
                                fontSize = 15.sp,
                                modifier = Modifier.semantics { contentDescription = subNumberText }
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = if (detailRevealed) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = if (detailRevealed) "Hide number" else "Reveal number",
                                tint = palette.tertiaryLabel,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        Text(
                            text = subNumberText,
                            color = palette.secondaryLabel,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                val subtitle = buildList {
                    add(PhoneFormatUtils.formatRelativeTime(activeItem.timestamp))
                    if (activeItem.durationSeconds > 0L) {
                        add(PhoneFormatUtils.formatDuration(activeItem.durationSeconds))
                    }
                    if (!activeItem.simLabel.isNullOrBlank()) {
                        add(activeItem.simLabel)
                    }
                }.joinToString(" • ")
                Text(
                    text = subtitle,
                    color = palette.tertiaryLabel,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Apple circular action pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AppleSheetActionCircle(
                        icon = ApplePhoneIcons.MessageFill,
                        label = "message",
                        tint = palette.accentBlue,
                        onClick = {
                            if (activeItem.number.isNotBlank()) openSmsApp(context, activeItem.number)
                        }
                    )
                    AppleSheetActionCircle(
                        icon = ApplePhoneIcons.PhoneFill,
                        label = "call",
                        tint = palette.callGreen,
                        onClick = {
                            infoTarget = null
                            if (activeItem.number.isNotBlank()) onCallNumber(activeItem.number)
                        }
                    )
                    AppleSheetActionCircle(
                        icon = Icons.Rounded.ContentCopy,
                        label = "copy",
                        tint = palette.accentBlue,
                        onClick = {
                            if (activeItem.number.isNotBlank()) copyNumberToClipboard(context, activeItem.number)
                        }
                    )
                    if (activeItem.cachedName.isNullOrBlank() && activeItem.number.isNotBlank()) {
                        AppleSheetActionCircle(
                            icon = Icons.Rounded.PersonAdd,
                            label = "save",
                            tint = palette.accentBlue,
                            onClick = {
                                infoTarget = null
                                openAddContact(context, activeItem.number)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (activeGroup.entries.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.tertiaryFill)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (activeGroup.entries.size == 1) "Call Detail" else "Call History (${activeGroup.entries.size})",
                            color = palette.secondaryLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        activeGroup.entries.take(8).forEachIndexed { idx, entry ->
                            val typeLabel = when (entry.type) {
                                CallLogType.OUTGOING -> "Outgoing Call"
                                CallLogType.INCOMING -> "Incoming Call"
                                CallLogType.MISSED -> "Missed Call"
                                CallLogType.REJECTED -> "Declined Call"
                                CallLogType.BLOCKED -> "Blocked Call"
                                CallLogType.VOICEMAIL -> "Voicemail"
                                CallLogType.UNKNOWN -> "Call"
                            }
                            val entryIsMissed = entry.type == CallLogType.MISSED || entry.type == CallLogType.REJECTED
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = PhoneFormatUtils.formatRelativeTime(entry.timestamp),
                                    color = palette.secondaryLabel,
                                    fontSize = 13.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = typeLabel,
                                        color = if (entryIsMissed) palette.callRed else palette.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (entry.durationSeconds > 0L) {
                                        Text(
                                            text = " • ${PhoneFormatUtils.formatDuration(entry.durationSeconds)}",
                                            color = palette.tertiaryLabel,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            if (idx < activeGroup.entries.take(8).lastIndex) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = palette.separator.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.tertiaryFill)
                ) {
                    if (activeItem.number.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggleBlock(activeItem.number, !isBlocked)
                                    infoTarget = null
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = null,
                                tint = palette.callRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isBlocked) "Unblock Caller" else "Block Caller",
                                color = palette.callRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 48.dp),
                            thickness = 0.5.dp,
                            color = palette.separator.copy(alpha = 0.5f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDeleteEntries(activeGroup.entryIds)
                                infoTarget = null
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = palette.callRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Delete from Recents",
                            color = palette.callRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AppleAllMissedSegmentedControl(
    selectedFilter: RecentsFilter,
    onSelect: (RecentsFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalApplePalette.current
    val options = listOf(RecentsFilter.ALL, RecentsFilter.MISSED)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.secondaryFill)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { option ->
            val selected = selectedFilter == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) palette.cardSurface else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    color = palette.label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AppleRecentCallRow(
    item: CallLogItem,
    count: Int,
    showDivider: Boolean,
    maskNumbers: Boolean,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onRowClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val palette = LocalApplePalette.current
    val isMissed = item.type == CallLogType.MISSED || item.type == CallLogType.REJECTED
    val cnapName = if (item.cachedName.isNullOrBlank()) {
        CallManager.lookupCachedCnapName(item.number)
    } else null
    val isSpam = item.cachedName.isNullOrBlank() && PhoneFormatUtils.isSuspectedTelemarketer(item.number)
    val isUnsavedRawTitle = cnapName == null && item.cachedName.isNullOrBlank() && item.number.isNotBlank()
    val isTitleMasked = maskNumbers && isUnsavedRawTitle && !isRevealed
    val displayNumberText = if (isUnsavedRawTitle) {
        if (isTitleMasked) PhoneFormatUtils.maskPhoneNumber(item.number)
        else PhoneFormatUtils.formatDisplayNumber(item.number)
    } else {
        cnapName ?: item.displayTitle
    }
    val titleWithCount = if (count > 1) "$displayNumberText ($count)" else displayNumberText
    val avatarSafeName = if (isTitleMasked) displayNumberText else item.displayTitle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background)
            .clickable(onClick = onRowClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left outgoing/incoming icon column (matches authentic iOS Recents indentation)
            Box(
                modifier = Modifier.width(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (item.type) {
                    CallLogType.OUTGOING -> Icon(
                        imageVector = ApplePhoneIcons.PhoneArrowUpRight,
                        contentDescription = "Outgoing",
                        tint = palette.tertiaryLabel,
                        modifier = Modifier.size(14.dp)
                    )
                    CallLogType.INCOMING -> Icon(
                        imageVector = ApplePhoneIcons.PhoneArrowDownLeft,
                        contentDescription = "Incoming",
                        tint = palette.tertiaryLabel,
                        modifier = Modifier.size(14.dp)
                    )
                    else -> Unit
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            ContactAvatar(
                name = avatarSafeName,
                photoUri = item.photoUri,
                size = 38.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (maskNumbers && isUnsavedRawTitle) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleReveal)
                            .padding(vertical = 1.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = titleWithCount,
                            color = if (isMissed || isSpam) palette.callRed else palette.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .semantics { contentDescription = titleWithCount }
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = if (isRevealed) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (isRevealed) "Hide number" else "Reveal number",
                            tint = palette.tertiaryLabel,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Text(
                        text = titleWithCount,
                        color = if (isMissed || isSpam) palette.callRed else palette.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                val meta = buildList {
                    when {
                        isSpam -> add("Telemarketer")
                        cnapName != null -> add("Carrier ID")
                        item.type == CallLogType.MISSED -> add("Missed")
                        item.type == CallLogType.REJECTED -> add("Declined")
                        item.type == CallLogType.BLOCKED -> add("Blocked")
                        else -> add("mobile")
                    }
                    if (!item.simLabel.isNullOrBlank()) add(item.simLabel)
                    if (item.durationSeconds > 0L) add(PhoneFormatUtils.formatDuration(item.durationSeconds))
                }.joinToString(" • ")

                Text(
                    text = meta,
                    color = palette.secondaryLabel,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = PhoneFormatUtils.formatAppleRecentTime(item.timestamp),
                color = palette.secondaryLabel,
                fontSize = 13.sp,
                maxLines = 1
            )

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = ApplePhoneIcons.InfoCircle,
                    contentDescription = "Call Info",
                    tint = palette.accentBlue,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 94.dp),
                thickness = 0.5.dp,
                color = palette.separator.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
internal fun AppleSheetActionCircle(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val palette = LocalApplePalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(palette.tertiaryFill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
