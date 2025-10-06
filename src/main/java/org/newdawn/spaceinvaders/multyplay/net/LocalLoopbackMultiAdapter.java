package org.newdawn.spaceinvaders.multyplay.net;

import org.newdawn.spaceinvaders.multyplay.state.MultiGameState;
import org.newdawn.spaceinvaders.multyplay.state.PlayerRuntimeState;
import org.newdawn.spaceinvaders.multyplay.core.MultiGameController;
import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;

/**
 * 네트워크 없는 단일 JVM 시뮬레이션 (테스트/디버그)
 */
public class LocalLoopbackMultiAdapter extends MultiNetworkAdapter {
    private long tick;
    private final MultiGameController controller;
    private String lastWire;
    private SnapshotCodec.DecodedSnapshot lastDecoded;

    public LocalLoopbackMultiAdapter(MultiGameState state, MultiGameController controller) { super(state); this.controller = controller; }

    @Override public void start() { tick = 0; }
    @Override public void stop() { }

    @Override public void sendInput(PlayerInputMsg input) {
        PlayerRuntimeState p = state.ensurePlayer(input.slot);
        float speed = 0.4f; // placeholder
        if (input.left) p.x -= speed;
        if (input.right) p.x += speed;
        if (p.x < 0) p.x = 0; // 경계 간단 처리
        if (p.x > 780) p.x = 780; // 임시 값
        // fire 처리는 추후
    }

    @Override public void poll() {
        tick++;
        if (controller == null) {
            return; // 아직 초기화되지 않은 경우 (방어)
        }
        // 서버 시뮬레이션: 현재 컨트롤러 상태 스냅샷 생성
        GameSnapshotMsg snap = controller.buildSnapshot(tick);
        java.util.List<EntitySnapshot> ents = controller.buildEntitySnapshots();
        lastWire = SnapshotCodec.encode(snap, ents);
        lastDecoded = SnapshotCodec.decode(lastWire);
    }

    public String getLastWire(){ return lastWire; }
    public SnapshotCodec.DecodedSnapshot getLastDecoded(){ return lastDecoded; }
}
