package org.newdawn.spaceinvaders.multyplay.net.msg;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class PlayerInputMsg {
    private final String playerId;
    private final int sequence;
    private final Set<Integer> pressedKeys;
    private final int cursorX;
    private final int cursorY;
    private final boolean firing;

    public PlayerInputMsg(String playerId,
                          int sequence,
                          Set<Integer> pressedKeys,
                          int cursorX,
                          int cursorY,
                          boolean firing) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.sequence = sequence;
        if (pressedKeys == null || pressedKeys.isEmpty()) {
            this.pressedKeys = Collections.emptySet();
        } else {
            this.pressedKeys = Collections.unmodifiableSet(new HashSet<>(pressedKeys));
        }
        this.cursorX = cursorX;
        this.cursorY = cursorY;
        this.firing = firing;
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getSequence() {
        return sequence;
    }

    public Set<Integer> getPressedKeys() {
        return pressedKeys;
    }

    public int getCursorX() {
        return cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    public boolean isFiring() {
        return firing;
    }
}
