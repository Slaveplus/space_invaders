package org.newdawn.spaceinvaders.multyplay.net;

import org.newdawn.spaceinvaders.multyplay.state.MultiGameState;
import org.newdawn.spaceinvaders.multyplay.state.PlayerRuntimeState;
import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 멀티 클라이언트 어댑터 (스켈레톤)
 * - 서버와 TCP 연결, 입력 전송, 스냅샷 수신/디코드 보관
 * - 현재: 엔티티 재구성 로직 미구현(후속 예측/보정 단계에서 적용)
 */
public class MultiClientAdapter extends MultiNetworkAdapter {
    private final String host;
    private final int port;
    private volatile boolean running;
    private Socket socket;
    private Thread readerThread;
    private final BlockingQueue<PlayerInputMsg> outbound = new LinkedBlockingQueue<>();
    private volatile SnapshotCodec.DecodedSnapshot lastSnapshot;
    private volatile long lastSnapshotTick;

    // 간단한 entity mirror (향후 Interpolation 적용)
    private final List<EntitySnapshot> mirroredEntities = new ArrayList<>();

    // --- 예측 관련 ---
    private long nextInputSeq = 1;
    private static final int MAX_HISTORY = 128;
    private final List<PlayerInputMsg> pendingInputs = new ArrayList<>(); // 서버 미반영 입력
    private float predictedX; // slot0 가정 (MVP)
    private boolean predictedInitialized = false;
    private final Object predictionLock = new Object();

    public MultiClientAdapter(MultiGameState state, String host, int port) {
        super(state);
        this.host = host;
        this.port = port;
    }

    @Override public void start() {
        if (running) return;
        running = true;
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            startReader();
        } catch (IOException e) {
            System.err.println("[MultiClientAdapter] connect failed: " + e.getMessage());
            running = false;
        }
    }

    @Override public void stop() {
        running = false;
        if (readerThread != null) readerThread.interrupt();
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
    }

    @Override public void sendInput(PlayerInputMsg input) {
        // 시퀀스 부여
        input.clientSeq = nextInputSeq++;
        outbound.offer(input);
        // 로컬 예측 적용 (slot 0 전용 MVP)
        if(input.slot == 0) {
            applyPrediction(input);
            trackPending(input);
        }
    }

    @Override public void poll() {
        // 보내기
        flushOutbound();
        // 수신은 readerThread에서 처리
    }

    public SnapshotCodec.DecodedSnapshot getLastSnapshot() { return lastSnapshot; }
    public long getLastSnapshotTick(){ return lastSnapshotTick; }
    public List<EntitySnapshot> getMirroredEntities(){ return mirroredEntities; }
    public float getPredictedX(){ return predictedX; }

    private void flushOutbound() {
        if (socket == null || socket.isClosed()) return;
        try {
            OutputStream out = socket.getOutputStream();
            PlayerInputMsg msg;
            while ((msg = outbound.poll()) != null) {
                String line = String.format("IN|%d|%d|%d|%d|%d\n", msg.slot, msg.left?1:0, msg.right?1:0, msg.fire?1:0, msg.clientSeq);
                out.write(line.getBytes());
            }
            out.flush();
        } catch (IOException e) {
            stop();
        }
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                StringBuilder snapshotBuffer = new StringBuilder();
                while (running && (line = br.readLine()) != null) {
                    // SnapshotCodec.encode는 여러 라인 구성, 서버는 마지막에 공백 라인 없이 전송.
                    // 여기서는 SNAP 헤더로 시작하면 새로운 스냅 버퍼 초기화하고 누적.
                    if (line.startsWith("SNAP|")) {
                        if (snapshotBuffer.length() > 0) {
                            processSnapshot(snapshotBuffer.toString());
                            snapshotBuffer.setLength(0);
                        }
                        snapshotBuffer.append(line).append('\n');
                    } else if (line.startsWith("E|") || line.startsWith("P|")) {
                        snapshotBuffer.append(line).append('\n');
                    } else {
                        // 알 수 없는 라인 → 무시 (향후 EVENT 등 처리)
                    }
                }
                if (snapshotBuffer.length() > 0) {
                    processSnapshot(snapshotBuffer.toString());
                }
            } catch (IOException ignored) { }
            stop();
        }, "multi-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void processSnapshot(String text) {
        SnapshotCodec.DecodedSnapshot ds = SnapshotCodec.decode(text);
        if (ds == null) return;
        this.lastSnapshot = ds;
        this.lastSnapshotTick = ds.tick;
        mirroredEntities.clear();
        mirroredEntities.addAll(ds.entities);
        // 플레이어 좌표를 MultiGameState에 반영 (단순)
        for (GameSnapshotMsg.PlayerStateSnapshot ps : ds.players) {
            PlayerRuntimeState st = state.ensurePlayer(ps.slot);
            st.x = ps.x;
            st.y = ps.y;
            st.hp = ps.hp;
            st.skillPoints = ps.sp;
            if(ps.slot == 0){
                reconcile(ps.x);
            }
        }
    }

    // --- Prediction Helpers ---
    private void applyPrediction(PlayerInputMsg in){
        synchronized (predictionLock){
            if(!predictedInitialized){
                // 초기 상태: 서버에서 아직 스냅샷 안 받은 경우 0으로 시작
                PlayerRuntimeState p0 = state.ensurePlayer(0);
                predictedX = p0.x;
                predictedInitialized = true;
            }
            float speed = 0.4f; // 서버와 동일한 가정 (단위: tick poll update 마다)
            int dir = (in.left && !in.right) ? -1 : (in.right && !in.left) ? 1 : 0;
            predictedX += speed * dir;
            if(predictedX < 0) predictedX = 0; if(predictedX > 780) predictedX = 780;
            // 클라이언트 상태에도 반영
            PlayerRuntimeState p0 = state.ensurePlayer(0);
            p0.x = predictedX;
        }
    }

    private void trackPending(PlayerInputMsg in){
        synchronized (pendingInputs){
            pendingInputs.add(in);
            if(pendingInputs.size() > MAX_HISTORY){
                pendingInputs.remove(0);
            }
        }
    }

    private void reconcile(float serverX){
        synchronized (predictionLock){
            if(!predictedInitialized){ predictedX = serverX; predictedInitialized=true; return; }
            float error = Math.abs(serverX - predictedX);
            if(error < 0.01f){ // 오차 무시
                purgeAcknowledged();
                return;
            }
            // 서버 위치로 재설정 뒤 아직 서버 반영 안된 입력 재적용
            predictedX = serverX;
            PlayerRuntimeState p0 = state.ensurePlayer(0);
            p0.x = serverX;
            // 재적용
            java.util.List<PlayerInputMsg> copy;
            synchronized (pendingInputs){ copy = new ArrayList<>(pendingInputs); }
            for(PlayerInputMsg pi : copy){
                float before = predictedX;
                int dir = (pi.left && !pi.right) ? -1 : (pi.right && !pi.left) ? 1 : 0;
                predictedX += 0.4f * dir;
                if(predictedX < 0) predictedX = 0; if(predictedX > 780) predictedX = 780;
                if(Math.abs(predictedX - before) > 0){ p0.x = predictedX; }
            }
            purgeAcknowledged();
        }
    }

    private void purgeAcknowledged(){
        // 단순화: 서버 스냅샷마다 모든 pending 비움 (서버에서 seq ack 미구현)
        synchronized (pendingInputs){ pendingInputs.clear(); }
    }
}
