package io.nekohasekai.sagernet.ui.profile

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import io.nekohasekai.sagernet.ui.components.LocalEditorContentPadding
import io.nekohasekai.sagernet.ui.components.LocalEditorListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.fmt.internal.ChainBean
import io.nekohasekai.sagernet.ui.ProfileSelectActivity
import io.nekohasekai.sagernet.ui.components.ComposePreferenceStore
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressivePillBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSectionHeader
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.StoreEditTextPref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChainSettingsActivity : ProfileSettingsActivity<ChainBean>() {

    override fun createEntity() = ChainBean()

    private var chainIds: List<Long> = emptyList()

    override fun ChainBean.init() {
        DataStore.profileName = name
        DataStore.serverProtocol = proxies.joinToString(",")
        chainIds = proxies.toList()
    }

    override fun ChainBean.serialize() {
        name = DataStore.profileName
        proxies = DataStore.serverProtocol.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
    }

    @Composable
    override fun ProfileSettingsContent(store: ComposePreferenceStore) {
        val context = LocalContext.current
        var proxyIds by remember {
            mutableStateOf(
                store.getString(Key.SERVER_PROTOCOL)
                    .split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
            )
        }
        var entities by remember { mutableStateOf<List<ProxyEntity>>(emptyList()) }

        LaunchedEffect(proxyIds) {
            entities = withContext(Dispatchers.IO) {
                val map = ProfileManager.getProfiles(proxyIds).associateBy { it.id }
                proxyIds.mapNotNull { map[it] }
            }
        }

        fun updateIds(newIds: List<Long>) {
            proxyIds = newIds
            store.putString(Key.SERVER_PROTOCOL, newIds.joinToString(","))
        }

        val selectLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val id = result.data?.getLongExtra(ProfileSelectActivity.EXTRA_PROFILE_ID, 0L) ?: 0L
                if (id != 0L && id != DataStore.editingId) {
                    updateIds(proxyIds + id)
                }
            }
        }

        val listState = LocalEditorListState.current ?: rememberLazyListState()
        val contentPadding = LocalEditorContentPadding.current ?: PaddingValues(vertical = 8.dp)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item {
                PrefCategory(titleRes = R.string.profile_name) {
                    StoreEditTextPref(
                        store = store,
                        key = Key.PROFILE_NAME,
                        titleRes = R.string.profile_name,
                        icon = Icons.Rounded.Title,
                        position = ExpressiveGroupPosition.SINGLE,
                    )
                }
            }

            item {
                ExpressiveSectionHeader(title = stringResource(R.string.proxy_chain))
            }

            itemsIndexed(entities, key = { idx, ent -> "${ent.id}_$idx" }) { idx, entity ->
                val pos = when {
                    entities.size == 1 -> ExpressiveGroupPosition.SINGLE
                    idx == 0 -> ExpressiveGroupPosition.TOP
                    idx == entities.lastIndex -> ExpressiveGroupPosition.BOTTOM
                    else -> ExpressiveGroupPosition.MIDDLE
                }
                ExpressiveSurfaceItem(
                    position = pos,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${idx + 1}. ${entity.displayName()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            ExpressivePillBadge(text = entity.displayType())
                        }
                        if (idx > 0) {
                            IconButton(onClick = {
                                val mutable = proxyIds.toMutableList()
                                val tmp = mutable[idx - 1]
                                mutable[idx - 1] = mutable[idx]
                                mutable[idx] = tmp
                                updateIds(mutable)
                            }) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                            }
                        }
                        if (idx < proxyIds.lastIndex) {
                            IconButton(onClick = {
                                val mutable = proxyIds.toMutableList()
                                val tmp = mutable[idx + 1]
                                mutable[idx + 1] = mutable[idx]
                                mutable[idx] = tmp
                                updateIds(mutable)
                            }) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                            }
                        }
                        IconButton(onClick = {
                            val mutable = proxyIds.toMutableList()
                            if (idx in mutable.indices) {
                                mutable.removeAt(idx)
                                updateIds(mutable)
                            }
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        selectLauncher.launch(Intent(context, ProfileSelectActivity::class.java))
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("  " + stringResource(R.string.add_profile))
                }
            }
        }
    }
}