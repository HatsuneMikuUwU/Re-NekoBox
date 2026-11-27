@file:Suppress("DEPRECATION")

package io.nekohasekai.sagernet.ui.components

import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Tune
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.database.preference.PreferenceDataStore
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.preference.OnPreferenceDataStoreChangeListener
import io.nekohasekai.sagernet.database.preference.RoomPreferenceDataStore

/**
 * Reactive state bridge between RoomPreferenceDataStore and Jetpack Compose.
 * Automatically recomposes dependent preference rows whenever any key changes.
 */
@Stable
class ComposePreferenceStore(
    val store: RoomPreferenceDataStore,
    private val markProfileDirty: Boolean = false,
) : OnPreferenceDataStoreChangeListener {

    var revision by mutableIntStateOf(0)
        private set

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        revision++
        if (markProfileDirty && key != Key.PROFILE_DIRTY) {
            DataStore.dirty = true
        }
    }

    fun getString(key: String, default: String = ""): String {
        revision // register read for recomposition
        return store.getString(key, default) ?: default
    }

    fun putString(key: String, value: String) {
        store.putString(key, value)
        revision++
        if (markProfileDirty && key != Key.PROFILE_DIRTY) {
            DataStore.dirty = true
        }
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        revision
        return store.getBoolean(key, default)
    }

    fun putBoolean(key: String, value: Boolean) {
        store.putBoolean(key, value)
        revision++
        if (markProfileDirty && key != Key.PROFILE_DIRTY) {
            DataStore.dirty = true
        }
    }

    fun getInt(key: String, default: Int = 0): Int {
        revision
        return store.getInt(key, default)
    }

    fun putInt(key: String, value: Int) {
        store.putInt(key, value)
        revision++
        if (markProfileDirty && key != Key.PROFILE_DIRTY) {
            DataStore.dirty = true
        }
    }

    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> {
        revision
        return store.getStringSet(key, default) ?: default
    }

    fun putStringSet(key: String, value: Set<String>) {
        store.putStringSet(key, value)
        revision++
        if (markProfileDirty && key != Key.PROFILE_DIRTY) {
            DataStore.dirty = true
        }
    }
}

@Composable
fun rememberComposePreferenceStore(
    store: RoomPreferenceDataStore,
    markProfileDirty: Boolean = false,
): ComposePreferenceStore {
    val state = remember(store, markProfileDirty) {
        ComposePreferenceStore(store, markProfileDirty)
    }
    DisposableEffect(store, state) {
        store.registerChangeListener(state)
        onDispose {
            store.unregisterChangeListener(state)
        }
    }
    return state
}

@Composable
fun PrefCategory(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ExpressiveSectionHeader(title = title)
        ExpressiveGroupedContainer {
            content()
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun PrefCategory(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = PrefCategory(title = stringResource(titleRes), modifier = modifier, content = content)

@Composable
fun PrefSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    position: ExpressiveGroupPosition = ExpressiveGroupPosition.MIDDLE,
) {
    ExpressiveSurfaceItem(
        position = position,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.45f)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                ExpressiveIconBadge(icon = icon)
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!summary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
            )
        }
    }
}

@Composable
fun StoreSwitchPref(
    store: ComposePreferenceStore,
    key: String,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int? = null,
    defaultValue: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    position: ExpressiveGroupPosition = ExpressiveGroupPosition.MIDDLE,
    onChanged: ((Boolean) -> Boolean)? = null,
) {
    val checked = store.getBoolean(key, defaultValue)
    PrefSwitchRow(
        title = stringResource(titleRes),
        summary = summaryRes?.let { stringResource(it) },
        checked = checked,
        icon = icon,
        enabled = enabled,
        position = position,
        onCheckedChange = { newVal ->
            val allow = onChanged?.invoke(newVal) ?: true
            if (allow) {
                store.putBoolean(key, newVal)
            }
        },
    )
}

@Composable
fun PrefActionRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    position: ExpressiveGroupPosition = ExpressiveGroupPosition.MIDDLE,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    ExpressiveSurfaceItem(
        position = position,
        onClick = if (enabled) onClick else null,
        onLongClick = if (enabled) onLongClick else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.45f)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                ExpressiveIconBadge(icon = icon)
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!summary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(10.dp))
                trailing()
            } else if (showChevron) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun StoreEditTextPref(
    store: ComposePreferenceStore,
    key: String,
    @StringRes titleRes: Int,
    defaultValue: String = "",
    @StringRes dialogMessageRes: Int? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    isNumber: Boolean = false,
    multiline: Boolean = false,
    position: ExpressiveGroupPosition = ExpressiveGroupPosition.MIDDLE,
    onChanged: ((String) -> Unit)? = null,
) {
    val title = stringResource(titleRes)
    val dialogMessage = dialogMessageRes?.let { stringResource(it) }
    val currentValue = store.getString(key, defaultValue)
    var showDialog by remember { mutableStateOf(false) }

    val displaySummary = when {
        currentValue.isBlank() -> stringResource(R.string.not_set)
        isPassword -> "\u2022".repeat(currentValue.length.coerceAtMost(16))
        else -> currentValue.lineSequence().firstOrNull { it.isNotBlank() } ?: currentValue
    }

    PrefActionRow(
        title = title,
        summary = displaySummary,
        icon = icon,
        enabled = enabled,
        position = position,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        var draft by remember(currentValue) { mutableStateOf(currentValue) }
        var passwordVisible by remember { mutableStateOf(false) }

        AppDialog(
            icon = icon ?: Icons.Rounded.Edit,
            title = title,
            onDismiss = { showDialog = false },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!dialogMessage.isNullOrBlank()) {
                        Text(
                            text = dialogMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = !multiline,
                        minLines = if (multiline) 4 else 1,
                        maxLines = if (multiline) 8 else 1,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = when {
                                isNumber -> KeyboardType.Number
                                isPassword -> KeyboardType.Password
                                else -> KeyboardType.Text
                            }
                        ),
                        visualTransformation = if (isPassword && !passwordVisible) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        trailingIcon = if (isPassword) {
                            {
                                IconButton(onClick = hapticClick { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null,
                                    )
                                }
                            }
                        } else null,
                    )
                }
            },
            confirmButton = {
                DialogConfirmButton(label = stringResource(android.R.string.ok)) {
                    store.putString(key, draft)
                    onChanged?.invoke(draft)
                    showDialog = false
                }
            },
            dismissButton = {
                DialogCancelButton(label = stringResource(android.R.string.cancel)) {
                    showDialog = false
                }
            },
        )
    }
}

