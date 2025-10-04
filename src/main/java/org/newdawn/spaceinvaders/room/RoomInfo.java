package org.newdawn.spaceinvaders.room;

public class RoomInfo {
    public final String id;
    public final String name;
    public final boolean single; // true면 목록 표시 안됨 (단, 클라이언트 캐시에선 구분 보관)
    public final int current;
    public final int max;
    public RoomInfo(String id, String name, boolean single, int current, int max) {
        this.id = id; this.name = name; this.single = single; this.current = current; this.max = max;
    }
}
