package org.newdawn.spaceinvaders.gameplay.core;

/**
 * 싱글/멀티 공통 게임 규칙과 상수.
 * 싱글(Game/AlienEntity 등)에서 사용되는 공식/상수를 이 클래스로 모아
 * 서버(멀티)에서도 동일한 값을 사용하도록 합니다.
 */
public final class Rules {
    private Rules() {}

    // 기본 뷰포트 사이즈 (싱글과 동일)
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final float PLAYER_Y = 550f;
    // 플레이어(배) 스프라이트 절반 폭(센터 좌표계에서 경계 계산용)
    // 싱글의 좌상단 기준 경계(10..750)와 최대한 동일 체감을 주기 위해 보수적으로 16px 사용
    public static final float PLAYER_HALF_WIDTH = 16f;

    // 경계 (AlienEntity.move에서 사용되는 값과 동일하게 유지)
    public static final float ALIEN_MIN_X = 10f;
    public static final float ALIEN_MAX_X = 750f;
    public static final float ALIEN_MIN_Y = 50f;
    public static final float ALIEN_MAX_Y = 450f;

    // 플레이어/탄 속도
    public static final float PLAYER_MOVE_SPEED = 300f;     // px/sec
    public static final float BULLET_PLAYER_SPEED_Y = -300f; // 위로
    public static final float BULLET_ENEMY_SPEED_Y = 300f;   // 아래로

    // 발사 쿨다운(플레이어)
    public static final long PLAYER_BASE_FIRING_INTERVAL_MS = 500L;

    // 라운드별 글로벌 에일리언 사격 간격 (GameStateManager.advanceRound 로직과 일치)
    public static long globalAlienFiringIntervalMs(int round, long base) {
        long interval = base - (long) round * 300L;
        return Math.max(300L, interval);
    }

    // 라운드별 에일리언 HP (AlienEntity 생성자와 동일)
    public static int alienHpForRound(int round) {
        return 2 + round + (round > 3 ? round - 3 : 0); // 2,3,4,6,8...
    }

    // 라운드별 에일리언 이동 속도 (AlienEntity 생성자와 동일)
    public static float alienMoveSpeedForRound(int round) {
        return 75f + (round * 10f);
    }

    // 라운드별 개별 에일리언 사격 간격(난수 포함) - AlienEntity 생성자와 동일 공식
    public static long alienIndividualFiringIntervalMs(int round) {
        long fi;
        if (round <= 3) {
            fi = 2500L - (round * 500L); // 2500,2000,1500
        } else if (round <= 6) {
            fi = 1000L - ((round - 3L) * 100L); // 1000,900,800...
        } else {
            fi = Math.max(300L, 700L - ((round - 6L) * 100L));
        }
        double rf = 0.8 + (Math.random() * 0.4); // ±20%
        return (long) (fi * rf);
    }

    // 잔존 에일리언 속도 증가 배수 (notifyAlienKilled와 동일)
    public static double remainingAlienSpeedMultiplier(int round) {
        return 1.015 + (round * 0.01);
    }

    // 조준 오프셋 (Game.addAimedAlienShot 과 동일)
    public static float aimedOffsetX(float alienX, float playerX) {
        int offset = (int) ((playerX - alienX) * 0.15f);
        if (offset < -15) offset = -15; else if (offset > 15) offset = 15;
        return offset;
    }
}
