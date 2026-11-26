@file:Suppress("DEPRECATION")

package io.nekohasekai.sagernet.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.LocalBottomInset
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import moe.matsuri.nb4a.utils.WebViewUtil

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebviewScreen(
    onBack: () -> Unit,
) {
    var currentUrl by remember { mutableStateOf(DataStore.yacdURL) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val topInset = floatingTopBarSpace()
    val bottomInset = LocalBottomInset.current

    val topBackdrop = rememberBlurBackdrop()
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurBackdropSource(topBackdrop),
        ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset, bottom = bottomInset),
            factory = { ctx ->
                WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                WebView(ctx).apply {
                    settings.domStorageEnabled = true
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            WebViewUtil.onReceivedError(view, request, error)
                        }
                    }
                    loadUrl(currentUrl)
                    webViewRef = this
                }
            },
            update = { view ->
                if (view.url != currentUrl) {
                    view.loadUrl(currentUrl)
                }
            },
        )

        }

        FloatingTopBar(
            backdrop = topBackdrop,
            title = stringResource(R.string.menu_dashboard),
            onBack = onBack,
            actions = {
                IconButton(onClick = { webViewRef?.reload() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reload")
                }
                IconButton(onClick = { showUrlDialog = true }) {
                    Icon(Icons.Rounded.Link, contentDescription = stringResource(R.string.set_panel_url))
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (showUrlDialog) {
        var draft by remember(currentUrl) { mutableStateOf(currentUrl) }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.set_panel_url)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        DataStore.yacdURL = draft.trim()
                        currentUrl = DataStore.yacdURL
                        showUrlDialog = false
                    },
                    shape = CircleShape,
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
