package org.newdawn.spaceinvaders.gameplay;

import org.newdawn.spaceinvaders.Game;

import java.util.ArrayList;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShotEntity;

/**
 * 스킬 시스템을 관리하는 클래스
 * 스킬 효과, 인벤토리, 드롭 등을 관리
 */
public class SkillManager {
    // 스킬 비용
    private int attackPowerCost = 2;
    private int attackSpeedCost = 2;
    private int hpUpCost = 8;
    
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
    
    // 게임 참조
    private Game game;
    
    public SkillManager(Game game) {
        this.game = game;
    }
    
    /**
     * 게임 시작 시 스킬 시스템 초기화
     */
    public void reset() {
        // Reset skill costs
        attackPowerCost = 2;
        attackSpeedCost = 2;
        hpUpCost = 8;
        
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
            if (entity instanceof org.newdawn.spaceinvaders.entity.AlienEntity) {
                dropX = (int) entity.getX();
                dropY = (int) entity.getY();
                break;
            }
        }
        
        // Random skill type (0: Invincible, 1: Piercing, 2: Triple Shot)
        double random = Math.random();
        int skillType;
        int skillValue;
        
        if (random < 0.33) {
            skillType = 0; // Invincible
            skillValue = 5; // 5 seconds
        } else if (random < 0.66) {
            skillType = 1; // Piercing
            skillValue = 10; // 10 seconds
        } else {
            skillType = 2; // Triple Shot
            skillValue = 8; // 8 seconds
        }
        
        // Create skill entity
        addSkillDrop(dropX, dropY, skillType, skillValue);
    }
    
    /**
     * 스킬 드롭 엔티티 추가
     */
    private void addSkillDrop(int x, int y, int skillType, int skillValue) {
        // ShotEntity는 기본 생성자만 지원하므로 일반 ShotEntity로 생성
        ShotEntity skillDrop = new ShotEntity(game, "sprites/shot.gif", x, y);
        // TODO: 스킬 드롭 기능은 별도 엔티티로 구현 필요
    }
    
    /**
     * 스킬 선택 처리
     */
    public String handleSkillSelection(int selectedSkill, int skillPoints, int attackPower, 
                                     double attackSpeed, int maxHP) {
        int requiredCost = 0;
        
        // Check required cost for selected skill
        switch (selectedSkill) {
            case 0: // Attack Power
                requiredCost = attackPowerCost;
                break;
            case 1: // Attack Speed
                requiredCost = attackSpeedCost;
                break;
            case 2: // HP Up & Heal
                requiredCost = hpUpCost;
                break;
        }
        
        if (skillPoints < requiredCost) {
            return "스킬 포인트가 부족합니다! (필요: " + requiredCost + "포인트)";
        }
        
        switch (selectedSkill) {
            case 0: // Attack Power
                attackPowerCost++; // Increase cost for next upgrade
                return "공격력이 증가했습니다!";
            case 1: // Attack Speed
                attackSpeedCost++; // Increase cost for next upgrade
                return "공격속도가 증가했습니다!";
            case 2: // HP Up & Heal
                hpUpCost += 5; // Increase cost by 5 for next upgrade
                return "최대 HP가 증가하고 회복되었습니다!";
        }
        
        return "";
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
        return 0.08 + (currentRound * 0.02); // 10%, 12%, 14%, 16%, 18% for rounds 1-5
    }
    
    // Getters and Setters
    public int getAttackPowerCost() { return attackPowerCost; }
    public void setAttackPowerCost(int attackPowerCost) { this.attackPowerCost = attackPowerCost; }
    
    public int getAttackSpeedCost() { return attackSpeedCost; }
    public void setAttackSpeedCost(int attackSpeedCost) { this.attackSpeedCost = attackSpeedCost; }
    
    public int getHpUpCost() { return hpUpCost; }
    public void setHpUpCost(int hpUpCost) { this.hpUpCost = hpUpCost; }
    
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
    
    /**
     * 남은 시간 계산
     */
    public long getRemainingTime(long endTime) {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
}