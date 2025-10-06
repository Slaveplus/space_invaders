package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Graphics;
import java.awt.Rectangle;
import org.newdawn.spaceinvaders.gameplay.sprite.Sprite;
import org.newdawn.spaceinvaders.gameplay.sprite.SpriteStore;

/**
 * 멀티 전용 엔티티 (싱글 Entity 포크) - Game 참조는 후속 단계에서 MultiGameController로 교체 예정
 */
public abstract class Entity {
    private static final java.util.concurrent.atomic.AtomicLong ID_GENERATOR = new java.util.concurrent.atomic.AtomicLong();
    private final long entityId = ID_GENERATOR.incrementAndGet();
    private String ownerId;

    protected double x; protected double y; protected Sprite sprite; protected double dx; protected double dy;
    private Rectangle me = new Rectangle();
    private Rectangle him = new Rectangle();

    public Entity(String ref,int x,int y) { this.sprite = SpriteStore.get().getSprite(ref); this.x = x; this.y = y; }

    public long getEntityId() { return entityId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public EntitySnapshot toSnapshot() {
        return new EntitySnapshot(entityId, ownerId, spriteRef(), x, y, dx, dy, getBounds().width, getBounds().height);
    }
    protected String spriteRef() { return null; }
    public void applySnapshot(EntitySnapshot snap) { this.x = snap.x; this.y = snap.y; this.dx = snap.dx; this.dy = snap.dy; }

    public void move(long delta) { x += (delta * dx) / 1000; y += (delta * dy) / 1000; }
    public void setHorizontalMovement(double dx) { this.dx = dx; }
    public void setVerticalMovement(double dy) { this.dy = dy; }
    public double getHorizontalMovement() { return dx; }
    public double getVerticalMovement() { return dy; }

    public void draw(Graphics g) { sprite.draw(g,(int) x,(int) y); }
    public void doLogic() { }
    public int getX() { return (int) x; }
    public int getY() { return (int) y; }
    // 위치 강제 동기화용 (스냅샷 적용 단계에서 사용)
    public void setPosition(double x, double y){ this.x = x; this.y = y; }

    public boolean collidesWith(Entity other) {
        Rectangle myBounds = getBounds(); Rectangle otherBounds = other.getBounds();
        me.setBounds(myBounds); him.setBounds(otherBounds); return me.intersects(him);
    }

    public Rectangle getBounds() { return new Rectangle((int) x, (int) y, sprite.getWidth(), sprite.getHeight()); }
    public void changeSkin(String newSkinPath) { this.sprite = SpriteStore.get().getSprite(newSkinPath); }
    public abstract void collidedWith(Entity other);
}
