package org.newdawn.spaceinvaders.multyplay.net;

/**
 * 프레임(또는 틱) 단위로 전송되는 플레이어 입력.
 */
public class PlayerInput {
    public final String playerId;
    public final boolean left;
    public final boolean right;
    public final boolean fire;
    public final long clientTime;
    public final int sequence;
    public final int mask;

    public PlayerInput(String playerId, boolean left, boolean right, boolean fire, long clientTime) {
        this(playerId, left, right, fire, clientTime, 0);
    }

    public PlayerInput(String playerId, boolean left, boolean right, boolean fire, long clientTime, int sequence) {
        this.playerId = playerId;
        this.left = left;
        this.right = right;
        this.fire = fire;
        this.clientTime = clientTime;
        this.sequence = sequence;
        this.mask = computeMask(left, right, fire);
    }

    public static int computeMask(boolean left, boolean right, boolean fire) {
        int value = 0;
        if (left) value |= 1;
        if (right) value |= 1 << 1;
        if (fire) value |= 1 << 2;
        return value;
    }
}
