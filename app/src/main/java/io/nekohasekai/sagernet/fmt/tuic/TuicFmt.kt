package io.nekohasekai.sagernet.fmt.tuic

import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun parseTuic(url: String): TuicBean {
    // https://github.com/daeuniverse/dae/discussions/182
    val link = url.replace("tuic://", "https://").toHttpUrlOrNull() ?: error(
        "invalid tuic link $url"
    )
    return TuicBean().apply {
        protocolVersion = 5

        name = link.fragment
        serverAddress = link.host
        serverPort = link.port

        val rawUser = link.username
        val rawPass = link.password

        if (rawUser.contains(":")) {
            val parts = rawUser.split(":", limit = 2)
            uuid = parts[0]
            token = parts.getOrElse(1) { "" }
        } else {
            uuid = rawUser
            token = rawPass
        }

        link.queryParameter("sni")?.let {
            sni = it
        }
        link.queryParameter("congestion_control")?.let {
            congestionController = it
        }
        link.queryParameter("udp_relay_mode")?.let {
            udpRelayMode = it
        }
        link.queryParameter("alpn")?.let {
            if (it != "none") alpn = it
        }
        link.queryParameter("allow_insecure")?.let {
            if (it == "1") allowInsecure = true
        }
        link.queryParameter("disable_sni")?.let {
            if (it == "1") disableSNI = true
        }
    }
}

fun TuicBean.toUri(): String {
    val builder = linkBuilder().username(uuid).password(token).host(serverAddress).port(serverPort)

    builder.addQueryParameter("congestion_control", congestionController)
    builder.addQueryParameter("udp_relay_mode", udpRelayMode)

    if (sni.isNotBlank()) builder.addQueryParameter("sni", sni)
    if (alpn.isNotBlank()) builder.addQueryParameter("alpn", alpn)
    if (allowInsecure) builder.addQueryParameter("allow_insecure", "1")
    if (disableSNI) builder.addQueryParameter("disable_sni", "1")
    if (name.isNotBlank()) builder.encodedFragment(name.urlSafe())

    return builder.toLink("tuic")
}

fun buildSingBoxOutboundTuicBean(bean: TuicBean): SingBoxOptions.Outbound_TUICOptions {
    if (bean.protocolVersion == 4) throw Exception("TUIC v4 is no longer supported")
    return SingBoxOptions.Outbound_TUICOptions().apply {
        type = "tuic"
        server = bean.serverAddress
        server_port = bean.serverPort
        uuid = bean.uuid
        password = bean.token
        congestion_control = bean.congestionController
        when (bean.udpRelayMode) {
            "quic" -> udp_relay_mode = "quic"
        }
        zero_rtt_handshake = bean.reduceRTT
        if (bean.disablePathMtuDiscovery == true) _hack_config_map["disable_path_mtu_discovery"] = true
        bean.idleTimeout?.takeIf { it.isNotBlank() }?.let { _hack_config_map["idle_timeout"] = it }
        bean.keepAlivePeriod?.takeIf { it.isNotBlank() }?.let { _hack_config_map["keep_alive_period"] = it }
        if ((bean.initialPacketSize ?: 0) > 0) _hack_config_map["initial_packet_size"] = bean.initialPacketSize
        if ((bean.maxConcurrentStreams ?: 0) > 0) _hack_config_map["max_concurrent_streams"] = bean.maxConcurrentStreams
        if ((bean.streamReceiveWindow ?: 0) > 0) _hack_config_map["stream_receive_window"] = bean.streamReceiveWindow
        if ((bean.connectionReceiveWindow ?: 0) > 0) _hack_config_map["connection_receive_window"] = bean.connectionReceiveWindow
        tls = SingBoxOptions.OutboundTLSOptions().apply {
            if (bean.sni.isNotBlank()) {
                server_name = bean.sni
            }
            if (bean.alpn.isNotBlank()) {
                alpn = bean.alpn.listByLineOrComma()
            }
            if (bean.caText.isNotBlank()) {
                certificate = bean.caText
            }
            bean.clientCert?.takeIf { it.isNotBlank() }?.let { _hack_config_map["client_certificate"] = it.lines() }
            bean.clientKey?.takeIf { it.isNotBlank() }?.let { _hack_config_map["client_key"] = it.lines() }
            bean.certPublicKeySha256?.takeIf { it.isNotBlank() }?.let { _hack_config_map["certificate_public_key_sha256"] = it.listByLineOrComma() }
            bean.certificateSha256?.takeIf { it.isNotBlank() }?.let { _hack_config_map["certificate_sha256"] = it.listByLineOrComma() }
            if (bean.enableECH == true) {
                ech = SingBoxOptions.OutboundECHOptions().apply {
                    enabled = true
                    if (!bean.echConfig.isNullOrBlank()) {
                        config = if (bean.echConfig.contains("BEGIN ECH CONFIGS")) bean.echConfig.lines()
                        else listOf("-----BEGIN ECH CONFIGS-----", bean.echConfig.trim(), "-----END ECH CONFIGS-----")
                    }
                    bean.echQueryServerName?.takeIf { it.isNotBlank() }?.let { _hack_config_map["query_server_name"] = it }
                }
            }
            disable_sni = bean.disableSNI
            insecure = bean.allowInsecure || DataStore.globalAllowInsecure
            enabled = true
        }
    }
}
