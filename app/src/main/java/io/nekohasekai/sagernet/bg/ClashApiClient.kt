package io.nekohasekai.sagernet.bg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ClashConnectionItem(
    val id: String,
    val host: String,
    val destinationIp: String,
    val destinationPort: String,
    val network: String,
    val inboundType: String,
    val upload: Long,
    val download: Long,
    val chains: List<String>,
    val rule: String,
    val rulePayload: String,
    val start: String,
) {
    val displayTarget: String
        get() {
            val targetHost = host.ifBlank { destinationIp }
            return if (destinationPort.isNotBlank() && targetHost.isNotBlank()) {
                "$targetHost:$destinationPort"
            } else {
                targetHost.ifBlank { "-" }
            }
        }

    val displayOutbound: String
        get() = chains.firstOrNull()?.takeIf { it.isNotBlank() } ?: "-"

    val displayRule: String
        get() = when {
            rule.isNotBlank() && rulePayload.isNotBlank() -> "$rule ($rulePayload)"
            rule.isNotBlank() -> rule
            else -> ""
        }
}

data class ClashConnectionsSnapshot(
    val downloadTotal: Long = 0L,
    val uploadTotal: Long = 0L,
    val memoryBytes: Long = 0L,
    val connections: List<ClashConnectionItem> = emptyList(),
)

data class ClashRuleItem(
    val type: String,
    val payload: String,
    val proxy: String,
)

object ClashApiClient {
    const val CONTROLLER_ADDRESS = "127.0.0.1:9090"
    private const val BASE_URL = "http://$CONTROLLER_ADDRESS"
    private const val CONNECT_TIMEOUT_MS = 1500
    private const val READ_TIMEOUT_MS = 2000

    suspend fun fetchConnections(): ClashConnectionsSnapshot? = withContext(Dispatchers.IO) {
        val body = request("GET", "/connections") ?: return@withContext null
        runCatching {
            val root = JSONObject(body)
            val downloadTotal = root.optLong("downloadTotal", 0L)
            val uploadTotal = root.optLong("uploadTotal", 0L)
            val memoryBytes = root.optLong("memory", 0L)
            val arr = root.optJSONArray("connections")
            val list = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val meta = obj.optJSONObject("metadata")
                        val chainsArr = obj.optJSONArray("chains")
                        val chains = buildList {
                            if (chainsArr != null) {
                                for (j in 0 until chainsArr.length()) {
                                    val c = chainsArr.optString(j)
                                    if (c.isNotBlank()) add(c)
                                }
                            }
                        }
                        add(
                            ClashConnectionItem(
                                id = obj.optString("id"),
                                host = meta?.optString("host").orEmpty(),
                                destinationIp = meta?.optString("destinationIP").orEmpty(),
                                destinationPort = meta?.optString("destinationPort").orEmpty(),
                                network = meta?.optString("network").orEmpty().uppercase(),
                                inboundType = meta?.optString("type").orEmpty(),
                                upload = obj.optLong("upload", 0L),
                                download = obj.optLong("download", 0L),
                                chains = chains,
                                rule = obj.optString("rule"),
                                rulePayload = obj.optString("rulePayload"),
                                start = obj.optString("start"),
                            )
                        )
                    }
                }
            }
            ClashConnectionsSnapshot(
                downloadTotal = downloadTotal,
                uploadTotal = uploadTotal,
                memoryBytes = memoryBytes,
                connections = list,
            )
        }.getOrNull()
    }

    suspend fun closeConnection(id: String): Boolean = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext false
        requestStatus("DELETE", "/connections/$id") in 200..299
    }

    suspend fun closeAllConnections(): Boolean = withContext(Dispatchers.IO) {
        requestStatus("DELETE", "/connections") in 200..299
    }

    suspend fun fetchRules(): List<ClashRuleItem> = withContext(Dispatchers.IO) {
        val body = request("GET", "/rules") ?: return@withContext emptyList()
        runCatching {
            val root = JSONObject(body)
            val arr = root.optJSONArray("rules") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        ClashRuleItem(
                            type = obj.optString("type"),
                            payload = obj.optString("payload"),
                            proxy = obj.optString("proxy"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun request(method: String, path: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun requestStatus(method: String, path: String): Int {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            conn.responseCode
        } catch (_: Exception) {
            -1
        } finally {
            conn?.disconnect()
        }
    }
}
