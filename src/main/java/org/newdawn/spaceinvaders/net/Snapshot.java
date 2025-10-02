package org.newdawn.spaceinvaders.net;

import java.util.List;

/**
 * 서버 권위 스냅샷 (간소화): 모든 클라이언트가 같은 장면을 보도록 함.
 */
public class Snapshot {
    public long tick; // 서버 틱
    public List<PlayerState> players;
    public List<EntityState> aliens;
    public List<EntityState> bullets;
    public List<PowerupState> powerups;
    public List<Event> events;
    public int wave;

    public static class PlayerState {
        public String id; // 서버가 부여한 플레이어 ID
        public float x, y;
        public int hp;
        public int maxHp;
        public int score;
        // 싱글플레이 스탯 동기화
        public int skillPoints;
        public int attackPower;      // 기본 1, +1씩 증가
        public double attackSpeed;   // 기본 1.0, +0.2씩 증가
        // 업그레이드 비용(개인별 증가)
        public int costAtk;          // 기본 2, +1씩 증가
        public int costAspd;         // 기본 2, +1씩 증가
        public int costHp;           // 기본 8, +5씩 증가
    }

    public static class EntityState {
        public String id;
        public float x, y;
        public String type; // "alien", "bullet_player", "bullet_enemy"
    }

    public static class PowerupState {
        public String id;
        public String type; // e.g., "rapid"
        public float x, y;
    }

    public static class Event {
        public String type; // e.g., "shoot", "explode", "powerup"
        public float x, y;
    }
}
