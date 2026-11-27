@file:Suppress("DEPRECATION")

package io.nekohasekai.sagernet.ui.components

import androidx.compose.material3.SwitchDefaults

import androidx.compose.material3.Switch

import androidx.compose.material.icons.outlined.Close

import androidx.compose.material.icons.outlined.Check

import androidx.compose.material.icons.rounded.QrCode2
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.nio.charset.StandardCharsets

/**
 * Material 3 Expressive segmented card corner shapes for grouped lists.
 * Outer corners are generously rounded (24.dp) while inner corners between
 * stacked sibling items use crisp 6.dp radii with a 2.dp gap.
 */
enum class ExpressiveGroupPosition {
    SINGLE, TOP, MIDDLE, BOTTOM
}

fun expressiveGroupShape(position: ExpressiveGroupPosition): Shape = when (position) {
    ExpressiveGroupPosition.SINGLE -> RoundedCornerShape(24.dp)
    ExpressiveGroupPosition.TOP -> RoundedCornerShape(
        topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp
    )
    ExpressiveGroupPosition.MIDDLE -> RoundedCornerShape(6.dp)
    ExpressiveGroupPosition.BOTTOM -> RoundedCornerShape(
        topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp
    )
}

@Composable
fun ExpressiveSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        trailing?.invoke()
    }
}

@Composable
fun ExpressiveGroupedContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
fun ExpressiveSurfaceItem(
    position: ExpressiveGroupPosition = ExpressiveGroupPosition.SINGLE,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    clickHapticType: HapticFeedbackType = HapticFeedbackType.ContextClick,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = expressiveGroupShape(position)
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "expressiveCardColor",
    )
    val performClickHaptic = rememberHapticAction(clickHapticType)
    val performLongHaptic = rememberHapticAction(HapticFeedbackType.LongPress)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = {
                            if (onClick != null) {
                                performClickHaptic()
                                onClick()
                            }
                        },
                        onLongClick = onLongClick?.let { longAction ->
                            {
                                performLongHaptic()
                                longAction()
                            }
                        },
                        hapticFeedbackEnabled = false,
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = containerColor,
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        content()
    }
}

@Composable
fun ExpressiveIconBadge(
    icon: ImageVector,
    contentDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(MaterialShapes.Clover4Leaf.toShape())
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun ExpressivePillBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun ExpressiveSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = hapticClick { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                    )
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun ExpressiveEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "empty-state")
    val spin by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "spin",
    )
    val breathe by t.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val bob by t.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val bobPx = with(LocalDensity.current) { 4.dp.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(width = 200.dp, height = 190.dp)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(150.dp)
                    .graphicsLayer {
                        rotationZ = spin
                        scaleX = breathe
                        scaleY = breathe
                    }
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(cs.primaryContainer),
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(cs.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = cs.onPrimary,
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(54.dp)
                    .graphicsLayer {
                        rotationZ = -spin * 0.8f
                        translationY = bob * bobPx
                    }
                    .clip(MaterialShapes.Clover4Leaf.toShape())
                    .background(cs.primary),
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .size(36.dp)
                    .graphicsLayer {
                        rotationZ = spin * 1.2f
                        translationY = -bob * bobPx
                    }
                    .clip(MaterialShapes.Cookie6Sided.toShape())
                    .background(cs.secondary),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp)
                    .size(22.dp)
                    .graphicsLayer {
                        rotationZ = -spin * 1.6f
                        translationY = bob * bobPx
                    }
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(cs.tertiary),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                modifier = Modifier.widthIn(max = 280.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = hapticClick(block = onAction),
                shape = CircleShape,
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun QrCodeDialog(
    title: String,
    content: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val qrBitmap = remember(content) {
        generateQrBitmap(content, 640)
    }

    AppDialog(
        icon = Icons.Rounded.QrCode2,
        title = title,
        onDismiss = onDismiss,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = title,
                            modifier = Modifier
                                .size(240.dp)
                                .padding(12.dp),
                        )
                    }
                } else {
                    Text(
                        text = "Unable to generate QR code (content too large).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            DialogConfirmButton(label = "Copy", onClick = onCopy)
        },
        dismissButton = {
            DialogCancelButton(label = "Close", onClick = onDismiss)
        },
    )
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name(),
            EncodeHintType.MARGIN to 1,
        )
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}


@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Outlined.Check else Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}
