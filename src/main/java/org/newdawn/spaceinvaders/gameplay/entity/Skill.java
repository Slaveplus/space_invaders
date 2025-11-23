package org.newdawn.spaceinvaders.gameplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.skill.BaseSkillEntity;
import org.newdawn.spaceinvaders.common.entity.skill.SkillEnvironment;
import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 싱글플레이 스킬 드롭 구현.
 */
public class Skill extends BaseSkillEntity {
    public Skill(Game game, String sprite, int x, int y, int skillType, int skillValue) {
        super(new GameplaySkillEnvironment(game), sprite, x, y, skillType, skillValue);
    }

    private static final class GameplaySkillEnvironment implements SkillEnvironment {
        private final Game game;

        private GameplaySkillEnvironment(Game game) {
            this.game = game;
        }

        @Override
        public void removeEntity(Entity entity) {
            game.removeEntity(entity);
        }

        @Override
        public void grantSkill(String ownerId, int skillType, int skillValue) {
            game.addSkillToInventory(skillType, skillValue);
        }
    }
}
