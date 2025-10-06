package org.newdawn.spaceinvaders.multyplay.core;

import java.util.Objects;

import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;
import org.newdawn.spaceinvaders.multyplay.state.MultiGameState;

/**
 * Applies authoritative snapshots to local state and performs interpolation.
 */
public class MultiGameController {
    private final MultiGameState gameState;

    public MultiGameController(MultiGameState gameState) {
        this.gameState = gameState;
    }

    public MultiGameState getGameState() {
        return gameState;
    }

    /** Called every client frame to advance timers, effects, etc. */
    public void update(long deltaMillis) {
        gameState.advanceTime(deltaMillis);
        for (Entity entity : gameState.getEntities()) {
            entity.clientUpdate(deltaMillis);
        }
    }

    /** Applies the latest snapshot received from the server. */
    public void applySnapshot(GameSnapshotMsg snapshot) {
        if (snapshot == null) return;
        gameState.ingestSnapshot(snapshot);
    }

    /** Utility to clear local state when disconnecting. */
    public void reset() {
        gameState.reset();
    }
}
