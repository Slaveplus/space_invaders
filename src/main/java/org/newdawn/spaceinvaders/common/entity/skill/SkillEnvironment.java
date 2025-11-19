package org.newdawn.spaceinvaders.common.entity.skill;

import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 스킬 드롭이 상호작용해야 하는 게임 컨텍스트.
 */
public interface SkillEnvironment {
    void removeEntity(Entity entity);

    void grantSkill(String ownerId, int skillType, int skillValue);
}
