package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;

/**
 * 멀티플레이용 자기장 공격 엔티티.
 */
public class MagneticFieldEntity extends Entity {
    private final MultiplayerGameContext game;
    private double fieldRadius;
    private double fieldStrength;
    private final long duration;
    private final long startTime;
    private boolean active;
    private double currentRadius;

    public MagneticFieldEntity(MultiplayerGameContext game, int x, int y, double radius, double strength, long duration) {
        super("sprites/shot.gif", x, y);
        this.game = game;
        this.fieldRadius = radius;
        this.fieldStrength = strength;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.currentRadius = radius;
        this.active = true;
    }

    @Override
    public void move(long delta) {
        if (!active) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - startTime > duration) {
            active = false;
            game.removeEntity(this);
            return;
        }
        currentRadius = fieldRadius;
        applyMagneticEffects();
    }

    private void applyMagneticEffects() {
        List<Entity> entities = game.getEntities();
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
        double vx = ship.getHorizontalMovement() + forceX * 0.001;
        double vy = ship.getVerticalMovement() + forceY * 0.001;
        double maxSpeed = 0.5;
        double speed = Math.sqrt(vx * vx + vy * vy);
        if (speed > maxSpeed) {
            double ratio = maxSpeed / speed;
            vx *= ratio;
            vy *= ratio;
        }
        ship.setHorizontalMovement(vx);
        ship.setVerticalMovement(vy);
    }

    private void applyForceToShot(Entity shot, double forceX, double forceY) {
        double vx = shot.getHorizontalMovement() + forceX * 0.002;
        double vy = shot.getVerticalMovement() + forceY * 0.002;
        double maxSpeed = 1.0;
        double speed = Math.sqrt(vx * vx + vy * vy);
        if (speed > maxSpeed) {
            double ratio = maxSpeed / speed;
            vx *= ratio;
            vy *= ratio;
        }
        shot.setHorizontalMovement(vx);
        shot.setVerticalMovement(vy);
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

    @Override
    protected String snapshotMetadata() {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("radius", Double.toString(fieldRadius));
        meta.put("strength", Double.toString(fieldStrength));
        return MetadataCodec.encode(meta);
    }

    @Override
    protected void applySnapshotMetadata(String metadata) {
        Map<String, String> meta = MetadataCodec.decode(metadata);
        if (meta.isEmpty()) {
            return;
        }
        try {
            fieldRadius = Double.parseDouble(meta.getOrDefault("radius", Double.toString(fieldRadius)));
        } catch (NumberFormatException ignore) {
            // keep previous
        }
        try {
            fieldStrength = Double.parseDouble(meta.getOrDefault("strength", Double.toString(fieldStrength)));
        } catch (NumberFormatException ignore) {
            // keep previous
        }
    }
}
