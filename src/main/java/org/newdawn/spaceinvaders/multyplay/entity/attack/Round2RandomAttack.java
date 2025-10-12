package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Graphics;
import java.awt.Image;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;

/**
 * 멀티플레이용 2라운드 보스 베기 공격.
 */
public class Round2RandomAttack extends Entity {
    private final MultiplayerGameContext game;
    private final int damage = 2;
    private final long slashDuration = 1600;
    private final long startTime;
    private final Image attackImage;

    public Round2RandomAttack(MultiplayerGameContext game, int x, int y) {
        super("sprites/Boss_Attack/2round2.gif", x, y);
        this.game = game;
        this.x = x;
        this.y = 150;
        this.dx = 0;
        this.dy = 0;
        this.startTime = System.currentTimeMillis();
        Image loaded;
        try {
            java.net.URL imageURL = getClass().getClassLoader().getResource("sprites/Boss_Attack/2round2.gif");
            loaded = imageURL != null ? java.awt.Toolkit.getDefaultToolkit().createImage(imageURL) : null;
        } catch (Exception ex) {
            loaded = null;
        }
        attackImage = loaded;
    }

    @Override
    public void move(long delta) {
        if (System.currentTimeMillis() - startTime > slashDuration) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (attackImage != null) {
            int drawX = (int) x - 150;
            int drawY = 150;
            int drawWidth = 300;
            int drawHeight = 400;
            g.drawImage(attackImage, drawX, drawY, drawWidth, drawHeight, null);
        } else {
            g.setColor(java.awt.Color.YELLOW);
            g.fillRect((int) x - 350, 150, 700, 400);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            if (game.isPlayerInvincible(ship.getOwnerId())) {
                return;
            }
            double playerX = other.getX();
            double playerY = other.getY();
            boolean inSlashArea = (playerX >= (this.x - 150)) && (playerX <= (this.x + 150)) &&
                    (playerY >= 150) && (playerY <= 550);
            if (inSlashArea) {
                game.notifyPlayerDamaged(ship.getOwnerId(), damage);
            }
        }
    }
}
