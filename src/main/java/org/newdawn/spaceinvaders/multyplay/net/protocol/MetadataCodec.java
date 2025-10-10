package org.newdawn.spaceinvaders.multyplay.net.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple key-value metadata codec used inside entity snapshots.
 * Format: key=value pairs separated by ';', values escaped via TextMessage helpers.
 */
public final class MetadataCodec {
    private MetadataCodec() {}

    public static String encode(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                sb.append(';');
            }
            first = false;
            sb.append(TextMessage.escapeComponent(entry.getKey()));
            sb.append('=');
            sb.append(TextMessage.escapeComponent(entry.getValue()));
        }
        return sb.toString();
    }

    public static Map<String, String> decode(String metadata) {
        Map<String, String> map = new LinkedHashMap<>();
        if (metadata == null || metadata.isEmpty()) {
            return map;
        }
        String[] parts = metadata.split(";");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            int idx = part.indexOf('=');
            if (idx >= 0) {
                String key = TextMessage.unescapeComponent(part.substring(0, idx));
                String value = TextMessage.unescapeComponent(part.substring(idx + 1));
                map.put(key, value);
            } else {
                map.put(TextMessage.unescapeComponent(part), "");
            }
        }
        return map;
    }
}
