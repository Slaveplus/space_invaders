package org.newdawn.spaceinvaders.gameplay;

import java.util.ArrayList;

import org.newdawn.spaceinvaders.gameplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;

/**
 * 스킬 시스템을 관리하는 클래스
 * 스킬 효과, 인벤토리, 드롭 등을 관리
 */
public class SkillManager {
    // 스킬 강화 레벨 (점진적 비용 계산용)
    private int attackPowerLevel = 0;
    private int attackSpeedLevel = 0;
    private int hpUpLevel = 0;
    
    // 기본 스킬 비용 (밸런스 조정)
    private final int baseAttackPowerCost = 2;   // 공격력: 2, 4, 6, 8, 10...
    private final int baseAttackSpeedCost = 2;   // 공격속도: 2, 4, 6, 8, 10...
    private final int baseHpUpCost = 15;         // 체력: 15, 23, 31, 39...
    
    // 스킬 효과 상태
    private boolean isInvincible = false;
    private long invincibleEndTime = 0;
    private boolean hasPiercing = false;
    private long piercingEndTime = 0;
    private boolean hasTripleShot = false;
    private long tripleShotEndTime = 0;
    
    // 스킬 인벤토리
    private int invincibleSkills = 0;
    private int piercingSkills = 0;
    private int tripleShotSkills = 0;
    private int missileSkills = 0;
    
    // 게임 참조
    private Game game;
    
    public SkillManager(Game game) {
        this.game = game;
    }
    
    /**
     * 게임 시작 시 스킬 시스템 초기화
     */
    public void reset() {
        // Reset skill levels
        attackPowerLevel = 0;
        attackSpeedLevel = 0;
        hpUpLevel = 0;
        
        // Reset skill effects
        isInvincible = false;
        hasPiercing = false;
        hasTripleShot = false;
        
        // Reset skill inventory
        invincibleSkills = 0;
        piercingSkills = 0;
        tripleShotSkills = 0;
    }
    
    /**
     * 스킬 효과 업데이트 (만료 시간 확인)
     */
    public void updateSkillEffects() {
        long currentTime = System.currentTimeMillis();
        
        // Check invincibility expiration
        if (isInvincible && currentTime >= invincibleEndTime) {
            isInvincible = false;
        }
        
        // Check piercing expiration
        if (hasPiercing && currentTime >= piercingEndTime) {
            hasPiercing = false;
        }
        
        // Check triple shot expiration
        if (hasTripleShot && currentTime >= tripleShotEndTime) {
            hasTripleShot = false;
        }
    }
    
    /**
     * 스킬을 인벤토리에 추가
     */
    public void addSkillToInventory(int skillType, int skillValue) {
        if (skillType == 0) { // Invincible skill
            invincibleSkills++;
        } else if (skillType == 1) { // Piercing skill
            piercingSkills++;
        } else if (skillType == 2) { // Triple shot skill
            tripleShotSkills++;
        } else if (skillType == 3) { // Missile skill
            missileSkills++;
        }
    }
    
    /**
     * 스킬 효과 활성화
     */
    public void activateSkill(int skillType, int skillValue) {
        long currentTime = System.currentTimeMillis();
        
        if (skillType == 0) { // Invincible skill
            if (invincibleSkills > 0) {
                invincibleSkills--;
                isInvincible = true;
                invincibleEndTime = currentTime + (skillValue * 1000);
            }
        } else if (skillType == 1) { // Piercing skill
            if (piercingSkills > 0) {
                piercingSkills--;
                hasPiercing = true;
                piercingEndTime = currentTime + (skillValue * 1000);
            }
        } else if (skillType == 2) { // Triple shot skill
            if (tripleShotSkills > 0) {
                tripleShotSkills--;
                hasTripleShot = true;
                tripleShotEndTime = currentTime + (skillValue * 1000);
            }
        } else if (skillType == 3) { // Missile skill
            if (missileSkills > 0) {
                missileSkills--;
                // Fire missile at random enemy location
                fireMissileAtRandomTarget();
            }
        }
    }
    
