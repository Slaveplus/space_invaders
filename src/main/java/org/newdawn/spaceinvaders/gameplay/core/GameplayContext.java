package org.newdawn.spaceinvaders.gameplay.core;

import java.util.List;

import org.newdawn.spaceinvaders.gameplay.GameStateManager;
import org.newdawn.spaceinvaders.gameplay.SkillManager;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.entity.ShipEntity;

/**
 * Minimal contract exposing the state and helpers a gameplay coordinator needs.
 *
 * NOTE: This is intentionally small – it will be gradually adopted by the
 * existing single-player and multiplayer runtimes.
 */
public interface GameplayContext {

    GameStateManager getGameStateManager();

    SkillManager getSkillManager(String playerId);

    ShipEntity getShip(String playerId);

    ShipEntity createPlayerShip(String playerId);

    Entity createAlienEntity(int x, int y);

    Entity createNearEntity(int round, int index);

    Entity createBossEntity(int bossRound);

    List<Entity> getActiveEntities();

    List<Entity> getPendingRemovals();

    void addEntity(Entity entity);

    void removeEntity(Entity entity);

    void onRoundBackgroundChanged(int round);

    void addEarnedCoins(String playerId, int amount);

    void showCoinEarned(String playerId, int x, int y, int coinAmount);

    void onGameCompleted(String message);
}
