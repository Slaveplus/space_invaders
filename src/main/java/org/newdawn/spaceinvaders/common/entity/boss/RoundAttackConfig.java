package org.newdawn.spaceinvaders.common.entity.boss;

/**
 * 라운드별 공격 설정을 관리하는 값 객체
 * 각 라운드의 공격 타이머들을 그룹화
 */
public class RoundAttackConfig {
    private final AttackTimer mainAttackTimer;
    private final AttackTimer iceAttackTimer;
    private final AttackTimer iceBallAttackTimer;
    private final AttackTimer magneticFieldTimer;
    private int currentAttackPattern = 0;
    
    // 라운드 4 전용
    private final AttackTimer healAttackTimer;
    private final AttackTimer greenSphereAttackTimer;
    private final AttackTimer playerLineAttackTimer;
    private boolean timerStarted = false;
    private long startTime = 0;
    private final long timeLimit;
    
    private RoundAttackConfig(Builder builder) {
        this.mainAttackTimer = builder.mainAttackTimer;
        this.iceAttackTimer = builder.iceAttackTimer;
        this.iceBallAttackTimer = builder.iceBallAttackTimer;
        this.magneticFieldTimer = builder.magneticFieldTimer;
        this.healAttackTimer = builder.healAttackTimer;
        this.greenSphereAttackTimer = builder.greenSphereAttackTimer;
        this.playerLineAttackTimer = builder.playerLineAttackTimer;
        this.timeLimit = builder.timeLimit;
    }
    
    public AttackTimer getMainAttackTimer() {
        return mainAttackTimer;
    }
    
    public AttackTimer getIceAttackTimer() {
        return iceAttackTimer;
    }
    
    public AttackTimer getIceBallAttackTimer() {
        return iceBallAttackTimer;
    }
    
    public AttackTimer getMagneticFieldTimer() {
        return magneticFieldTimer;
    }
    
    public AttackTimer getHealAttackTimer() {
        return healAttackTimer;
    }
    
    public AttackTimer getGreenSphereAttackTimer() {
        return greenSphereAttackTimer;
    }
    
    public AttackTimer getPlayerLineAttackTimer() {
        return playerLineAttackTimer;
    }
    
    /**
     * 타이머가 존재하는지 확인
     */
    public boolean hasMainAttackTimer() {
        return mainAttackTimer != null;
    }
    
    public boolean hasIceAttackTimer() {
        return iceAttackTimer != null;
    }
    
    public boolean hasIceBallAttackTimer() {
        return iceBallAttackTimer != null;
    }
    
    public boolean hasMagneticFieldTimer() {
        return magneticFieldTimer != null;
    }
    
    public boolean hasHealAttackTimer() {
        return healAttackTimer != null;
    }
    
    public boolean hasGreenSphereAttackTimer() {
        return greenSphereAttackTimer != null;
    }
    
    public boolean hasPlayerLineAttackTimer() {
        return playerLineAttackTimer != null;
    }
    
    public int getCurrentAttackPattern() {
        return currentAttackPattern;
    }
    
    public void setCurrentAttackPattern(int pattern) {
        this.currentAttackPattern = pattern;
    }
    
    public void nextAttackPattern(int maxPattern) {
        this.currentAttackPattern = (this.currentAttackPattern + 1) % maxPattern;
    }
    
    public boolean isTimerStarted() {
        return timerStarted;
    }
    
    public void startTimer() {
        if (!timerStarted) {
            timerStarted = true;
            startTime = System.currentTimeMillis();
        }
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public boolean isTimeLimitExceeded() {
        if (!timerStarted) {
            return false;
        }
        return (System.currentTimeMillis() - startTime) > timeLimit;
    }
    
    public long getTimeLimit() {
        return timeLimit;
    }
    
    public static class Builder {
        private AttackTimer mainAttackTimer;
        private AttackTimer iceAttackTimer;
        private AttackTimer iceBallAttackTimer;
        private AttackTimer magneticFieldTimer;
        private AttackTimer healAttackTimer;
        private AttackTimer greenSphereAttackTimer;
        private AttackTimer playerLineAttackTimer;
        private long timeLimit = 0;
        
        public Builder mainAttackTimer(long interval) {
            this.mainAttackTimer = new AttackTimer(interval);
            return this;
        }
        
        public Builder iceAttackTimer(long interval) {
            this.iceAttackTimer = new AttackTimer(interval);
            return this;
        }
        
        public Builder iceBallAttackTimer(long interval) {
            this.iceBallAttackTimer = new AttackTimer(interval);
            return this;
        }
        
        public Builder magneticFieldTimer(long interval) {
            this.magneticFieldTimer = new AttackTimer(interval);
            return this;
        }
        
        public Builder healAttackTimer(long interval) {
            this.healAttackTimer = new AttackTimer(interval);
            return this;
        }
        
        public Builder greenSphereAttackTimer(long interval) {
            this.greenSphereAttackTimer = new AttackTimer(interval);
            return this;
        }
        
        public Builder playerLineAttackTimer(long interval) {
            this.playerLineAttackTimer = new AttackTimer(interval);
            return this;
        }
        
        public Builder timeLimit(long timeLimit) {
            this.timeLimit = timeLimit;
            return this;
        }
        
        public RoundAttackConfig build() {
            return new RoundAttackConfig(this);
        }
    }
}

