package org.newdawn.spaceinvaders.common.entity.attack;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 2 보스의 아이스 볼 공격.
 */
public class IceBallAttack extends BaseAttackEntity {
    private final double moveSpeed = 150;
    private final int damage = 1;
    private final double scale = 0.6;
    private final double dirX;
    private final double dirY;
    private Image iceBallImage;

    public IceBallAttack(GameContext game, int x, int y, double dirX, double dirY) {
        super(game, SpriteConstants.ICE_BALL_GIF, x, y);
        this.dirX = dirX;
        this.dirY = dirY;
        this.dx = dirX * moveSpeed;
        this.dy = dirY * moveSpeed;
        loadIceBallImage();
    }

    private void loadIceBallImage() {
        iceBallImage = loadImageFromToolkit(SpriteConstants.ICE_BALL_GIF);
    }

    @Override
    public void move(long delta) {
        x += (delta * dx) / 1000;
        y += (delta * dy) / 1000;
        checkAndRemoveIfOutOfBounds(-100, 800, -100, 600);
    }

    @Override
    public void draw(Graphics g) {
        if (iceBallImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int scaledWidth = (int) (iceBallImage.getWidth(null) * scale);
            int scaledHeight = (int) (iceBallImage.getHeight(null) * scale);
            int drawX = (int) x - scaledWidth / 2;
            int drawY = (int) y - scaledHeight / 2;
            g2d.drawImage(iceBallImage, drawX, drawY, scaledWidth, scaledHeight, null);
        } else {
            g.setColor(java.awt.Color.CYAN);
            g.fillOval((int) x - 10, (int) y - 10, 20, 20);
        }
    }

    @Override
    public Rectangle getBounds() {
        if (iceBallImage != null) {
            int scaledWidth = (int) (iceBallImage.getWidth(null) * scale);
            int scaledHeight = (int) (iceBallImage.getHeight(null) * scale);
            int drawX = (int) x - scaledWidth / 2;
            int drawY = (int) y - scaledHeight / 2;
            return new Rectangle(drawX, drawY, scaledWidth, scaledHeight);
        }
        return new Rectangle((int) x - 8, (int) y - 8, 16, 16);
    }

    @Override
    public void collidedWith(Entity other) {
        handleShipCollision(other, damage);
    }
}