    /**
     * 스킬 효과 연장 (이미 활성화된 스킬도 다시 사용 가능)
     */
    public void extendSkill(int skillType, int skillValue) {
        long currentTime = System.currentTimeMillis();
        
        if (skillType == 0) { // Invincible skill
            if (invincibleSkills > 0) {
                invincibleSkills--;
                isInvincible = true;
                invincibleEndTime = currentTime + (skillValue * 1000); // Reset duration
            }
        } else if (skillType == 1) { // Piercing skill
            if (piercingSkills > 0) {
                piercingSkills--;
                hasPiercing = true;
                piercingEndTime = currentTime + (skillValue * 1000); // Reset duration
            }
        } else if (skillType == 2) { // Triple shot skill
            if (tripleShotSkills > 0) {
                tripleShotSkills--;
                hasTripleShot = true;
                tripleShotEndTime = currentTime + (skillValue * 1000); // Reset duration
            }
        }
    }
    
    /**
     * 스킬 드롭 생성
     */
    public void dropSkill(int currentRound) {
        // Find the last killed alien's position (approximate)
        int dropX = 400; // Default center position
        int dropY = 100; // Default top position
        
        // Try to find an alien position for more realistic dropping
        ArrayList<Entity> entities = game.getEntities();
        for (Entity entity : entities) {
            if (entity instanceof org.newdawn.spaceinvaders.gameplay.entity.AlienEntity) {
                dropX = (int) entity.getX();
                dropY = (int) entity.getY();
                break;
            }
        }
        
        // Random skill type (0: Attack Power, 1: Attack Speed, 2: HP Recovery, 3: Missile)
        double random = Math.random();
        int skillType;
        int skillValue;
        
        if (random < 0.25) {
            skillType = 0; // Attack Power
            skillValue = 5; // 5 seconds
        } else if (random < 0.5) {
            skillType = 1; // Attack Speed
            skillValue = 10; // 10 seconds
        } else if (random < 0.75) {
            skillType = 2; // HP Recovery
            skillValue = 8; // 8 seconds
        } else {
            skillType = 3; // Missile
            skillValue = 1; // 1 missile
        }
        
        // Create skill entity
        addSkillDrop(dropX, dropY, skillType, skillValue);
    }
    
    /**
     * 스킬 드롭 엔티티 추가
     */
    private void addSkillDrop(int x, int y, int skillType, int skillValue) {
        // Game의 createSkillDrop 메서드를 사용하여 스킬 드롭 생성
        game.createSkillDrop(x, y, skillType, skillValue);
    }
    
    
    /**
     * 스킬 포인트 계산 (라운드별)
     */
    public int getRandomSkillPoints(int currentRound) {
        double random = Math.random() * 100; // 0.0 to 99.9
        int basePoints = 0;
        
        // Base probability changes with round
        if (currentRound <= 2) {
            // Rounds 1-2: 0,1,2 points
            if (random < 50.0) {
                basePoints = 0; // 50% chance
            } else if (random < 85.0) {
                basePoints = 1; // 35% chance
            } else {
                basePoints = 2; // 15% chance
            }
        } else if (currentRound <= 4) {
            // Rounds 3-4: 1,2,3,4 points
            if (random < 40.0) {
                basePoints = 1; // 40% chance
            } else if (random < 70.0) {
                basePoints = 2; // 30% chance
            } else if (random < 90.0) {
                basePoints = 3; // 20% chance
            } else {
                basePoints = 4; // 10% chance
            }
        } else {
            // Round 5: 2,3,4,5,6 points
            if (random < 35.0) {
                basePoints = 2; // 35% chance
            } else if (random < 65.0) {
                basePoints = 3; // 30% chance
            } else if (random < 85.0) {
                basePoints = 4; // 20% chance
            } else if (random < 95.0) {
                basePoints = 5; // 10% chance
            } else {
                basePoints = 6; // 5% chance
            }
        }
        
        return basePoints;
    }
    
    /**
     * 스킬 드롭 확률 계산
     */
    public double getSkillDropChance(int currentRound) {
        return 0.15 + (currentRound * 0.05); // 20%, 25%, 30%, 35%, 40% for rounds 1-5 (increased for testing)
    }
    
