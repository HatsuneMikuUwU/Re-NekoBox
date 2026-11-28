package io.nekohasekai.sagernet.ui.profile
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas
import io.nekohasekai.sagernet.ui.components.ComposePreferenceStore

import android.os.Bundle
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.tuic.TuicBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class TuicSettingsActivity : ProfileSettingsActivity<TuicBean>() {

    override fun createEntity() = TuicBean().applyDefaultValues()

    private val pbm = PreferenceBindingManager().apply {
        add(PreferenceBinding(Type.Text, "clientCert"))
        add(PreferenceBinding(Type.Text, "clientKey"))
        add(PreferenceBinding(Type.Text, "certPublicKeySha256"))
        add(PreferenceBinding(Type.Text, "certificateSha256"))
        add(PreferenceBinding(Type.Bool, "enableECH"))
        add(PreferenceBinding(Type.Text, "echConfig"))
        add(PreferenceBinding(Type.Text, "echQueryServerName"))
        add(PreferenceBinding(Type.Bool, "disablePathMtuDiscovery"))
        add(PreferenceBinding(Type.Text, "idleTimeout"))
        add(PreferenceBinding(Type.Text, "keepAlivePeriod"))
        add(PreferenceBinding(Type.TextToInt, "initialPacketSize"))
        add(PreferenceBinding(Type.TextToInt, "maxConcurrentStreams"))
        add(PreferenceBinding(Type.TextToInt, "streamReceiveWindow"))
        add(PreferenceBinding(Type.TextToInt, "connectionReceiveWindow"))
    }

    override fun TuicBean.init() {
        DataStore.profileName = name
        DataStore.serverAddress = serverAddress
        DataStore.serverPort = serverPort
        DataStore.serverUsername = uuid
        DataStore.serverPassword = token
        DataStore.serverALPN = alpn
        DataStore.serverCertificates = caText
        DataStore.serverUDPRelayMode = udpRelayMode
        DataStore.serverCongestionController = congestionController
        DataStore.serverDisableSNI = disableSNI
        DataStore.serverSNI = sni
        DataStore.serverReduceRTT = reduceRTT
        DataStore.serverAllowInsecure = allowInsecure
        pbm.writeToCacheAll(this)
    }

    override fun TuicBean.serialize() {
        name = DataStore.profileName
        serverAddress = DataStore.serverAddress
        serverPort = DataStore.serverPort
        uuid = DataStore.serverUsername
        token = DataStore.serverPassword
        alpn = DataStore.serverALPN
        caText = DataStore.serverCertificates
        udpRelayMode = DataStore.serverUDPRelayMode
        congestionController = DataStore.serverCongestionController
        disableSNI = DataStore.serverDisableSNI
        sni = DataStore.serverSNI
        reduceRTT = DataStore.serverReduceRTT
        allowInsecure = DataStore.serverAllowInsecure
        pbm.fromCacheAll(this)
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.Tuic
    override val passwordKeys: Set<String> = setOf(Key.SERVER_PASSWORD)

    override fun isPreferenceEnabled(key: String, store: ComposePreferenceStore): Boolean {
        if (key == Key.SERVER_SNI) {
            return !store.getBoolean(Key.SERVER_DISABLE_SNI, false)
        }
        return true
    }

}
