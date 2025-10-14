package org.newdawn.spaceinvaders.gameplay.ui;

/**
 * 공유 게임플레이 HUD를 렌더링하는 데 필요한 최소 데이터 계약입니다.
 */
public interface HudContext {
    int getCurrentRound();
    int getMaxRound();

    int getCurrentHp();
    int getMaxHp();

    int getSkillPoints();
    int getAttackPower();
    double getAttackSpeed();

    int getInvincibleSkillCount();
    int getTripleShotSkillCount();
    int getMissileSkillCount();

    boolean isInvincibleActive();
    long getInvincibleRemainingMs();
    boolean isTripleShotActive();
    long getTripleShotRemainingMs();

    String getFormattedPlayTime();
    int getEarnedCoins();
}
