package org.newdawn.spaceinvaders.multyplay.core.authoritative;

/**
 * Simple marching alien enemy used in the server simulation prototype.
 */
public class AuthoritativeAlienEntity extends AuthoritativeEntity {
    private static final double HORIZONTAL_SPEED = 80.0;
    private static final double DESCENT = 20.0;
    private static final long FIRE_COOLDOWN_MS = 2000L;

    private double direction = 1.0;
    private long lastFireTimestamp;

    public AuthoritativeAlienEntity(double x, double y) {
        super("alien", x, y);
        this.velocityX = HORIZONTAL_SPEED;
        this.maxHp = 1;
        this.hp = 1;
        this.collisionRadius = 18.0;
    }

    @Override
    public void tick(long deltaMillis, AuthoritativeWorld world) {
        super.tick(deltaMillis, world);
        int stageWidth = world.getStageWidth();
        if ((direction < 0 && x < 20) || (direction > 0 && x > stageWidth - 40)) {
            direction *= -1;
            velocityX = HORIZONTAL_SPEED * direction;
            y += DESCENT;
        }

        long now = System.currentTimeMillis();
        if (now - lastFireTimestamp >= FIRE_COOLDOWN_MS) {
            world.spawnEntity(new AuthoritativeAlienShotEntity(x + 12, y + 20));
            lastFireTimestamp = now;
        }
    }
}
