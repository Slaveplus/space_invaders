package org.newdawn.spaceinvaders.common.entity.near;

import org.newdawn.spaceinvaders.common.GameContext;

/**
 * 근접 몬스터가 필요로 하는 최소 게임 환경.
 */
public interface NearEnvironment extends GameContext {
    boolean canEnemiesAttack();
    void onNearMonsterDestroyed(NearEntity entity, String killerPlayerId, double killX, double killY);
}
