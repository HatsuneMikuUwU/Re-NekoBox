package io.nekohasekai.sagernet.fmt.tailscale

import io.nekohasekai.sagernet.ktx.blankAsNull
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma

fun buildSingBoxOutboundTailcatBean(bean: TailcatBean): SingBoxOptions.SingBoxOption {
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "tailcat"
        bean.privateKey.blankAsNull()?.let { _hack_config_map["private_key"] = it }
        bean.serverPublicKey.blankAsNull()?.let { _hack_config_map["server_public_key"] = it }
        bean.serverDiscoKey.blankAsNull()?.let { _hack_config_map["server_disco_key"] = it }
        bean.preSharedKey.blankAsNull()?.let { _hack_config_map["pre_shared_key"] = it }
        val servers = bean.derpServers.blankAsNull()?.listByLineOrComma()
        if (servers.isNullOrEmpty()) {
            bean.derpMapURL.blankAsNull()?.let { _hack_config_map["derp_map_url"] = it }
            if ((bean.derpRegion ?: 0) > 0) _hack_config_map["derp_region"] = bean.derpRegion
        } else {
            _hack_config_map["derp_servers"] = servers.map { mapOf("host" to it) }
        }
    }
}
