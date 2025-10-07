package org.newdawn.spaceinvaders.room;

import java.util.Collections;
import java.util.List;

import org.newdawn.spaceinvaders.multyplay.net.GameEvent;
import org.newdawn.spaceinvaders.multyplay.net.protocol.GameEventCodec;
import org.newdawn.spaceinvaders.multyplay.net.protocol.TextMessage;

/**
 * 서버에서 전달되는 GAME_EVENT 메시지를 파싱하는 DTO.
 */
public final class GameEventPayload {
    public final String roomId;
    public final long tick;
    public final List<GameEvent> events;

    private GameEventPayload(String roomId, long tick, List<GameEvent> events) {
        this.roomId = roomId;
        this.tick = tick;
        this.events = events;
    }

    public static GameEventPayload fromLine(String line) {
        try {
            TextMessage msg = TextMessage.parse(line);
            String roomId = msg.get("roomId");
            long tick = parseLong(msg.get("tick"));
            List<GameEvent> events = GameEventCodec.decode(msg.get("events"));
            return new GameEventPayload(roomId, tick, Collections.unmodifiableList(events));
        } catch (Exception ex) {
            System.err.println("[GameEventPayload] Failed to parse GAME_EVENT: " + ex.getMessage());
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
}