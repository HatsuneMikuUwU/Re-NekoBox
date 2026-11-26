package io.nekohasekai.sagernet.ui.profile
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas
import io.nekohasekai.sagernet.ui.components.ComposePreferenceStore

import android.os.Bundle
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.http.HttpBean
import io.nekohasekai.sagernet.fmt.trojan.TrojanBean
import io.nekohasekai.sagernet.fmt.v2ray.StandardV2RayBean
import io.nekohasekai.sagernet.fmt.v2ray.VMessBean
import moe.matsuri.nb4a.proxy.PreferenceBinding
import moe.matsuri.nb4a.proxy.PreferenceBindingManager
import moe.matsuri.nb4a.proxy.Type

abstract class StandardV2RaySettingsActivity : ProfileSettingsActivity<StandardV2RayBean>() {

    var tmpBean: StandardV2RayBean? = null

    private val pbm = PreferenceBindingManager()
    private val name = pbm.add(PreferenceBinding(Type.Text, "name"))
    private val serverAddress = pbm.add(PreferenceBinding(Type.Text, "serverAddress"))
    private val serverPort = pbm.add(PreferenceBinding(Type.TextToInt, "serverPort"))
    private val uuid = pbm.add(PreferenceBinding(Type.Text, "uuid"))
    private val username = pbm.add(PreferenceBinding(Type.Text, "username"))
    private val password = pbm.add(PreferenceBinding(Type.Text, "password"))
    private val alterId = pbm.add(PreferenceBinding(Type.TextToInt, "alterId"))
    private val encryption = pbm.add(PreferenceBinding(Type.Text, "encryption"))
    private val type = pbm.add(PreferenceBinding(Type.Text, "type"))
    private val host = pbm.add(PreferenceBinding(Type.Text, "host"))
    private val path = pbm.add(PreferenceBinding(Type.Text, "path"))
    private val packetEncoding = pbm.add(PreferenceBinding(Type.TextToInt, "packetEncoding"))
    private val wsMaxEarlyData = pbm.add(PreferenceBinding(Type.TextToInt, "wsMaxEarlyData"))
    private val earlyDataHeaderName = pbm.add(PreferenceBinding(Type.Text, "earlyDataHeaderName"))
    private val security = pbm.add(PreferenceBinding(Type.Text, "security"))
    private val sni = pbm.add(PreferenceBinding(Type.Text, "sni"))
    private val alpn = pbm.add(PreferenceBinding(Type.Text, "alpn"))
    private val certificates = pbm.add(PreferenceBinding(Type.Text, "certificates"))
    private val allowInsecure = pbm.add(PreferenceBinding(Type.Bool, "allowInsecure"))
    private val utlsFingerprint = pbm.add(PreferenceBinding(Type.Text, "utlsFingerprint"))
    private val realityPubKey = pbm.add(PreferenceBinding(Type.Text, "realityPubKey"))
    private val realityShortId = pbm.add(PreferenceBinding(Type.Text, "realityShortId"))

    private val enableECH = pbm.add(PreferenceBinding(Type.Bool, "enableECH"))
    private val echConfig = pbm.add(PreferenceBinding(Type.Text, "echConfig"))

    private val enableMux = pbm.add(PreferenceBinding(Type.Bool, "enableMux"))
    private val muxPadding = pbm.add(PreferenceBinding(Type.Bool, "muxPadding"))
    private val muxType = pbm.add(PreferenceBinding(Type.TextToInt, "muxType"))
    private val muxConcurrency = pbm.add(PreferenceBinding(Type.TextToInt, "muxConcurrency"))
    private val muxMode = pbm.add(PreferenceBinding(Type.TextToInt, "muxMode"))
    private val muxMaxConnections = pbm.add(PreferenceBinding(Type.TextToInt, "muxMaxConnections"))
    private val muxMinStreams = pbm.add(PreferenceBinding(Type.TextToInt, "muxMinStreams"))
    private val muxBrutal = pbm.add(PreferenceBinding(Type.Bool, "muxBrutal"))
    private val muxBrutalUpMbps = pbm.add(PreferenceBinding(Type.TextToInt, "muxBrutalUpMbps"))
    private val muxBrutalDownMbps = pbm.add(PreferenceBinding(Type.TextToInt, "muxBrutalDownMbps"))

    private val xhttpMode = pbm.add(PreferenceBinding(Type.Text, "xhttpMode"))
    private val xhttpExtra = pbm.add(PreferenceBinding(Type.Text, "xhttpExtra"))
    private val vlessEncryption = pbm.add(PreferenceBinding(Type.Text, "vlessEncryption"))

    // KCP
    private val mKcpSeed = pbm.add(PreferenceBinding(Type.Text, "mKcpSeed"))
    private val headerType = pbm.add(PreferenceBinding(Type.Text, "headerType"))
    private val kcpMtu = pbm.add(PreferenceBinding(Type.TextToInt, "kcpMtu"))
    private val kcpTti = pbm.add(PreferenceBinding(Type.TextToInt, "kcpTti"))
    private val kcpCwndMultiplier = pbm.add(PreferenceBinding(Type.TextToInt, "kcpCwndMultiplier"))

    override fun StandardV2RayBean.init() {
        if (this is TrojanBean) {
            this@StandardV2RaySettingsActivity.uuid.fieldName = "password"
            this@StandardV2RaySettingsActivity.password.disable = true
        }

        tmpBean = this // copy bean
        pbm.writeToCacheAll(this)
    }

    override fun StandardV2RayBean.serialize() {
        pbm.fromCacheAll(this)
    }





    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.StandardV2Ray
    override val passwordKeys: Set<String> = setOf(Key.SERVER_USER_ID, Key.SERVER_PASSWORD)

    override fun isPreferenceVisible(key: String, store: ComposePreferenceStore): Boolean {
        val isHttp = tmpBean is HttpBean
        val isVmess = tmpBean is VMessBean && !(tmpBean as VMessBean).isVLESS
        val isVless = tmpBean is VMessBean && (tmpBean as VMessBean).isVLESS
        return when (key) {
            Key.SERVER_USER_ID -> !isHttp
            "alterId" -> isVmess
            Key.SERVER_ENCRYPTION -> isVmess || isVless
            Key.SERVER_USERNAME, Key.SERVER_PASSWORD -> isHttp
            else -> true
        }
    }
}
