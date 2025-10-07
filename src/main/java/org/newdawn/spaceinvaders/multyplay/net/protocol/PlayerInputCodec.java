package org.newdawn.spaceinvaders.multyplay.net.protocol;

import org.newdawn.spaceinvaders.multyplay.net.PlayerInput;

/**
 * PlayerInput 마스크 변환/재구성을 위한 헬퍼.
 */
public final class PlayerInputCodec {
    private static final int LEFT_BIT = 0;
    private static final int RIGHT_BIT = 1;
    private static final int FIRE_BIT = 2;

    private PlayerInputCodec() {}

    public static boolean isLeft(int mask) {
        return (mask & (1 << LEFT_BIT)) != 0;
    }

    public static boolean isRight(int mask) {
        return (mask & (1 << RIGHT_BIT)) != 0;
    }

    public static boolean isFire(int mask) {
        return (mask & (1 << FIRE_BIT)) != 0;
    }

    public static PlayerInput fromMask(String playerId, int mask, long clientTime, int sequence) {
        return new PlayerInput(
                playerId,
                isLeft(mask),
                isRight(mask),
                isFire(mask),
                clientTime,
                sequence
        );
    }
}
