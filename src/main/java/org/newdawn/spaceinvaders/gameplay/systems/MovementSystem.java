package org.newdawn.spaceinvaders.gameplay.systems;

import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.gameplay.GameStateManager;
import org.newdawn.spaceinvaders.gameplay.InputManager;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;

import java.util.ArrayList;

/**
 * 이동/행동 업데이트를 담당하는 시스템.
 */
public final class MovementSystem {
    private MovementSystem() {}

    public static void update(GameStateManager gsm, Game game, InputManager input, long delta) {
        if (gsm.isWaitingForKeyPress() || gsm.isShowingPauseMenu() || gsm.isShowingSkillMenu()) {
            return;
        }

        // 스킬 효과 등 시간 경과 기반 업데이트는 Game/SkillManager에서 처리

        // 엔티티 이동
        ArrayList<Entity> entities = gsm.getEntities();
        for (Entity entity : entities) {
            entity.move(delta);
        }

        // 플레이어 입력에 의한 이동/발사 처리 (중복 제거)
        if (input != null) {
            input.updateGameplayInput();
        }
    }
}
