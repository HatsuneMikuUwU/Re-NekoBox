package io.nekohasekai.sagernet.ui

import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.PrefActionRow
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.StoreEditTextPref
import io.nekohasekai.sagernet.ui.components.rememberComposePreferenceStore
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WebDAVSettingsActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            val store = rememberComposePreferenceStore(DataStore.configurationStore)

            Scaffold(
                snackbarHost = { SnackbarHost(composeSnackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.webdav_settings), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    item {
                        PrefCategory(titleRes = R.string.webdav_settings) {
                            StoreEditTextPref(
                                store = store,
                                key = "webdavServer",
                                titleRes = R.string.webdav_server,
                                icon = Icons.Rounded.CloudQueue,
                                position = ExpressiveGroupPosition.TOP,
                            )
                            StoreEditTextPref(
                                store = store,
                                key = "webdavUsername",
                                titleRes = R.string.webdav_username,
                                icon = Icons.Rounded.Person,
                            )
                            StoreEditTextPref(
                                store = store,
                                key = "webdavPassword",
                                titleRes = R.string.webdav_password,
                                icon = Icons.Rounded.Password,
                                isPassword = true,
                            )
                            StoreEditTextPref(
                                store = store,
                                key = "webdavPath",
                                titleRes = R.string.webdav_path,
                                defaultValue = "NekoBox",
                                icon = Icons.Rounded.FolderOpen,
                            )
                            PrefActionRow(
                                title = stringResource(R.string.webdav_test),
                                icon = Icons.Rounded.NetworkCheck,
                                position = ExpressiveGroupPosition.BOTTOM,
                                onClick = {
                                    val server = store.getString("webdavServer").trim()
                                    val user = store.getString("webdavUsername")
                                    val pass = store.getString("webdavPassword")
                                    if (server.isBlank()) {
                                        showComposeSnackbar("WebDAV server URL is empty")
                                        return@PrefActionRow
                                    }
                                    runOnDefaultDispatcher {
                                        try {
                                            val client = OkHttpClient.Builder()
                                                .connectTimeout(10, TimeUnit.SECONDS)
                                                .build()
                                            val req = Request.Builder()
                                                .url(server)
                                                .method("OPTIONS", null)
                                                .apply {
                                                    if (user.isNotBlank()) {
                                                        header("Authorization", Credentials.basic(user, pass))
                                                    }
                                                }
                                                .build()
                                            client.newCall(req).execute().use { resp ->
                                                onMainDispatcher {
                                                    showComposeSnackbar("HTTP ${resp.code} ${resp.message}")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            onMainDispatcher {
                                                showComposeSnackbar(e.readableMessage)
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
