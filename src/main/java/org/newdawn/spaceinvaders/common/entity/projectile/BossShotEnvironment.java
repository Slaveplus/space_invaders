package org.newdawn.spaceinvaders.common.entity.projectile;

import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * BossShot이 상호작용해야 하는 게임 환경.
 */
public interface BossShotEnvironment {
    void addEntity(Entity entity);

    void removeEntity(Entity entity);

    void notifyPlayerHit(String ownerId);
}
