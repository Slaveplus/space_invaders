package org.newdawn.spaceinvaders.multyplay.net;

/**
 * 클라이언트 -> 서버 입력 메시지 (텍스트 직렬화 예정)
 */
public class PlayerInputMsg {
    public int slot; // 플레이어 슬롯
    public boolean left;
    public boolean right;
    public boolean fire;
    public long clientSeq; // 예측 rollback 대비
}
