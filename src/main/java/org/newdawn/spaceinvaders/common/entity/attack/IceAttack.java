package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 라운드 2 보스가 사용하는 공통 아이스 공격.
 */
public class IceAttack extends BaseAttackEntity {
    private final double moveSpeed = 200;
    private final int damage = 3;
    private final double scale = 0.8;
    private Image iceImage;

    public IceAttack(GameContext game, int x, int y) {
        super(game, "sprites/Boss_Attack/ice.gif", x, y);
        this.dy = moveSpeed;
        loadIceImage();
    }

    private void loadIceImage() {
        iceImage = loadImageFromToolkit("sprites/Boss_Attack/ice.gif");
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        checkAndRemoveIfOutOfBoundsY(600);
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
        return new Rectangle((int) x - 8, (int) y - 8, 16, 16);
    }

    @Override
    public void collidedWith(Entity other) {
        handleShipCollision(other, damage);
    }
}
