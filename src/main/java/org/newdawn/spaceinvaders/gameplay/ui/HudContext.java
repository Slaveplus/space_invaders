package org.newdawn.spaceinvaders.gameplay.ui;

/**
 * Minimal data contract required to render the shared gameplay HUD.
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
