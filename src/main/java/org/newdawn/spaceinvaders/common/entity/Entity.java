package org.newdawn.spaceinvaders.common.entity;

import java.awt.Graphics;
import java.awt.Rectangle;
import org.newdawn.spaceinvaders.common.sprite.Sprite;
import org.newdawn.spaceinvaders.common.sprite.SpriteStore;

/**
 * 싱글/멀티 공통 엔티티 베이스 클래스.
 */
public abstract class Entity {
    private static final java.util.concurrent.atomic.AtomicLong ID_GENERATOR =
            new java.util.concurrent.atomic.AtomicLong();
    private final long entityId = ID_GENERATOR.incrementAndGet();
    private String ownerId;

    protected double x;
    protected double y;
    protected Sprite sprite;
    protected String spritePath;
    protected double dx;
    protected double dy;
    private final Rectangle me = new Rectangle();
    private final Rectangle him = new Rectangle();

    public Entity(String ref, int x, int y) {
        this.spritePath = ref;
        this.sprite = SpriteStore.get().getSprite(ref);
        this.x = x;
        this.y = y;
    }

    public long getEntityId() {
        return entityId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public EntitySnapshot toSnapshot() {
        return new EntitySnapshot(
                entityId,
                ownerId,
                typeId(),
                spriteRef(),
                x,
                y,
                dx,
                dy,
                getBounds().width,
                getBounds().height,
                snapshotMetadata());
    }

    protected String spriteRef() {
        return spritePath;
    }

    protected String typeId() {
        return getClass().getSimpleName();
    }

    protected String snapshotMetadata() {
        return null;
    }

    protected void applySnapshotMetadata(String metadata) {
        // default no-op
    }

    public void applySnapshot(EntitySnapshot snap) {
        if (snap.sprite != null && !snap.sprite.equals(spritePath)) {
            spritePath = snap.sprite;
            sprite = SpriteStore.get().getSprite(spritePath);
        }
        this.ownerId = snap.ownerId;
        this.x = snap.x;
        this.y = snap.y;
        this.dx = snap.dx;
        this.dy = snap.dy;
        if (snap.metadata != null) {
            applySnapshotMetadata(snap.metadata);
        }
    }

    public void move(long delta) {
        x += (delta * dx) / 1000;
        y += (delta * dy) / 1000;
    }

    public void setHorizontalMovement(double dx) {
        this.dx = dx;
    }

    public void setVerticalMovement(double dy) {
        this.dy = dy;
    }

    public double getHorizontalMovement() {
        return dx;
    }

    public double getVerticalMovement() {
        return dy;
    }

    public void draw(Graphics g) {
        sprite.draw(g, (int) x, (int) y);
    }

    public void doLogic() {
        // default no-op
    }

    public int getX() {
        return (int) x;
    }

    public int getY() {
        return (int) y;
    }

    public boolean collidesWith(Entity other) {
        Rectangle myBounds = getBounds();
        Rectangle otherBounds = other.getBounds();

        me.setBounds(myBounds);
        him.setBounds(otherBounds);

        return me.intersects(him);
    }

    public Rectangle getBounds() {
        int width = sprite != null ? sprite.getWidth() : 0;
        int height = sprite != null ? sprite.getHeight() : 0;
        return new Rectangle((int) x, (int) y, width, height);
    }

    public void changeSkin(String newSkinPath) {
        this.spritePath = newSkinPath;
        this.sprite = SpriteStore.get().getSprite(newSkinPath);
    }

    // Legacy-style accessors for code that previously referenced dx/dy directly
    public double getDX() {
        return dx;
    }

    public double getDY() {
        return dy;
    }

    public void setDX(double dx) {
        this.dx = dx;
    }

    public void setDY(double dy) {
        this.dy = dy;
    }

    public abstract void collidedWith(Entity other);
}
