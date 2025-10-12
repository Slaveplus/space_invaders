package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;

/**
 * 멀티플레이용 4라운드 초록 구체 공격.
 */
public class Round4GreenSphereAttack extends Entity {
    private final MultiplayerGameContext game;
    private final int damage = 2;
    private final long attackDuration = 5000;
    private final long startTime;
    private final int sphereSize = 8;

    public Round4GreenSphereAttack(MultiplayerGameContext game, int x, int y, double dirX, double dirY) {
        super("sprites/Skill/Heat.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        this.dx = dirX * 200;
        this.dy = dirY * 200;
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (System.currentTimeMillis() - startTime > attackDuration) {
            game.removeEntity(this);
            return;
        }
        if (x < -50 || x > 850 || y < -50 || y > 650) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.GREEN);
        g2d.fillOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
        g2d.setColor(Color.WHITE);
        g2d.drawOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
        g2d.setColor(new Color(0, 255, 0, 100));
        g2d.fillOval((int) x - sphereSize / 4, (int) y - sphereSize / 4, sphereSize / 2, sphereSize / 2);
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
