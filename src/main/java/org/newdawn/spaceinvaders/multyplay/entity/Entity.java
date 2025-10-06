package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Graphics2D;
import java.util.Objects;

import org.newdawn.spaceinvaders.gameplay.sprite.Sprite;
import org.newdawn.spaceinvaders.gameplay.sprite.SpriteStore;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;
import org.newdawn.spaceinvaders.multyplay.util.EntitySpriteResolver;

/**
 * Base entity representation used by the multiplayer renderer.
 * Holds snapshot interpolated state and sprite information.
 */
public class Entity {
    private final String id;
    private final String type;
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private int hp;
    private int stateFlags;

    private Sprite sprite;
    private boolean spriteInitialized;

    public Entity(String id, String type) {
        this.id = Objects.requireNonNull(id, "entityId");
        this.type = Objects.requireNonNull(type, "type");
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

    public void apply(GameSnapshotMsg.EntityState state) {
        x = state.getX();
        y = state.getY();
        velocityX = state.getVelocityX();
        velocityY = state.getVelocityY();
        hp = state.getHp();
        stateFlags = state.getStateFlags();
        ensureSpriteLoaded();
    }

    public void clientUpdate(long deltaMillis) {
        // Basic dead-reckoning using velocity between snapshots.
        double dt = deltaMillis / 1000.0;
        x += velocityX * dt;
        y += velocityY * dt;
    }

    public void render(Graphics2D g) {
        ensureSpriteLoaded();
        if (sprite != null) {
            sprite.draw(g, (int) Math.round(x), (int) Math.round(y));
        }
    }

    private void ensureSpriteLoaded() {
        if (spriteInitialized) {
            return;
        }
        String ref = EntitySpriteResolver.resolve(type);
        if (ref != null) {
            sprite = SpriteStore.get().getSprite(ref);
        }
        spriteInitialized = true;
    }
}
