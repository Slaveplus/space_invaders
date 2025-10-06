package org.newdawn.spaceinvaders.multyplay.entity;

/**
 * Holds interpolatable state for a single entity.
 */
public class EntitySnapshot {
    public final String id;
    public final double x;
    public final double y;
    public final double velocityX;
    public final double velocityY;
    public final int hp;
    public final int stateFlags;
    public final long timestamp;

    public EntitySnapshot(String id,
                          double x,
                          double y,
                          double velocityX,
                          double velocityY,
                          int hp,
                          int stateFlags,
                          long timestamp) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.hp = hp;
        this.stateFlags = stateFlags;
        this.timestamp = timestamp;
    }
}
