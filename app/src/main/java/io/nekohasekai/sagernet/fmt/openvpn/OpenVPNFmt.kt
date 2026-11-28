package io.nekohasekai.sagernet.fmt.openvpn

import io.nekohasekai.sagernet.ktx.blankAsNull
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma

fun buildSingBoxEndpointOpenVPNBean(bean: OpenVPNBean): SingBoxOptions.SingBoxOption {
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "openvpn-client"
        _hack_config_map["server"] = bean.serverAddress
        _hack_config_map["server_port"] = bean.serverPort
        bean.network.blankAsNull()?.let { _hack_config_map["network"] = it }
        bean.username.blankAsNull()?.let { _hack_config_map["username"] = it }
        bean.password.blankAsNull()?.let { _hack_config_map["password"] = it }
        bean.cipher.blankAsNull()?.let { _hack_config_map["cipher"] = it }
        bean.dataCiphers.blankAsNull()?.listByLineOrComma()?.takeIf { it.isNotEmpty() }?.let {
            _hack_config_map["data_ciphers"] = it
        }
        bean.auth.blankAsNull()?.let { _hack_config_map["auth"] = it }
        bean.compression.blankAsNull()?.let { _hack_config_map["compression"] = it }
        if (bean.redirectGateway == true) _hack_config_map["redirect_gateway"] = true
        if ((bean.mtu ?: 0) > 0) _hack_config_map["mtu"] = bean.mtu

        val tlsMap = mutableMapOf<String, Any>()
        bean.serverName.blankAsNull()?.let { tlsMap["server_name"] = it }
        bean.certificate.blankAsNull()?.let { tlsMap["certificate"] = listOf(it) }
        bean.clientCertificate.blankAsNull()?.let { tlsMap["client_certificate"] = listOf(it) }
        bean.clientKey.blankAsNull()?.let { tlsMap["client_key"] = listOf(it) }
        bean.peerFingerprint.blankAsNull()?.listByLineOrComma()?.takeIf { it.isNotEmpty() }?.let {
            tlsMap["peer_fingerprint"] = it
        }
        if (!bean.controlWrapType.isNullOrBlank() || !bean.controlWrapKey.isNullOrBlank()) {
            val cw = mutableMapOf<String, Any>()
            bean.controlWrapType.blankAsNull()?.let { cw["type"] = it }
            bean.controlWrapKey.blankAsNull()?.let { cw["key"] = listOf(it) }
            bean.controlWrapDirection.blankAsNull()?.let { cw["direction"] = it }
            if (cw.isNotEmpty()) tlsMap["control_wrap"] = cw
        }
        if (tlsMap.isNotEmpty()) {
            _hack_config_map["tls"] = tlsMap
        }
    }
}
