package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 멀티플레이용 2라운드 보스 4갈래 공격.
 */
public class Round2QuadAttack extends Entity {
    private final MultiplayerGameContext game;
    private final int damage = 2;
    private final int direction;
    private final long attackDuration = 3000;
    private final long startTime;
    private Image attackImage;

    public Round2QuadAttack(MultiplayerGameContext game, int x, int y, int direction) {
        super("sprites/Boss_Attack/2round1.gif", x, y);
        this.game = game;
        this.direction = direction;
        this.startTime = System.currentTimeMillis();
        double moveSpeed = 150;
        switch (direction) {
            case 0:
                this.dx = -moveSpeed * 0.3;
                this.dy = moveSpeed * 0.9;
                break;
            case 1:
                this.dx = 0;
                this.dy = moveSpeed;
                break;
            case 2:
                this.dx = moveSpeed * 0.3;
                this.dy = moveSpeed * 0.9;
                break;
            case 3:
            default:
                this.dx = moveSpeed * 0.5;
                this.dy = moveSpeed * 0.8;
                break;
        }
        loadAttackImage();
    }

    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/2round1.gif");
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
        if (x < -50 || x > 850 || y < -50 || y > 650) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (attackImage != null) {
            g.drawImage(attackImage, (int) x - 25, (int) y - 25, 50, 50, null);
        } else {
            g.setColor(java.awt.Color.ORANGE);
            g.fillOval((int) x - 15, (int) y - 15, 30, 30);
            g.setColor(java.awt.Color.RED);
            g.drawOval((int) x - 15, (int) y - 15, 30, 30);
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
            double collisionRadius = 25.0;
            if (distance <= collisionRadius) {
                game.notifyPlayerDamaged(ship.getOwnerId(), damage);
                game.removeEntity(this);
            }
        }
    }
}
