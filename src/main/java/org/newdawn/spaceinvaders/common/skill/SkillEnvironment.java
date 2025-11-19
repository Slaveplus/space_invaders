package org.newdawn.spaceinvaders.common.skill;

import java.util.List;

import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 스킬 매니저가 필요로 하는 최소한의 환경.
 */
public interface SkillEnvironment {
    List<Entity> getEntities();

    void createSkillDrop(int x, int y, int skillType, int skillValue);

    void fireMissile(String ownerId, double targetX, double targetY);
}
