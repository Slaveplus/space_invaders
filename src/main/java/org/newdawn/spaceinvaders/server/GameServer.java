package org.newdawn.spaceinvaders.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.newdawn.spaceinvaders.server.game.ServerGameManager;

/** 매우 단순한 스레드형 게임 서버 (학습/프로토타입 용) */
public class GameServer implements Runnable {
    private final int port;
    private final ServerRoomManager roomManager = new ServerRoomManager();
    private final Set<ClientConnection> connections = Collections.synchronizedSet(new HashSet<>());
    private volatile boolean running = true;
    private Thread maintenanceThread;
    private final ServerGameManager gameManager = new ServerGameManager(this);
    // 타임아웃 (ms)
    private static final long SESSION_TIMEOUT = 15000; // 15초 활동 없으면 제거
    private static final long MAINT_INTERVAL = 5000;    // 5초마다 점검

    public GameServer(int port) { this.port = port; }

    public void run() {
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("[Server] Listening on port " + port);
            startMaintenance();
            while (running) {
                Socket s = ss.accept();
                System.out.println("[Server] New connection from " + s.getInetAddress() + ":" + s.getPort());
                ClientConnection cc = new ClientConnection(this, s);
                connections.add(cc);
                new Thread(cc, "client-"+s.getInetAddress()).start();
            }
        } catch (IOException e) {
            if (running) e.printStackTrace();
        }
    }

    private void startMaintenance() {
        maintenanceThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(MAINT_INTERVAL);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
                long now = System.currentTimeMillis();
                cleanupStaleRooms();
                cleanupInactiveSessions(now);
            }
        }, "Server-Maintenance");
        maintenanceThread.setDaemon(true);
        maintenanceThread.start();
    }

    private void cleanupStaleRooms() {
        synchronized (roomManager) {
            roomManager.getRoomsInternal().entrySet().removeIf(entry -> {
                Room room = entry.getValue();
                if (room.getPlayers().isEmpty() || room.getHost() == null) {
                    System.out.println("[Maint] Removing stale/empty room " + room.getId());
                    gameManager.removeSession(room.getId());
                    return true;
                }
                return false;
            });
        }
    }

    private void cleanupInactiveSessions(long now) {
        connections.removeIf(connection -> {
            try {
                PlayerSession session = connection.getSession();
                if (session == null || (now - session.getLastActivity() <= SESSION_TIMEOUT)) {
                    return false;
                }
                System.out.println("[Maint] Session timeout: " + session.getUsername());
                connection.forceClose();
                return true;
            } catch (Exception ex) {
                System.err.println("[Maint] Error during session cleanup: " + ex.getMessage());
                return true;
            }
        });
    }

    void removeConnection(ClientConnection cc) {
        connections.remove(cc);
        System.out.println("[Server] Connection removed: " + cc);
    }
    public ServerRoomManager getRoomManager() { return roomManager; }
    public ServerGameManager getGameManager() { return gameManager; }

    // 브로드캐스트 유틸
    void sendToRoom(Room room, String line) {
        for (PlayerSession ps : room.getPlayers()) {
            ps.getOut().println(line);
        }
        System.out.println("[Server] Sent to room " + room.getId() + ": " + line);
    }

    public static void main(String[] args) {
        int port = 7777;
        if (args.length>0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        GameServer server = new GameServer(port);
        new Thread(server, "GameServer").start();
    }
}
