package io.nekohasekai.sagernet.ui.profile
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas
import io.nekohasekai.sagernet.ui.components.ComposePreferenceStore

import android.os.Bundle
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.hysteria.HysteriaBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class HysteriaSettingsActivity : ProfileSettingsActivity<HysteriaBean>() {

    override fun createEntity() = HysteriaBean().applyDefaultValues()

    private val pbm = PreferenceBindingManager().apply {
        add(PreferenceBinding(Type.Text, "bbrProfile"))
        add(PreferenceBinding(Type.Bool, "disableSNI"))
        add(PreferenceBinding(Type.Text, "clientCert"))
        add(PreferenceBinding(Type.Text, "clientKey"))
        add(PreferenceBinding(Type.Text, "certPublicKeySha256"))
        add(PreferenceBinding(Type.Text, "certificateSha256"))
        add(PreferenceBinding(Type.Bool, "enableECH"))
        add(PreferenceBinding(Type.Text, "echConfig"))
        add(PreferenceBinding(Type.Text, "echQueryServerName"))
        add(PreferenceBinding(Type.Text, "idleTimeout"))
        add(PreferenceBinding(Type.Text, "keepAlivePeriod"))
        add(PreferenceBinding(Type.TextToInt, "initialPacketSize"))
        add(PreferenceBinding(Type.TextToInt, "maxConcurrentStreams"))
    }

    override fun HysteriaBean.init() {
        DataStore.profileName = name
        DataStore.protocolVersion = protocolVersion
        DataStore.serverAddress = serverAddress
        DataStore.serverPorts = serverPorts
        DataStore.serverObfs = obfuscation
        DataStore.serverAuthType = authPayloadType
        DataStore.serverProtocolInt = protocol
        DataStore.serverPassword = authPayload
        DataStore.serverSNI = sni
        DataStore.serverALPN = alpn
        DataStore.serverCertificates = caText
        DataStore.serverAllowInsecure = allowInsecure
        DataStore.serverUploadSpeed = uploadMbps
        DataStore.serverDownloadSpeed = downloadMbps
        DataStore.serverStreamReceiveWindow = streamReceiveWindow
        DataStore.serverConnectionReceiveWindow = connectionReceiveWindow
        DataStore.serverDisableMtuDiscovery = disableMtuDiscovery
        DataStore.serverHopInterval = hopInterval
        pbm.writeToCacheAll(this)
    }

    override fun HysteriaBean.serialize() {
        name = DataStore.profileName
        protocolVersion = DataStore.protocolVersion
        serverAddress = DataStore.serverAddress
        serverPorts = DataStore.serverPorts
        obfuscation = DataStore.serverObfs
        authPayloadType = DataStore.serverAuthType
        authPayload = DataStore.serverPassword
        protocol = DataStore.serverProtocolInt
        sni = DataStore.serverSNI
        alpn = DataStore.serverALPN
        caText = DataStore.serverCertificates
        allowInsecure = DataStore.serverAllowInsecure
        uploadMbps = DataStore.serverUploadSpeed
        downloadMbps = DataStore.serverDownloadSpeed
        streamReceiveWindow = DataStore.serverStreamReceiveWindow
        connectionReceiveWindow = DataStore.serverConnectionReceiveWindow
        disableMtuDiscovery = DataStore.serverDisableMtuDiscovery
        hopInterval = DataStore.serverHopInterval
        pbm.fromCacheAll(this)
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.Hysteria
    override val passwordKeys: Set<String> = setOf(Key.SERVER_PASSWORD)

    override fun isPreferenceVisible(key: String, store: ComposePreferenceStore): Boolean {
        val version = store.getString(Key.PROTOCOL_VERSION, "2").toIntOrNull() ?: 2
        val authType = store.getString(Key.SERVER_AUTH_TYPE, "0")
        return if (version == 2) {
            key !in setOf(
                Key.SERVER_AUTH_TYPE,
                Key.SERVER_PROTOCOL,
                Key.SERVER_ALPN,
                Key.SERVER_STREAM_RECEIVE_WINDOW,
                Key.SERVER_CONNECTION_RECEIVE_WINDOW,
                Key.SERVER_DISABLE_MTU_DISCOVERY,
            )
        } else {
            if (key == Key.SERVER_PASSWORD) authType != "${HysteriaBean.TYPE_NONE}" else true
        }
    }
}
