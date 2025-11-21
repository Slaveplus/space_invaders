package org.newdawn.spaceinvaders.common.entity.projectile;

import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 미사일이 의존하는 환경.
 */
public interface MissileEnvironment extends GameContext {
    /**
     * 목표 지점에서 폭발을 생성한다.
     */
    Entity createExplosion(double targetX, double targetY);

    void onAlienHit(BaseMissileEntity missile, Entity alien);

    void onBossHit(BaseMissileEntity missile, Entity boss);
}
