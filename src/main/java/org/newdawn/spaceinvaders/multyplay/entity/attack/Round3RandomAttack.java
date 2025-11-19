package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 멀티플레이용 3라운드 랜덤 레이저 공격.
 */
public class Round3RandomAttack extends Entity {
    private final MultiplayerGameContext game;
    private final int damage = 2;
    private final long attackDuration = 3000;
    private final long damageDelay = 500;
    private final long startTime;
    private boolean damageApplied = false;
    private final int attackWidth = 400;
    private final int attackHeight = 1400;
    private Image attackImage;

    public Round3RandomAttack(MultiplayerGameContext game, int x, int y) {
        super("sprites/Boss_Attack/3round2.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        this.dx = 0;
        this.dy = 0;
        loadAttackImage();
    }

    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/3round2.gif");
            if (imageUrl != null) {
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                MediaTracker tracker = new MediaTracker(new java.awt.Canvas());
                tracker.addImage(attackImage, 0);
                tracker.waitForAll();
                if (tracker.isErrorAny()) {
                    attackImage = null;
                }
            }
        } catch (Exception e) {
            attackImage = null;
        }
    }

    @Override
    public void move(long delta) {
        if (System.currentTimeMillis() - startTime > attackDuration) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (attackImage != null) {
            int drawX = (int) x - attackWidth / 2;
            int drawY = (int) y - attackHeight / 2;
            g2d.drawImage(attackImage, drawX, drawY, drawX + attackWidth, drawY + attackHeight, 0, 0,
                    attackImage.getWidth(null), attackImage.getHeight(null), null);
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
            if (game.isPlayerInvincible(ship.getOwnerId())) {
                return;
            }
            double playerX = other.getX();
            double playerY = other.getY();
            boolean inLaserArea = (playerX >= (this.x - attackWidth / 2)) && (playerX <= (this.x + attackWidth / 2))
                    && (playerY >= (this.y - attackHeight / 2)) && (playerY <= (this.y + attackHeight / 2));

            if (inLaserArea) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (!damageApplied && elapsed >= damageDelay) {
                    game.notifyPlayerDamaged(ship.getOwnerId(), damage);
                    damageApplied = true;
                }
            }
        }
    }
}
