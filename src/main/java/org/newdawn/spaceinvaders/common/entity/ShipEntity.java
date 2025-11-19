package org.newdawn.spaceinvaders.common.entity;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import org.newdawn.spaceinvaders.common.GameContext;

/**
 * 싱글/멀티 공용 플레이어 우주선 엔티티.
 */
public class ShipEntity extends Entity {
    private final GameContext game;

    public ShipEntity(GameContext game, String ref, int x, int y) {
        super(ref, x, y);
        this.game = game;
    }

    @Override
    public void move(long delta) {
        applyMagneticFieldEffects();

        if ((dx < 0) && (x < 10)) {
            return;
        }
        if ((dx > 0) && (x > 750)) {
            return;
        }

        super.move(delta);
    }

    private void applyMagneticFieldEffects() {
        if (game == null) {
            return;
        }
        List<Entity> entities = game.getEntities();
        if (entities == null) {
            return;
        }
        for (Entity entity : entities) {
            if (entity == this) {
                continue;
            }
            if (entity instanceof MagneticFieldEntity) {
                MagneticFieldEntity field = (MagneticFieldEntity) entity;
                applyField(field.isActive(), field.getX(), field.getY(), field.getCurrentRadius(), field.getFieldStrength());
            }
        }
    }

    private void applyField(boolean active, double fieldX, double fieldY, double radius, double strength) {
        if (!active) {
            return;
        }
        double dxToField = fieldX - this.x;
        double dyToField = fieldY - this.y;
        double distance = Math.sqrt(dxToField * dxToField + dyToField * dyToField);
        if (distance <= 0 || distance > radius) {
            return;
        }

        double force = strength * (1.0 - distance / radius);
        double normalizedX = dxToField / distance;
        double normalizedY = dyToField / distance;
        double magneticForceX = normalizedX * force * 0.5;
        double magneticForceY = normalizedY * force * 0.5;
        this.dx += magneticForceX;
        this.dy += magneticForceY;

        double maxVelocity = 0.8;
        double currentSpeed = Math.sqrt(this.dx * this.dx + this.dy * this.dy);
        if (currentSpeed > maxVelocity) {
            this.dx = (this.dx / currentSpeed) * maxVelocity;
            this.dy = (this.dy / currentSpeed) * maxVelocity;
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (isPlayerInvincible()) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        }
        sprite.draw(g2d, (int) x, (int) y);
        if (isPlayerInvincible()) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    private boolean isPlayerInvincible() {
        return game != null && game.isPlayerInvincible(getOwnerId());
    }

    public void pullTowards(double pullX, double pullY) {
        this.x += pullX;
        this.y += pullY;
        if (this.x < 10) {
            this.x = 10;
        }
        if (this.x > 750) {
            this.x = 750;
        }
        if (this.y < 50) {
            this.y = 50;
        }
        if (this.y > 500) {
            this.y = 500;
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (isAlien(other)) {
            if (game != null) {
                game.notifyDeath(getOwnerId());
            }
        }
    }

    private boolean isAlien(Entity other) {
        return other instanceof org.newdawn.spaceinvaders.gameplay.entity.AlienEntity
                || other instanceof org.newdawn.spaceinvaders.multyplay.entity.AlienEntity;
    }

    @Override
    public Rectangle getBounds() {
        int width = sprite != null ? sprite.getWidth() : 33;
        int height = sprite != null ? sprite.getHeight() : 23;
        int hitboxWidth = (int) Math.max(10, width * 0.8);
        int hitboxHeight = (int) Math.max(10, height * 0.8);
        int offsetX = (width - hitboxWidth) / 2;
        int offsetY = (height - hitboxHeight) / 2;
        return new Rectangle((int) x + offsetX, (int) y + offsetY, hitboxWidth, hitboxHeight);
    }
}
