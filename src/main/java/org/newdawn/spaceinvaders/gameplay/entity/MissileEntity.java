package org.newdawn.spaceinvaders.gameplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.effect.ExplosionEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.BaseMissileEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.MissileEnvironment;
import org.newdawn.spaceinvaders.gameplay.Game;

public class MissileEntity extends BaseMissileEntity {
    public MissileEntity(Game game, String sprite, int x, int y, double targetX, double targetY) {
        super(new GameplayMissileEnvironment(game), sprite, x, y, targetX, targetY);
    }

    @Override
    protected void applyAlienDamage(Entity alien) {
        environment.removeEntity(this);
        environment.removeEntity(alien);
        environment.notifyAlienKilled(getOwnerId(), alien.getX(), alien.getY());
        used = true;
    }

    @Override
    protected void applyBossDamage(Entity boss) {
        try {
            boss.getClass().getMethod("takeDamage", int.class)
                    .invoke(boss, 50);
        } catch (Exception ignore) {
        }
        environment.removeEntity(this);
        used = true;
    }

    private static final class GameplayMissileEnvironment implements MissileEnvironment {
        private final Game game;

        private GameplayMissileEnvironment(Game game) {
            this.game = game;
        }

        @Override
        public Entity createExplosion(double targetX, double targetY) {
            return new ExplosionEntity(game, "sprites/Skill/Explosion.png",
                    (int) targetX - 25, (int) targetY - 25, 100.0);
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
        public java.util.List<Entity> getEntities() {
            return game.getEntities();
        }

        @Override
        public void createHeatEffect(int x, int y, double size) {
            game.createHeatEffect(x, y, size);
        }

        @Override
        public int getPlayerAttackPower(String ownerId) {
            return game.getPlayerAttackPower();
        }

        @Override
        public boolean isPlayerInvincible(String ownerId) {
            return game.isPlayerInvincible();
        }

        @Override
        public void notifyAlienKilled(String ownerId, double x, double y) {
            game.notifyAlienKilled();
        }

        @Override
        public void notifyPlayerDamaged(String ownerId, int damage) {
            game.notifyPlayerDamaged(damage);
        }

        @Override
        public void notifyDeath(String ownerId) {
            game.notifyDeath();
        }

        @Override
        public void addSkillToInventory(String ownerId, int skillType, int skillValue) {
            game.addSkillToInventory(skillType, skillValue);
        }
    }
}
