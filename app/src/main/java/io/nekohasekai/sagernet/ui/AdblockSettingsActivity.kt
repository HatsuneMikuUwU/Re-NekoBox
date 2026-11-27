package io.nekohasekai.sagernet.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.background.screenContainerColor
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.PrefSwitchRow
import io.nekohasekai.sagernet.ui.components.StoreEditTextPref
import io.nekohasekai.sagernet.ui.components.StoreSwitchPref
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.rememberComposePreferenceStore
import io.nekohasekai.sagernet.utils.AdblockConfig

class AdblockSettingsActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            val store = rememberComposePreferenceStore(DataStore.configurationStore)
            val enabled = store.getBoolean(Key.ADBLOCK_ENABLED, false)
            var selectedPresets by remember { mutableStateOf(AdblockConfig.selectedPresets()) }

            val backdrop = rememberBlurBackdrop()
            val topInset = floatingTopBarSpace()
            val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val listState = rememberLazyListState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenContainerColor()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blurBackdropSource(backdrop),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = topInset + 6.dp,
                            bottom = navBarInset + 24.dp,
                        ),
                    ) {
                        item {
                            PrefCategory(titleRes = R.string.adblock_settings) {
                                StoreSwitchPref(
                                    store = store,
                                    key = Key.ADBLOCK_ENABLED,
                                    titleRes = R.string.adblock_enable,
                                    summaryRes = R.string.adblock_enable_sum,
                                    icon = Icons.Rounded.Shield,
                                    position = ExpressiveGroupPosition.TOP,
                                )
                                StoreSwitchPref(
                                    store = store,
                                    key = Key.ADBLOCK_DNS_REJECT,
                                    titleRes = R.string.adblock_dns_reject,
                                    summaryRes = R.string.adblock_dns_reject_sum,
                                    defaultValue = true,
                                    enabled = enabled,
                                    icon = Icons.Rounded.Dns,
                                )
                                StoreSwitchPref(
                                    store = store,
                                    key = Key.ADBLOCK_ROUTE_REJECT,
                                    titleRes = R.string.adblock_route_reject,
                                    summaryRes = R.string.adblock_route_reject_sum,
                                    defaultValue = true,
                                    enabled = enabled,
                                    icon = Icons.Rounded.Block,
                                    position = ExpressiveGroupPosition.BOTTOM,
                                )
                            }
                        }

                        item {
                            PrefCategory(titleRes = R.string.adblock_filter_lists) {
                                AdblockConfig.presets.forEachIndexed { idx, preset ->
                                    val pos = when {
                                        AdblockConfig.presets.size == 1 -> ExpressiveGroupPosition.SINGLE
                                        idx == 0 -> ExpressiveGroupPosition.TOP
                                        idx == AdblockConfig.presets.lastIndex -> ExpressiveGroupPosition.BOTTOM
                                        else -> ExpressiveGroupPosition.MIDDLE
                                    }
                                    PrefSwitchRow(
                                        title = stringResource(preset.titleRes),
                                        summary = stringResource(preset.summaryRes),
                                        checked = preset.id in selectedPresets,
                                        enabled = enabled,
                                        icon = Icons.Rounded.FilterList,
                                        position = pos,
                                        onCheckedChange = { checked ->
                                            val next = if (checked) {
                                                selectedPresets + preset.id
                                            } else {
                                                selectedPresets - preset.id
                                            }
                                            selectedPresets = next
                                            DataStore.adblockBundledFilters = next
                                            DataStore.adblockBundledFiltersInitialized = true
                                        },
                                    )
                                }
                            }
                        }

                        item {
                            PrefCategory(titleRes = R.string.adblock_custom_rules_category) {
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ADBLOCK_CUSTOM_FILTER_URLS,
                                    titleRes = R.string.adblock_custom_urls,
                                    dialogMessageRes = R.string.adblock_custom_urls_sum,
                                    icon = Icons.Rounded.Link,
                                    multiline = true,
                                    enabled = enabled,
                                    position = ExpressiveGroupPosition.TOP,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ADBLOCK_CUSTOM_RULES,
                                    titleRes = R.string.adblock_custom_rules,
                                    dialogMessageRes = R.string.adblock_custom_rules_sum,
                                    icon = Icons.Rounded.Rule,
                                    multiline = true,
                                    enabled = enabled,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ADBLOCK_ALLOWLIST,
                                    titleRes = R.string.adblock_allowlist,
                                    dialogMessageRes = R.string.adblock_allowlist_sum,
                                    icon = Icons.Rounded.CheckCircleOutline,
                                    multiline = true,
                                    enabled = enabled,
                                    position = ExpressiveGroupPosition.BOTTOM,
                                )
                            }
                        }
                    }
                }

                FloatingTopBar(
                    backdrop = backdrop,
                    title = stringResource(R.string.adblock_settings),
                    onBack = { finish() },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}
