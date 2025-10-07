package org.newdawn.spaceinvaders.multyplay.net.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 텍스트 기반 파이프/키-값 프로토콜(line-based)에 대한 범용 헬퍼.
 * 기존 서버 메시지와 동일한 escape 규칙(|,; 등)과 호환된다.
 */
public final class TextMessage {
    private final String type;
    private final Map<String, String> params;

    private TextMessage(String type, Map<String, String> params) {
        this.type = type;
        this.params = params;
    }

    public String type() {
        return type;
    }

    public Map<String, String> params() {
        return params;
    }

    public String get(String key) {
        return params.get(key);
    }

    public boolean has(String key) {
        return params.containsKey(key);
    }

    public static TextMessage parse(String line) {
        if (line == null || line.isEmpty()) {
            throw new IllegalArgumentException("empty message line");
        }
        String[] parts = line.split("\\|");
        if (parts.length == 0 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("invalid message: " + line);
        }
        String type = parts[0];
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            int idx = part.indexOf('=');
            if (idx >= 0) {
                String key = unescape(part.substring(0, idx));
                String value = unescape(part.substring(idx + 1));
                map.put(key, value);
            } else {
                map.put(unescape(part), "");
            }
        }
        return new TextMessage(type, Collections.unmodifiableMap(map));
    }

    public static Builder builder(String type) {
        return new Builder(type);
    }

    public static String format(String type, Map<String, String> params) {
        Objects.requireNonNull(type, "type");
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(type);
        if (params != null) {
            params.forEach((k, v) -> joiner.add(escape(k) + "=" + escape(v)));
        }
        return joiner.toString();
    }

    public String toLine() {
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(type);
        params.forEach((k, v) -> joiner.add(escape(k) + "=" + escape(v)));
        return joiner.toString();
    }

    public static final class Builder {
        private final String type;
        private final Map<String, String> map = new LinkedHashMap<>();

        private Builder(String type) {
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("type required");
            }
            this.type = type;
        }

        public Builder put(String key, Object value) {
            Objects.requireNonNull(key, "key");
            map.put(key, value == null ? "" : value.toString());
            return this;
        }

        public Builder putIfNotNull(String key, Object value) {
            if (value != null) {
                put(key, value);
            }
            return this;
        }

        public TextMessage build() {
            return new TextMessage(type, Collections.unmodifiableMap(new LinkedHashMap<>(map)));
        }

        public String toLine() {
            return build().toLine();
        }
    }

    public static String escapeComponent(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("%", "%25")
                .replace("|", "%7C")
                .replace(";", "%3B")
                .replace(",", "%2C")
                .replace("=", "%3D")
                .replace("\n", "%0A")
                .replace("\r", "%0D");
    }

    public static String unescapeComponent(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s
                .replace("%0D", "\r")
                .replace("%0A", "\n")
                .replace("%3D", "=")
                .replace("%2C", ",")
                .replace("%3B", ";")
                .replace("%7C", "|")
                .replace("%25", "%");
    }

    private static String escape(String s) {
        return escapeComponent(s);
    }

    private static String unescape(String s) {
        return unescapeComponent(s);
    }
}
