package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 멀티플레이용 3라운드 직선 공격.
 */
public class Round3StraightAttack extends Entity {
    private final MultiplayerGameContext game;
    private final int damage = 3;
    private final long attackDuration = 5000;
    private final long startTime;
    private final int attackWidth = 600;
    private final int attackHeight = 300;
    private Image attackImage;

    public Round3StraightAttack(MultiplayerGameContext game, int x, int y) {
        super("sprites/Boss_Attack/3round4.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        this.dx = 0;
        this.dy = 120;
        loadAttackImage();
    }

    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/3round4.gif");
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
        super.move(delta);
        if (System.currentTimeMillis() - startTime > attackDuration) {
            game.removeEntity(this);
            return;
        }
        if (x < -attackWidth || x > 800 + attackWidth || y < -attackHeight || y > 600 + attackHeight) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (attackImage != null) {
            int drawX = (int) x - attackWidth / 2;
            int drawY = (int) y - attackHeight / 2;
            g.drawImage(attackImage, drawX, drawY, attackWidth, attackHeight, null);
        } else {
            g.setColor(java.awt.Color.RED);
            g.fillRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
            g.setColor(java.awt.Color.DARK_GRAY);
            g.drawRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
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
            double dx = this.x - other.getX();
            double dy = this.y - other.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            double collisionRadius = Math.min(attackWidth, attackHeight) / 2.0;
            if (distance <= collisionRadius) {
                game.notifyPlayerDamaged(ship.getOwnerId(), damage);
                game.removeEntity(this);
            }
        }
    }
}
