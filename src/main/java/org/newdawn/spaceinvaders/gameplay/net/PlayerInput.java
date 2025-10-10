package org.newdawn.spaceinvaders.gameplay.net;

/**
 * 프레임(또는 틱) 단위로 전송되는 플레이어 입력.
 */
public class PlayerInput {
    public final String playerId;
    public final boolean left;
    public final boolean right;
    public final boolean fire;
    public final long clientTime;

    public PlayerInput(String playerId, boolean left, boolean right, boolean fire, long clientTime) {
        this.playerId = playerId; this.left = left; this.right = right; this.fire = fire; this.clientTime = clientTime;
    }
}
