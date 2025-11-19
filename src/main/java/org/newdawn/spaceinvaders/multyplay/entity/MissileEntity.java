package org.newdawn.spaceinvaders.multyplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.effect.ExplosionEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.BaseMissileEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.MissileEnvironment;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;

public class MissileEntity extends BaseMissileEntity {
    public MissileEntity(MultiplayerGameContext game, String sprite, int x, int y, double targetX, double targetY) {
        super(new MultiplayerMissileEnvironment(game), sprite, x, y, targetX, targetY);
    }

    @Override
    protected void applyAlienDamage(Entity alien) {
        if (alien instanceof org.newdawn.spaceinvaders.common.entity.alien.BaseAlienEntity) {
            org.newdawn.spaceinvaders.common.entity.alien.BaseAlienEntity base =
                    (org.newdawn.spaceinvaders.common.entity.alien.BaseAlienEntity) alien;
            int damage = Math.max(base.getCurrentHP(),
                    environment.getPlayerAttackPower(getOwnerId()) * 4);
            base.takeDamage(damage);
            environment.notifyAlienKilled(getOwnerId(), alien.getX(), alien.getY());
        }
        environment.removeEntity(this);
        used = true;
    }

    @Override
    protected void applyBossDamage(Entity boss) {
        try {
            boss.getClass().getMethod("takeDamage", int.class, String.class)
                    .invoke(boss, environment.getPlayerAttackPower(getOwnerId()), getOwnerId());
        } catch (Exception ignore) {
        }
        environment.removeEntity(this);
        used = true;
    }

    private static final class MultiplayerMissileEnvironment implements MissileEnvironment {
        private final MultiplayerGameContext game;

        private MultiplayerMissileEnvironment(MultiplayerGameContext game) {
            this.game = game;
        }

        @Override
        public Entity createExplosion(double targetX, double targetY) {
            ExplosionEntity explosion = new ExplosionEntity(game, "sprites/Skill/Explosion.png",
                    (int) targetX - 25, (int) targetY - 25, 100.0);
            explosion.setOwnerId(null);
            return explosion;
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
            return game.getPlayerAttackPower(ownerId);
        }

        @Override
        public boolean isPlayerInvincible(String ownerId) {
            return game.isPlayerInvincible(ownerId);
        }

        @Override
        public void notifyAlienKilled(String ownerId, double x, double y) {
            game.notifyAlienKilled(ownerId, x, y);
        }

        @Override
        public void notifyPlayerDamaged(String ownerId, int damage) {
            game.notifyPlayerDamaged(ownerId, damage);
        }

        @Override
        public void notifyDeath(String ownerId) {
            game.notifyDeath(ownerId);
        }

        @Override
        public void addSkillToInventory(String ownerId, int skillType, int skillValue) {
            game.addSkillToInventory(ownerId, skillType, skillValue);
        }
    }
}
