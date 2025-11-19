package org.newdawn.spaceinvaders.server.game;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.newdawn.spaceinvaders.server.GameServer;
import org.newdawn.spaceinvaders.server.PlayerSession;
import org.newdawn.spaceinvaders.server.Room;

/**
 * 방마다 생성되는 ServerGameSession을 추적/관리한다.
 */
public class ServerGameManager {
    private final GameServer server;
    private final Map<String, ServerGameSession> sessions = new ConcurrentHashMap<>();

    public ServerGameManager(GameServer server) {
        this.server = server;
    }

    public ServerGameSession createSession(Room room) {
        return sessions.compute(room.getId(), (id, existing) -> {
            if (existing != null) {
                existing.shutdown();
            }
            ServerGameSession session = new ServerGameSession(server, room);
            session.startHandshake();
            return session;
        });
    }

    public ServerGameSession getSession(String roomId) {
        return sessions.get(roomId);
    }

    public void removeSession(String roomId) {
        ServerGameSession session = sessions.remove(roomId);
        if (session != null) {
            session.shutdown();
        }
    }

    public void handlePlayerDeparture(PlayerSession session, Room room) {
        if (room == null) {
            return;
        }
        ServerGameSession gameSession = sessions.get(room.getId());
        if (gameSession != null) {
            gameSession.handlePlayerLeft(session);
        }
    }
}
