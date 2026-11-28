package io.nekohasekai.sagernet.ui.profile

import io.nekohasekai.sagernet.fmt.openconnect.OpenConnectBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class OpenConnectSettingsActivity : ProfileSettingsActivity<OpenConnectBean>() {

    override fun createEntity() = OpenConnectBean().applyDefaultValues()

    private val pbm = PreferenceBindingManager()
    private val name = pbm.add(PreferenceBinding(Type.Text, "name"))
    private val server = pbm.add(PreferenceBinding(Type.Text, "server"))
    private val flavor = pbm.add(PreferenceBinding(Type.Text, "flavor"))
    private val username = pbm.add(PreferenceBinding(Type.Text, "username"))
    private val password = pbm.add(PreferenceBinding(Type.Text, "password"))
    private val authGroup = pbm.add(PreferenceBinding(Type.Text, "authGroup"))
    private val reportedOS = pbm.add(PreferenceBinding(Type.Text, "reportedOS"))
    private val userAgent = pbm.add(PreferenceBinding(Type.Text, "userAgent"))
    private val allowInsecureCrypto = pbm.add(PreferenceBinding(Type.Bool, "allowInsecureCrypto"))
    private val tlsInsecure = pbm.add(PreferenceBinding(Type.Bool, "tlsInsecure"))
    private val tlsServerName = pbm.add(PreferenceBinding(Type.Text, "tlsServerName"))
    private val tlsPeerFingerprint = pbm.add(PreferenceBinding(Type.Text, "tlsPeerFingerprint"))
    private val certificateAuthority = pbm.add(PreferenceBinding(Type.Text, "certificateAuthority"))
    private val clientCertificate = pbm.add(PreferenceBinding(Type.Text, "clientCertificate"))
    private val clientKey = pbm.add(PreferenceBinding(Type.Text, "clientKey"))
    private val clientKeyPassword = pbm.add(PreferenceBinding(Type.Text, "clientKeyPassword"))

    override fun OpenConnectBean.init() {
        pbm.writeToCacheAll(this)
    }

    override fun OpenConnectBean.serialize() {
        pbm.fromCacheAll(this)
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.OpenConnect
    override val passwordKeys: Set<String> = setOf("password", "clientKey", "clientKeyPassword")
}
