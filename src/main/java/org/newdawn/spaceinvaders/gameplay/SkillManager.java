package org.newdawn.spaceinvaders.gameplay;

import java.util.List;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.skill.BaseSkillManager;
import org.newdawn.spaceinvaders.common.skill.SkillEnvironment;
import org.newdawn.spaceinvaders.gameplay.core.GameplayContext;

/**
 * 싱글플레이 스킬 매니저. 공통 베이스를 사용하며 GameplayContext에 위임한다.
 */
public class SkillManager extends BaseSkillManager {
    private final GameplayContext context;

    public SkillManager(GameplayContext context) {
        super(new GameplaySkillEnvironment(context));
        this.context = context;
    }

    private static final class GameplaySkillEnvironment implements SkillEnvironment {
        private final GameplayContext context;

        private GameplaySkillEnvironment(GameplayContext context) {
            this.context = context;
        }

        @Override
        public List<Entity> getEntities() {
            return context != null ? context.getActiveEntities() : null;
        }

        @Override
        public void createSkillDrop(int x, int y, int skillType, int skillValue) {
            if (context != null) {
                context.createSkillDrop(x, y, skillType, skillValue);
            }
        }

        @Override
        public void fireMissile(String ownerId, double targetX, double targetY) {
            if (context != null) {
                context.fireMissile(ownerId, targetX, targetY);
            }
        }
    }

    public GameplayContext getContext() {
        return context;
    }
}
