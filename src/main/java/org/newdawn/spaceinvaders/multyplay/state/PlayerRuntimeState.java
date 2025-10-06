package org.newdawn.spaceinvaders.multyplay.state;

/**
 * 런타임에서만 쓰는 간단한 플레이어 상태 (싱글 PlayerState 포크 예정)
 */
public class PlayerRuntimeState {
    public final int slot; // 0..3

    public float x;
    public float y;
    public int hp = 100;
    public int skillPoints = 0;

    public PlayerRuntimeState(int slot) { this.slot = slot; }
}
