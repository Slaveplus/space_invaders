package org.newdawn.spaceinvaders.multyplay.core.authoritative;

import java.util.Collection;

import org.newdawn.spaceinvaders.multyplay.net.msg.PlayerInputMsg;

/**
 * Provides context for authoritative entities (spawning, events, player state access).
 */
public interface AuthoritativeWorld {
    void spawnEntity(AuthoritativeEntity entity);
    void removeEntity(AuthoritativeEntity entity);
    Collection<PlayerInputMsg> consumePendingInputs(String playerId);
    int getStageWidth();
    int getStageHeight();
    void onPlayerDamaged(String playerId, int amount);
    void onAlienKilled();
}
