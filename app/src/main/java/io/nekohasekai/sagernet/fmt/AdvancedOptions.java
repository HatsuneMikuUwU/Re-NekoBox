package io.nekohasekai.sagernet.fmt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.InetAddress;
import java.util.Arrays;

/** sing-box 1.14/1.15 advanced DNS, inbound, TLS, and cache_file options applied before core migration. */
public final class AdvancedOptions {

    private AdvancedOptions() {}

    public static class Settings {
        public boolean dnsCache = true;
        public boolean optimistic = false;
        public boolean blockAAAA = false;
        public boolean blockHttps = false;
        public boolean systemHosts = false;
        public boolean cacheFile = false;
        public int dnsTimeout = 10;
        public int dnsCapacity = 4096;
        public String bootstrap = "";
        public String cachePath = "";
        public String cacheId = "";
        public String fingerprint = "";
        public String tunDnsMode = "";
        public String tlsCertSha256 = "";
    }

    public static void validateIp(String value) {
        try {
            if (!value.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")
                    && !(value.contains(":") && value.matches("[0-9a-fA-F:.]+"))) {
                throw new IllegalArgumentException("A numeric IP address is required");
            }
            if (!value.contains(":")) {
                for (String part : value.split("\\.")) {
                    if (Integer.parseInt(part) > 255 || part.length() > 3) {
                        throw new IllegalArgumentException("Invalid IPv4 address");
                    }
                }
            }
            InetAddress.getByName(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IP address: " + value);
        }
    }

    public static String apply(String config, Settings options) {
        JsonObject root = JsonParser.parseString(config).getAsJsonObject();

        if (root.has("dns")) {
            int timeout = Math.max(1, Math.min(60, options.dnsTimeout));
            int capacity = Math.max(1024, Math.min(65536, options.dnsCapacity));
            JsonObject dns = root.getAsJsonObject("dns");
            dns.addProperty("disable_cache", !options.dnsCache);
            dns.addProperty("optimistic", options.dnsCache && options.optimistic);
            if (options.optimistic && options.dnsCache) {
                dns.remove("disable_expire");
            }
            dns.addProperty("cache_capacity", capacity);
            dns.addProperty("timeout", timeout + "s");

            JsonArray servers = array(dns, "servers");
            if (options.bootstrap != null && !options.bootstrap.isBlank()) {
                String cleanBootstrap = options.bootstrap.trim();
                validateIp(cleanBootstrap);
                JsonObject bootstrap = new JsonObject();
                bootstrap.addProperty("type", "udp");
                bootstrap.addProperty("tag", "renekobox-bootstrap");
                bootstrap.addProperty("server", cleanBootstrap);
                servers.add(bootstrap);
                for (JsonElement element : servers) {
                    JsonObject server = element.getAsJsonObject();
                    if (server.has("tag") && "dns-direct".equals(server.get("tag").getAsString())) {
                        server.addProperty("address_resolver", "renekobox-bootstrap");
                    }
                }
            }

            JsonArray rules = new JsonArray();
            if (options.blockAAAA || options.blockHttps) {
                JsonObject block = new JsonObject();
                JsonArray types = new JsonArray();
                if (options.blockAAAA) types.add("AAAA");
                if (options.blockHttps) {
                    types.add("SVCB");
                    types.add("HTTPS");
                }
                block.add("query_type", types);
                block.addProperty("action", "predefined");
                block.addProperty("rcode", "NOERROR");
                rules.add(block);
            }
            if (options.systemHosts) {
                JsonObject hosts = new JsonObject();
                hosts.addProperty("type", "hosts");
                hosts.addProperty("tag", "renekobox-system-hosts");
                JsonArray paths = new JsonArray();
                paths.add("/system/etc/hosts");
                hosts.add("path", paths);
                servers.add(hosts);
                JsonObject rule = new JsonObject();
                rule.addProperty("preferred_by", "renekobox-system-hosts");
                rule.addProperty("action", "route");
                rule.addProperty("server", "renekobox-system-hosts");
                rules.add(rule);
            }
            if (dns.has("rules")) {
                rules.addAll(dns.getAsJsonArray("rules"));
            }
            dns.add("servers", servers);
            dns.add("rules", rules);
        }

        // TUN DNS mode (sing-box 1.14+ / 1.15-alpha)
        if (options.tunDnsMode != null && !options.tunDnsMode.isBlank() && root.has("inbounds")) {
            for (JsonElement item : root.getAsJsonArray("inbounds")) {
                JsonObject inbound = item.getAsJsonObject();
                if (inbound.has("type") && "tun".equals(inbound.get("type").getAsString())) {
                    inbound.addProperty("dns_mode", options.tunDnsMode.trim());
                }
            }
        }

        // Default uTLS fingerprint & sing-box 1.15-alpha TLS leaf certificate SHA-256 pinning
        boolean hasFingerprint = options.fingerprint != null && !options.fingerprint.isBlank();
        boolean hasCertPin = options.tlsCertSha256 != null && !options.tlsCertSha256.isBlank();
        if ((hasFingerprint || hasCertPin) && root.has("outbounds")) {
            for (JsonElement element : root.getAsJsonArray("outbounds")) {
                JsonObject out = element.getAsJsonObject();
                if (!out.has("type") || !out.has("tls")) continue;
                String type = out.get("type").getAsString();
                if (!Arrays.asList("vmess", "vless", "trojan", "anytls", "http", "hysteria2", "tuic").contains(type)) {
                    continue;
                }
                JsonObject tls = out.getAsJsonObject("tls");
                if (!tls.has("enabled") || !tls.get("enabled").getAsBoolean()) continue;
                if (hasFingerprint && !tls.has("utls")
                        && Arrays.asList("vmess", "vless", "trojan", "anytls", "http").contains(type)) {
                    JsonObject utls = new JsonObject();
                    utls.addProperty("enabled", true);
                    utls.addProperty("fingerprint", options.fingerprint.trim());
                    tls.add("utls", utls);
                }
                if (hasCertPin && !tls.has("certificate_sha256")) {
                    tls.addProperty("certificate_sha256", options.tlsCertSha256.trim());
                }
            }
        }

        // Persistent core cache file for remote rule-sets & FakeIP
        if (options.cacheFile && options.cachePath != null && !options.cachePath.isBlank()) {
            JsonObject experimental = root.has("experimental")
                    ? root.getAsJsonObject("experimental")
                    : new JsonObject();
            if (!experimental.has("cache_file")) {
                JsonObject cache = new JsonObject();
                cache.addProperty("enabled", true);
                cache.addProperty("path", options.cachePath);
                if (options.cacheId != null && !options.cacheId.isBlank()) {
                    cache.addProperty("cache_id", options.cacheId);
                }
                cache.addProperty("store_fakeip", true);
                experimental.add("cache_file", cache);
            }
            root.add("experimental", experimental);
        }

        return root.toString();
    }

    private static JsonArray array(JsonObject root, String key) {
        return root.has(key) ? root.getAsJsonArray(key) : new JsonArray();
    }
}
