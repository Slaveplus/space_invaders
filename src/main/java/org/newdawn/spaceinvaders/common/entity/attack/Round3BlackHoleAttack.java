package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 3 블랙홀 공격.
 */
public class Round3BlackHoleAttack extends BaseAttackEntity {
    private final int damage = 3;
    private final long attackDuration = 3000;
    private final long startTime;
    private final int attackWidth = 150;
    private final int attackHeight = 150;
    private final double targetX;
    private final double targetY;
    private Image attackImage;

    public Round3BlackHoleAttack(GameContext game, int startX, int startY) {
        super(game, "sprites/Boss_Attack/3round.gif", startX, startY);
        this.startTime = System.currentTimeMillis();
        this.targetX = startX;
        this.targetY = startY;
        double bossX = 400;
        double bossY = 120;
        double dx = targetX - bossX;
        double dy = targetY - bossY;
        double distance = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        this.dx = (dx / distance) * 200;
        this.dy = (dy / distance) * 200;
        this.x = bossX;
        this.y = bossY;
        loadAttackImage();
    }

    private void loadAttackImage() {
        attackImage = loadImageWithMediaTracker("sprites/Boss_Attack/3round.gif");
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (checkAndRemoveIfExpired(startTime, attackDuration)) {
            return;
        }
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 80) {
            game.removeEntity(this);
            return;
        }
        checkAndRemoveIfOutOfBounds();
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
        double collisionRadius = Math.min(attackWidth, attackHeight) / 2.0;
        handleShipCollisionWithRadius(other, damage, collisionRadius);
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(attackWidth, attackHeight);
    }
}
