package org.newdawn.spaceinvaders.multyplay.net;

import java.util.List;
import java.util.ArrayList;

/**
 * 서버 -> 클라이언트 전체 or 부분 스냅샷.
 */
public class GameSnapshotMsg {
    public long tick;
    public final List<PlayerStateSnapshot> players = new ArrayList<>();

    public static class PlayerStateSnapshot {
        public int slot;
        public float x;
        public float y;
        public int hp;
        public int sp; // skill points
    }
}
