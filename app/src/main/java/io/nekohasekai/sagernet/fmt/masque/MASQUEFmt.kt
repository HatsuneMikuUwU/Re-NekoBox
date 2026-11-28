package io.nekohasekai.sagernet.fmt.masque

import io.nekohasekai.sagernet.ktx.blankAsNull
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma

fun buildSingBoxEndpointMASQUEBean(bean: MASQUEBean): SingBoxOptions.SingBoxOption {
    return SingBoxOptions.SingBoxOption().apply {
        _hack_config_map["type"] = "masque-client"
        _hack_config_map["server"] = bean.serverAddress
        _hack_config_map["server_port"] = bean.serverPort
        bean.username.blankAsNull()?.let { _hack_config_map["username"] = it }
        bean.password.blankAsNull()?.let { _hack_config_map["password"] = it }
        bean.path.blankAsNull()?.let { _hack_config_map["path"] = it }
        if ((bean.httpVersion ?: 0) in 1..3) _hack_config_map["version"] = bean.httpVersion
        if (bean.disableVersionFallback == true) _hack_config_map["disable_version_fallback"] = true
        if ((bean.mtu ?: 0) > 0) _hack_config_map["mtu"] = bean.mtu
        if (bean.enableTLS != false) {
            val tlsMap = mutableMapOf<String, Any>("enabled" to true)
            bean.sni.blankAsNull()?.let { tlsMap["server_name"] = it }
            if (bean.allowInsecure == true) tlsMap["insecure"] = true
            if (bean.disableSNI == true) tlsMap["disable_sni"] = true
            bean.alpn.blankAsNull()?.listByLineOrComma()?.takeIf { it.isNotEmpty() }?.let {
                tlsMap["alpn"] = it
            }
            bean.certificates.blankAsNull()?.let { tlsMap["certificate"] = it }
            bean.utlsFingerprint.blankAsNull()?.let {
                tlsMap["utls"] = mapOf("enabled" to true, "fingerprint" to it)
            }
            bean.echConfig.blankAsNull()?.let {
                val cfg = if (it.contains("BEGIN ECH CONFIGS")) listOf(it)
                else listOf("-----BEGIN ECH CONFIGS-----", it.trim(), "-----END ECH CONFIGS-----")
                tlsMap["ech"] = mapOf("enabled" to true, "config" to cfg)
            }
            _hack_config_map["tls"] = tlsMap
        }
    }
}