    // 점진적 비용 계산 메서드들
    public int getAttackPowerCost() { 
        return baseAttackPowerCost + (attackPowerLevel * 2); // 2, 4, 6, 8, 10...
    }
    
    public int getAttackSpeedCost() { 
        return baseAttackSpeedCost + (attackSpeedLevel * 2); // 2, 4, 6, 8, 10...
    }
    
    public int getHpUpCost() { 
        return baseHpUpCost + (hpUpLevel * 8); // 15, 23, 31, 39...
    }
    
    // 강화 레벨 증가 메서드들
    public void increaseAttackPowerLevel() { attackPowerLevel++; }
    public void increaseAttackSpeedLevel() { attackSpeedLevel++; }
    public void increaseHpUpLevel() { hpUpLevel++; }
    
    // 강화 레벨 getter 메서드들
    public int getAttackPowerLevel() { return attackPowerLevel; }
    public int getAttackSpeedLevel() { return attackSpeedLevel; }
    public int getHpUpLevel() { return hpUpLevel; }
    
    public boolean isInvincible() { return isInvincible; }
    public void setInvincible(boolean invincible) { isInvincible = invincible; }
    
    public long getInvincibleEndTime() { return invincibleEndTime; }
    public void setInvincibleEndTime(long invincibleEndTime) { this.invincibleEndTime = invincibleEndTime; }
    
    public boolean hasPiercing() { return hasPiercing; }
    public void setHasPiercing(boolean hasPiercing) { this.hasPiercing = hasPiercing; }
    
    public long getPiercingEndTime() { return piercingEndTime; }
    public void setPiercingEndTime(long piercingEndTime) { this.piercingEndTime = piercingEndTime; }
    
    public boolean hasTripleShot() { return hasTripleShot; }
    public void setHasTripleShot(boolean hasTripleShot) { this.hasTripleShot = hasTripleShot; }
    
    public long getTripleShotEndTime() { return tripleShotEndTime; }
    public void setTripleShotEndTime(long tripleShotEndTime) { this.tripleShotEndTime = tripleShotEndTime; }
    
    public int getInvincibleSkills() { return invincibleSkills; }
    public void setInvincibleSkills(int invincibleSkills) { this.invincibleSkills = invincibleSkills; }
    
    public int getPiercingSkills() { return piercingSkills; }
    public void setPiercingSkills(int piercingSkills) { this.piercingSkills = piercingSkills; }
    
    public int getTripleShotSkills() { return tripleShotSkills; }
    public void setTripleShotSkills(int tripleShotSkills) { this.tripleShotSkills = tripleShotSkills; }
    
    public int getMissileSkills() { return missileSkills; }
    public void setMissileSkills(int missileSkills) { this.missileSkills = missileSkills; }
    
    /**
     * 남은 시간 계산
     */
    public long getRemainingTime(long endTime) {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    /**
     * 미사일을 랜덤 적 위치로 발사
     */
    private void fireMissileAtRandomTarget() {
        try {
            // Find a random enemy to target
            java.util.List<Object> entities = game.getEntities();
            java.util.List<Entity> enemies = new java.util.ArrayList<>();
            
            for (Object obj : entities) {
                if (obj instanceof Entity) {
                    Entity entity = (Entity) obj;
                    if (entity instanceof AlienEntity) {
                        enemies.add(entity);
                    }
                }
            }
            
            if (!enemies.isEmpty()) {
                // Pick a random enemy
                Entity target = enemies.get((int)(Math.random() * enemies.size()));
                
                // Fire missile at target location
                game.fireMissile(target.getX() + 15, target.getY() + 15);
            } else {
                // No enemies, fire missile at random screen position
                double randomX = 100 + Math.random() * 600;
                double randomY = 100 + Math.random() * 300;
                game.fireMissile(randomX, randomY);
            }
        } catch (Exception e) {
            System.err.println("Error firing missile: " + e.getMessage());
            e.printStackTrace();
            // Fallback: fire missile at center of screen
            game.fireMissile(400, 300);
        }
    }
}