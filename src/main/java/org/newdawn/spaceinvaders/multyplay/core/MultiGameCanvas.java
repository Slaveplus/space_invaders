package org.newdawn.spaceinvaders.multyplay.core;

import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.multyplay.state.MultiGameState;
import org.newdawn.spaceinvaders.multyplay.net.MultiNetworkAdapter;

import org.newdawn.spaceinvaders.multyplay.system.MultiInputManager;
import org.newdawn.spaceinvaders.multyplay.ui.MultiBackgroundRenderer;
import org.newdawn.spaceinvaders.multyplay.ui.MultiUIRenderer;
import org.newdawn.spaceinvaders.multyplay.net.LocalLoopbackMultiAdapter;
import org.newdawn.spaceinvaders.multyplay.net.SnapshotCodec;

import java.awt.*;

/**
 * 멀티플레이 전용 Game Canvas (싱글 Game.java Fork 예정)
 * - 현재는 뼈대: 이후 싱글 로직 복사 + 멀티 적용
 */
public class MultiGameCanvas extends Canvas implements Screen {
    private final MultiGameState state; // 향후 UI/HUD 렌더링에 사용 예정
    private final MultiGameController controller = new MultiGameController();
    private final MultiInputManager inputManager = new MultiInputManager(controller);
    private final MultiBackgroundRenderer backgroundRenderer = new MultiBackgroundRenderer();
    private final MultiUIRenderer uiRenderer = new MultiUIRenderer(controller);
    private MultiNetworkAdapter network;
    private boolean initialized;

    public MultiGameCanvas(MultiGameState state) {
        this.state = state;
        setIgnoreRepaint(true);
        setFocusable(true);
        setBackground(Color.BLACK);
    }

    public void setNetworkAdapter(MultiNetworkAdapter adapter) { this.network = adapter; }

    @Override public void init() {
        if(!initialized){
            controller.startNewGame();
            // 기본 로컬 루프백 어댑터 자동 부착 (외부에서 설정되지 않은 경우)
            if(this.network == null){
                this.network = new LocalLoopbackMultiAdapter(state, controller);
                this.network.start();
            }
            addKeyListener(inputManager.new KeyHandler());
            // state 객체가 현재 단계에서는 직접 사용되지 않지만 추후 HUD/플레이어 슬롯 표시 등에 활용 예정
            if(state == null) {
                throw new IllegalStateException("MultiGameState 주입 실패");
            }
            initialized=true;
        }
    }
    @Override public void onShow() { requestFocusInWindow(); }
    @Override public void onHide() { /* cleanup if needed */ }

    @Override public void update(long delta) {
        // 입력 상태 -> 컨트롤러 반영
        inputManager.applyToController();
        controller.tick(delta);
        if(network!=null){ network.poll(); }
        // 디코드된 최신 스냅샷을 활용한 보정(placeholder)
        if(network instanceof LocalLoopbackMultiAdapter){
            LocalLoopbackMultiAdapter ll = (LocalLoopbackMultiAdapter) network;
            SnapshotCodec.DecodedSnapshot ds = ll.getLastDecoded();
            if(ds != null){
                // 현재는 단일 플레이어만: 서버 좌표 강제 동기화(예측 없음)
                if(!ds.players.isEmpty() && controller.getShip()!=null){
                    controller.getShip().setHorizontalMovement(0); // 보정 시 순간 이동만 수행
                    controller.getShip().setVerticalMovement(0);
                    org.newdawn.spaceinvaders.multyplay.net.GameSnapshotMsg.PlayerStateSnapshot ps = ds.players.get(0);
                    controller.getShip().setPosition(ps.x, ps.y);
                }
            }
        }
    }

    @Override public void render(Graphics2D g) {
        backgroundRenderer.draw(g);
        for(org.newdawn.spaceinvaders.multyplay.entity.Entity e : controller.getEntities()) { e.draw(g); }
        uiRenderer.drawHUD(g);
    }
}
