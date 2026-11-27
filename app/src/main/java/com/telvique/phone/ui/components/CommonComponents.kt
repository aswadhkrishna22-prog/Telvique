package com.telvique.phone.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.telvique.phone.data.SimAccountInfo
import com.telvique.phone.ui.glass.clearLiquidGlassPill
import com.telvique.phone.ui.icons.ApplePhoneIcons
import com.telvique.phone.ui.theme.LocalApplePalette
import kotlin.math.absoluteValue

private val AppleAvatarGradients = listOf(
    Color(0xFF9EA5B1) to Color(0xFF7D838F),
    Color(0xFF5AC8FA) to Color(0xFF007AFF),
    Color(0xFF64D2FF) to Color(0xFF5E5CE6),
    Color(0xFF34C759) to Color(0xFF00A86B),
    Color(0xFFFF9F0A) to Color(0xFFFF6B00),
    Color(0xFFFF6482) to Color(0xFFFF2D55),
    Color(0xFFBF5AF2) to Color(0xFF8944AB),
)

fun posterColorsForName(name: String): Pair<Color, Color> =
    AppleAvatarGradients[(name.hashCode().absoluteValue) % AppleAvatarGradients.size]

@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    photoUriLarge: String? = null
) {
    val (topColor, bottomColor) = AppleAvatarGradients[0]
    val words = name.trim().split(' ').filter { it.isNotBlank() }
    val initials = when {
        words.size >= 2 -> "${words.first().first().uppercaseChar()}${words.last().first().uppercaseChar()}"
        words.size == 1 && words.first().first().isLetter() -> words.first().first().uppercaseChar().toString()
        else -> null
    }

    val effectivePhotoUri = if (size >= 56.dp) {
        photoUriLarge?.takeIf { it.isNotBlank() } ?: photoUri
    } else {
        photoUri
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(topColor, bottomColor))),
        contentAlignment = Alignment.Center
    ) {
        if (!effectivePhotoUri.isNullOrBlank()) {
            AsyncImage(
                model = effectivePhotoUri,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!initials.isNullOrBlank()) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(size * 0.56f)
            )
        }
    }
}

@Composable
fun AppleLargeHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val palette = LocalApplePalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = palette.label,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@Composable
fun SetupPromptBanner(
    isDefaultDialer: Boolean,
    missingPermissionsCount: Int,
    onSetDefaultDialer: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    if (isDefaultDialer && missingPermissionsCount == 0) return
    val palette = LocalApplePalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clearLiquidGlassPill(
                shape = RoundedCornerShape(18.dp),
                hazeState = null,
                isDark = palette.isDark,
                blurRadius = 10.dp,
                shadowElevation = 4.dp,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = palette.accentBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (!isDefaultDialer) {
                    "Set Telvique as Default Phone"
                } else {
                    "Permissions needed ($missingPermissionsCount)"
                },
                color = palette.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (!isDefaultDialer) {
                    "Required for incoming & outgoing calls."
                } else {
                    "Allow contacts, call log, and phone access."
                },
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryLabel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = if (!isDefaultDialer) onSetDefaultDialer else onRequestPermissions,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accentBlue,
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (!isDefaultDialer) "Set Default" else "Allow",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun SimPickerDialog(
    number: String,
    sims: List<SimAccountInfo>,
    onSelectSim: (SimAccountInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalApplePalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.cardSurface,
        titleContentColor = palette.label,
        textContentColor = palette.secondaryLabel,
        title = { Text("Choose Calling Line", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryLabel
                )
                Spacer(modifier = Modifier.height(2.dp))
                sims.forEachIndexed { index, sim ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.tertiaryFill)
                            .clickable { onSelectSim(sim) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SimCard,
                            contentDescription = null,
                            tint = palette.accentBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sim.label,
                                color = palette.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "SIM ${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryLabel
                            )
                        }
                        Icon(
                            imageVector = ApplePhoneIcons.PhoneFill,
                            contentDescription = "Call",
                            tint = palette.callGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.accentBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

fun openSmsApp(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}"))
    runCatching { context.startActivity(intent) }
}

fun openAddContact(context: Context, number: String = "") {
    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        if (number.isNotBlank()) {
            putExtra(ContactsContract.Intents.Insert.PHONE, number)
        }
    }
    runCatching { context.startActivity(intent) }
}

fun openSystemContact(context: Context, contactId: Long, lookupKey: String) {
    val uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    runCatching { context.startActivity(intent) }
}

fun copyNumberToClipboard(context: Context, number: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Phone Number", number))
    Toast.makeText(context, "Copied $number", Toast.LENGTH_SHORT).show()
}
