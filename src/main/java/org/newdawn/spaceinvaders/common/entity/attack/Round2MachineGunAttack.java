package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 2 기관총 공격.
 */
public class Round2MachineGunAttack extends BaseAttackEntity {
    private final int damage = 1;
    private final double targetY = 300;
    private Image attackImage;

    public Round2MachineGunAttack(GameContext game, int x, int y) {
        super(game, "sprites/Boss_Attack/2round1.gif", x, y);
        this.x = x;
        this.y = 0;
        this.dx = 0;
        this.dy = 180;
        loadAttackImage();
    }

    private void loadAttackImage() {
        attackImage = loadImageWithMediaTracker("sprites/Boss_Attack/2round1.gif");
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (y >= targetY) {
            game.removeEntity(this);
            return;
        }
        checkAndRemoveIfOutOfBounds(-50, 850, -50, 650);
    }

    @Override
    public void draw(Graphics g) {
        if (attackImage != null) {
            g.drawImage(attackImage, (int) x - 15, (int) y - 15, 30, 30, null);
        } else {
            g.setColor(java.awt.Color.YELLOW);
            g.fillOval((int) x - 10, (int) y - 10, 20, 20);
            g.setColor(java.awt.Color.ORANGE);
            g.drawOval((int) x - 10, (int) y - 10, 20, 20);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        handleShipCollision(other, damage);
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(30, 30);
    }
}
