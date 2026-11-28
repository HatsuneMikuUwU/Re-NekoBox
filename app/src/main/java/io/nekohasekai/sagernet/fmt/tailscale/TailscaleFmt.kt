package io.nekohasekai.sagernet.fmt.tailscale

import io.nekohasekai.sagernet.ktx.blankAsNull
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma

fun buildSingBoxEndpointTailscaleBean(bean: TailscaleBean): SingBoxOptions.SingBoxOption {
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "tailscale"
        bean.stateDirectory.blankAsNull()?.let { _hack_config_map["state_directory"] = it }
        bean.authKey.blankAsNull()?.let { _hack_config_map["auth_key"] = it }
        bean.controlURL.blankAsNull()?.let { _hack_config_map["control_url"] = it }
        if (bean.ephemeral == true) _hack_config_map["ephemeral"] = true
        bean.hostname.blankAsNull()?.let { _hack_config_map["hostname"] = it }
        if (bean.acceptRoutes == true) _hack_config_map["accept_routes"] = true
        bean.exitNode.blankAsNull()?.let { _hack_config_map["exit_node"] = it }
        if (bean.exitNodeAllowLANAccess == true) _hack_config_map["exit_node_allow_lan_access"] = true
        bean.advertiseRoutes.blankAsNull()?.listByLineOrComma()?.takeIf { it.isNotEmpty() }?.let {
            _hack_config_map["advertise_routes"] = it
        }
        if (bean.advertiseExitNode == true) _hack_config_map["advertise_exit_node"] = true
        bean.advertiseTags.blankAsNull()?.listByLineOrComma()?.takeIf { it.isNotEmpty() }?.let {
            _hack_config_map["advertise_tags"] = it
        }
        if ((bean.listenPort ?: 0) > 0) _hack_config_map["listen_port"] = bean.listenPort
    }
}
