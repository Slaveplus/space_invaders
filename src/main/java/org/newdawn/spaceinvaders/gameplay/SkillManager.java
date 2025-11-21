package org.newdawn.spaceinvaders.gameplay;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.spaceinvaders.gameplay.core.GameplayContext;
import org.newdawn.spaceinvaders.gameplay.core.SkillDropTable;
import org.newdawn.spaceinvaders.gameplay.core.SkillDropTable.SkillDrop;
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
    private final int baseAttackPowerCost = 2;   // 공격력: 2, 3, 4, 5, 6... (점진적 증가)
    private final int baseAttackSpeedCost = 2;   // 공격속도: 2, 3, 4, 5, 6... (점진적 증가)
    private final int baseHpUpCost = 2;          // 체력: 2, 3, 4, 5, 6... (점진적 증가)
    
    // 스킬 효과 상태
    private boolean isInvincible = false;
    private long invincibleEndTime = 0;
    private boolean hasTripleShot = false;
    private long tripleShotEndTime = 0;
    
    // 스킬 인벤토리
    private int invincibleSkills = 0;
    private int tripleShotSkills = 0;
    private int missileSkills = 0;
    
    // 게임 컨텍스트
    private final GameplayContext context;
    
    public SkillManager(GameplayContext context) {
        this.context = context;
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
        hasTripleShot = false;
        
        // Reset skill inventory
        invincibleSkills = 0;
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
        
        // 관통 스킬 제거됨
        
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
        // 관통 스킬 제거됨
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
        // 관통 스킬 제거됨
        } else if (skillType == 2) { // Triple shot skill
            if (tripleShotSkills > 0) {
                tripleShotSkills--;
                hasTripleShot = true;
                tripleShotEndTime = currentTime + (skillValue * 1000); // Reset duration
            }
        }
    }
    
    /**
     * 스킬 드롭 생성 (위치 정보 없음 - 레거시 호환)
     */
    public void dropSkill(int currentRound) {
        dropSkill(currentRound, 400, 100); // 기본 위치 사용
    }
    
    /**
     * 스킬 드롭 생성 (위치 정보 포함)
     */
    public void dropSkill(int currentRound, int x, int y) {
        SkillDrop drop = SkillDropTable.rollDrop(currentRound);
        addSkillDrop(x, y, drop.skillType, drop.skillValue);
    }
    
    /**
     * 스킬 드롭 엔티티 추가
     */
    private void addSkillDrop(int x, int y, int skillType, int skillValue) {
        context.createSkillDrop(x, y, skillType, skillValue);
    }
    
    
    /**
     * 스킬 포인트 계산 - 항상 2포인트씩 지급
     */
    public int getRandomSkillPoints(int currentRound) {
        // 항상 2포인트씩 지급
        return 2;
    }
    
    /**
     * 스킬 드롭 확률 계산
     */
    public double getSkillDropChance(int currentRound) {
        return 0.15 + (currentRound * 0.05); // 20%, 25%, 30%, 35%, 40% for rounds 1-5 (increased for testing)
    }
    
    // 점진적 비용 계산 메서드들 (2, 3, 4, 5, 6... 형태로 증가)
    public int getAttackPowerCost() { 
        return baseAttackPowerCost + attackPowerLevel; // 2, 3, 4, 5, 6...
    }
    
    public int getAttackSpeedCost() { 
        return baseAttackSpeedCost + attackSpeedLevel; // 2, 3, 4, 5, 6...
    }
    
    public int getHpUpCost() { 
        return baseHpUpCost + hpUpLevel; // 2, 3, 4, 5, 6...
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
    
    // 관통 스킬 제거됨
    
    public boolean hasTripleShot() { return hasTripleShot; }
    public void setHasTripleShot(boolean hasTripleShot) { this.hasTripleShot = hasTripleShot; }
    
    public long getTripleShotEndTime() { return tripleShotEndTime; }
    public void setTripleShotEndTime(long tripleShotEndTime) { this.tripleShotEndTime = tripleShotEndTime; }
    
    public int getInvincibleSkills() { return invincibleSkills; }
    public void setInvincibleSkills(int invincibleSkills) { this.invincibleSkills = invincibleSkills; }
    
    // 관통 스킬 제거됨
    
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
            List<Entity> entities = context.getActiveEntities();
            List<Entity> enemies = new ArrayList<>();
            
            for (Entity entity : entities) {
                if (entity instanceof AlienEntity) {
                    enemies.add(entity);
                }
            }
            
            if (!enemies.isEmpty()) {
                // Pick a random enemy
                Entity target = enemies.get((int)(Math.random() * enemies.size()));
                
                // Fire missile at target location
                context.fireMissile(null, target.getX() + 15, target.getY() + 15);
            } else {
                // No enemies, fire missile at random screen position
                double randomX = 100 + Math.random() * 600;
                double randomY = 100 + Math.random() * 300;
                context.fireMissile(null, randomX, randomY);
            }
        } catch (Exception e) {
            System.err.println("Error firing missile: " + e.getMessage());
            e.printStackTrace();
            // Fallback: fire missile at center of screen
            context.fireMissile(null, 400, 300);
        }
    }
    
}
