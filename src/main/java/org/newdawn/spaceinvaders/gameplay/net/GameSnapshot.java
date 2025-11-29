package org.newdawn.spaceinvaders.gameplay.net;

import org.newdawn.spaceinvaders.common.entity.EntitySnapshot;
import java.util.List;
import java.util.Map;

/**
 * authoritative 게임 상태 스냅샷 (서버 -> 클라이언트).
 */
public class GameSnapshot {
    public final long serverTime;
    public final int round;
    public final List<EntitySnapshot> entities;
    public final Map<String, PlayerScalarState> players; // 단순 수치 정보

    public GameSnapshot(long serverTime, int round, List<EntitySnapshot> entities, Map<String, PlayerScalarState> players) {
        this.serverTime = serverTime; this.round = round; this.entities = entities; this.players = players;
    }

    /** 플레이어의 단순 수치 상태 */
    public static class PlayerScalarState {
        public final int hp; public final int maxHp; public final int atk; public final double aspd; public final int skillPts;
        public PlayerScalarState(int hp, int maxHp, int atk, double aspd, int skillPts) {
            this.hp=hp; this.maxHp=maxHp; this.atk=atk; this.aspd=aspd; this.skillPts=skillPts;
        }
    }
}
