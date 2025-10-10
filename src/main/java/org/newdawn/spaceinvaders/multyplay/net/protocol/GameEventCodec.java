package org.newdawn.spaceinvaders.multyplay.net.protocol;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.spaceinvaders.multyplay.net.GameEvent;

/**
 * GameEvent 리스트 직렬화/역직렬화 헬퍼.
 */
public final class GameEventCodec {
    private GameEventCodec() {}

    public static String encode(List<GameEvent> events) {
        if (events == null || events.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (GameEvent ev : events) {
            if (sb.length() > 0) sb.append(';');
            sb.append(ev.type.name()).append(',')
              .append(ev.time).append(',')
              .append(escape(ev.fromPlayerId)).append(',')
              .append(escape(ev.message));
        }
        return sb.toString();
    }

    public static List<GameEvent> decode(String payload) {
        List<GameEvent> list = new ArrayList<>();
        if (payload == null || payload.isEmpty()) {
            return list;
        }
        String[] entries = payload.split(";");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            String[] fields = entry.split(",", -1);
            GameEvent.Type type = GameEvent.Type.valueOf(get(fields, 0, GameEvent.Type.SYSTEM.name()));
            long time = parseLong(fields, 1, 0L);
            String from = unescape(get(fields, 2, ""));
            String msg = unescape(get(fields, 3, ""));
            list.add(new GameEvent(type, from.isEmpty() ? null : from, msg, time));
        }
        return list;
    }

    private static String get(String[] arr, int idx, String def) {
        return idx >= 0 && idx < arr.length ? arr[idx] : def;
    }

    private static long parseLong(String[] arr, int idx, long def) {
        try {
            return Long.parseLong(get(arr, idx, Long.toString(def)));
        } catch (Exception e) {
            return def;
        }
    }

    private static String escape(String value) {
        return TextMessage.escapeComponent(value);
    }

    private static String unescape(String value) {
        return TextMessage.unescapeComponent(value);
    }
}
