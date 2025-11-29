package org.newdawn.spaceinvaders.common.entity.projectile;

import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 공용 미사일 엔티티 구현체.
 */
public class MissileEntity extends BaseMissileEntity {
    public MissileEntity(MissileEnvironment environment,
                         String sprite,
                         int x,
                         int y,
                         double targetX,
                         double targetY) {
        super(environment, sprite, x, y, targetX, targetY);
    }

    @Override
    protected void applyAlienDamage(Entity alien) {
        environment.onAlienHit(this, alien);
        used = true;
    }

    @Override
    protected void applyBossDamage(Entity boss) {
        environment.onBossHit(this, boss);
        used = true;
    }
}
