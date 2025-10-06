package org.newdawn.spaceinvaders.multyplay.net;

import org.newdawn.spaceinvaders.multyplay.state.MultiGameState;

/**
 * 서버/클라이언트 공통 추상 어댑터. 텍스트 프로토콜 (| 구분) 전제.
 */
public abstract class MultiNetworkAdapter {
    protected final MultiGameState state;

    protected MultiNetworkAdapter(MultiGameState state) {
        this.state = state;
    }

    public abstract void start();
    public abstract void stop();

    /** 로컬 입력 전송 */
    public abstract void sendInput(PlayerInputMsg input);

    /** 네트워크 펌프(수신 처리) - Game Loop 내 주기 호출 */
    public abstract void poll();
}
