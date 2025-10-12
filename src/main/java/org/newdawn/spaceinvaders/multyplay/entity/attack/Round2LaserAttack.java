package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;

/**
 * 멀티플레이용 2라운드 보스 레이저 공격.
 */
public class Round2LaserAttack extends Entity {
    private final MultiplayerGameContext game;
    private int damage = 3;
    private Image laserImage;
    private final double rotationAngle = Math.PI / 2;
    private final double laserLength = 700;
    private final double laserWidth = 300;
    private final long startTime;

    public Round2LaserAttack(MultiplayerGameContext game, int x, int y) {
        super("sprites/Boss_Attack/2round.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        loadLaserImage();
    }

    private void loadLaserImage() {
        try {
            java.net.URL imageURL = getClass().getClassLoader().getResource("sprites/Boss_Attack/2round.gif");
            if (imageURL != null) {
                laserImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageURL);
            }
        } catch (Exception e) {
            laserImage = null;
        }
    }

    @Override
    public void move(long delta) {
        if (System.currentTimeMillis() - startTime > 3000) {
            game.removeEntity(this);
        }
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
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            game.removeEntity(this);
            game.notifyPlayerDamaged(ship.getOwnerId(), damage);
        }
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }
}
