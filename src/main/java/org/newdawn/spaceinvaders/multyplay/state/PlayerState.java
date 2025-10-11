package org.newdawn.spaceinvaders.multyplay.state;

/**
 * 단일 플레이어의 전투/성장 상태를 캡슐화.
 * 멀티플레이 확장을 위해 MultiplayerGameStateManager로부터 분리.
 */
public class PlayerState {
    private final String playerId; // 네트워크 세션 또는 로컬 식별자

    private int attackPower = 1;
    private double attackSpeed = 1.0;
    private int maxHP = 3;
   private int currentHP = 3;
   private int skillPoints = 0;
    private int earnedCoins = 0;

    public PlayerState(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() { return playerId; }

    public int getAttackPower() { return attackPower; }
    public void setAttackPower(int attackPower) { this.attackPower = attackPower; }

    public double getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(double attackSpeed) { this.attackSpeed = attackSpeed; }

    public int getMaxHP() { return maxHP; }
    public void setMaxHP(int maxHP) { this.maxHP = maxHP; }

    public int getCurrentHP() { return currentHP; }
    public void setCurrentHP(int currentHP) { this.currentHP = currentHP; }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = skillPoints; }

    public void addSkillPoints(int delta) { this.skillPoints += delta; }

    public int getEarnedCoins() { return earnedCoins; }
    public void setEarnedCoins(int earnedCoins) { this.earnedCoins = earnedCoins; }
    public void addCoins(int delta) { this.earnedCoins += delta; }

    public void resetForNewGame() {
        attackPower = 1;
        attackSpeed = 1.0;
        maxHP = 3;
        currentHP = 3;
        skillPoints = 0;
        earnedCoins = 0;
    }

    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        currentHP = Math.max(0, currentHP - amount);
    }

    public void restoreFullHealth() {
        currentHP = maxHP;
    }

    public boolean isAlive() {
        return currentHP > 0;
    }

    public boolean isDead() { return currentHP <= 0; }
}
