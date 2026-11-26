package moe.matsuri.nb4a.proxy.config

import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas
import io.nekohasekai.sagernet.ui.profile.ProfileSettingsActivity

class ConfigSettingActivity : ProfileSettingsActivity<ConfigBean>() {

    private val isOutboundOnlyKey = "isOutboundOnly"

    override fun createEntity() = ConfigBean()

    override fun ConfigBean.init() {
        DataStore.profileCacheStore.putBoolean(isOutboundOnlyKey, type == 1)
        DataStore.profileName = name
        DataStore.serverConfig = config
    }

    override fun ConfigBean.serialize() {
        type = if (DataStore.profileCacheStore.getBoolean(isOutboundOnlyKey, false)) 1 else 0
        name = DataStore.profileName
        config = DataStore.serverConfig
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.Config
    override val passwordKeys: Set<String> = emptySet()
}
