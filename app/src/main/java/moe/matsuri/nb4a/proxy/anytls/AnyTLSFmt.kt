package moe.matsuri.nb4a.proxy.anytls

import io.nekohasekai.sagernet.ktx.blankAsNull
import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun buildSingBoxOutboundAnyTLSBean(bean: AnyTLSBean): SingBoxOptions.Outbound_AnyTLSOptions {
    return SingBoxOptions.Outbound_AnyTLSOptions().apply {
        type = "anytls"
        server = bean.serverAddress
        server_port = bean.serverPort
        password = bean.password
        bean.idleSessionCheckInterval.blankAsNull()?.let { _hack_config_map["idle_session_check_interval"] = it }
        bean.idleSessionTimeout.blankAsNull()?.let { _hack_config_map["idle_session_timeout"] = it }
        if ((bean.minIdleSession ?: 0) > 0) _hack_config_map["min_idle_session"] = bean.minIdleSession

        tls = SingBoxOptions.OutboundTLSOptions().apply {
            enabled = true
            server_name = bean.sni.blankAsNull()
            if (bean.allowInsecure) insecure = true
            alpn = bean.alpn.blankAsNull()?.listByLineOrComma()
            bean.certificates.blankAsNull()?.let {
                certificate = it
            }
            var fingerprint = bean.utlsFingerprint.blankAsNull()
            if (!bean.realityPubKey.isNullOrBlank()) {
                reality = SingBoxOptions.OutboundRealityOptions().apply {
                    enabled = true
                    public_key = bean.realityPubKey
                    short_id = bean.realityShortId
                }
                if (fingerprint.isNullOrBlank()) {
                    fingerprint = "chrome"
                }
            }
            fingerprint?.let {
                utls = SingBoxOptions.OutboundUTLSOptions().apply {
                    enabled = true
                    fingerprint = it
                }
            }
            if (bean.disableSNI == true) _hack_config_map["disable_sni"] = true
            bean.clientCert.blankAsNull()?.let { _hack_config_map["client_certificate"] = it.lines() }
            bean.clientKey.blankAsNull()?.let { _hack_config_map["client_key"] = it.lines() }
            bean.certPublicKeySha256.blankAsNull()?.let { _hack_config_map["certificate_public_key_sha256"] = it.listByLineOrComma() }
            bean.certificateSha256.blankAsNull()?.let { _hack_config_map["certificate_sha256"] = it.listByLineOrComma() }
            if (bean.tlsFragment == true) {
                _hack_config_map["fragment"] = true
                bean.tlsFragmentFallbackDelay.blankAsNull()?.let { _hack_config_map["fragment_fallback_delay"] = it }
            }
            if (bean.tlsRecordFragment == true) _hack_config_map["record_fragment"] = true
            bean.tlsSpoof.blankAsNull()?.let {
                _hack_config_map["spoof"] = it
                bean.tlsSpoofMethod.blankAsNull()?.let { m -> _hack_config_map["spoof_method"] = m }
            }
            if (!bean.echConfig.isNullOrBlank() || !bean.echQueryServerName.isNullOrBlank()) {
                ech = SingBoxOptions.OutboundECHOptions().apply {
                    enabled = true
                    bean.echConfig.blankAsNull()?.let {
                        config = if (it.contains("BEGIN ECH CONFIGS")) {
                            listOf(it)
                        } else {
                            listOf("-----BEGIN ECH CONFIGS-----", it.trim(), "-----END ECH CONFIGS-----")
                        }
                    }
                    bean.echQueryServerName.blankAsNull()?.let { _hack_config_map["query_server_name"] = it }
                }
            }
        }
    }
}

fun AnyTLSBean.toUri(): String {
    val builder = linkBuilder()
        .host(serverAddress)
        .port(serverPort)
        .username(password)
    if (!name.isNullOrBlank()) {
        builder.encodedFragment(name.urlSafe())
    }
    if (allowInsecure) {
        builder.addQueryParameter("insecure", "1")
    }
    if (!sni.isNullOrBlank()) {
        builder.addQueryParameter("sni", sni)
    }
    if (!utlsFingerprint.isNullOrBlank()) {
        builder.addQueryParameter("fp", utlsFingerprint)
    }
    if (!realityPubKey.isNullOrBlank()) {
        builder.addQueryParameter("pbk", realityPubKey)
    }
    if (!realityShortId.isNullOrBlank()) {
        builder.addQueryParameter("sid", realityShortId)
    }
    return builder.toLink("anytls")
}

fun parseAnytls(url: String): AnyTLSBean {
    // https://github.com/anytls/anytls-go/blob/main/docs/uri_scheme.md
    val link = url.replace("anytls://", "https://").toHttpUrlOrNull() ?: error(
        "invalid anytls link $url"
    )
    return AnyTLSBean().apply {
        serverAddress = link.host
        serverPort = link.port
        name = link.fragment
        password = link.username
        sni = link.queryParameter("sni") ?: ""
        link.queryParameter("insecure")?.also {
            allowInsecure = it == "1" || it == "true"
        }
        link.queryParameter("fp")?.let {
            utlsFingerprint = it
        }
        link.queryParameter("pbk")?.let {
            realityPubKey = it
        }
        link.queryParameter("sid")?.let {
            realityShortId = it
        }
    }
}
