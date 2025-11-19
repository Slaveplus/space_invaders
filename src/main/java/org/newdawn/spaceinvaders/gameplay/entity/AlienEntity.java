package org.newdawn.spaceinvaders.gameplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEnvironment;
import org.newdawn.spaceinvaders.common.entity.alien.BaseAlienEntity;
import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 싱글플레이 Alien 구현 (공통 베이스 사용).
 */
public class AlienEntity extends BaseAlienEntity {
    private final Game game;

    public AlienEntity(Game game, int x, int y) {
        super(new GameplayAlienEnvironment(game), spriteForRound(game.getCurrentRound()), x, y);
        this.game = game;
    }

    private static String spriteForRound(int round) {
        switch (round) {
            case 1:
                return "sprites/Boss/1near.png";
            case 2:
                return "sprites/Boss/2near.png";
            case 3:
                return "sprites/Boss/3near.png";
            case 4:
                return "sprites/Boss/4near.png";
            case 5:
                return "sprites/Boss/5near.png";
            default:
                return "sprites/Boss/5near.png";
        }
    }

    @Override
    protected int resolveMaxHp(int round) {
        switch (round) {
            case 1: return 20;
            case 2: return 4;
            case 3: return 10;
            case 4: return 18;
            case 5: return 30;
            default: return 30 + ((round - 5) * 15);
        }
    }

    private static final class GameplayAlienEnvironment implements AlienEnvironment {
        private final Game game;

        private GameplayAlienEnvironment(Game game) {
            this.game = game;
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
}
