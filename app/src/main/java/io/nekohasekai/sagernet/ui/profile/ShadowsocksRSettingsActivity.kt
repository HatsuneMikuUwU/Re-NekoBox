package io.nekohasekai.sagernet.ui.profile
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas

import android.os.Bundle
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.shadowsocksr.ShadowsocksRBean

class ShadowsocksRSettingsActivity : ProfileSettingsActivity<ShadowsocksRBean>() {

    override fun createEntity() = ShadowsocksRBean()

    override fun ShadowsocksRBean.init() {
        DataStore.profileName = name
        DataStore.serverAddress = serverAddress
        DataStore.serverPort = serverPort
        DataStore.serverMethod = method
        DataStore.serverPassword = password
        DataStore.serverProtocol = protocol
        DataStore.serverObfs = obfs
        DataStore.serverProtocolParam = protocolParam
        DataStore.serverObfsParam = obfsParam
    }

    override fun ShadowsocksRBean.serialize() {
        name = DataStore.profileName
        serverAddress = DataStore.serverAddress
        serverPort = DataStore.serverPort
        method = DataStore.serverMethod
        password = DataStore.serverPassword
        protocol = DataStore.serverProtocol
        obfs = DataStore.serverObfs
        protocolParam = DataStore.serverProtocolParam
        obfsParam = DataStore.serverObfsParam
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.ShadowsocksR
    override val passwordKeys: Set<String> = setOf(Key.SERVER_PASSWORD)

}
