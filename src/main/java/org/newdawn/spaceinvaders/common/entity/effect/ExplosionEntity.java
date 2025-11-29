package org.newdawn.spaceinvaders.common.entity.effect;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 공용 폭발 엔티티.
 */
public class ExplosionEntity extends Entity {
    private final GameContext game;
    private final double maxRadius;
    private final long explosionDuration;

    private long startTime;
    private double currentRadius;
    private long lastDamageTime;
    private final long damageInterval = 100;
    private final Set<Entity> damagedEntities = new HashSet<>();
    private BufferedImage explosionImage;

    public ExplosionEntity(GameContext game, String sprite, int x, int y, double maxRadius) {
        this(game, sprite, x, y, maxRadius, 1000);
    }

    public ExplosionEntity(GameContext game, String sprite, int x, int y, double maxRadius, long durationMs) {
        super(sprite, x, y);
        this.game = game;
        this.maxRadius = maxRadius;
        this.explosionDuration = durationMs;
        this.startTime = System.currentTimeMillis();
        loadExplosionImage();
    }

    @Override
    public void move(long delta) {
        long currentTime = System.currentTimeMillis();
        double progress = (double) (currentTime - startTime) / explosionDuration;
        if (progress >= 1.0) {
            game.removeEntity(this);
            return;
        }
        currentRadius = maxRadius * progress;
        if (currentRadius > maxRadius * 0.2 && currentTime - lastDamageTime >= damageInterval) {
            dealDamageToEnemies();
            lastDamageTime = currentTime;
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        if (explosionImage != null) {
            int centerX = (int) x + explosionImage.getWidth() / 2;
            int centerY = (int) y + explosionImage.getHeight() / 2;
            double scale = currentRadius / (explosionImage.getWidth() / 2.0);
            int drawWidth = (int) (explosionImage.getWidth() * scale);
            int drawHeight = (int) (explosionImage.getHeight() * scale);
            int drawX = centerX - drawWidth / 2;
            int drawY = centerY - drawHeight / 2;
            g2d.drawImage(explosionImage, drawX, drawY, drawWidth, drawHeight, null);
        } else {
            g2d.setColor(new Color(255, 100, 0, 150));
            g2d.fillOval((int) (x - currentRadius), (int) (y - currentRadius),
                    (int) (currentRadius * 2), (int) (currentRadius * 2));
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.drawOval((int) (x - currentRadius), (int) (y - currentRadius),
                    (int) (currentRadius * 2), (int) (currentRadius * 2));
        }
    }

    @Override
    public void collidedWith(Entity other) {
        // noop
    }

    private void loadExplosionImage() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/Explosion/explosion.png")) {
            if (is != null) {
                explosionImage = ImageIO.read(is);
            }
        } catch (IOException e) {
            org.newdawn.spaceinvaders.common.util.Logger logger = 
                org.newdawn.spaceinvaders.common.util.LoggerFactory.getLogger(ExplosionEntity.class);
            logger.error("Failed to load explosion image: " + e.getMessage(), e);
        }
    }

    private void dealDamageToEnemies() {
        for (Entity entity : game.getEntities()) {
            if (entity == this || damagedEntities.contains(entity)) {
                continue;
            }
            double dx = entity.getX() - x;
            double dy = entity.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= currentRadius) {
                applyDamage(entity);
                damagedEntities.add(entity);
            }
        }
    }

    private void applyDamage(Entity entity) {
        String type = entity.getClass().getSimpleName();
        if (type.equals("AlienEntity")) {
            game.removeEntity(entity);
            game.notifyAlienKilled(getOwnerId(), entity.getX(), entity.getY());
        } else if (type.equals("BossEntity")) {
            try {
                entity.getClass().getMethod("takeDamage", int.class, String.class)
                        .invoke(entity, 30, getOwnerId());
            } catch (Exception ignore) {
            }
        }
    }
}
