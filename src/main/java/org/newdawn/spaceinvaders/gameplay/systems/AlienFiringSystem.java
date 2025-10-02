package org.newdawn.spaceinvaders.gameplay.systems;

import org.newdawn.spaceinvaders.gameplay.GameStateManager;
import org.newdawn.spaceinvaders.gameplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;

import java.util.ArrayList;

/**
 * 에일리언 사격 타이밍 및 발사를 담당하는 시스템.
 */
public final class AlienFiringSystem {
    private AlienFiringSystem() {}

    public static interface Shooter {
        void fireFromAlien(AlienEntity alien);
    }

    public static void update(GameStateManager gsm, Shooter shooter, Entity player) {
        if (player == null) return;
        if (System.currentTimeMillis() - gsm.getLastAlienFire() < gsm.getAlienFiringInterval()) {
            return;
        }
        ArrayList<AlienEntity> nearAliens = new ArrayList<>();
        for (Entity e : gsm.getEntities()) {
            if (e instanceof AlienEntity) {
                if (Math.abs(e.getY() - player.getY()) < 200) {
                    nearAliens.add((AlienEntity) e);
                }
            }
        }
        if (!nearAliens.isEmpty()) {
            int idx = (int) (Math.random() * nearAliens.size());
            shooter.fireFromAlien(nearAliens.get(idx));
            gsm.setLastAlienFire(System.currentTimeMillis());
        }
    }
}
