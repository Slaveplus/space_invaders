package org.newdawn.spaceinvaders.gameplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.projectile.BaseBossShotEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.BossShotEnvironment;
import org.newdawn.spaceinvaders.gameplay.Game;

public class BossShotEntity extends BaseBossShotEntity {
    private final Game game;

    public BossShotEntity(Game game,
                          int x,
                          int y,
                          double directionX,
                          double directionY,
                          double speed) {
        this(game, x, y, directionX, directionY, speed, 8, false, 0, 0);
    }

    public BossShotEntity(Game game,
                          int x,
                          int y,
                          double directionX,
                          double directionY,
                          double speed,
                          int radius,
                          boolean canSplit,
                          double splitY,
                          int splitCount) {
        super(new GameplayBossShotEnvironment(game),
                x, y, directionX, directionY, speed, radius, canSplit, splitY, splitCount);
        this.game = game;
    }

    @Override
    protected BaseBossShotEntity createChild(int x, int y, double directionX, double directionY, double speed, int radius) {
        return new BossShotEntity(game, x, y, directionX, directionY, speed, radius, false, 0, 0);
    }

    private static final class GameplayBossShotEnvironment implements BossShotEnvironment {
        private final Game game;

        private GameplayBossShotEnvironment(Game game) {
            this.game = game;
        }

        @Override
        public void addEntity(Entity entity) {
            game.addEntity(entity);
        }

        @Override
        public void removeEntity(Entity entity) {
            game.removeEntity(entity);
        }

        @Override
        public void notifyPlayerHit(String ownerId) {
            game.notifyDeath();
        }
    }
}
