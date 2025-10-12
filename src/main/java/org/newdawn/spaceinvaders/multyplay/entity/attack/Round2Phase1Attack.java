package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Graphics;
import java.awt.Image;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;
import org.newdawn.spaceinvaders.multyplay.sprite.SpriteStore;

/**
 * 멀티플레이용 2라운드 1단계 공격.
 */
public class Round2Phase1Attack extends Entity {
    private final MultiplayerGameContext game;
    private final int damage = 2;
    private final double targetY;
    private boolean phase1Complete = false;
    private final Image projectileImage;

    public Round2Phase1Attack(MultiplayerGameContext game, int x, int y) {
        super("sprites/Boss_Attack/2round3.gif", x, y);
        this.game = game;
        this.dy = 250;
        this.dx = 0;
        this.targetY = 400;
        Image loaded;
        try {
            loaded = SpriteStore.get().getSprite("sprites/Boss_Attack/2round3.gif").getImage();
        } catch (Exception ex) {
            loaded = null;
        }
        this.projectileImage = loaded;
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
        double[] angles = {0, Math.PI / 4, Math.PI / 2, 3 * Math.PI / 4, Math.PI, 5 * Math.PI / 4, 3 * Math.PI / 2, 7 * Math.PI / 4};
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
            ShipEntity ship = (ShipEntity) other;
            game.removeEntity(this);
            game.notifyPlayerDamaged(ship.getOwnerId(), damage);
        }
    }

    public boolean isPhase1Complete() {
        return phase1Complete;
    }
}
