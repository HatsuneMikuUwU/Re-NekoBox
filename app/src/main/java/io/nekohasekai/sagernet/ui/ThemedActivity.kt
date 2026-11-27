package io.nekohasekai.sagernet.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.app.ActivityCompat
import io.nekohasekai.sagernet.ui.theme.Appearance
import io.nekohasekai.sagernet.ui.theme.ReNekoBoxTheme
import io.nekohasekai.sagernet.ui.theme.UiScaled
import io.nekohasekai.sagernet.utils.Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/**
 * Lightweight handle returned by [ThemedActivity.snackbar] for call-site
 * compatibility with background adapters without requiring Material Views Snackbar.
 */
class ComposeSnackbarHandle(private val trigger: () -> Unit) {
    fun show() = trigger()
}

abstract class ThemedActivity : AppCompatActivity() {

    var themeResId = 0
    var uiMode = 0
    open val isDialog = false

    val composeSnackbarHostState = SnackbarHostState()
    private val snackbarScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isDialog) {
            Theme.apply(this)
        } else {
            Theme.applyDialog(this)
        }
        Theme.applyNightTheme()

        Appearance.init(this)
        super.onCreate(savedInstanceState)
        if (!isDialog) {
            enableEdgeToEdge()
        }
        uiMode = resources.configuration.uiMode
    }

    protected fun setComposeContent(content: @Composable () -> Unit) {
        setContent {
            CompositionLocalProvider(LocalSnackbarHostState provides composeSnackbarHostState) {
                UiScaled {
                    ReNekoBoxTheme {
                        content()
                    }
                }
            }
        }
    }

    fun showComposeSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onAction: (() -> Unit)? = null,
    ) {
        snackbarScope.launch {
            composeSnackbarHostState.currentSnackbarData?.dismiss()
            val result = composeSnackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration,
                withDismissAction = actionLabel != null,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }

    override fun setTheme(resId: Int) {
        super.setTheme(resId)
        themeResId = resId
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.uiMode != uiMode) {
            uiMode = newConfig.uiMode
            ActivityCompat.recreate(this)
        }
    }

    fun snackbar(@StringRes resId: Int): ComposeSnackbarHandle = snackbar(getString(resId))

    fun snackbar(text: CharSequence): ComposeSnackbarHandle =
        ComposeSnackbarHandle { showComposeSnackbar(text.toString()) }
}
