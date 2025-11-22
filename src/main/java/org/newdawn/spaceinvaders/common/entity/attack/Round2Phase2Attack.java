package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Graphics;
import java.awt.Image;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.sprite.SpriteStore;

/**
 * 라운드 2 단계 2 공격.
 */
public class Round2Phase2Attack extends BaseAttackEntity {
    private final int damage = 2;
    private final double angle;
    private final Image projectileImage;

    public Round2Phase2Attack(GameContext game, int x, int y, double angle) {
        super(game, "sprites/Boss_Attack/2round1.gif", x, y);
        this.angle = angle;
        this.dx = Math.cos(angle) * 200;
        this.dy = Math.sin(angle) * 200;
        this.projectileImage = loadImage("sprites/Boss_Attack/2round1.gif");
    }

    private Image loadImage(String ref) {
        return loadImageFromSpriteStore(ref);
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        checkAndRemoveIfOutOfBounds(-100, 800, -100, 600);
    }

    @Override
    public void draw(Graphics g) {
        if (projectileImage != null) {
            g.drawImage(projectileImage, (int) x - 25, (int) y - 25, 50, 50, null);
        } else {
            g.setColor(java.awt.Color.RED);
            g.fillRect((int) x - 25, (int) y - 25, 50, 50);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        double projectileRadius = 25;
        double shipRadius = 20;
        double collisionRadius = projectileRadius + shipRadius;
        handleShipCollisionWithRadius(other, damage, collisionRadius);
    }

    public int getDamage() {
        return damage;
    }

    public double getAngle() {
        return angle;
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(50, 50);
    }
}
