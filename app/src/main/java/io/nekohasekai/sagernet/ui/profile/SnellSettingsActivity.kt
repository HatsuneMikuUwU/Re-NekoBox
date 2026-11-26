package io.nekohasekai.sagernet.ui.profile
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas

import android.os.Bundle
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.fmt.snell.SnellBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class SnellSettingsActivity : ProfileSettingsActivity<SnellBean>() {

    override fun createEntity() = SnellBean().applyDefaultValues()

    private val pbm = PreferenceBindingManager()
    private val name = pbm.add(PreferenceBinding(Type.Text, "name"))
    private val serverAddress = pbm.add(PreferenceBinding(Type.Text, "serverAddress"))
    private val serverPort = pbm.add(PreferenceBinding(Type.TextToInt, "serverPort"))
    private val psk = pbm.add(PreferenceBinding(Type.Text, "psk"))
    private val userKey = pbm.add(PreferenceBinding(Type.Text, "userKey"))
    private val version = pbm.add(PreferenceBinding(Type.TextToInt, "version"))
    private val network = pbm.add(PreferenceBinding(Type.Text, "network"))
    private val obfsMode = pbm.add(PreferenceBinding(Type.Text, "obfsMode"))
    private val obfsHost = pbm.add(PreferenceBinding(Type.Text, "obfsHost"))
    private val mode = pbm.add(PreferenceBinding(Type.Text, "mode"))
    private val quicProxyMode = pbm.add(PreferenceBinding(Type.Bool, "quicProxyMode"))
    private val reuse = pbm.add(PreferenceBinding(Type.Bool, "reuse"))

    override fun SnellBean.init() {
        pbm.writeToCacheAll(this)
    }

    override fun SnellBean.serialize() {
        pbm.fromCacheAll(this)
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.Snell
    override val passwordKeys: Set<String> = setOf(Key.SERVER_PASSWORD)

}
