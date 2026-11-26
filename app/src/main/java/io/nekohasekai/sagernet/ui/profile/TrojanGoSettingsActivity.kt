package io.nekohasekai.sagernet.ui.profile
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas
import io.nekohasekai.sagernet.ui.components.ComposePreferenceStore

import android.os.Bundle
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.trojan_go.TrojanGoBean
import io.nekohasekai.sagernet.ktx.app

class TrojanGoSettingsActivity : ProfileSettingsActivity<TrojanGoBean>() {

    override fun createEntity() = TrojanGoBean()

    override fun TrojanGoBean.init() {
        DataStore.profileName = name
        DataStore.serverAddress = serverAddress
        DataStore.serverPort = serverPort
        DataStore.serverPassword = password
        DataStore.serverSNI = sni
        DataStore.serverAllowInsecure = allowInsecure
        DataStore.serverNetwork = type
        DataStore.serverHost = host
        DataStore.serverPath = path
        if (encryption.startsWith("ss;")) {
            DataStore.serverEncryption = "ss"
            DataStore.serverMethod = encryption.substringAfter(";").substringBefore(":")
            DataStore.serverPassword1 = encryption.substringAfter(":")
        } else {
            DataStore.serverEncryption = encryption
        }
    }

    override fun TrojanGoBean.serialize() {
        name = DataStore.profileName
        serverAddress = DataStore.serverAddress
        serverPort = DataStore.serverPort
        password = DataStore.serverPassword
        sni = DataStore.serverSNI
        allowInsecure = DataStore.serverAllowInsecure
        type = DataStore.serverNetwork
        host = DataStore.serverHost
        path = DataStore.serverPath
        encryption = when (val security = DataStore.serverEncryption) {
            "ss" -> {
                "ss;" + DataStore.serverMethod + ":" + DataStore.serverPassword1
            }
            else -> {
                security
            }
        }
    }



    val trojanGoMethods = app.resources.getStringArray(R.array.trojan_go_methods)
    val trojanGoNetworks = app.resources.getStringArray(R.array.trojan_go_networks_value)

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.TrojanGo
    override val passwordKeys: Set<String> = setOf(Key.SERVER_PASSWORD, Key.SERVER_ENCRYPTION)

    override fun isPreferenceVisible(key: String, store: ComposePreferenceStore): Boolean {
        val network = store.getString(Key.SERVER_NETWORK, "original")
        val encryption = store.getString(Key.SERVER_ENCRYPTION, "none")
        return when (key) {
            Key.SERVER_WS_CATEGORY, Key.SERVER_HOST, Key.SERVER_PATH -> network == "ws"
            Key.SERVER_SS_CATEGORY, Key.SERVER_METHOD -> encryption == "ss"
            else -> true
        }
    }
}
