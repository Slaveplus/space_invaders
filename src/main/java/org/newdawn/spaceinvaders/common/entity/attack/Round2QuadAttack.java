package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 2 사방 발사 공격.
 */
public class Round2QuadAttack extends BaseAttackEntity {
    private final int damage = 2;
    private final long attackDuration = 3000;
    private final long startTime;
    private final int direction;
    private Image attackImage;

    public Round2QuadAttack(GameContext game, int x, int y, int direction) {
        super(game, "sprites/Boss_Attack/2round1.gif", x, y);
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
        attackImage = loadImageWithMediaTracker("sprites/Boss_Attack/2round1.gif");
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (checkAndRemoveIfExpired(startTime, attackDuration)) {
            return;
        }
        checkAndRemoveIfOutOfBounds();
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
        handleShipCollisionWithRadius(other, damage, 25.0);
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(50, 50);
    }
}
