package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Graphics;
import java.awt.Image;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.sprite.SpriteStore;

/**
 * 멀티플레이용 2라운드 2단계 공격.
 */
public class Round2Phase2Attack extends Entity {
    private final MultiplayerGameContext game;
    private final int damage = 2;
    private final double angle;
    private final Image projectileImage;

    public Round2Phase2Attack(MultiplayerGameContext game, int x, int y, double angle) {
        super("sprites/Boss_Attack/2round1.gif", x, y);
        this.game = game;
        this.angle = angle;
        this.dx = Math.cos(angle) * 200;
        this.dy = Math.sin(angle) * 200;
        Image loaded;
        try {
            loaded = SpriteStore.get().getSprite("sprites/Boss_Attack/2round1.gif").getImage();
        } catch (Exception ex) {
            loaded = null;
        }
        projectileImage = loaded;
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (y > 600 || y < -100 || x < -100 || x > 800) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (projectileImage != null) {
            g.drawImage(projectileImage, (int) x - 25, (int) y - 25, 50, 50, null);
        } else {
            g.setColor(java.awt.Color.RED);
            g.fillRect((int) x - 25, (int) y - 25, 50, 50);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            if (game.isPlayerInvincible(ship.getOwnerId())) {
                game.removeEntity(this);
                return;
            }
            double distance = Math.sqrt(
                    Math.pow(this.x - other.getX(), 2) + Math.pow(this.y - other.getY(), 2));
            double projectileRadius = 25;
            double shipRadius = 20;
            if (distance <= (projectileRadius + shipRadius)) {
                game.removeEntity(this);
                game.notifyPlayerDamaged(ship.getOwnerId(), damage);
            }
        }
    }

    public int getDamage() {
        return damage;
    }

    public double getAngle() {
        return angle;
    }
}
