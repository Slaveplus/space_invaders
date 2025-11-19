package org.newdawn.spaceinvaders.common.entity.projectile;

import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 싱글/멀티 공용 미사일 구현.
 */
public abstract class BaseMissileEntity extends Entity {
    protected final MissileEnvironment environment;
    protected final double targetX;
    protected final double targetY;
    protected double speed = 400;
    protected boolean used = false;

    protected BaseMissileEntity(MissileEnvironment environment,
                                String sprite,
                                int x,
                                int y,
                                double targetX,
                                double targetY) {
        super(sprite, x, y);
        this.environment = environment;
        this.targetX = targetX;
        this.targetY = targetY;

        double dxToTarget = targetX - x;
        double dyToTarget = targetY - y;
        double distance = Math.sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget);
        if (distance > 0) {
            this.dx = (dxToTarget / distance) * speed;
            this.dy = (dyToTarget / distance) * speed;
        } else {
            this.dx = 0;
            this.dy = -speed;
        }
    }

    @Override
    public void move(long delta) {
        super.move(delta);

        double distanceToTarget = Math.sqrt((targetX - x) * (targetX - x) + (targetY - y) * (targetY - y));
        if (distanceToTarget < 30 || y < -100 || y > 700 || x < -100 || x > 800) {
            Entity explosion = environment.createExplosion(targetX, targetY);
            if (explosion != null) {
                environment.addEntity(explosion);
            }
            environment.removeEntity(this);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (used) {
            return;
        }
        if (isAlien(other)) {
            applyAlienDamage(other);
        } else if (isBoss(other)) {
            applyBossDamage(other);
        }
    }

    protected abstract void applyAlienDamage(Entity alien);

    protected abstract void applyBossDamage(Entity boss);

    protected boolean isAlien(Entity entity) {
        return entity.getClass().getSimpleName().equals("AlienEntity")
                || entity instanceof org.newdawn.spaceinvaders.common.entity.alien.BaseAlienEntity;
    }

    protected boolean isBoss(Entity entity) {
        return entity.getClass().getSimpleName().equals("BossEntity");
    }
}
