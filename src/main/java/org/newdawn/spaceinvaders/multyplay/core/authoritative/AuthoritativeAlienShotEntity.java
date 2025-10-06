package org.newdawn.spaceinvaders.multyplay.core.authoritative;

/**
 * Downward projectile fired by aliens.
 */
public class AuthoritativeAlienShotEntity extends AuthoritativeEntity {
    private static final double SPEED = 200.0;

    public AuthoritativeAlienShotEntity(double x, double y) {
        super("alien_shot_" + System.nanoTime(), "boss_shot", x, y);
        this.velocityY = SPEED;
        this.collisionRadius = 10.0;
    }

    @Override
    public void tick(long deltaMillis, AuthoritativeWorld world) {
        super.tick(deltaMillis, world);
        if (y > world.getStageHeight() + 50) {
            world.removeEntity(this);
        }
    }
}
