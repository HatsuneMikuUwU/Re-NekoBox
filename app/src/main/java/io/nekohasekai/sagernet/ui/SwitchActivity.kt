package io.nekohasekai.sagernet.ui

import android.os.Bundle
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.ktx.runOnMainDispatcher
import io.nekohasekai.sagernet.ui.screens.ConfigurationScreen

class SwitchActivity : ThemedActivity() {

    override val isDialog = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            ConfigurationScreen(
                onBack = { finish() },
                refreshSignal = 0,
                onShowMessage = { showComposeSnackbar(it) },
                selectMode = true,
                onProfileSelected = { entity ->
                    val old = DataStore.selectedProxy
                    DataStore.selectedProxy = entity.id
                    runOnMainDispatcher {
                        ProfileManager.postUpdate(old, true)
                        ProfileManager.postUpdate(entity.id, true)
                    }
                    SagerNet.reloadService()
                    finish()
                },
            )
        }
    }
}