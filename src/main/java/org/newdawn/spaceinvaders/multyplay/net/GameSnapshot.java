package org.newdawn.spaceinvaders.multyplay.net;

import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * authoritative 게임 상태 스냅샷 (서버 -> 클라이언트).
 */
public class GameSnapshot {
    public enum Phase { ACTIVE, INTERMISSION, COMPLETED }

    public final long tick;
    public final long serverTime;
    public final long deltaMillis;
    public final int round;
    public final List<EntitySnapshot> entities;
    public final Map<String, PlayerScalarState> players; // 단순 수치 정보
    public final Phase phase;
    public final boolean waitingForPlayers;
    public final Map<String, Boolean> readyStates;
    public final String message;

    public GameSnapshot(long tick,
                        long serverTime,
                        long deltaMillis,
                        int round,
                        List<EntitySnapshot> entities,
                        Map<String, PlayerScalarState> players) {
        this(tick, serverTime, deltaMillis, round, entities, players,
                Phase.ACTIVE, false, Collections.emptyMap(), null);
    }

    public GameSnapshot(long tick,
                        long serverTime,
                        long deltaMillis,
                        int round,
                        List<EntitySnapshot> entities,
                        Map<String, PlayerScalarState> players,
                        Phase phase,
                        boolean waitingForPlayers,
                        Map<String, Boolean> readyStates,
                        String message) {
        this.tick = tick;
        this.serverTime = serverTime;
        this.deltaMillis = deltaMillis;
        this.round = round;
        this.entities = entities != null ? entities : Collections.emptyList();
        this.players = players != null ? players : Collections.emptyMap();
        this.phase = phase != null ? phase : Phase.ACTIVE;
        this.waitingForPlayers = waitingForPlayers;
        if (readyStates == null || readyStates.isEmpty()) {
            this.readyStates = Collections.emptyMap();
        } else {
            this.readyStates = Collections.unmodifiableMap(new LinkedHashMap<>(readyStates));
        }
        this.message = message;
    }

    /** 플레이어의 단순 수치 상태 */
    public static class PlayerScalarState {
        public final int hp; public final int maxHp; public final int atk; public final double aspd; public final int skillPts;
        public PlayerScalarState(int hp, int maxHp, int atk, double aspd, int skillPts) {
            this.hp=hp; this.maxHp=maxHp; this.atk=atk; this.aspd=aspd; this.skillPts=skillPts;
        }
    }
}
