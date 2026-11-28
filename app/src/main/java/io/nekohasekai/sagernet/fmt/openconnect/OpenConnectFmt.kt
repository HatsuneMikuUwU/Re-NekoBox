package io.nekohasekai.sagernet.fmt.openconnect

import io.nekohasekai.sagernet.ktx.blankAsNull
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma

fun buildSingBoxEndpointOpenConnectBean(bean: OpenConnectBean): SingBoxOptions.SingBoxOption {
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "openconnect"
        val srv = bean.server.blankAsNull() ?: bean.serverAddress
        _hack_config_map["server"] = srv
        bean.flavor.blankAsNull()?.let { _hack_config_map["flavor"] = it }
        bean.username.blankAsNull()?.let { _hack_config_map["username"] = it }
        bean.password.blankAsNull()?.let { _hack_config_map["password"] = it }
        bean.authGroup.blankAsNull()?.let { _hack_config_map["auth_group"] = it }
        bean.reportedOS.blankAsNull()?.let { _hack_config_map["reported_os"] = it }
        bean.userAgent.blankAsNull()?.let { _hack_config_map["user_agent"] = it }
        if (bean.allowInsecureCrypto == true) _hack_config_map["allow_insecure_crypto"] = true

        val tlsMap = mutableMapOf<String, Any>()
        if (bean.tlsInsecure == true) tlsMap["insecure"] = true
        bean.tlsServerName.blankAsNull()?.let { tlsMap["server_name"] = it }
        bean.tlsPeerFingerprint.blankAsNull()?.listByLineOrComma()?.takeIf { it.isNotEmpty() }?.let {
            tlsMap["peer_fingerprint"] = it
        }
        bean.certificateAuthority.blankAsNull()?.listByLineOrComma()?.takeIf { it.isNotEmpty() }?.let {
            tlsMap["certificate_authority"] = it
        }
        bean.clientCertificate.blankAsNull()?.let { tlsMap["client_certificate"] = listOf(it) }
        bean.clientKey.blankAsNull()?.let { tlsMap["client_key"] = listOf(it) }
        bean.clientKeyPassword.blankAsNull()?.let { tlsMap["client_key_password"] = it }
        if (tlsMap.isNotEmpty()) {
            _hack_config_map["tls"] = tlsMap
        }
    }
}
