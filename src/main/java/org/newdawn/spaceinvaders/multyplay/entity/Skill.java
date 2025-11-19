package org.newdawn.spaceinvaders.multyplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.skill.BaseSkillEntity;
import org.newdawn.spaceinvaders.common.entity.skill.SkillEnvironment;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;

/**
 * 멀티플레이 스킬 드롭 구현.
 */
public class Skill extends BaseSkillEntity {
    public Skill(MultiplayerGameContext game, String sprite, int x, int y, int skillType, int skillValue) {
        super(new MultiplayerSkillEnvironment(game), sprite, x, y, skillType, skillValue);
    }

    private static final class MultiplayerSkillEnvironment implements SkillEnvironment {
        private final MultiplayerGameContext game;

        private MultiplayerSkillEnvironment(MultiplayerGameContext game) {
            this.game = game;
        }

        @Override
        public void removeEntity(Entity entity) {
            game.removeEntity(entity);
        }

        @Override
        public void grantSkill(String ownerId, int skillType, int skillValue) {
            game.addSkillToInventory(ownerId, skillType, skillValue);
        }
    }
}
