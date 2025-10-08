package org.newdawn.spaceinvaders.multyplay.net.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;
import org.newdawn.spaceinvaders.multyplay.net.GameSnapshot;

/**
 * GameSnapshot 및 관련 DTO를 텍스트 기반 메시지로 직렬화/역직렬화하는 헬퍼.
 * 포맷: 세미콜론(;)으로 엔트리 구분, 콤마(,)로 필드 구분.
 * 문자열 필드는 TextMessage와 동일한 escape 규칙을 사용.
 */
public final class GameSnapshotCodec {
    private GameSnapshotCodec() {}

    /**
     * 엔티티 스냅샷 목록을 직렬화.
     */
    public static String encodeEntities(List<EntitySnapshot> entities) {
        if (entities == null || entities.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (EntitySnapshot e : entities) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.id).append(',')
              .append(escape(e.type)).append(',')
              .append(escape(e.sprite)).append(',')
              .append(doubleString(e.x)).append(',')
              .append(doubleString(e.y)).append(',')
              .append(doubleString(e.dx)).append(',')
              .append(doubleString(e.dy)).append(',')
              .append(e.w).append(',')
              .append(e.h).append(',')
              .append(escape(e.ownerId)).append(',')
              .append(escape(e.metadata));
        }
        return sb.toString();
    }

    /**
     * 플레이어 스칼라 상태를 직렬화.
     */
    public static String encodePlayers(Map<String, GameSnapshot.PlayerScalarState> players) {
        if (players == null || players.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        players.forEach((playerId, state) -> {
            if (sb.length() > 0) sb.append(';');
            sb.append(escape(playerId)).append(',')
              .append(state.hp).append(',')
              .append(state.maxHp).append(',')
              .append(state.atk).append(',')
              .append(doubleString(state.aspd)).append(',')
                            .append(state.skillPts).append(',')
                            .append(state.invincibleCharges).append(',')
                            .append(state.piercingCharges).append(',')
                            .append(state.tripleShotCharges).append(',')
                            .append(state.missileCharges).append(',')
                            .append(state.invincibleRemainingMs).append(',')
                            .append(state.piercingRemainingMs).append(',')
                            .append(state.tripleShotRemainingMs).append(',')
                            .append(state.attackPowerLevel).append(',')
                            .append(state.attackSpeedLevel).append(',')
                            .append(state.hpUpLevel);
        });
        return sb.toString();
    }

    public static List<EntitySnapshot> decodeEntities(String payload) {
        List<EntitySnapshot> list = new ArrayList<>();
        if (payload == null || payload.isEmpty()) {
            return list;
        }
        String[] entries = payload.split(";");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            String[] fields = entry.split(",", -1);
            long id = parseLong(fields, 0, 0L);
            String type = unescape(get(fields, 1));
            String sprite = unescape(get(fields, 2));
            double x = parseDouble(fields, 3, 0);
            double y = parseDouble(fields, 4, 0);
            double dx = parseDouble(fields, 5, 0);
            double dy = parseDouble(fields, 6, 0);
            int w = parseInt(fields, 7, 0);
            int h = parseInt(fields, 8, 0);
            String owner = unescape(get(fields, 9));
            String meta = unescape(get(fields, 10));
            list.add(new EntitySnapshot(id, owner, type, sprite, x, y, dx, dy, w, h, meta.isEmpty() ? null : meta));
        }
        return list;
    }

    public static Map<String, GameSnapshot.PlayerScalarState> decodePlayers(String payload) {
        Map<String, GameSnapshot.PlayerScalarState> map = new LinkedHashMap<>();
        if (payload == null || payload.isEmpty()) {
            return map;
        }
        String[] entries = payload.split(";");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            String[] fields = entry.split(",", -1);
            String playerId = unescape(get(fields, 0));
            int hp = parseInt(fields, 1, 0);
            int maxHp = parseInt(fields, 2, 0);
            int atk = parseInt(fields, 3, 1);
            double aspd = parseDouble(fields, 4, 1.0);
            int skillPts = parseInt(fields, 5, 0);
            int inv = parseInt(fields, 6, 0);
            int pierce = parseInt(fields, 7, 0);
            int triple = parseInt(fields, 8, 0);
            int missile = parseInt(fields, 9, 0);
            long invRem = parseLong(fields, 10, 0L);
            long pierceRem = parseLong(fields, 11, 0L);
            long tripleRem = parseLong(fields, 12, 0L);
            int atkLvl = parseInt(fields, 13, 0);
            int aspdLvl = parseInt(fields, 14, 0);
            int hpLvl = parseInt(fields, 15, 0);
            map.put(playerId, new GameSnapshot.PlayerScalarState(hp, maxHp, atk, aspd, skillPts,
                    inv, pierce, triple, missile,
                    invRem, pierceRem, tripleRem,
                    atkLvl, aspdLvl, hpLvl));
        }
        return map;
    }

    public static String doubleString(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "0";
        }
        if (v == (long) v) {
            return Long.toString((long) v);
        }
        return String.format(java.util.Locale.US, "%.4f", v);
    }

    private static String get(String[] arr, int idx) {
        return idx >= 0 && idx < arr.length ? arr[idx] : "";
    }

    private static long parseLong(String[] arr, int idx, long def) {
        try {
            return Long.parseLong(get(arr, idx));
        } catch (Exception e) {
            return def;
        }
    }

    private static int parseInt(String[] arr, int idx, int def) {
        try {
            return Integer.parseInt(get(arr, idx));
        } catch (Exception e) {
            return def;
        }
    }

    private static double parseDouble(String[] arr, int idx, double def) {
        try {
            return Double.parseDouble(get(arr, idx));
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
