package io.nekohasekai.sagernet.database

import android.os.Parcel
import android.os.Parcelable
import moe.matsuri.nb4a.utils.JavaUtil
import org.json.JSONArray
import org.json.JSONObject

data class CustomDnsServerEntity(
    var id: Long = 0L,
    var tag: String = "",
    var type: String = "udp",
    var userOrder: Long = 0L,
    var enabled: Boolean = true,
    var server: String = "",
    var serverPort: Int = 0,
    var path: String = "",
    var addressResolver: String = "",
    var strategy: String = "",
    var detour: String = "direct",
    var domains: String = "",
) : Parcelable {

    companion object {
        val RESERVED_TAGS = setOf("dns-direct", "dns-remote", "dns-local", "dns-fake", "dns-block", "dns-hosts")

        @JvmField
        val CREATOR = object : Parcelable.Creator<CustomDnsServerEntity> {
            override fun createFromParcel(parcel: Parcel): CustomDnsServerEntity =
                CustomDnsServerEntity(parcel)

            override fun newArray(size: Int): Array<CustomDnsServerEntity?> = arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        tag = parcel.readString().orEmpty(),
        type = parcel.readString().orEmpty().ifBlank { "udp" },
        userOrder = parcel.readLong(),
        enabled = parcel.readByte() != 0.toByte(),
        server = parcel.readString().orEmpty(),
        serverPort = parcel.readInt(),
        path = parcel.readString().orEmpty(),
        addressResolver = parcel.readString().orEmpty(),
        strategy = parcel.readString().orEmpty(),
        detour = parcel.readString().orEmpty().ifBlank { "direct" },
        domains = parcel.readString().orEmpty(),
    )

    fun displayAddress(): String {
        if (type == "local") return "local"
        val host = server.trim()
        if (host.isBlank()) return type
        val formattedHost = if (host.contains(":") && !host.startsWith("[")) "[$host]" else host
        return buildString {
            append(type).append("://").append(formattedHost)
            if (serverPort > 0) append(":").append(serverPort)
            if ((type == "https" || type == "h3") && path.isNotBlank()) {
                val cleanPath = path.trim()
                if (!cleanPath.startsWith("/")) append("/")
                append(cleanPath)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(tag)
        parcel.writeString(type)
        parcel.writeLong(userOrder)
        parcel.writeByte(if (enabled) 1.toByte() else 0.toByte())
        parcel.writeString(server)
        parcel.writeInt(serverPort)
        parcel.writeString(path)
        parcel.writeString(addressResolver)
        parcel.writeString(strategy)
        parcel.writeString(detour)
        parcel.writeString(domains)
    }

    override fun describeContents(): Int = 0
}

object CustomDnsServerStore {
    fun allServers(): List<CustomDnsServerEntity> {
        val raw = DataStore.customDnsServers
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(JavaUtil.gson.fromJson(array.getJSONObject(i).toString(), CustomDnsServerEntity::class.java))
                }
            }.sortedBy { it.userOrder }
        }.getOrDefault(emptyList())
    }

    fun enabledServers(): List<CustomDnsServerEntity> = allServers().filter { it.enabled }

    fun getById(id: Long): CustomDnsServerEntity? = allServers().firstOrNull { it.id == id }

    fun save(server: CustomDnsServerEntity): CustomDnsServerEntity {
        val current = allServers().toMutableList()
        val saved = if (server.id == 0L) {
            server.copy(
                id = (current.maxOfOrNull { it.id } ?: 0L) + 1L,
                userOrder = (current.maxOfOrNull { it.userOrder } ?: 0L) + 1L,
            ).also(current::add)
        } else {
            server.also { updated ->
                val idx = current.indexOfFirst { it.id == updated.id }
                if (idx >= 0) current[idx] = updated else current.add(updated)
            }
        }
        write(current)
        return saved
    }

    fun delete(server: CustomDnsServerEntity) {
        write(allServers().filterNot { it.id == server.id })
    }

    fun replaceAll(servers: List<CustomDnsServerEntity>) {
        write(servers)
    }

    private fun write(servers: List<CustomDnsServerEntity>) {
        DataStore.customDnsServers = JSONArray().apply {
            servers.sortedBy { it.userOrder }.forEach { put(JSONObject(JavaUtil.gson.toJson(it))) }
        }.toString()
    }
}
