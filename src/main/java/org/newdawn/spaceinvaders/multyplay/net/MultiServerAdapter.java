package org.newdawn.spaceinvaders.multyplay.net;

import org.newdawn.spaceinvaders.multyplay.state.MultiGameState;
import org.newdawn.spaceinvaders.multyplay.core.MultiGameController;
import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 멀티 서버 어댑터 (스켈레톤)
 * - 텍스트 기반 프로토콜
 *   클라이언트 -> 서버: IN|slot|left|right|fire|seq (left/right/fire: 0 or 1)
 *   서버 -> 클라이언트: SnapshotCodec.encode() 결과 (SNAP ...)
 * - 최소 기능: 연결 / 입력 수신 / 주기적 스냅샷 브로드캐스트
 * - 고급 기능(권한검증, 지연측정, 부분 스냅샷, 이벤트 채널)은 추후 단계
 */
public class MultiServerAdapter extends MultiNetworkAdapter {
    private final MultiGameController controller;
    private final int port;
    private volatile boolean running;
    private Thread acceptThread;
    private Thread broadcastThread;

    private final Map<Integer, ClientConn> clients = new ConcurrentHashMap<>();
    private final BlockingQueue<PlayerInputMsg> inputQueue = new LinkedBlockingQueue<>();

    // 브로드캐스트 주기(ms)
    private final long snapshotIntervalMs = 100; // 10Hz 기본

    private static int NEXT_CLIENT_ID = 0;

    public MultiServerAdapter(MultiGameState state, MultiGameController controller, int port) {
        super(state);
        this.controller = controller;
        this.port = port;
    }

    @Override public void start() {
        if (running) return;
        running = true;
        startAcceptLoop();
        startBroadcastLoop();
    }

    @Override public void stop() {
        running = false;
        if (acceptThread != null) acceptThread.interrupt();
        if (broadcastThread != null) broadcastThread.interrupt();
        clients.values().forEach(ClientConn::close);
        clients.clear();
    }

    @Override public void sendInput(PlayerInputMsg input) {
        // 서버 자체에서는 사용 안함 (클라이언트 전용). 필요 시 관리자 콘솔 입력 등.
    }

    @Override public void poll() {
        // 누적된 입력을 컨트롤러에 반영 (현재 단일 플레이어 ship 직접 제어)
        PlayerInputMsg msg;
        while ((msg = inputQueue.poll()) != null) {
            state.ensurePlayer(msg.slot); // 슬롯 보장 (향후 HP/스킬) - 현재는 이동/발사만
            // 이동 처리: 간단히 즉시 반영 (예측 없음)
            int dir = (msg.left && !msg.right) ? -1 : (msg.right && !msg.left) ? 1 : 0;
            controller.playerSetHorizontal(dir);
            if (msg.fire) controller.playerTryFire();
        }
    }

    private void startAcceptLoop() {
        acceptThread = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                server.setReuseAddress(true);
                while (running) {
                    Socket socket = server.accept();
                    socket.setTcpNoDelay(true);
                    ClientConn conn = new ClientConn(socket);
                    clients.put(conn.clientId, conn);
                    conn.start();
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("[MultiServerAdapter] Accept loop terminated: " + e.getMessage());
                }
            }
        }, "multi-accept-thread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void startBroadcastLoop() {
        broadcastThread = new Thread(() -> {
            long last = System.currentTimeMillis();
            while (running) {
                long now = System.currentTimeMillis();
                if (now - last >= snapshotIntervalMs) {
                    last = now;
                    broadcastSnapshot();
                }
                try { Thread.sleep(5); } catch (InterruptedException ignored) { }
            }
        }, "multi-broadcast-thread");
        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    private void broadcastSnapshot() {
        long tick = System.currentTimeMillis();
        GameSnapshotMsg snap = controller.buildSnapshot(tick);
        List<EntitySnapshot> ents = controller.buildEntitySnapshots();
        String wire = SnapshotCodec.encode(snap, ents);
        byte[] bytes = (wire + "\n").getBytes();
        for (ClientConn c : clients.values()) {
            c.send(bytes);
        }
    }

    private class ClientConn {
        private static final int NEXT_ID_STEP = 1; // placeholder
        final int clientId;
        final Socket socket;
        final Thread readerThread;
        final OutputStream out;

        ClientConn(Socket socket) throws IOException {
            this.clientId = (NEXT_CLIENT_ID += NEXT_ID_STEP);
            this.socket = socket;
            this.out = socket.getOutputStream();
            this.readerThread = new Thread(this::runReader, "multi-client-reader-" + clientId);
        }

        void start() { readerThread.setDaemon(true); readerThread.start(); }

        void runReader() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while (running && (line = br.readLine()) != null) {
                    handleLine(line);
                }
            } catch (IOException ignored) { }
            close();
        }

        void handleLine(String line) {
            // IN|slot|l|r|f|seq
            if (line.startsWith("IN|")) {
                String[] p = line.split("\\|");
                if (p.length >= 6) {
                    try {
                        PlayerInputMsg im = new PlayerInputMsg();
                        im.slot = Integer.parseInt(p[1]);
                        im.left = "1".equals(p[2]);
                        im.right = "1".equals(p[3]);
                        im.fire = "1".equals(p[4]);
                        im.clientSeq = Long.parseLong(p[5]);
                        inputQueue.offer(im);
                    } catch (NumberFormatException ignored) { }
                }
            }
        }

        void send(byte[] bytes) {
            try { out.write(bytes); out.flush(); } catch (IOException e) { close(); }
        }

        void close() {
            try { socket.close(); } catch (IOException ignored) { }
            clients.remove(clientId);
        }
    }
}
