package org.newdawn.spaceinvaders.multyplay.core;

import java.util.List;

import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;

/**
 * 게임 엔티티/스킬 시스템이 상호작용해야 하는 최소한의 게임 컨텍스트.
 * Canvas 기반 구현과 서버 사이에서 공통으로 사용된다.
 */
public interface MultiplayerGameContext {
    MultiplayerGameStateManager getGameStateManager();
    MultiplayerSkillManager getSkillManager(String playerId);

    void addEntity(Entity entity);
    void removeEntity(Entity entity);

    void addAimedAlienShot(int x, int y, int alienX);
    void addSkillToInventory(String playerId, int skillType, int skillValue);
    void createSkillDrop(int x, int y, int skillType, int skillValue);
    void createExplosion(int x, int y, double radius);
    void fireMissile(String playerId, double targetX, double targetY);

    void notifyAlienKilled(String killerPlayerId);
    void notifyBossDefeated(String killerPlayerId);
    void notifyDeath(String playerId);

    int getCurrentRound();
    int getPlayerAttackPower(String playerId);
    boolean hasPiercingShots(String playerId);
    boolean isPlayerInvincible(String playerId);

    Entity getShip(String playerId);
    int getShipX(String playerId);
    int getShipY(String playerId);

    List<Entity> getEntities();

    void addScore(String playerId, int points);
    void addSkillPoints(String playerId, int points);
}
