package io.nekohasekai.sagernet.fmt

import io.nekohasekai.sagernet.database.CustomDnsServerEntity
import io.nekohasekai.sagernet.ktx.isIpAddress
import moe.matsuri.nb4a.SingBoxOptions.DNSRule_DefaultOptions
import moe.matsuri.nb4a.SingBoxOptions.DNSServerOptions
import moe.matsuri.nb4a.checkEmpty
import moe.matsuri.nb4a.makeSingBoxRule
import moe.matsuri.nb4a.utils.listByLineOrComma

val SUPPORTED_CUSTOM_DNS_TYPES = listOf("udp", "tcp", "tls", "quic", "https", "h3", "local")
val CUSTOM_DNS_RESOLVERS = listOf("", "dns-direct", "dns-remote", "dns-local")
val CUSTOM_DNS_STRATEGIES = listOf("", "prefer_ipv4", "prefer_ipv6", "ipv4_only", "ipv6_only")

fun normalizeCustomDnsDetour(detour: String): String =
    if (detour.trim() == TAG_PROXY) TAG_PROXY else TAG_DIRECT

fun validateCustomDnsServer(
    server: CustomDnsServerEntity,
    existing: List<CustomDnsServerEntity>,
): String? {
    val tag = server.tag.trim()
    if (tag.isBlank()) return "DNS server tag is required"
    if (tag in CustomDnsServerEntity.RESERVED_TAGS) return "DNS server tag is reserved"
    if (server.type !in SUPPORTED_CUSTOM_DNS_TYPES) return "Unsupported DNS server type"
    if (server.type != "local" && server.server.trim().isBlank()) return "DNS server address is required"
    if (server.serverPort !in 0..65535) return "DNS server port must be 0..65535"
    if (existing.any { it.id != server.id && it.tag.equals(tag, ignoreCase = true) }) {
        return "DNS server tag already exists"
    }
    return null
}

fun buildCustomDnsServerOptions(server: CustomDnsServerEntity): DNSServerOptions {
    val host = server.server.trim()
    return DNSServerOptions().apply {
        tag = server.tag.trim()
        address = server.displayAddress()
        detour = normalizeCustomDnsDetour(server.detour)
        val resolver = server.addressResolver.trim()
        if (resolver.isNotEmpty()) {
            address_resolver = resolver
        } else if (server.type != "local" && host.isNotEmpty() && !host.isIpAddress()) {
            address_resolver = "dns-direct"
        }
        val st = server.strategy.trim()
        if (st.isNotEmpty()) {
            strategy = st
        }
    }
}

fun buildCustomDnsDomainRule(server: CustomDnsServerEntity): DNSRule_DefaultOptions? {
    val domainEntries = server.domains.listByLineOrComma()
    if (domainEntries.isEmpty()) return null
    return DNSRule_DefaultOptions().apply {
        makeSingBoxRule(domainEntries)
        this.server = server.tag.trim()
    }.takeUnless { it.checkEmpty() }
}
