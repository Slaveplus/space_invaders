package org.newdawn.spaceinvaders.common.entity.boss;

/**
 * 공격 타이밍을 관리하는 값 객체
 * lastAttackTime과 attackInterval을 묶어서 관리
 */
public class AttackTimer {
    private long lastAttackTime = 0;
    private final long attackInterval;
    
    public AttackTimer(long attackInterval) {
        this.attackInterval = attackInterval;
    }
    
    /**
     * 공격 가능한지 확인
     * 
     * @return 공격 가능하면 true
     */
    public boolean canAttack() {
        long now = System.currentTimeMillis();
        return (now - lastAttackTime) >= attackInterval;
    }
    
    /**
     * 공격 시간 기록
     */
    public void recordAttack() {
        lastAttackTime = System.currentTimeMillis();
    }
    
    /**
     * 타이머 리셋
     */
    public void reset() {
        lastAttackTime = 0;
    }
    
    /**
     * 남은 시간 반환
     * 
     * @return 남은 시간 (밀리초)
     */
    public long getRemainingTime() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastAttackTime;
        return Math.max(0, attackInterval - elapsed);
    }
    
    /**
     * 공격 간격 반환
     * 
     * @return 공격 간격 (밀리초)
     */
    public long getInterval() {
        return attackInterval;
    }
    
    /**
     * 마지막 공격 시간 반환
     * 
     * @return 마지막 공격 시간
     */
    public long getLastAttackTime() {
        return lastAttackTime;
    }
}

