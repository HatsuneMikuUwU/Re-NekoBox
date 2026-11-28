package moe.matsuri.nb4a.proxy.anytls
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas

import android.os.Bundle
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ui.profile.ProfileSettingsActivity
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

class AnyTLSSettingsActivity : ProfileSettingsActivity<AnyTLSBean>() {
    override fun createEntity() = AnyTLSBean().applyDefaultValues()

    private val pbm = PreferenceBindingManager()
    private val name = pbm.add(PreferenceBinding(Type.Text, "name"))
    private val serverAddress = pbm.add(PreferenceBinding(Type.Text, "serverAddress"))
    private val serverPort = pbm.add(PreferenceBinding(Type.TextToInt, "serverPort"))
    private val password = pbm.add(PreferenceBinding(Type.Text, "password"))
    private val sni = pbm.add(PreferenceBinding(Type.Text, "sni"))
    private val alpn = pbm.add(PreferenceBinding(Type.Text, "alpn"))
    private val certificates = pbm.add(PreferenceBinding(Type.Text, "certificates"))
    private val allowInsecure = pbm.add(PreferenceBinding(Type.Bool, "allowInsecure"))
    private val utlsFingerprint = pbm.add(PreferenceBinding(Type.Text, "utlsFingerprint"))
    private val realityPubKey = pbm.add(PreferenceBinding(Type.Text, "realityPubKey"))
    private val realityShortId = pbm.add(PreferenceBinding(Type.Text, "realityShortId"))
    private val echConfig = pbm.add(PreferenceBinding(Type.Text, "echConfig"))
    private val echQueryServerName = pbm.add(PreferenceBinding(Type.Text, "echQueryServerName"))
    private val idleSessionCheckInterval = pbm.add(PreferenceBinding(Type.Text, "idleSessionCheckInterval"))
    private val idleSessionTimeout = pbm.add(PreferenceBinding(Type.Text, "idleSessionTimeout"))
    private val minIdleSession = pbm.add(PreferenceBinding(Type.TextToInt, "minIdleSession"))
    private val disableSNI = pbm.add(PreferenceBinding(Type.Bool, "disableSNI"))
    private val clientCert = pbm.add(PreferenceBinding(Type.Text, "clientCert"))
    private val clientKey = pbm.add(PreferenceBinding(Type.Text, "clientKey"))
    private val certPublicKeySha256 = pbm.add(PreferenceBinding(Type.Text, "certPublicKeySha256"))
    private val certificateSha256 = pbm.add(PreferenceBinding(Type.Text, "certificateSha256"))
    private val tlsFragment = pbm.add(PreferenceBinding(Type.Bool, "tlsFragment"))
    private val tlsFragmentFallbackDelay = pbm.add(PreferenceBinding(Type.Text, "tlsFragmentFallbackDelay"))
    private val tlsRecordFragment = pbm.add(PreferenceBinding(Type.Bool, "tlsRecordFragment"))
    private val tlsSpoof = pbm.add(PreferenceBinding(Type.Text, "tlsSpoof"))
    private val tlsSpoofMethod = pbm.add(PreferenceBinding(Type.Text, "tlsSpoofMethod"))

    override fun AnyTLSBean.init() {
        pbm.writeToCacheAll(this)

    }

    override fun AnyTLSBean.serialize() {
        pbm.fromCacheAll(this)
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.AnyTLS
    override val passwordKeys: Set<String> = setOf(Key.SERVER_PASSWORD)

}
