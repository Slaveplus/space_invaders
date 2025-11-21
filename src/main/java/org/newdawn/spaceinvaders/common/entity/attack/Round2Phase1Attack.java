package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Graphics;
import java.awt.Image;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.sprite.SpriteStore;

/**
 * 라운드 2 단계 1 공격 (확산 전개).
 */
public class Round2Phase1Attack extends BaseAttackEntity {
    private final int damage = 2;
    private final double targetY = 400;
    private boolean phase1Complete;
    private final Image projectileImage;

    public Round2Phase1Attack(GameContext game, int x, int y) {
        super(game, "sprites/Boss_Attack/2round3.gif", x, y);
        this.dy = 250;
        this.dx = 0;
        this.projectileImage = loadImage("sprites/Boss_Attack/2round3.gif");
    }

    private Image loadImage(String ref) {
        try {
            return SpriteStore.get().getSprite(ref).getImage();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (y >= targetY && !phase1Complete) {
            phase1Complete = true;
            launchPhase2();
            game.removeEntity(this);
            return;
        }
        if (y > 600 || y < -100) {
            game.removeEntity(this);
        }
    }

    private void launchPhase2() {
        double[] angles = {0, Math.PI / 4, Math.PI / 2, 3 * Math.PI / 4, Math.PI,
                5 * Math.PI / 4, 3 * Math.PI / 2, 7 * Math.PI / 4};
        for (double angle : angles) {
            Round2Phase2Attack phase2 = new Round2Phase2Attack(game, (int) x, (int) y, angle);
            game.addEntity(phase2);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (projectileImage != null) {
            g.drawImage(projectileImage, (int) x - 40, (int) y - 40, 80, 80, null);
        } else {
            g.setColor(java.awt.Color.ORANGE);
            g.fillRect((int) x - 40, (int) y - 40, 80, 80);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            damageShip((ShipEntity) other, damage);
            game.removeEntity(this);
        }
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(80, 80);
    }

    public boolean isPhase1Complete() {
        return phase1Complete;
    }
}
