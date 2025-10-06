package org.newdawn.spaceinvaders.multyplay.core.authoritative;

/**
 * Simple upward-travelling projectile spawned by ships.
 */
public class AuthoritativeShotEntity extends AuthoritativeEntity {
    private static final double SPEED = -500.0;
    private final String ownerId;

    public AuthoritativeShotEntity(String ownerId, double x, double y) {
        super(ownerId + "_shot_" + System.nanoTime(), "shot", x, y);
        this.ownerId = ownerId;
        this.velocityY = SPEED;
        this.stateFlags = 0;
        this.collisionRadius = 6.0;
    }

    @Override
    public void tick(long deltaMillis, AuthoritativeWorld world) {
        super.tick(deltaMillis, world);
        if (y < -50) {
            world.removeEntity(this);
        }
    }

    public String getOwnerId() {
        return ownerId;
    }
}
