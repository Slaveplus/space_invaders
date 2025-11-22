package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 3 직선 돌진 공격.
 */
public class Round3StraightAttack extends BaseAttackEntity {
    private final int damage = 3;
    private final long attackDuration = 5000;
    private final long startTime;
    private final int attackWidth = 600;
    private final int attackHeight = 300;
    private Image attackImage;

    public Round3StraightAttack(GameContext game, int x, int y) {
        super(game, "sprites/Boss_Attack/3round4.gif", x, y);
        this.startTime = System.currentTimeMillis();
        this.dy = 120;
        loadAttackImage();
    }

    private void loadAttackImage() {
        attackImage = loadImageWithMediaTracker("sprites/Boss_Attack/3round4.gif");
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (checkAndRemoveIfExpired(startTime, attackDuration)) {
            return;
        }
        checkAndRemoveIfOutOfBounds(-attackWidth, 800 + attackWidth, -attackHeight, 600 + attackHeight);
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
        double collisionRadius = Math.min(attackWidth, attackHeight) / 2.0;
        handleShipCollisionWithRadius(other, damage, collisionRadius);
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(attackWidth, attackHeight);
    }
}
