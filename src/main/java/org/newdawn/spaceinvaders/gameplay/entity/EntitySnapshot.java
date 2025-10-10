package org.newdawn.spaceinvaders.gameplay.entity;

/**
 * 네트워크 동기화를 위한 경량 Entity Snapshot 구조체 (변경 불가 데이터 클래스).
 */
public class EntitySnapshot {
    public final long id; 
    public final String ownerId; 
    public final String sprite; // 현재는 null (SpriteStore ref 필요시 확장)
    public final double x; 
    public final double y; 
    public final double dx; 
    public final double dy;
    public final int w; 
    public final int h;

    public EntitySnapshot(long id, String ownerId, String sprite, double x, double y, double dx, double dy, int w, int h) {
        this.id = id; this.ownerId = ownerId; this.sprite = sprite; this.x = x; this.y = y; this.dx = dx; this.dy = dy; this.w = w; this.h = h;
    }
}
