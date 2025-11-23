package org.newdawn.spaceinvaders.common.entity.attack;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 4 초록 구체 공격.
 */
public class Round4GreenSphereAttack extends BaseAttackEntity {
    private final int damage = 2;
    private final long attackDuration = 5000;
    private final long startTime;
    private final int fallbackSize = 24;
    private final double scale = 0.6;
    private Image sphereImage;

    public Round4GreenSphereAttack(GameContext game, int x, int y, double dirX, double dirY) {
        super(game, SpriteConstants.BOSS_ATTACK_5ROUND1_GIF, x, y);
        this.startTime = System.currentTimeMillis();
        this.dx = dirX * 200;
        this.dy = dirY * 200;
        loadSphereImage();
    }

    private void loadSphereImage() {
        sphereImage = loadImageFromToolkit(SpriteConstants.BOSS_ATTACK_5ROUND1_GIF);
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
        Graphics2D g2d = (Graphics2D) g;
        if (sphereImage != null) {
            int width = (int) Math.round(sphereImage.getWidth(null) * scale);
            int height = (int) Math.round(sphereImage.getHeight(null) * scale);
            int drawX = (int) Math.round(x) - width / 2;
            int drawY = (int) Math.round(y) - height / 2;
            g2d.drawImage(sphereImage, drawX, drawY, width, height, null);
        } else {
            int drawX = (int) Math.round(x) - fallbackSize / 2;
            int drawY = (int) Math.round(y) - fallbackSize / 2;
            g2d.setColor(new java.awt.Color(0, 255, 120, 220));
            g2d.fillOval(drawX, drawY, fallbackSize, fallbackSize);
            g2d.setColor(java.awt.Color.WHITE);
            g2d.drawOval(drawX, drawY, fallbackSize, fallbackSize);
        }
    }

    @Override
    public Rectangle getBounds() {
        if (sphereImage != null) {
            int width = (int) Math.round(sphereImage.getWidth(null) * scale);
            int height = (int) Math.round(sphereImage.getHeight(null) * scale);
            int trimmedWidth = Math.max(12, (int) (width * 0.8));
            int trimmedHeight = Math.max(12, (int) (height * 0.8));
            int drawX = (int) Math.round(x) - trimmedWidth / 2;
            int drawY = (int) Math.round(y) - trimmedHeight / 2;
            return new Rectangle(drawX, drawY, trimmedWidth, trimmedHeight);
        }
        return super.getBounds();
    }

    @Override
    public void collidedWith(Entity other) {
        handleShipCollision(other, damage);
    }
}
