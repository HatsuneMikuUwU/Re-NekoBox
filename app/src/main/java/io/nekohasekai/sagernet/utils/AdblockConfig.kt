package io.nekohasekai.sagernet.utils

import androidx.annotation.StringRes
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.TAG_PROXY
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.checkEmpty
import moe.matsuri.nb4a.makeSingBoxRule
import moe.matsuri.nb4a.utils.listByLineOrComma

data class AdblockFilterPreset(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    val tag: String,
    val isLocal: Boolean,
    val localPath: String = "",
    val remoteUrl: String = "",
    val defaultEnabled: Boolean = false,
)

object AdblockConfig {
    val presets = listOf(
        AdblockFilterPreset(
            id = "geosite_ads",
            titleRes = R.string.adblock_preset_geosite_ads,
            summaryRes = R.string.adblock_preset_geosite_ads_sum,
            tag = "geosite:category-ads-all",
            isLocal = true,
            localPath = "geosite:category-ads-all",
            defaultEnabled = true,
        ),
        AdblockFilterPreset(
            id = "adguard_dns",
            titleRes = R.string.adblock_preset_adguard,
            summaryRes = R.string.adblock_preset_adguard_sum,
            tag = "adblock-adguard",
            isLocal = false,
            remoteUrl = "https://raw.githubusercontent.com/SagerNet/sing-geosite/rule-set/geosite-category-ads-all.srs",
            defaultEnabled = false,
        ),
        AdblockFilterPreset(
            id = "oisd_small",
            titleRes = R.string.adblock_preset_oisd,
            summaryRes = R.string.adblock_preset_oisd_sum,
            tag = "adblock-oisd-small",
            isLocal = false,
            remoteUrl = "https://raw.githubusercontent.com/lyc8503/sing-box-rules/rule-set-geosite/geosite-oisd-small.srs",
            defaultEnabled = false,
        ),
        AdblockFilterPreset(
            id = "telemetry_spyware",
            titleRes = R.string.adblock_preset_telemetry,
            summaryRes = R.string.adblock_preset_telemetry_sum,
            tag = "adblock-telemetry",
            isLocal = false,
            remoteUrl = "https://raw.githubusercontent.com/SagerNet/sing-geosite/rule-set/geosite-win-spy.srs",
            defaultEnabled = false,
        ),
    )

    fun selectedPresets(): Set<String> {
        val raw = DataStore.adblockBundledFilters
        return if (raw.isEmpty() && !DataStore.adblockBundledFiltersInitialized) {
            val defaults = presets.filter { it.defaultEnabled }.map { it.id }.toSet()
            DataStore.adblockBundledFilters = defaults
            DataStore.adblockBundledFiltersInitialized = true
            defaults
        } else {
            raw
        }
    }

    fun applyToConfig(
        dns: SingBoxOptions.DNSOptions,
        route: SingBoxOptions.RouteOptions,
    ) {
        if (!DataStore.adblockEnabled) return
        val dnsReject = DataStore.adblockDnsReject
        val routeReject = DataStore.adblockRouteReject
        if (!dnsReject && !routeReject) return

        val selectedIds = selectedPresets()
        val activeRuleSetTags = mutableListOf<String>()
        val extraRuleSets = mutableListOf<SingBoxOptions.RuleSet>()

        presets.filter { it.id in selectedIds }.forEach { preset ->
            activeRuleSetTags.add(preset.tag)
            extraRuleSets.add(
                SingBoxOptions.RuleSet().apply {
                    tag = preset.tag
                    format = "binary"
                    if (preset.isLocal) {
                        type = "local"
                        path = preset.localPath
                    } else {
                        type = "remote"
                        url = preset.remoteUrl
                        update_interval = "1d"
                        _hack_config_map["download_detour"] = TAG_PROXY
                    }
                },
            )
        }

        val customUrls = DataStore.adblockCustomFilterUrls
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
            .toList()

        customUrls.forEachIndexed { idx, remoteUrl ->
            val tagName = "adblock-custom-${idx + 1}"
            activeRuleSetTags.add(tagName)
            extraRuleSets.add(
                SingBoxOptions.RuleSet().apply {
                    type = "remote"
                    tag = tagName
                    format = if (remoteUrl.endsWith(".json", ignoreCase = true)) "source" else "binary"
                    url = remoteUrl
                    update_interval = "1d"
                    _hack_config_map["download_detour"] = TAG_PROXY
                },
            )
        }

        val customBlockDomains = DataStore.adblockCustomRules.listByLineOrComma()
        val allowDomains = DataStore.adblockAllowlist.listByLineOrComma()

        if (activeRuleSetTags.isEmpty() && customBlockDomains.isEmpty()) return

        if (extraRuleSets.isNotEmpty()) {
            val merged = (route.rule_set.orEmpty() + extraRuleSets).distinctBy { it.tag }
            route.rule_set = merged.toMutableList()
        }

        if (dnsReject) {
            val blockDnsRule = SingBoxOptions.DNSRule_DefaultOptions().apply {
                if (customBlockDomains.isNotEmpty()) {
                    makeSingBoxRule(customBlockDomains)
                }
                if (activeRuleSetTags.isNotEmpty()) {
                    rule_set = (rule_set.orEmpty() + activeRuleSetTags).distinct()
                }
                server = "dns-block"
            }
            if (!blockDnsRule.checkEmpty()) {
                dns.rules.add(0, blockDnsRule)
            }
            if (allowDomains.isNotEmpty()) {
                val allowDnsRule = SingBoxOptions.DNSRule_DefaultOptions().apply {
                    makeSingBoxRule(allowDomains)
                    server = "dns-remote"
                }
                if (!allowDnsRule.checkEmpty()) {
                    dns.rules.add(0, allowDnsRule)
                }
            }
        }

        if (routeReject) {
            val blockRouteRule = SingBoxOptions.Rule_DefaultOptions().apply {
                if (customBlockDomains.isNotEmpty()) {
                    makeSingBoxRule(customBlockDomains, false)
                }
                if (activeRuleSetTags.isNotEmpty()) {
                    rule_set = (rule_set.orEmpty() + activeRuleSetTags).distinct()
                }
                action = "reject"
            }
            if (!blockRouteRule.checkEmpty()) {
                val insertAt = route.rules.indexOfLast { (it as? SingBoxOptions.Rule_DefaultOptions)?.action == "hijack-dns" } + 1
                route.rules.add(insertAt.coerceIn(0, route.rules.size), blockRouteRule)
            }
        }
    }
}
