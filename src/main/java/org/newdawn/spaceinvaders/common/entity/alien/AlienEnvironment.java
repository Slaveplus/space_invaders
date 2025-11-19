package org.newdawn.spaceinvaders.common.entity.alien;

import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * Alien 엔티티가 필요로 하는 게임 환경.
 */
public interface AlienEnvironment {
    int getCurrentRound();

    void addAimedAlienShot(int x, int y, int alienX);

    void removeEntity(Entity entity);

    void notifyAlienKilled();

    int getPlayerShipX();

    int getPlayerShipY();
}
