package org.newdawn.spaceinvaders.gameplay.entity;

import java.util.function.IntUnaryOperator;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEnvironment;
import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 싱글 모드 Alien 엔티티가 상호작용할 게임 환경 어댑터.
 */
public class GameplayAlienEnvironment implements AlienEnvironment {
    private final Game game;

    public GameplayAlienEnvironment(Game game) {
        this.game = game;
    }

    public static IntUnaryOperator hpResolver() {
        return round -> {
            switch (round) {
                case 1:
                    return 20;
                case 2:
                    return 4;
                case 3:
                    return 10;
                case 4:
                    return 18;
                case 5:
                    return 30;
                default:
                    return 30 + Math.max(0, (round - 5) * 15);
            }
        };
    }

    @Override
    public int getCurrentRound() {
        return game.getCurrentRound();
    }

    @Override
    public void addAimedAlienShot(int x, int y, int alienX) {
        game.addAimedAlienShot(x, y, alienX);
    }

    @Override
    public void removeEntity(Entity entity) {
        game.removeEntity(entity);
    }

    @Override
    public void notifyAlienKilled() {
        game.notifyAlienKilled();
    }

    @Override
    public int getPlayerShipX() {
        return game.getShipX();
    }

    @Override
    public int getPlayerShipY() {
        return game.getShipY();
    }
}
