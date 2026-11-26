package com.telvique.phone.ui.dialer

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.ui.components.ContactAvatar
import com.telvique.phone.ui.components.openAddContact
import com.telvique.phone.ui.theme.TelviqueColors

private data class DialKey(
    val digit: String,
    val letters: String = "",
    val longPressDigit: String? = null
)

private val DialPadRows = listOf(
    listOf(DialKey("1", ""), DialKey("2", "ABC"), DialKey("3", "DEF")),
    listOf(DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO")),
    listOf(DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ")),
    listOf(DialKey("*", ""), DialKey("0", "+", "+"), DialKey("#", ""))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    input: String,
    t9Matches: List<ContactItem>,
    lastDialedNumber: String?,
    dialpadTonesEnabled: Boolean,
    dialpadVibrateEnabled: Boolean,
    onDigitPress: (String) -> Unit,
    onSetNumber: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onCallClick: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 65) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { toneGenerator?.release() }
        }
    }

    fun playKeyFeedback(digit: String) {
        if (dialpadVibrateEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (dialpadTonesEnabled && toneGenerator != null) {
            val tone = when (digit) {
                "0" -> ToneGenerator.TONE_DTMF_0
                "1" -> ToneGenerator.TONE_DTMF_1
                "2" -> ToneGenerator.TONE_DTMF_2
                "3" -> ToneGenerator.TONE_DTMF_3
                "4" -> ToneGenerator.TONE_DTMF_4
                "5" -> ToneGenerator.TONE_DTMF_5
                "6" -> ToneGenerator.TONE_DTMF_6
                "7" -> ToneGenerator.TONE_DTMF_7
                "8" -> ToneGenerator.TONE_DTMF_8
                "9" -> ToneGenerator.TONE_DTMF_9
                "*" -> ToneGenerator.TONE_DTMF_S
                "#" -> ToneGenerator.TONE_DTMF_P
                else -> -1
            }
            if (tone >= 0) {
                runCatching { toneGenerator.startTone(tone, 110) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // T9 matched contacts list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (t9Matches.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(t9Matches, key = { it.id }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (contact.primaryNumber.isNotBlank()) {
                                        onCallClick(contact.primaryNumber, false)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(
                                name = contact.name,
                                photoUri = contact.photoUri,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = contact.primaryNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (contact.primaryNumber.isNotBlank()) {
                                        onCallClick(contact.primaryNumber, false)
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
            } else if (input.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = { openAddContact(context, input) }) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create new contact")
                    }
                }
            }
        }

        // Number display bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val pasted = clipboard.getText()?.text?.toString().orEmpty()
                        val cleaned = PhoneFormatUtils.normalizeDigits(pasted)
                        if (cleaned.isNotBlank()) {
                            onSetNumber(cleaned)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentPaste,
                        contentDescription = "Paste number",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = input.ifEmpty { "Enter number" },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = if (input.length > 13) 22.sp else 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (input.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            enabled = input.isNotEmpty(),
                            onClick = onBackspace,
                            onLongClick = onClearAll
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (input.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(10.dp))

        // Keypad Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DialPadRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        DialPadKeyButton(
                            key = key,
                            onTap = {
                                playKeyFeedback(key.digit)
                                onDigitPress(key.digit)
                            },
                            onLongTap = key.longPressDigit?.let { alt ->
                                {
                                    playKeyFeedback(key.digit)
                                    onDigitPress(alt)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Call action button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = TelviqueColors.CallGreen,
                contentColor = Color.White,
                shape = CircleShape,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = {
                            if (input.isNotBlank()) {
                                onCallClick(input, false)
                            } else if (!lastDialedNumber.isNullOrBlank()) {
                                onSetNumber(lastDialedNumber)
                            }
                        },
                        onLongClick = {
                            if (input.isNotBlank()) {
                                onCallClick(input, true)
                            }
                        }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Place Call",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialPadKeyButton(
    key: DialKey,
    onTap: () -> Unit,
    onLongTap: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .size(width = 92.dp, height = 64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongTap
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = key.digit,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (key.letters.isNotEmpty()) {
            Text(
                text = key.letters,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
