package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;

/**
 * 멀티플레이용 4라운드 초록 구체 공격.
 */
public class Round4GreenSphereAttack extends Entity {
    private final MultiplayerGameContext game;
    private final int damage = 2;
    private final long attackDuration = 5000;
    private final long startTime;
    private Image sphereImage;
    private final int fallbackSize = 24;
    private final double scale = 0.6;

    public Round4GreenSphereAttack(MultiplayerGameContext game, int x, int y, double dirX, double dirY) {
        super("sprites/Boss_Attack/5round1.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        this.dx = dirX * 200;
        this.dy = dirY * 200;
        loadSphereImage();
    }

    private void loadSphereImage() {
        try {
            java.net.URL imageURL = getClass().getClassLoader().getResource("sprites/Boss_Attack/5round1.gif");
            if (imageURL != null) {
                sphereImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageURL);
            }
        } catch (Exception ignore) {
            sphereImage = null;
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
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            game.notifyPlayerDamaged(ship.getOwnerId(), damage);
            game.removeEntity(this);
        }
    }

    @Override
    public java.awt.Rectangle getBounds() {
        if (sphereImage != null) {
            int width = (int) Math.round(sphereImage.getWidth(null) * scale);
            int height = (int) Math.round(sphereImage.getHeight(null) * scale);
            int drawX = (int) Math.round(x) - width / 2;
            int drawY = (int) Math.round(y) - height / 2;
            return new java.awt.Rectangle(drawX, drawY, width, height);
        }
        return super.getBounds();
    }
}
