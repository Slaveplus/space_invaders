package org.newdawn.spaceinvaders.common.entity.boss;

/**
 * 공격 패턴을 나타내는 값 객체
 * 패턴 ID와 사용 여부를 관리
 */
public class AttackPattern {
    private final int id;
    private final String name;
    private final Runnable attackAction;
    private boolean used = false;
    
    public AttackPattern(int id, String name, Runnable attackAction) {
        this.id = id;
        this.name = name;
        this.attackAction = attackAction;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isUsed() {
        return used;
    }
    
    public void setUsed(boolean used) {
        this.used = used;
    }
    
    public void execute() {
        if (attackAction != null) {
            attackAction.run();
        }
    }
}

