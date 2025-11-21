package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Rectangle;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 보스 공격 엔티티의 공통 베이스.
 */
public abstract class BaseAttackEntity extends Entity {
    protected final GameContext game;

    protected BaseAttackEntity(GameContext game, String spriteRef, int x, int y) {
        super(spriteRef, x, y);
        this.game = game;
    }

    protected void damageShip(ShipEntity ship, int damage) {
        if (ship != null) {
            game.notifyPlayerDamaged(ship.getOwnerId(), damage);
        } else {
            game.notifyPlayerDamaged(damage);
        }
    }

    protected boolean isInvincible(ShipEntity ship) {
        return game.isPlayerInvincible(ship != null ? ship.getOwnerId() : null);
    }

    protected Rectangle centeredBounds(int width, int height) {
        int drawX = (int) Math.round(x) - width / 2;
        int drawY = (int) Math.round(y) - height / 2;
        return new Rectangle(drawX, drawY, width, height);
    }

    protected Rectangle topLeftBounds(int width, int height) {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    protected Rectangle offsetBounds(int offsetX, int offsetY, int width, int height) {
        return new Rectangle((int) Math.round(x) + offsetX, (int) Math.round(y) + offsetY, width, height);
    }
}
