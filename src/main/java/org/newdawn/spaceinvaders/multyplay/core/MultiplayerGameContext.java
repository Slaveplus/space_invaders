package org.newdawn.spaceinvaders.multyplay.core;

import java.util.List;

import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.NearEntity;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;

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

    void notifyAlienKilled(String killerPlayerId, double killX, double killY);
    void notifyBossDefeated(String killerPlayerId);
    void notifyDeath(String playerId);
    boolean canEnemiesAttack();
    void notifyPlayerDamaged(String playerId, int damage);

    int getCurrentRound();
    int getPlayerAttackPower(String playerId);
    boolean isPlayerInvincible(String playerId);

    ShipEntity getShip(String playerId);
    int getShipX(String playerId);
    int getShipY(String playerId);

    List<Entity> getEntities();

    void addScore(String playerId, int points);
    void addSkillPoints(String playerId, int points);

    void onNearMonsterDestroyed(NearEntity nearEntity, String killerPlayerId, double killX, double killY);

    ShipEntity createPlayerShip(String playerId);
    Entity createNearEntity(int round, int index);
    Entity createBossEntity(int bossRound);
    void onRoundBackgroundChanged(int round);
    void addCoins(String playerId, int amount);
    void showCoinEarned(String playerId, int x, int y, int coinAmount);
}
