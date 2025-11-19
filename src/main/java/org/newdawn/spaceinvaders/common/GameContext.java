package org.newdawn.spaceinvaders.common;

import java.util.List;

import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 싱글/멀티 공통으로 엔티티와 상호작용하는 컨텍스트 추상화.
 */
public interface GameContext {
    void addEntity(Entity entity);
    void removeEntity(Entity entity);
    List<Entity> getEntities();

    void createHeatEffect(int x, int y, double size);

    default Entity createExplosion(double x, double y) { return null; }

    int getPlayerAttackPower(String ownerId);
    boolean isPlayerInvincible(String ownerId);
    default int getPlayerShipX(String ownerId) { return 0; }
    default int getPlayerShipY(String ownerId) { return 0; }

    void notifyAlienKilled(String ownerId, double x, double y);
    void notifyPlayerDamaged(String ownerId, int damage);
    void notifyDeath(String ownerId);

    default void addSkillToInventory(String ownerId, int skillType, int skillValue) {
        // optional
    }

    default void notifyAlienKilled() {
        notifyAlienKilled(null, Double.NaN, Double.NaN);
    }

    default void notifyPlayerDamaged(int damage) {
        notifyPlayerDamaged(null, damage);
    }

    default void notifyDeath() {
        notifyDeath(null);
    }
}
