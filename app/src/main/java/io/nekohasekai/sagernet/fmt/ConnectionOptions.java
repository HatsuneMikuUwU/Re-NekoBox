package io.nekohasekai.sagernet.fmt;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;

/** sing-box 1.14/1.15 connection defaults (UDP timeout, native TLS fragmentation); explicit profile values win. */
public final class ConnectionOptions {

    private ConnectionOptions() {}

    public static String apply(
            String config,
            int udpSeconds,
            String fragmentMode,
            int fragmentDelayMs
    ) {
        int safeUdp = Math.max(0, Math.min(86400, udpSeconds));
        int safeDelay = Math.max(1, Math.min(1000, fragmentDelayMs));
        String mode = Arrays.asList("off", "record", "packet").contains(fragmentMode) ? fragmentMode : "off";

        JsonObject root = JsonParser.parseString(config).getAsJsonObject();

        if (safeUdp > 0 && root.has("inbounds")) {
            for (JsonElement element : root.getAsJsonArray("inbounds")) {
                JsonObject inbound = element.getAsJsonObject();
                if (inbound.has("type")
                        && Arrays.asList("tun", "mixed", "socks").contains(inbound.get("type").getAsString())
                        && !inbound.has("udp_timeout")) {
                    inbound.addProperty("udp_timeout", safeUdp + "s");
                }
            }
        }

        if (!"off".equals(mode) && root.has("outbounds")) {
            for (JsonElement element : root.getAsJsonArray("outbounds")) {
                JsonObject outbound = element.getAsJsonObject();
                if (!outbound.has("type") || !outbound.has("tls")) continue;
                String type = outbound.get("type").getAsString();
                if (!Arrays.asList("vmess", "vless", "trojan", "anytls", "http", "shadowtls").contains(type)) {
                    continue;
                }
                JsonObject tls = outbound.getAsJsonObject("tls");
                if (tls.has("enabled") && tls.get("enabled").getAsBoolean()
                        && !tls.has("fragment") && !tls.has("record_fragment")) {
                    tls.addProperty("record".equals(mode) ? "record_fragment" : "fragment", true);
                    if ("packet".equals(mode)) {
                        tls.addProperty("fragment_fallback_delay", safeDelay + "ms");
                    }
                }
            }
        }

        return new Gson().toJson(root);
    }
}
