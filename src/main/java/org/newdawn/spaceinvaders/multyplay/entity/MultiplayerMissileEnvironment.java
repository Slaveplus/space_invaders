package org.newdawn.spaceinvaders.multyplay.entity;

import java.util.List;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.effect.ExplosionEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.BaseMissileEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.MissileEnvironment;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;

/**
 * 멀티플레이 미사일 환경.
 */
public class MultiplayerMissileEnvironment implements MissileEnvironment {
    private final MultiplayerGameContext game;

    public MultiplayerMissileEnvironment(MultiplayerGameContext game) {
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
    public List<Entity> getEntities() {
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

    @Override
    public void onAlienHit(BaseMissileEntity missile, Entity alien) {
        if (alien instanceof org.newdawn.spaceinvaders.common.entity.alien.BaseAlienEntity) {
            org.newdawn.spaceinvaders.common.entity.alien.BaseAlienEntity base =
                    (org.newdawn.spaceinvaders.common.entity.alien.BaseAlienEntity) alien;
            int damage = Math.max(base.getCurrentHP(),
                    getPlayerAttackPower(missile.getOwnerId()) * 4);
            base.takeDamage(damage);
            notifyAlienKilled(missile.getOwnerId(), alien.getX(), alien.getY());
        }
        removeEntity(missile);
    }

    @Override
    public void onBossHit(BaseMissileEntity missile, Entity boss) {
        try {
            boss.getClass().getMethod("takeDamage", int.class, String.class)
                    .invoke(boss, getPlayerAttackPower(missile.getOwnerId()), missile.getOwnerId());
        } catch (Exception ignore) {
        }
        removeEntity(missile);
    }
}
