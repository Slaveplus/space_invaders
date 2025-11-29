package org.newdawn.spaceinvaders.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.newdawn.spaceinvaders.multyplay.net.protocol.TextMessage;

/**
 * 서버에서 전달되는 GAME_INIT 메시지를 파싱해 보관하는 DTO.
 */
public final class GameInitInfo {
    public final String roomId;
    public final long seed;
    public final long initialTick;
    public final List<Player> players;

    private GameInitInfo(String roomId, long seed, long initialTick, List<Player> players) {
        this.roomId = roomId;
        this.seed = seed;
        this.initialTick = initialTick;
        this.players = players;
    }
    
    public static GameInitInfo fromLine(String line) {
        try {
            TextMessage msg = TextMessage.parse(line);
            String roomId = msg.get("roomId");
            long seed = parseLong(msg.get("seed"));
            long tick = parseLong(msg.get("tick"));
            String playersField = msg.get("players");
            List<Player> players = parsePlayers(playersField);
            return new GameInitInfo(roomId, seed, tick, players);
        } catch (Exception ex) {
            System.err.println("[GameInitInfo] Failed to parse GAME_INIT: " + ex.getMessage());
            return null;
        }
    }
    
    private static List<Player> parsePlayers(String payload) {
        if (payload == null || payload.isEmpty()) {
            return Collections.emptyList();
        }
        List<Player> list = new ArrayList<>();
        String[] entries = payload.split(";");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            String[] fields = entry.split(",", -1);
            String id = unescape(get(fields, 0));
            String username = unescape(get(fields, 1));
            list.add(new Player(id, username));
        }
        return Collections.unmodifiableList(list);
    }
    
    private static String get(String[] arr, int idx) {
        return idx >= 0 && idx < arr.length ? arr[idx] : "";
    }
    
    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }
    
    private static String unescape(String value) {
        return TextMessage.unescapeComponent(value);
    }
    
    public static final class Player {
        public final String id;
        public final String username;
        
        private Player(String id, String username) {
            this.id = id;
            this.username = username;
        }
    }
}