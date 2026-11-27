@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.nekohasekai.sagernet.ui.components

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import java.util.function.Consumer

private const val DefaultWindowBlurRadius = 10

@Composable
fun WindowBlurEffect(
    blurRadius: Int = DefaultWindowBlurRadius,
    enabled: Boolean = true,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val view = LocalView.current
    val systemAllows = rememberCrossWindowBlurEnabled()
    val radius = if (enabled && systemAllows) blurRadius else 0

    DisposableEffect(view, radius) {
        val apply = Runnable { view.setWindowBlur(radius) }
        if (view.rootView.isAttachedToWindow) apply.run() else view.post(apply)
        onDispose { view.removeCallbacks(apply) }
    }
    DisposableEffect(view) {
        onDispose { view.setWindowBlur(0) }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun rememberCrossWindowBlurEnabled(): Boolean {
    val context = LocalContext.current
    val wm = remember(context) { context.getSystemService(WindowManager::class.java) }
    var enabled by remember { mutableStateOf(wm.isCrossWindowBlurEnabled) }
    DisposableEffect(wm) {
        val listener = Consumer<Boolean> { enabled = it }
        wm.addCrossWindowBlurEnabledListener(listener)
        onDispose { wm.removeCrossWindowBlurEnabledListener(listener) }
    }
    return enabled
}

@RequiresApi(Build.VERSION_CODES.S)
private fun View.setWindowBlur(radius: Int) {
    val dialogWindow = (this as? DialogWindowProvider)?.window ?: (parent as? DialogWindowProvider)?.window
    if (dialogWindow != null) {
        dialogWindow.setBlur(radius)
        return
    }
    val root = rootView
    val lp = root.layoutParams as? WindowManager.LayoutParams ?: return
    if (!root.isAttachedToWindow) return
    lp.applyBlur(radius)
    runCatching { context.getSystemService(WindowManager::class.java).updateViewLayout(root, lp) }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Window.setBlur(radius: Int) {
    val lp = attributes
    lp.applyBlur(radius)
    attributes = lp
}

@RequiresApi(Build.VERSION_CODES.S)
private fun WindowManager.LayoutParams.applyBlur(radius: Int) {
    if (radius > 0) {
        flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        blurBehindRadius = radius.coerceIn(0, 150)
    } else {
        flags = flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        blurBehindRadius = 0
    }
}

@Composable
fun AppDialog(
    icon: ImageVector? = null,
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier
        .padding(horizontal = 24.dp)
        .widthIn(max = 560.dp),
    dismissButton: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = {
            WindowBlurEffect()
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = text,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
fun DialogOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun DialogCheckRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = label,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun DialogCancelButton(
    label: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    FilledTonalButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(label)
    }
}

@Composable
fun DialogConfirmButton(
    label: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    FilledTonalButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(label)
    }
}

@Composable
fun DialogDestructiveButton(
    label: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    FilledTonalButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Text(label)
    }
}

private val SheetItemOuter = 19.dp
private val SheetItemInner = 4.dp

fun groupedSheetItemShape(
    index: Int,
    count: Int,
): Shape = RoundedCornerShape(
    topStart = if (index == 0) SheetItemOuter else SheetItemInner,
    topEnd = if (index == 0) SheetItemOuter else SheetItemInner,
    bottomStart = if (index == count - 1) SheetItemOuter else SheetItemInner,
    bottomEnd = if (index == count - 1) SheetItemOuter else SheetItemInner,
)

@Composable
fun SheetItemSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val outer = modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
    if (onClick != null) {
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            enabled = enabled,
            shape = shape,
            color = container,
            modifier = outer,
            content = content,
        )
    } else {
        Surface(
            shape = shape,
            color = container,
            modifier = outer,
            content = content,
        )
    }
}
