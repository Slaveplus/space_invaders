package org.newdawn.spaceinvaders.common.entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import org.newdawn.spaceinvaders.common.GameContext;

/**
 * 공용 자기장 엔티티. GameContext를 통해 싱글/멀티 양쪽에서 재사용된다.
 */
public class MagneticFieldEntity extends Entity {
    protected final GameContext game;
    protected double fieldRadius;
    protected double fieldStrength;
    private final long duration;
    private final long startTime;
    private boolean active = true;
    private double currentRadius;

    public MagneticFieldEntity(GameContext game, int x, int y, double radius, double strength, long duration) {
        super("sprites/shot.gif", x, y);
        this.game = game;
        this.fieldRadius = radius;
        this.fieldStrength = strength;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.currentRadius = radius;
    }

    @Override
    public void move(long delta) {
        if (!active) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - startTime > duration) {
            active = false;
            onExpired();
            return;
        }
        currentRadius = fieldRadius;
        applyMagneticEffects();
    }

    protected void onExpired() {
        if (game != null) {
            game.removeEntity(this);
        }
    }

    private void applyMagneticEffects() {
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
            double distance = distanceTo(entity);
            if (distance <= currentRadius && distance > 0) {
                double force = fieldStrength * (1.0 - distance / currentRadius);
                double angle = Math.atan2(entity.getY() - y, entity.getX() - x);
                double forceX = Math.cos(angle) * force;
                double forceY = Math.sin(angle) * force;

                String type = entity.getClass().getSimpleName();
                if ("ShipEntity".equals(type)) {
                    applyForceToShip(entity, forceX, forceY);
                } else if ("ShotEntity".equals(type)) {
                    applyForceToShot(entity, forceX, forceY);
                }
            }
        }
    }

    private void applyForceToShip(Entity ship, double forceX, double forceY) {
        ship.setDX(ship.getDX() + forceX * 0.001);
        ship.setDY(ship.getDY() + forceY * 0.001);
        double maxSpeed = 0.5;
        double speed = Math.sqrt(ship.getDX() * ship.getDX() + ship.getDY() * ship.getDY());
        if (speed > maxSpeed) {
            ship.setDX(ship.getDX() / speed * maxSpeed);
            ship.setDY(ship.getDY() / speed * maxSpeed);
        }
    }

    private void applyForceToShot(Entity shot, double forceX, double forceY) {
        shot.setDX(shot.getDX() + forceX * 0.002);
        shot.setDY(shot.getDY() + forceY * 0.002);
        double maxSpeed = 1.0;
        double speed = Math.sqrt(shot.getDX() * shot.getDX() + shot.getDY() * shot.getDY());
        if (speed > maxSpeed) {
            shot.setDX(shot.getDX() / speed * maxSpeed);
            shot.setDY(shot.getDY() / speed * maxSpeed);
        }
    }

    private double distanceTo(Entity other) {
        double dx = other.getX() - this.x;
        double dy = other.getY() - this.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public void draw(Graphics g) {
        if (!active) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = (int) x;
        int centerY = (int) y;
        int radius = (int) currentRadius;
        float alpha = 0.4f;

        if (fieldStrength > 0) {
            g2d.setColor(new Color(1.0f, 0.3f, 0.3f, alpha));
        } else {
            g2d.setColor(new Color(0.3f, 0.3f, 1.0f, alpha));
        }
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(new Color(1.0f, 1.0f, 1.0f, alpha * 0.8f));
        g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        g2d.setColor(Color.WHITE);
        g2d.fillOval(centerX - 3, centerY - 3, 6, 6);
        g2d.dispose();
    }

    @Override
    public void collidedWith(Entity other) {
        // no-op
    }

    public boolean isActive() {
        return active;
    }

    public double getFieldRadius() {
        return fieldRadius;
    }

    public double getFieldStrength() {
        return fieldStrength;
    }

    public double getCurrentRadius() {
        return currentRadius;
    }
}
