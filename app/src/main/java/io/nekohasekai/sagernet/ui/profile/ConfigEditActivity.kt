@file:Suppress("DEPRECATION")

package io.nekohasekai.sagernet.ui.profile

import io.nekohasekai.sagernet.ui.background.screenContainerColor

import io.nekohasekai.sagernet.ui.components.hapticClick

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.ThemedActivity
import org.json.JSONArray
import org.json.JSONObject

class ConfigEditActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val key = intent.getStringExtra("key") ?: Key.SERVER_CONFIG
        val useConfigStore = intent.getBooleanExtra("useConfigStore", false)
        val store = if (useConfigStore) DataStore.configurationStore else DataStore.profileCacheStore

        setComposeContent {
            var content by remember { mutableStateOf(store.getString(key, "") ?: "") }
            val quickSymbols = remember { listOf("{", "}", "[", "]", "\"", ":", ",", "  ") }

            Scaffold(
                containerColor = screenContainerColor(),
                snackbarHost = { SnackbarHost(composeSnackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.custom_config), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = hapticClick { finish() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(onClick = hapticClick {
                                val trimmed = content.trim()
                                try {
                                    content = when {
                                        trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                                        trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                                        else -> content
                                    }
                                } catch (e: Exception) {
                                    showComposeSnackbar("Invalid JSON: ${e.message}")
                                }
                            }) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = "Format JSON")
                            }
                            IconButton(onClick = hapticClick {
                                store.putString(key, content)
                                DataStore.dirty = true
                                setResult(RESULT_OK)
                                finish()
                            }) {
                                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.apply))
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding(),
                ) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        ),
                        shape = RoundedCornerShape(20.dp),
                        placeholder = { Text("{ }") },
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(quickSymbols) { sym ->
                            FilledTonalButton(
                                onClick = hapticClick { content += sym },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = if (sym == "  ") "TAB" else sym,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}