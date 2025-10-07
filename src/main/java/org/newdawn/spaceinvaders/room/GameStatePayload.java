package org.newdawn.spaceinvaders.room;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;

import org.newdawn.spaceinvaders.multyplay.net.GameSnapshot;
import org.newdawn.spaceinvaders.multyplay.net.protocol.GameSnapshotCodec;
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
            GameSnapshot snapshot = new GameSnapshot(tick, serverTime, delta, round, entities, players);
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