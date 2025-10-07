package org.newdawn.spaceinvaders.multyplay.entity;

/**
 * 네트워크 동기화를 위한 경량 Entity Snapshot 구조체 (변경 불가 데이터 클래스).
 */
public class EntitySnapshot {
    public final long id; 
    public final String ownerId; 
    public final String type;
    public final String sprite; // 현재는 null (SpriteStore ref 필요시 확장)
    public final double x; 
    public final double y; 
    public final double dx; 
    public final double dy;
    public final int w; 
    public final int h;
    public final String metadata;

    public EntitySnapshot(long id, String ownerId, String type, String sprite, double x, double y, double dx, double dy, int w, int h, String metadata) {
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

    public EntitySnapshot(long id, String ownerId, String type, String sprite, double x, double y, double dx, double dy, int w, int h) {
        this(id, ownerId, type, sprite, x, y, dx, dy, w, h, null);
    }

    public EntitySnapshot(long id, String ownerId, String sprite, double x, double y, double dx, double dy, int w, int h) {
        this(id, ownerId, null, sprite, x, y, dx, dy, w, h, null);
    }
}
