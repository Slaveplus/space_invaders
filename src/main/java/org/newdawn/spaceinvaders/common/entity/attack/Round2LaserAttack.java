package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.Toolkit;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 2 레이저 공격.
 */
public class Round2LaserAttack extends BaseAttackEntity {
    private int damage = 3;
    private Image laserImage;
    private final double rotationAngle = Math.PI / 2;
    private final double laserLength = 700;
    private final double laserWidth = 300;
    private final long startTime;

    public Round2LaserAttack(GameContext game, int x, int y) {
        super(game, "sprites/Boss_Attack/2round.gif", x, y);
        this.startTime = System.currentTimeMillis();
        loadLaserImage();
    }

    private void loadLaserImage() {
        laserImage = loadImageFromToolkit("sprites/Boss_Attack/2round.gif");
    }

    @Override
    public void move(long delta) {
        checkAndRemoveIfExpired(startTime, 3000);
    }

    @Override
    public void draw(Graphics g) {
        if (laserImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            AffineTransform original = g2d.getTransform();
            g2d.translate(x + laserLength / 2.0, y + laserWidth / 2.0);
            g2d.rotate(rotationAngle);
            g2d.drawImage(
                    laserImage,
                    -(int) (laserLength / 2),
                    -(int) (laserWidth / 2),
                    (int) laserLength,
                    (int) laserWidth,
                    null);
            g2d.setTransform(original);
        } else {
            g.setColor(java.awt.Color.CYAN);
            g.fillRect((int) x, (int) y, (int) laserLength, (int) laserWidth);
            g.setColor(java.awt.Color.BLUE);
            g.drawRect((int) x, (int) y, (int) laserLength, (int) laserWidth);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        handleShipCollision(other, damage);
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return topLeftBounds((int) laserLength, (int) laserWidth);
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }
}
