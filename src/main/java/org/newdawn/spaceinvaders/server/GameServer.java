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
                try { Thread.sleep(MAINT_INTERVAL); } catch (InterruptedException ignored) {}
                long now = System.currentTimeMillis();
                // 고아 세션/방 정리: rooms 맵 접근
                synchronized (roomManager) {
                    // 방 순회하며 호스트가 null 이거나 빈 방 제거
                    roomManager.getRoomsInternal().entrySet().removeIf(e -> {
                        Room r = e.getValue();
                        boolean remove = r.getPlayers().isEmpty() || r.getHost()==null;
                        if (remove) {
                            System.out.println("[Maint] Removing stale/empty room " + r.getId());
                            gameManager.removeSession(r.getId());
                        }
                        return remove;
                    });
                }
                // 세션 타임아웃: 연결 목록 복사 후 검사
                connections.removeIf(cc -> {
                    try {
                        PlayerSession ps = cc.getSession();
                        if (ps == null) return false;
                        if (now - ps.getLastActivity() > SESSION_TIMEOUT) {
                            System.out.println("[Maint] Session timeout: " + ps.getUsername());
                            cc.forceClose();
                            return true;
                        }
                    } catch (Exception ex) {
                        return true;
                    }
                    return false;
                });
            }
        }, "Server-Maintenance");
        maintenanceThread.setDaemon(true);
        maintenanceThread.start();
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
