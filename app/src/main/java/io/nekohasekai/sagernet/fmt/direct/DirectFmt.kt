package io.nekohasekai.sagernet.fmt.direct

import moe.matsuri.nb4a.SingBoxOptions

fun buildSingBoxOutboundDirectBean(bean: DirectBean): SingBoxOptions.SingBoxOption {
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "direct"
        _hack_config_map["reuse_addr"] = true
    }
}
