package org.newdawn.spaceinvaders.common.entity.boss;

import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.BaseBossShotEntity;

/**
 * 보스 엔티티가 필요로 하는 게임 환경.
 */
public interface BossEnvironment extends GameContext {
    boolean canEnemiesAttack();
    ShipEntity getShip(String playerId);
    void addScore(String playerId, int points);
    void addSkillPoints(String playerId, int points);
    void addCoins(String playerId, int amount);
    void showCoinEarned(String playerId, int x, int y, int coinAmount);
    void notifyBossDefeated(String playerId);
    void createExplosion(int x, int y, double radius);
    void notifyPlayerDamaged(String playerId, int damage);
    BaseBossShotEntity createBossShot(int x, int y, double directionX, double directionY, double speed,
                                      int radius, boolean canSplit, double splitY, int splitCount);

    default ShipEntity getShip() {
        return getShip(null);
    }

    default boolean isPlayerInvincible() {
        return isPlayerInvincible(null);
    }

    default void addScore(int points) {
        addScore(null, points);
    }

    default void addSkillPoints(int points) {
        addSkillPoints(null, points);
    }

    default void addCoins(int amount) {
        addCoins(null, amount);
    }

    default void showCoinEarned(int x, int y, int coinAmount) {
        showCoinEarned(null, x, y, coinAmount);
    }

    default void notifyBossDefeated() {
        notifyBossDefeated(null);
    }
}
