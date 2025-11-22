package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Graphics;
import java.awt.Image;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 2 베기 공격.
 */
public class Round2RandomAttack extends BaseAttackEntity {
    private final int damage = 2;
    private final long slashDuration = 1600;
    private final long startTime;
    private final Image attackImage;

    public Round2RandomAttack(GameContext game, int x, int y) {
        super(game, "sprites/Boss_Attack/2round2.gif", x, y);
        this.x = x;
        this.y = 150;
        this.startTime = System.currentTimeMillis();
        this.dx = 0;
        this.dy = 0;
        attackImage = loadImageFromToolkit("sprites/Boss_Attack/2round2.gif");
    }

    @Override
    public void move(long delta) {
        checkAndRemoveIfExpired(startTime, slashDuration);
    }

    @Override
    public void draw(Graphics g) {
        if (attackImage != null) {
            int drawX = (int) x - 150;
            g.drawImage(attackImage, drawX, 150, 300, 400, null);
        } else {
            g.setColor(java.awt.Color.YELLOW);
            g.fillRect((int) x - 350, 150, 700, 400);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            if (isInvincible(ship)) {
                return;
            }
            double playerX = other.getX();
            double playerY = other.getY();
            boolean inSlashArea = (playerX >= (this.x - 150)) && (playerX <= (this.x + 150))
                    && (playerY >= 150) && (playerY <= 550);
            if (inSlashArea) {
                damageShip(ship, damage);
            }
        }
    }

    @Override
    public java.awt.Rectangle getBounds() {
        int width = 300;
        int height = 400;
        int drawX = (int) Math.round(x) - width / 2;
        return new java.awt.Rectangle(drawX, 150, width, height);
    }
}
