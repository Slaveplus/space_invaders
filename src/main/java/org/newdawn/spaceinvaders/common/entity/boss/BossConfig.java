package org.newdawn.spaceinvaders.common.entity.boss;

/**
 * 보스 설정을 나타내는 값 객체
 * 라운드별 HP, 이동 속도, 스프라이트 경로 등을 관리
 */
public class BossConfig {
    private final int round;
    private final int maxHP;
    private final String spritePath;
    private final int bossSize;
    private final double moveSpeed;
    private final int scoreValue;
    private final int skillPointsReward;
    
    private BossConfig(Builder builder) {
        this.round = builder.round;
        this.maxHP = builder.maxHP;
        this.spritePath = builder.spritePath;
        this.bossSize = builder.bossSize;
        this.moveSpeed = builder.moveSpeed;
        this.scoreValue = builder.scoreValue;
        this.skillPointsReward = builder.skillPointsReward;
    }
    
    public int getRound() {
        return round;
    }
    
    public int getMaxHP() {
        return maxHP;
    }
    
    public String getSpritePath() {
        return spritePath;
    }
    
    public int getBossSize() {
        return bossSize;
    }
    
    public double getMoveSpeed() {
        return moveSpeed;
    }
    
    public int getScoreValue() {
        return scoreValue;
    }
    
    public int getSkillPointsReward() {
        return skillPointsReward;
    }
    
    public static class Builder {
        private int round;
        private int maxHP;
        private String spritePath;
        private int bossSize = 280;
        private double moveSpeed = 50;
        private int scoreValue = 1000;
        private int skillPointsReward = 5;
        
        public Builder round(int round) {
            this.round = round;
            return this;
        }
        
        public Builder maxHP(int maxHP) {
            this.maxHP = maxHP;
            return this;
        }
        
        public Builder spritePath(String spritePath) {
            this.spritePath = spritePath;
            return this;
        }
        
        public Builder bossSize(int bossSize) {
            this.bossSize = bossSize;
            return this;
        }
        
        public Builder moveSpeed(double moveSpeed) {
            this.moveSpeed = moveSpeed;
            return this;
        }
        
        public Builder scoreValue(int scoreValue) {
            this.scoreValue = scoreValue;
            return this;
        }
        
        public Builder skillPointsReward(int skillPointsReward) {
            this.skillPointsReward = skillPointsReward;
            return this;
        }
        
        public BossConfig build() {
            return new BossConfig(this);
        }
    }
}

