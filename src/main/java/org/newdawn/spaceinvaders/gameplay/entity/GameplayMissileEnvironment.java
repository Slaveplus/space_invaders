package org.newdawn.spaceinvaders.gameplay.entity;

import java.util.List;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.effect.ExplosionEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.BaseMissileEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.MissileEnvironment;
import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 싱글플레이 미사일 환경.
 */
public class GameplayMissileEnvironment implements MissileEnvironment {
    private final Game game;

    public GameplayMissileEnvironment(Game game) {
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
    public List<Entity> getEntities() {
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

    @Override
    public void onAlienHit(BaseMissileEntity missile, Entity alien) {
        removeEntity(missile);
        removeEntity(alien);
        notifyAlienKilled(missile.getOwnerId(), alien.getX(), alien.getY());
    }

    @Override
    public void onBossHit(BaseMissileEntity missile, Entity boss) {
        try {
            boss.getClass().getMethod("takeDamage", int.class)
                    .invoke(boss, 50);
        } catch (Exception ignore) {
        }
        removeEntity(missile);
    }
}
