package org.newdawn.spaceinvaders.common.entity;

/**
 * 네트워크/저장 스냅샷용 엔티티 구조체.
 */
public class EntitySnapshot {
    public final long id;
    public final String ownerId;
    public final String type;
    public final String sprite;
    public final double x;
    public final double y;
    public final double dx;
    public final double dy;
    public final int w;
    public final int h;
    public final String metadata;

    public EntitySnapshot(long id, String ownerId, String type, String sprite,
                          double x, double y, double dx, double dy,
                          int w, int h, String metadata) {
        this.id = id;
        this.ownerId = ownerId;
        this.type = type;
        this.sprite = sprite;
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.w = w;
        this.h = h;
        this.metadata = metadata;
    }

    public EntitySnapshot(long id, String ownerId, String sprite, double x, double y, double dx, double dy, int w, int h) {
        this(id, ownerId, null, sprite, x, y, dx, dy, w, h, null);
    }
}
