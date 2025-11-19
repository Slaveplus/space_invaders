package org.newdawn.spaceinvaders.common.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.core.SkillDropTable;
import org.newdawn.spaceinvaders.gameplay.core.SkillDropTable.SkillDrop;

/**
 * 싱글/멀티 공용 스킬 매니저 베이스 클래스.
 */
public abstract class BaseSkillManager {
    private final SkillEnvironment environment;

    private int attackPowerLevel;
    private int attackSpeedLevel;
    private int hpUpLevel;

    private final int baseAttackPowerCost = 2;
    private final int baseAttackSpeedCost = 2;
    private final int baseHpUpCost = 15;

    private boolean invincible;
    private long invincibleEndTime;
    private boolean tripleShot;
    private long tripleShotEndTime;

    private int invincibleSkills;
    private int tripleShotSkills;
    private int missileSkills;

    protected BaseSkillManager(SkillEnvironment environment) {
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    public void reset() {
        attackPowerLevel = 0;
        attackSpeedLevel = 0;
        hpUpLevel = 0;
        invincible = false;
        tripleShot = false;
        invincibleSkills = 0;
        tripleShotSkills = 0;
        missileSkills = 0;
    }

    public void updateSkillEffects() {
        long now = System.currentTimeMillis();
        if (invincible && now >= invincibleEndTime) {
            invincible = false;
        }
        if (tripleShot && now >= tripleShotEndTime) {
            tripleShot = false;
        }
    }

    public void addSkillToInventory(int skillType, int skillValue) {
        if (skillType == 0) {
            invincibleSkills++;
        } else if (skillType == 2) {
            tripleShotSkills++;
        } else if (skillType == 3) {
            missileSkills++;
        }
    }

    public void activateSkill(int skillType, int skillValue) {
        long now = System.currentTimeMillis();
        if (skillType == 0 && invincibleSkills > 0) {
            invincibleSkills--;
            invincible = true;
            invincibleEndTime = now + (skillValue * 1000L);
        } else if (skillType == 2 && tripleShotSkills > 0) {
            tripleShotSkills--;
            tripleShot = true;
            tripleShotEndTime = now + (skillValue * 1000L);
        } else if (skillType == 3 && missileSkills > 0) {
            missileSkills--;
            fireMissileAtRandomTarget();
        }
    }

    public void extendSkill(int skillType, int skillValue) {
        long now = System.currentTimeMillis();
        if (skillType == 0 && invincibleSkills > 0) {
            invincibleSkills--;
            invincible = true;
            invincibleEndTime = now + (skillValue * 1000L);
        } else if (skillType == 2 && tripleShotSkills > 0) {
            tripleShotSkills--;
            tripleShot = true;
            tripleShotEndTime = now + (skillValue * 1000L);
        }
    }

    public void dropSkill(int currentRound) {
        dropSkill(currentRound, Double.NaN, Double.NaN);
    }

    public void dropSkill(int currentRound, double killX, double killY) {
        int dropX = 400;
        int dropY = 120;

        boolean hasCoords = !Double.isNaN(killX) && !Double.isNaN(killY);
        if (hasCoords) {
            dropX = (int) Math.round(killX);
            dropY = (int) Math.round(killY);
        } else {
            for (Entity entity : getEntities()) {
                if (isAlien(entity)) {
                    dropX = (int) Math.round(entity.getX());
                    dropY = (int) Math.round(entity.getY());
                    break;
                }
            }
        }

        dropX = Math.max(32, Math.min(768, dropX));
        dropY = Math.max(48, Math.min(560, dropY));

        SkillDrop drop = SkillDropTable.rollDrop(currentRound);
        environment.createSkillDrop(dropX, dropY, drop.skillType, drop.skillValue);
    }

    public int getRandomSkillPoints(int round) {
        double random = Math.random() * 100;
        if (round <= 2) {
            return random < 50 ? 0 : random < 85 ? 1 : 2;
        } else if (round <= 4) {
            if (random < 40) return 1;
            if (random < 70) return 2;
            if (random < 90) return 3;
            return 4;
        } else {
            if (random < 35) return 2;
            if (random < 65) return 3;
            if (random < 85) return 4;
            if (random < 95) return 5;
            return 6;
        }
    }

    public double getSkillDropChance(int currentRound) {
        return 0.15 + (currentRound * 0.05);
    }

    public int getAttackPowerCost() {
        return baseAttackPowerCost + (attackPowerLevel * 2);
    }

    public int getAttackSpeedCost() {
        return baseAttackSpeedCost + (attackSpeedLevel * 2);
    }

    public int getHpUpCost() {
        return baseHpUpCost + (hpUpLevel * 8);
    }

    public void increaseAttackPowerLevel() { attackPowerLevel++; }
    public void increaseAttackSpeedLevel() { attackSpeedLevel++; }
    public void increaseHpUpLevel() { hpUpLevel++; }
    public void setAttackPowerLevel(int level) { attackPowerLevel = Math.max(0, level); }
    public void setAttackSpeedLevel(int level) { attackSpeedLevel = Math.max(0, level); }
    public void setHpUpLevel(int level) { hpUpLevel = Math.max(0, level); }

    public int getAttackPowerLevel() { return attackPowerLevel; }
    public int getAttackSpeedLevel() { return attackSpeedLevel; }
    public int getHpUpLevel() { return hpUpLevel; }

    public boolean isInvincible() { return invincible; }
    public void setInvincible(boolean value) { invincible = value; }

    public long getInvincibleEndTime() { return invincibleEndTime; }
    public void setInvincibleEndTime(long value) { invincibleEndTime = value; }

    public boolean hasTripleShot() { return tripleShot; }
    public void setHasTripleShot(boolean value) { tripleShot = value; }

    public long getTripleShotEndTime() { return tripleShotEndTime; }
    public void setTripleShotEndTime(long value) { tripleShotEndTime = value; }

    public int getInvincibleSkills() { return invincibleSkills; }
    public void setInvincibleSkills(int value) { invincibleSkills = value; }

    public int getTripleShotSkills() { return tripleShotSkills; }
    public void setTripleShotSkills(int value) { tripleShotSkills = value; }

    public int getMissileSkills() { return missileSkills; }
    public void setMissileSkills(int value) { missileSkills = value; }

    public long getRemainingTime(long endTime) {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    protected String getOwnerId() {
        return null;
    }

    private void fireMissileAtRandomTarget() {
        String ownerId = getOwnerId();
        try {
            List<Entity> enemies = new ArrayList<>();
            for (Entity entity : getEntities()) {
                if (isAlien(entity)) {
                    enemies.add(entity);
                }
            }
            if (!enemies.isEmpty()) {
                Entity target = enemies.get((int) (Math.random() * enemies.size()));
                environment.fireMissile(ownerId, target.getX() + 15, target.getY() + 15);
            } else {
                double randomX = 100 + Math.random() * 600;
                double randomY = 100 + Math.random() * 300;
                environment.fireMissile(ownerId, randomX, randomY);
            }
        } catch (Exception e) {
            System.err.println("Error firing missile: " + e.getMessage());
            environment.fireMissile(ownerId, 400, 300);
        }
    }

    private List<Entity> getEntities() {
        List<Entity> entities = environment.getEntities();
        return entities != null ? entities : Collections.emptyList();
    }

    private boolean isAlien(Entity entity) {
        return entity != null && "AlienEntity".equals(entity.getClass().getSimpleName());
    }
}
