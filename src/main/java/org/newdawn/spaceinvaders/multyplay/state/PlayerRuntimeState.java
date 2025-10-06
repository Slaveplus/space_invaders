package org.newdawn.spaceinvaders.multyplay.state;

import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;

/**
 * Tracks per-player scalars used for HUD display and prediction.
 */
public class PlayerRuntimeState {
    private final String playerId;
    private int hp;
    private int maxHp;
    private int attackPower;
    private double attackSpeed;
    private int skillPoints;

    public PlayerRuntimeState(String playerId) {
        this.playerId = playerId;
    }

    public void updateFromSnapshot(GameSnapshotMsg.PlayerState state) {
        hp = state.getHp();
        maxHp = state.getMaxHp();
        attackPower = state.getAttackPower();
        attackSpeed = state.getAttackSpeed();
        skillPoints = state.getSkillPoints();
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public double getAttackSpeed() {
        return attackSpeed;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public String summaryLine() {
        return String.format("%s HP %d/%d ATK %d SPD %.2f SP %d", playerId, hp, maxHp, attackPower, attackSpeed, skillPoints);
    }
}