@Composable
fun StoreListPref(
    store: ComposePreferenceStore,
    key: String,
    @StringRes titleRes: Int,
    @ArrayRes entriesRes: Int,
    @ArrayRes entryValuesRes: Int = entriesRes,
    defaultValue: String = "",
    icon: ImageVector? = null,
    enabled: Boolean = true,
    position: ExpressiveGroupPosition = ExpressiveGroupPosition.MIDDLE,
    onLongClick: (() -> Unit)? = null,
    onChanged: ((String) -> Unit)? = null,
) {
    val entries = stringArrayResource(entriesRes)
    val values = stringArrayResource(entryValuesRes)
    StoreListPrefRaw(
        store = store,
        key = key,
        title = stringResource(titleRes),
        entries = entries.toList(),
        entryValues = values.toList(),
        defaultValue = defaultValue,
        icon = icon,
        enabled = enabled,
        position = position,
        onLongClick = onLongClick,
        onChanged = onChanged,
    )
}

@Composable
fun StoreListPrefRaw(
    store: ComposePreferenceStore,
    key: String,
    title: String,
    entries: List<String>,
    entryValues: List<String> = entries,
    defaultValue: String = "",
    icon: ImageVector? = null,
    enabled: Boolean = true,
    position: ExpressiveGroupPosition = ExpressiveGroupPosition.MIDDLE,
    onLongClick: (() -> Unit)? = null,
    onChanged: ((String) -> Unit)? = null,
) {
    val currentValue = store.getString(key, defaultValue)
    val selectedIndex = entryValues.indexOf(currentValue)
    val summary = if (selectedIndex in entries.indices) entries[selectedIndex] else currentValue.ifBlank { stringResource(R.string.not_set) }
    var showDialog by remember { mutableStateOf(false) }

    PrefActionRow(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        position = position,
        onLongClick = onLongClick,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        AppDialog(
            icon = icon ?: Icons.Rounded.Tune,
            title = title,
            onDismiss = { showDialog = false },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(entries) { idx, label ->
                        val value = entryValues.getOrElse(idx) { label }
                        DialogOptionRow(
                            label = label,
                            selected = value == currentValue,
                            onClick = {
                                store.putString(key, value)
                                onChanged?.invoke(value)
                                showDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                DialogCancelButton(label = stringResource(android.R.string.cancel)) {
                    showDialog = false
                }
            },
        )
    }
}

@Composable
fun StoreMultiSelectPref(
    store: ComposePreferenceStore,
    key: String,
    @StringRes titleRes: Int,
    @ArrayRes entriesRes: Int,
    @ArrayRes entryValuesRes: Int = entriesRes,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    position: ExpressiveGroupPosition = ExpressiveGroupPosition.MIDDLE,
    onChanged: ((Set<String>) -> Unit)? = null,
) {
    val title = stringResource(titleRes)
    val entries = stringArrayResource(entriesRes)
    val values = stringArrayResource(entryValuesRes)
    val selected = store.getStringSet(key)
    var showDialog by remember { mutableStateOf(false) }

    val summary = if (selected.isEmpty()) {
        stringResource(R.string.not_set)
    } else {
        selected.joinToString(", ") { v ->
            val idx = values.indexOf(v)
            if (idx in entries.indices) entries[idx] else v
        }
    }

    PrefActionRow(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        position = position,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        var draft by remember(selected) { mutableStateOf(selected) }
        AppDialog(
            icon = icon ?: Icons.Rounded.Checklist,
            title = title,
            onDismiss = { showDialog = false },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(entries.toList()) { idx, label ->
                        val v = values.getOrElse(idx) { label }
                        val checked = v in draft
                        DialogCheckRow(
                            label = label,
                            checked = checked,
                            onClick = { draft = if (checked) draft - v else draft + v },
                        )
                    }
                }
            },
            confirmButton = {
                DialogConfirmButton(label = stringResource(android.R.string.ok)) {
                    store.putStringSet(key, draft)
                    onChanged?.invoke(draft)
                    showDialog = false
                }
            },
            dismissButton = {
                DialogCancelButton(label = stringResource(android.R.string.cancel)) {
                    showDialog = false
                }
            },
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MtuPickerDialog(
    currentMtu: String,
    presets: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(currentMtu) { mutableStateOf(currentMtu) }
    AppDialog(
        icon = Icons.Rounded.Public,
        title = stringResource(R.string.mtu),
        onDismiss = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = draft == preset,
                            onClick = { draft = preset },
                            label = { Text(preset) },
                            shape = CircleShape,
                        )
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.mtu)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            DialogConfirmButton(label = stringResource(android.R.string.ok)) {
                if (draft.isNotBlank()) onConfirm(draft)
                onDismiss()
            }
        },
        dismissButton = {
            DialogCancelButton(label = stringResource(android.R.string.cancel), onClick = onDismiss)
        },
    )
}
