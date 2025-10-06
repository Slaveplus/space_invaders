package org.newdawn.spaceinvaders.multyplay.net;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.newdawn.spaceinvaders.multyplay.core.authoritative.AuthoritativeGameState;
import org.newdawn.spaceinvaders.multyplay.net.GameEventCodec;
import org.newdawn.spaceinvaders.multyplay.net.SnapshotCodec;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameEventMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.PlayerInputMsg;
import org.newdawn.spaceinvaders.server.GameServer;
import org.newdawn.spaceinvaders.server.PlayerSession;
import org.newdawn.spaceinvaders.server.Room;

/**
 * Hooks multiplayer simulations into the existing GameServer.
 */
public class MultiServerAdapter {
    private static final long TICK_INTERVAL_MS = 50L; // 20 ticks per second

    private final GameServer server;
    private final Map<String, MultiServerSession> sessions = new ConcurrentHashMap<>();

    public MultiServerAdapter(GameServer server) {
        this.server = server;
        if (server != null) {
            server.setMultiAdapter(this);
        }
    }

    public void onRoomStart(String roomId) {
        if (roomId == null) return;
        Room room = server.getRoomManager().getRoom(roomId);
        MultiServerSession session = sessions.computeIfAbsent(roomId, MultiServerSession::new);
        session.start(room);
    }

    public void onRoomEnd(String roomId) {
        if (roomId == null) return;
        MultiServerSession session = sessions.remove(roomId);
        if (session != null) {
            session.shutdown();
        }
    }

    public void handleInput(String roomId, PlayerInputMsg msg) {
        MultiServerSession session = sessions.get(roomId);
        if (session != null && msg != null) {
            session.enqueueInput(msg);
        }
    }

    public void handleEvent(String roomId, GameEventMsg event) {
        MultiServerSession session = sessions.get(roomId);
        if (session != null && event != null) {
            session.broadcastEvent(event);
        }
    }

    private class MultiServerSession {
        private final String roomId;
        private final AuthoritativeGameState gameState = new AuthoritativeGameState(800, 600);
        private final ScheduledExecutorService executor;
        private volatile boolean running;

        private MultiServerSession(String roomId) {
            this.roomId = roomId;
            this.executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "multi-session-" + roomId);
                t.setDaemon(true);
                return t;
            });
        }

        void enqueueInput(PlayerInputMsg msg) {
            ensureRunning();
            gameState.registerPlayer(msg.getPlayerId());
            gameState.queueInput(msg);
        }

        void broadcastEvent(GameEventMsg event) {
            try {
                byte[] payload = GameEventCodec.encode(event);
                String line = "MULTI_EVENT|roomId=" + roomId + "|payload=" + Base64.getEncoder().encodeToString(payload);
                server.broadcastToRoom(roomId, line);
            } catch (IOException e) {
                System.out.println("[MultiServerAdapter] Failed to encode event: " + e.getMessage());
            }
        }

        void start(Room room) {
            if (running) {
                syncPlayers(room);
                return;
            }
            running = true;
            syncPlayers(room);
            gameState.spawnInitialWave(3, 6);
            executor.scheduleAtFixedRate(() -> {
                try {
                    gameState.tick(TICK_INTERVAL_MS);
                    GameSnapshotMsg snapshot = gameState.buildSnapshot(roomId);
                    sendSnapshot(snapshot);
                } catch (Exception e) {
                    System.out.println("[MultiServerAdapter] Tick failure for room " + roomId + ": " + e.getMessage());
                }
            }, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        private void ensureRunning() {
            if (!running) {
                start(server.getRoomManager().getRoom(roomId));
            }
        }

        private void syncPlayers(Room room) {
            if (room == null) {
                return;
            }
            for (PlayerSession player : room.getPlayers()) {
                gameState.registerPlayer(player.getId());
            }
        }

        private void sendSnapshot(GameSnapshotMsg snapshot) {
            try {
                byte[] payload = SnapshotCodec.encode(snapshot);
                String line = "MULTI_SNAPSHOT|roomId=" + roomId + "|payload=" + Base64.getEncoder().encodeToString(payload);
                server.broadcastToRoom(roomId, line);
            } catch (IOException e) {
                System.out.println("[MultiServerAdapter] Failed to encode snapshot: " + e.getMessage());
            }
        }

        void shutdown() {
            running = false;
            executor.shutdownNow();
            gameState.reset();
        }
    }
}
