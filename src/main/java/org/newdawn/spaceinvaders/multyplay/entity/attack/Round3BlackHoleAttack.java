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
 * 멀티플레이용 3라운드 블랙홀 공격.
 */
public class Round3BlackHoleAttack extends Entity {
    private final MultiplayerGameContext game;
    private final int damage = 3;
    private final long attackDuration = 3000;
    private final long startTime;
    private final int attackWidth = 150;
    private final int attackHeight = 150;
    private final double targetX;
    private final double targetY;
    private Image attackImage;

    public Round3BlackHoleAttack(MultiplayerGameContext game, int startX, int startY) {
        super("sprites/Boss_Attack/3round.gif", startX, startY);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        this.targetX = startX;
        this.targetY = startY;

        double bossX = 400;
        double bossY = 120;

        double dx = targetX - bossX;
        double dy = targetY - bossY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        this.dx = (dx / distance) * 200;
        this.dy = (dy / distance) * 200;
        this.x = bossX;
        this.y = bossY;

        loadAttackImage();
    }

    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/3round.gif");
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
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 80) {
            game.removeEntity(this);
        }
        if (x < -50 || x > 850 || y < -50 || y > 650) {
            game.removeEntity(this);
        }
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
            g2d.setColor(Color.BLUE);
            g2d.fillRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
            g2d.setColor(Color.MAGENTA);
            g2d.drawRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
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
