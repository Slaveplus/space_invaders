package org.newdawn.spaceinvaders.multyplay.system;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import org.newdawn.spaceinvaders.multyplay.core.MultiGameController;

/**
 * 멀티플레이 입력 매니저 (로컬 테스트 전용)
 * - 최대 4 플레이어 슬롯 전제, 현재는 slot 0 만 활성.
 * - 키 매핑 (slot0): LEFT/RIGHT 이동, SPACE 발사
 * - 확장 포인트: 네트워크 입력(or gamepad) 합성 -> per-slot InputState
 */
public class MultiInputManager {
    public static final int MAX_PLAYERS = 4;

    /** 플레이어별 입력 상태 */
    public static class InputState {
        public boolean left;
        public boolean right;
        public boolean fire;
        public int horizontalAxis() { return (left && right) ? 0 : (left ? -1 : (right ? 1 : 0)); }
        public void clear() { left = right = fire = false; }
    }

    private final InputState[] playerInputs = new InputState[MAX_PLAYERS];
    private final MultiGameController controller;

    public MultiInputManager(MultiGameController controller){
        this.controller = controller;
        for(int i=0;i<MAX_PLAYERS;i++){ playerInputs[i] = new InputState(); }
    }

    /**
     * 로컬 키 입력 처리 (현재 slot 0 전용)
     */
    public class KeyHandler extends KeyAdapter {
        @Override public void keyPressed(KeyEvent e){ applyKey(e.getKeyCode(), true); }
        @Override public void keyReleased(KeyEvent e){ applyKey(e.getKeyCode(), false); }
    }

    private void applyKey(int keyCode, boolean pressed){
        InputState s0 = playerInputs[0];
        switch(keyCode){
            case KeyEvent.VK_LEFT: s0.left = pressed; break;
            case KeyEvent.VK_RIGHT: s0.right = pressed; break;
            case KeyEvent.VK_SPACE: s0.fire = pressed; break;
            default: /* ignore */
        }
    }

    /** 틱 시작 시 입력을 게임 로직에 반영 */
    public void applyToController(){
        InputState s0 = playerInputs[0];
        controller.playerSetHorizontal(s0.horizontalAxis());
        if(s0.fire){ controller.playerTryFire(); }
    }

    /** 네트워크로부터 수신한 입력을 머지하는 확장 포인트 */
    public void mergeRemoteInput(int playerSlot, boolean left, boolean right, boolean fire){
        if(playerSlot < 0 || playerSlot >= MAX_PLAYERS) return;
        InputState st = playerInputs[playerSlot];
        st.left = left; st.right = right; st.fire = fire;
    }

    public InputState getState(int slot){ return (slot>=0 && slot<MAX_PLAYERS)? playerInputs[slot]: null; }

    public void resetAll(){ Arrays.stream(playerInputs).forEach(InputState::clear); }
}
