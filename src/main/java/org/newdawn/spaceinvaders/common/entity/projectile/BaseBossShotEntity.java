package org.newdawn.spaceinvaders.common.entity.projectile;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 싱글/멀티 공용 BossShot 베이스 클래스.
 */
public abstract class BaseBossShotEntity extends Entity {
    protected final BossShotEnvironment environment;
    protected double directionX;
    protected double directionY;
    protected double speed;
    protected int radius;
    protected boolean canSplit;
    protected double splitY;
    protected int splitCount;
    protected boolean hasSplit;
    protected boolean used;

    protected BaseBossShotEntity(BossShotEnvironment environment,
                                 int x,
                                 int y,
                                 double directionX,
                                 double directionY,
                                 double speed,
                                 int radius,
                                 boolean canSplit,
                                 double splitY,
                                 int splitCount) {
        super(SpriteConstants.SHOT_GIF, x, y);
        this.environment = environment;
        this.directionX = directionX;
        this.directionY = directionY;
        this.speed = speed;
        this.radius = radius;
        this.canSplit = canSplit;
        this.splitY = splitY;
        this.splitCount = splitCount;
        this.dx = directionX * speed;
        this.dy = directionY * speed;
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (canSplit && !hasSplit && y >= splitY) {
            split();
            hasSplit = true;
            environment.removeEntity(this);
            return;
        }
        if (y > 750 || y < -100 || x < -100 || x > 900) {
            environment.removeEntity(this);
        }
    }

    private void split() {
        for (int i = 0; i < splitCount; i++) {
            double angle = 2 * Math.PI * i / splitCount;
            double newDirX = Math.cos(angle);
            double newDirY = Math.sin(angle);
            BaseBossShotEntity child = createChild((int) x, (int) y, newDirX, newDirY, speed * 0.8, Math.max(4, radius / 2));
            environment.addEntity(child);
        }
    }

    protected abstract BaseBossShotEntity createChild(int x,
                                                      int y,
                                                      double directionX,
                                                      double directionY,
                                                      double speed,
                                                      int radius);

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int centerX = (int) x + sprite.getWidth() / 2;
        int centerY = (int) y + sprite.getHeight() / 2;
        int glowAlpha = canSplit ? 100 : 60;
        int coreAlpha = canSplit ? 240 : 220;
        g2d.setColor(new Color(255, 100, 100, glowAlpha));
        g2d.fillOval(centerX - radius - 3, centerY - radius - 3, (radius + 3) * 2, (radius + 3) * 2);
        g2d.setColor(new Color(255, 50, 50, coreAlpha));
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        if (radius > 4) {
            g2d.setColor(new Color(255, 200, 200, 180));
            g2d.fillOval(centerX - radius + 2, centerY - radius + 2, (radius - 2) * 2, (radius - 2) * 2);
        }
        if (radius > 6) {
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillOval(centerX - radius + 4, centerY - radius + 4, (radius - 4) * 2, (radius - 4) * 2);
        }
        if (speed > 0 && radius > 4) {
            int trailDistance = Math.max(8, radius);
            int trailX = centerX - (int) (directionX * trailDistance);
            int trailY = centerY - (int) (directionY * trailDistance);
            g2d.setColor(new Color(255, 100, 100, 80));
            int trailRadius = Math.max(3, radius - 1);
            g2d.fillOval(trailX - trailRadius, trailY - trailRadius, trailRadius * 2, trailRadius * 2);
            trailX = centerX - (int) (directionX * trailDistance * 1.5);
            trailY = centerY - (int) (directionY * trailDistance * 1.5);
            g2d.setColor(new Color(255, 100, 100, 40));
            trailRadius = Math.max(2, radius - 2);
            g2d.fillOval(trailX - trailRadius, trailY - trailRadius, trailRadius * 2, trailRadius * 2);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (used) {
            return;
        }
        if (other instanceof ShipEntity) {
            environment.removeEntity(this);
            environment.notifyPlayerHit(other.getOwnerId());
            used = true;
        }
    }

    public double getDirectionX() {
        return directionX;
    }

    public double getDirectionY() {
        return directionY;
    }

    public double getSpeed() {
        return speed;
    }

    public boolean isUsed() {
        return used;
    }

    @Override
    public Rectangle getBounds() {
        int effectiveRadius = Math.max(4, radius);
        int centerX = (int) Math.round(x + (sprite != null ? sprite.getWidth() / 2.0 : effectiveRadius));
        int centerY = (int) Math.round(y + (sprite != null ? sprite.getHeight() / 2.0 : effectiveRadius));
        int diameter = effectiveRadius * 2;
        return new Rectangle(centerX - effectiveRadius, centerY - effectiveRadius, diameter, diameter);
    }
}
