package io.nekohasekai.sagernet.ui.profile

import io.nekohasekai.sagernet.fmt.tailscale.TailcatBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class TailcatSettingsActivity : ProfileSettingsActivity<TailcatBean>() {

    override fun createEntity() = TailcatBean().applyDefaultValues()

    private val pbm = PreferenceBindingManager()
    private val name = pbm.add(PreferenceBinding(Type.Text, "name"))
    private val privateKey = pbm.add(PreferenceBinding(Type.Text, "privateKey"))
    private val serverPublicKey = pbm.add(PreferenceBinding(Type.Text, "serverPublicKey"))
    private val serverDiscoKey = pbm.add(PreferenceBinding(Type.Text, "serverDiscoKey"))
    private val preSharedKey = pbm.add(PreferenceBinding(Type.Text, "preSharedKey"))
    private val derpMapURL = pbm.add(PreferenceBinding(Type.Text, "derpMapURL"))
    private val derpRegion = pbm.add(PreferenceBinding(Type.TextToInt, "derpRegion"))
    private val derpServers = pbm.add(PreferenceBinding(Type.Text, "derpServers"))

    override fun TailcatBean.init() {
        pbm.writeToCacheAll(this)
    }

    override fun TailcatBean.serialize() {
        pbm.fromCacheAll(this)
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.Tailcat
    override val passwordKeys: Set<String> = setOf("privateKey", "preSharedKey")
}
