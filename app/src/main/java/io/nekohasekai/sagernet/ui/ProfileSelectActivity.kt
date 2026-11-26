package io.nekohasekai.sagernet.ui

import android.content.Intent
import android.os.Bundle
import io.nekohasekai.sagernet.ui.screens.ConfigurationScreen

class ProfileSelectActivity : ThemedActivity() {

    companion object {
        const val EXTRA_SELECTED = "selected"
        const val EXTRA_PROFILE_ID = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            ConfigurationScreen(
                onOpenDrawer = { finish() },
                refreshSignal = 0,
                onShowMessage = { showComposeSnackbar(it) },
                selectMode = true,
                onProfileSelected = { entity ->
                    setResult(RESULT_OK, Intent().apply {
                        putExtra(EXTRA_PROFILE_ID, entity.id)
                    })
                    finish()
                },
            )
        }
    }
}