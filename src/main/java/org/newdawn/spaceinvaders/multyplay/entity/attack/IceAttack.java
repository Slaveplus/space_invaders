package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;

/**
 * 멀티플레이용 아이스 공격 엔티티.
 */
public class IceAttack extends Entity {
    private final MultiplayerGameContext game;
    private double moveSpeed = 200;
    private int damage = 3;
    private Image iceImage;
    private double scale = 0.8;

    public IceAttack(MultiplayerGameContext game, int x, int y) {
        super("sprites/Boss_Attack/ice.gif", x, y);
        this.game = game;
        this.dy = moveSpeed;
        loadIceImage();
    }

    private void loadIceImage() {
        try {
            java.net.URL imageURL = getClass().getClassLoader().getResource("sprites/Boss_Attack/ice.gif");
            if (imageURL != null) {
                iceImage = Toolkit.getDefaultToolkit().createImage(imageURL);
            }
        } catch (Exception e) {
            iceImage = null;
        }
    }

    @Override
    public void move(long delta) {
        y += (delta * dy) / 1000;
        if (y > 600) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (iceImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int scaledWidth = (int) (iceImage.getWidth(null) * scale);
            int scaledHeight = (int) (iceImage.getHeight(null) * scale);
            int drawX = (int) x - scaledWidth / 2;
            int drawY = (int) y - scaledHeight / 2;

            g2d.drawImage(iceImage, drawX, drawY, scaledWidth, scaledHeight, null);
        } else {
            g.setColor(java.awt.Color.CYAN);
            g.fillOval((int) x - 10, (int) y - 10, 20, 20);
        }
    }

    @Override
    public Rectangle getBounds() {
        if (iceImage != null) {
            int scaledWidth = (int) (iceImage.getWidth(null) * scale);
            int scaledHeight = (int) (iceImage.getHeight(null) * scale);
            int drawX = (int) x - scaledWidth / 2;
            int drawY = (int) y - scaledHeight / 2;
            return new Rectangle(drawX, drawY, scaledWidth, scaledHeight);
        }
        return new Rectangle((int) x - 10, (int) y - 10, 20, 20);
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            game.notifyPlayerDamaged(ship.getOwnerId(), damage);
            game.removeEntity(this);
        }
    }
}
