package org.newdawn.spaceinvaders.multyplay.core;

import java.util.Collections;
import java.util.List;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.skill.BaseSkillManager;
import org.newdawn.spaceinvaders.common.skill.SkillEnvironment;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;

/**
 * 멀티플레이 스킬 매니저. GameContext 위임 + 소유자 개념 추가.
 */
public class MultiplayerSkillManager extends BaseSkillManager {
    private final MultiplayerGameContext game;
    private String ownerId;

    public MultiplayerSkillManager(MultiplayerGameContext game) {
        this(game, null);
    }

    public MultiplayerSkillManager(MultiplayerGameContext game, String ownerId) {
        super(new MultiplayerSkillEnvironment(game));
        this.game = game;
        this.ownerId = ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String getOwnerId() {
        return resolveOwnerId();
    }

    private String resolveOwnerId() {
        if (ownerId != null) {
            return ownerId;
        }
        MultiplayerGameStateManager gsm = game != null ? game.getGameStateManager() : null;
        return gsm != null ? gsm.getLocalPlayerId() : null;
    }

    private static final class MultiplayerSkillEnvironment implements SkillEnvironment {
        private final MultiplayerGameContext game;

        private MultiplayerSkillEnvironment(MultiplayerGameContext game) {
            this.game = game;
        }

        @Override
        public List<Entity> getEntities() {
            return game != null ? game.getEntities() : Collections.emptyList();
        }

        @Override
        public void createSkillDrop(int x, int y, int skillType, int skillValue) {
            if (game != null) {
                game.createSkillDrop(x, y, skillType, skillValue);
            }
        }

        @Override
        public void fireMissile(String ownerId, double targetX, double targetY) {
            if (game != null) {
                game.fireMissile(ownerId, targetX, targetY);
            }
        }
    }
}
