@file:Suppress("DEPRECATION")

package io.nekohasekai.sagernet.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.components.AppDialog
import io.nekohasekai.sagernet.ui.components.DialogCancelButton
import io.nekohasekai.sagernet.ui.components.DialogConfirmButton
import io.nekohasekai.sagernet.ui.components.ExpressiveEmptyState
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.LocalBottomInset
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.hapticClick
import moe.matsuri.nb4a.utils.WebViewUtil

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebviewScreen(
    onBack: () -> Unit,
) {
    var currentUrl by remember { mutableStateOf(DataStore.yacdURL.ifBlank { "http://127.0.0.1:9090/ui" }) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }
    var mainFrameError by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    val topInset = floatingTopBarSpace()
    val bottomInset = LocalBottomInset.current
    val isLocalController = remember(currentUrl) {
        currentUrl.contains("127.0.0.1:9090") || currentUrl.contains("localhost:9090")
    }
    val connected = DataStore.serviceState.connected
    val showDisconnected = isLocalController && !connected

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webViewRef?.apply {
                    stopLoading()
                    onPause()
                    clearHistory()
                    removeAllViews()
                    destroy()
                }
            }
            webViewRef = null
        }
    }

    LaunchedEffect(currentUrl, reloadNonce, showDisconnected) {
        if (!showDisconnected) {
            val view = webViewRef ?: return@LaunchedEffect
            if (lastLoadedUrl != currentUrl || reloadNonce > 0) {
                mainFrameError = null
                lastLoadedUrl = currentUrl
                view.loadUrl(currentUrl)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showDisconnected) {
            ExpressiveEmptyState(
                icon = Icons.Rounded.CloudOff,
                title = stringResource(R.string.dashboard_not_connected),
                subtitle = stringResource(R.string.dashboard_not_connected_sum),
                actionLabel = stringResource(R.string.dashboard_retry),
                onAction = { reloadNonce++ },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset, bottom = bottomInset),
            )
        } else {
            // Keep WebView outside hardware layerBackdrop/RenderEffect to prevent Chromium surface crashes.
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset + 8.dp, bottom = bottomInset),
                factory = { ctx ->
                    runCatching { WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG) }
                    WebView(ctx).apply {
                        settings.domStorageEnabled = true
                        settings.javaScriptEnabled = true
                        settings.databaseEnabled = true
                        settings.allowFileAccess = false
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url != "about:blank") {
                                    mainFrameError = null
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                WebViewUtil.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    mainFrameError = error?.description?.toString()
                                        ?: ctx.getString(R.string.dashboard_load_failed)
                                }
                            }
                        }
                        lastLoadedUrl = currentUrl
                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                update = { view ->
                    webViewRef = view
                },
            )

            mainFrameError?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topInset + 8.dp, bottom = bottomInset)
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    ExpressiveEmptyState(
                        icon = Icons.Rounded.CloudOff,
                        title = stringResource(R.string.dashboard_load_failed),
                        subtitle = err,
                        actionLabel = stringResource(R.string.dashboard_retry),
                        onAction = {
                            mainFrameError = null
                            reloadNonce++
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        FloatingTopBar(
            backdrop = null,
            title = stringResource(R.string.menu_dashboard),
            onBack = onBack,
            actions = {
                IconButton(onClick = hapticClick {
                    mainFrameError = null
                    reloadNonce++
                }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.dashboard_retry))
                }
                IconButton(onClick = hapticClick { showCleanupDialog = true }) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = stringResource(R.string.webview_cleanup))
                }
                IconButton(onClick = hapticClick { showUrlDialog = true }) {
                    Icon(Icons.Rounded.Link, contentDescription = stringResource(R.string.set_panel_url))
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (showUrlDialog) {
        var draft by remember(currentUrl) { mutableStateOf(currentUrl) }
        AppDialog(
            icon = Icons.Rounded.Link,
            title = stringResource(R.string.set_panel_url),
            onDismiss = { showUrlDialog = false },
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
                DialogConfirmButton(
                    label = stringResource(android.R.string.ok),
                    onClick = {
                        val cleaned = draft.trim().ifBlank { "http://127.0.0.1:9090/ui" }
                        DataStore.yacdURL = cleaned
                        currentUrl = cleaned
                        mainFrameError = null
                        reloadNonce++
                        showUrlDialog = false
                    },
                )
            },
            dismissButton = {
                DialogCancelButton(
                    label = stringResource(android.R.string.cancel),
                    onClick = { showUrlDialog = false },
                )
            },
        )
    }

    if (showCleanupDialog) {
        AppDialog(
            icon = Icons.Rounded.CleaningServices,
            title = stringResource(R.string.webview_cleanup),
            onDismiss = { showCleanupDialog = false },
            text = { Text(stringResource(R.string.webview_cleanup_confirmation)) },
            confirmButton = {
                DialogConfirmButton(
                    label = stringResource(R.string.yes),
                    onClick = {
                        runCatching {
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()
                            WebStorage.getInstance().deleteAllData()
                            webViewRef?.clearCache(true)
                            webViewRef?.clearHistory()
                        }
                        showCleanupDialog = false
                        mainFrameError = null
                        reloadNonce++
                    },
                )
            },
            dismissButton = {
                DialogCancelButton(
                    label = stringResource(R.string.no),
                    onClick = { showCleanupDialog = false },
                )
            },
        )
    }
}
