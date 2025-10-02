package org.newdawn.spaceinvaders.gameplay.systems;

import org.newdawn.spaceinvaders.gameplay.GameStateManager;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;

import java.util.ArrayList;

/**
 * 충돌 검사 및 제거 리스트 정리를 담당하는 시스템.
 */
public final class CollisionSystem {
    private CollisionSystem() {}

    public static void update(GameStateManager gsm) {
        if (gsm.isShowingPauseMenu() || gsm.isShowingSkillMenu()) {
            return;
        }
        ArrayList<Entity> entities = gsm.getEntities();
        // 충돌
        for (int i = 0; i < entities.size(); i++) {
            Entity e1 = entities.get(i);
            for (int j = i + 1; j < entities.size(); j++) {
                Entity e2 = entities.get(j);
                if (e1.collidesWith(e2)) {
                    e1.collidedWith(e2);
                    e2.collidedWith(e1);
                }
            }
        }
        // 제거 리스트 적용
        entities.removeAll(gsm.getRemoveList());
        gsm.getRemoveList().clear();

        // 로직 플래그 처리
        if (gsm.isLogicRequiredThisLoop()) {
            for (Entity e : entities) e.doLogic();
            gsm.setLogicRequiredThisLoop(false);
        }
    }
}
