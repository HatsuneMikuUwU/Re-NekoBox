package io.nekohasekai.sagernet.ui.profile

import io.nekohasekai.sagernet.fmt.masque.MASQUEBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class MASQUESettingsActivity : ProfileSettingsActivity<MASQUEBean>() {

    override fun createEntity() = MASQUEBean().applyDefaultValues()

    private val pbm = PreferenceBindingManager()
    private val name = pbm.add(PreferenceBinding(Type.Text, "name"))
    private val serverAddress = pbm.add(PreferenceBinding(Type.Text, "serverAddress"))
    private val serverPort = pbm.add(PreferenceBinding(Type.TextToInt, "serverPort"))
    private val username = pbm.add(PreferenceBinding(Type.Text, "username"))
    private val password = pbm.add(PreferenceBinding(Type.Text, "password"))
    private val path = pbm.add(PreferenceBinding(Type.Text, "path"))
    private val httpVersion = pbm.add(PreferenceBinding(Type.TextToInt, "httpVersion"))
    private val disableVersionFallback = pbm.add(PreferenceBinding(Type.Bool, "disableVersionFallback"))
    private val mtu = pbm.add(PreferenceBinding(Type.TextToInt, "mtu"))
    private val enableTLS = pbm.add(PreferenceBinding(Type.Bool, "enableTLS"))
    private val sni = pbm.add(PreferenceBinding(Type.Text, "sni"))
    private val allowInsecure = pbm.add(PreferenceBinding(Type.Bool, "allowInsecure"))
    private val disableSNI = pbm.add(PreferenceBinding(Type.Bool, "disableSNI"))
    private val alpn = pbm.add(PreferenceBinding(Type.Text, "alpn"))
    private val certificates = pbm.add(PreferenceBinding(Type.Text, "certificates"))
    private val utlsFingerprint = pbm.add(PreferenceBinding(Type.Text, "utlsFingerprint"))
    private val echConfig = pbm.add(PreferenceBinding(Type.Text, "echConfig"))

    override fun MASQUEBean.init() {
        pbm.writeToCacheAll(this)
    }

    override fun MASQUEBean.serialize() {
        pbm.fromCacheAll(this)
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.MASQUE
    override val passwordKeys: Set<String> = setOf("password")
}
