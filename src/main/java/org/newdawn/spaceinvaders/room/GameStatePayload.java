package org.newdawn.spaceinvaders.room;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;

import org.newdawn.spaceinvaders.multyplay.net.GameSnapshot;
import org.newdawn.spaceinvaders.multyplay.net.protocol.GameSnapshotCodec;
import org.newdawn.spaceinvaders.multyplay.net.protocol.ProtocolKeys;
import org.newdawn.spaceinvaders.multyplay.net.protocol.TextMessage;
/**
 * 
 * 서버에서 전달되는 GAME_STATE 메시지를 파싱해 제공하는 DTO.
 */
public final class GameStatePayload {
    public final String roomId;
    public final GameSnapshot snapshot;
    public final List<EntitySnapshot> entities;
    public final Map<String, GameSnapshot.PlayerScalarState> players;

    private GameStatePayload(String roomId, GameSnapshot snapshot,
                             List<EntitySnapshot> entities,
                             Map<String, GameSnapshot.PlayerScalarState> players) {
        this.roomId = roomId;
        this.snapshot = snapshot;
        this.entities = entities;
        this.players = players;
    }

    private static Map<String, Boolean> decodeReady(String payload) {
        if (payload == null || payload.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Boolean> map = new LinkedHashMap<>();
        String[] entries = payload.split(";");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            int idx = entry.indexOf('=');
            if (idx <= 0) continue;
            String id = TextMessage.unescapeComponent(entry.substring(0, idx));
            boolean ready = idx + 1 < entry.length() && entry.charAt(idx + 1) == '1';
            map.put(id, ready);
        }
        return map;
    }

    public static GameStatePayload fromLine(String line) {
        try {
            TextMessage msg = TextMessage.parse(line);
            String roomId = msg.get("roomId");
            long tick = parseLong(msg.get("tick"));
            long delta = parseLong(msg.get("dt"));
            long serverTime = parseLong(msg.get("time"));
            int round = parseInt(msg.get("round"));
    
            List<EntitySnapshot> entities = GameSnapshotCodec.decodeEntities(msg.get("entities"));
            Map<String, GameSnapshot.PlayerScalarState> players = GameSnapshotCodec.decodePlayers(msg.get("players"));
            String phaseStr = msg.get(ProtocolKeys.PHASE);
            GameSnapshot.Phase phase = phaseStr != null ? GameSnapshot.Phase.valueOf(phaseStr) : GameSnapshot.Phase.ACTIVE;
            boolean waitingForPlayers = "1".equals(msg.get(ProtocolKeys.WAITING));
            Map<String, Boolean> readyStates = decodeReady(msg.get(ProtocolKeys.READY));
            String message = msg.get(ProtocolKeys.MESSAGE);

            GameSnapshot snapshot = new GameSnapshot(tick, serverTime, delta, round, entities, players,
                    phase, waitingForPlayers, readyStates, message);
            return new GameStatePayload(roomId, snapshot,
                    Collections.unmodifiableList(entities),
                    Collections.unmodifiableMap(players));
        } catch (Exception ex) {
            System.err.println("[GameStatePayload] Failed to parse GAME_STATE: " + ex.getMessage());
            return null;
        }
    }
    
    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }
    
    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
