package org.newdawn.spaceinvaders.multyplay.core.authoritative;

import java.util.UUID;

import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;

/**
 * Minimal deterministic entity representation used by the server simulation.
 */
public abstract class AuthoritativeEntity {
    private final String id;
    private final String type;
    protected double x;
    protected double y;
    protected double velocityX;
    protected double velocityY;
    protected int hp;
    protected int maxHp;
    protected int stateFlags;
    protected double collisionRadius = 16.0;

    protected AuthoritativeEntity(String type, double x, double y) {
        this(UUID.randomUUID().toString(), type, x, y);
    }

    protected AuthoritativeEntity(String id, String type, double x, double y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public int getHp() {
        return hp;
    }

    public int getStateFlags() {
        return stateFlags;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(hp, maxHp > 0 ? maxHp : hp));
    }

    public double getCollisionRadius() {
        return collisionRadius;
    }

    /**
     * Advance the entity simulation by {@code deltaMillis}.
     */
    public void tick(long deltaMillis, AuthoritativeWorld world) {
        double dt = deltaMillis / 1000.0;
        x += velocityX * dt;
        y += velocityY * dt;
    }

    public GameSnapshotMsg.EntityState toSnapshot() {
        return new GameSnapshotMsg.EntityState(id, type, x, y, velocityX, velocityY, hp, stateFlags);
    }

    public boolean collides(AuthoritativeEntity other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double combined = collisionRadius + other.collisionRadius;
        return (dx * dx + dy * dy) <= combined * combined;
    }
}
