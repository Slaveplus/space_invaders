package org.newdawn.spaceinvaders.multyplay.core;

import org.newdawn.spaceinvaders.multyplay.entity.*;
import java.util.List;

/**
 * 멀티 엔티티들이 상호작용할 최소 게임 런타임 인터페이스.
 * (싱글 Game 대체용) 후속 단계에서 MultiGameController가 구현 예정.
 */
public interface MultiGameRuntime {
    int getCurrentRound();
    boolean isPlayerInvincible();
    void notifyDeath();
    void notifyAlienKilled();
    void notifyBossDefeated();
    void addAimedAlienShot(int x, int y, int sourceX);
    void addEntity(Entity e);
    void removeEntity(Entity e);
    void addSkillToInventory(int type, int value);
    int getPlayerAttackPower();
    boolean hasPiercingShots();
    int getShipX();
    int getShipY();
    ShipEntity getShip();
    void createExplosion(int x, int y, double radius);
    void addScore(int score);
    void addSkillPoints(int points);
    List<Entity> getEntities();
}
