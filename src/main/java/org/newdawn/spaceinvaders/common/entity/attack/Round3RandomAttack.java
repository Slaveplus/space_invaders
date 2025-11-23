package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity; 

/**
 * 라운드 3 랜덤 레이저 공격.
 */
public class Round3RandomAttack extends BaseAttackEntity {
    private final int damage = 2;
    private final long attackDuration = 3000;
    private final long damageDelay = 500;
    private final long startTime;
    private boolean damageApplied;
    private final int attackWidth = 400;
    private final int attackHeight = 1400;
    private Image attackImage;

    public Round3RandomAttack(GameContext game, int x, int y) {
        super(game, "sprites/Boss_Attack/3round2.gif", x, y);
        this.startTime = System.currentTimeMillis();
        loadAttackImage();
    }

    private void loadAttackImage() {
        attackImage = loadImageWithMediaTracker("sprites/Boss_Attack/3round2.gif");
    }

    @Override
    public void move(long delta) {
        checkAndRemoveIfExpired(startTime, attackDuration);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (attackImage != null) {
            int drawX = (int) x - attackWidth / 2;
            int drawY = (int) y - attackHeight / 2;
            g2d.drawImage(attackImage, drawX, drawY, drawX + attackWidth, drawY + attackHeight,
                    0, 0, attackImage.getWidth(null), attackImage.getHeight(null), null);
        } else {
            g2d.setColor(Color.RED);
            g2d.fillRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
            g2d.setColor(Color.YELLOW);
            g2d.drawRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
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
            boolean inLaserArea = (playerX >= (this.x - attackWidth / 2)) && (playerX <= (this.x + attackWidth / 2))
                    && (playerY >= (this.y - attackHeight / 2)) && (playerY <= (this.y + attackHeight / 2));
            if (inLaserArea) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (!damageApplied && elapsed >= damageDelay) {
                    damageShip(ship, damage);
                    damageApplied = true;
                }
            }
        }
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(attackWidth, attackHeight);
    }
}
