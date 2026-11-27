package io.nekohasekai.sagernet.ui.profile
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceSchemas
import io.nekohasekai.sagernet.ui.components.ComposePreferenceStore

import android.os.Bundle
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.ssh.SSHBean

class SSHSettingsActivity : ProfileSettingsActivity<SSHBean>() {

    override fun createEntity() = SSHBean()

    override fun SSHBean.init() {
        DataStore.profileName = name
        DataStore.serverAddress = serverAddress
        DataStore.serverPort = serverPort
        DataStore.serverUsername = username
        DataStore.serverAuthType = authType
        DataStore.serverPassword = password
        DataStore.serverPrivateKey = privateKey
        DataStore.serverPassword1 = privateKeyPassphrase
        DataStore.serverCertificates = publicKey
    }

    override fun SSHBean.serialize() {
        name = DataStore.profileName
        serverAddress = DataStore.serverAddress
        serverPort = DataStore.serverPort
        username = DataStore.serverUsername
        authType = DataStore.serverAuthType
        when (authType) {
            SSHBean.AUTH_TYPE_NONE -> {
            }
            SSHBean.AUTH_TYPE_PASSWORD -> {
                password = DataStore.serverPassword
            }
            SSHBean.AUTH_TYPE_PRIVATE_KEY -> {
                privateKey = DataStore.serverPrivateKey
                privateKeyPassphrase = DataStore.serverPassword1
            }
        }
        publicKey = DataStore.serverCertificates
    }

    override val preferenceSchema: List<PrefSectionSpec> = ProtocolPreferenceSchemas.SSH
    override val passwordKeys: Set<String> = setOf(Key.SERVER_PASSWORD, Key.SERVER_PRIVATE_KEY, Key.SERVER_PASSWORD1)

    override fun isPreferenceVisible(key: String, store: ComposePreferenceStore): Boolean {
        val authType = store.getString(Key.SERVER_AUTH_TYPE, "0").toIntOrNull() ?: 0
        return when (key) {
            Key.SERVER_PASSWORD -> authType == SSHBean.AUTH_TYPE_PASSWORD
            Key.SERVER_PRIVATE_KEY, Key.SERVER_PASSWORD1 -> authType == SSHBean.AUTH_TYPE_PRIVATE_KEY
            else -> true
        }
    }
}
