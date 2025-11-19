package org.newdawn.spaceinvaders.common.entity.effect;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 공용 히트 이펙트 엔티티.
 */
public class HeatEffectEntity extends Entity {
    private final GameContext game;
    private final long heatDuration;
    private final long startTime;
    private BufferedImage heatImage;
    private float alpha = 1.0f;
    private float scale = 1.0f;

    public HeatEffectEntity(GameContext game, int x, int y) {
        this(game, x, y, 800);
    }

    public HeatEffectEntity(GameContext game, int x, int y, long durationMs) {
        super("sprites/Skill/Heat.gif", x, y);
        this.game = game;
        this.heatDuration = durationMs;
        this.startTime = System.currentTimeMillis();
        loadHeatImage();
    }

    @Override
    public void move(long delta) {
        long currentTime = System.currentTimeMillis();
        double progress = (double) (currentTime - startTime) / heatDuration;

        if (progress >= 1.0) {
            game.removeEntity(this);
            return;
        }
        alpha = (float) (1.0 - progress);

        if (progress < 0.3) {
            scale = 1.5f + (float) (progress * 1.0);
        } else {
            float shrinkProgress = (float) ((progress - 0.3) / 0.7);
            scale = 2.5f - (float) (shrinkProgress * 1.0);
        }
    }

    @Override
    public void draw(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, alpha));

            if (heatImage != null) {
                int originalWidth = heatImage.getWidth();
                int originalHeight = heatImage.getHeight();
                int scaledWidth = (int) (originalWidth * scale);
                int scaledHeight = (int) (originalHeight * scale);
                int drawX = (int) x - scaledWidth / 2;
                int drawY = (int) y - scaledHeight / 2;
                g2d.drawImage(heatImage, drawX, drawY, scaledWidth, scaledHeight, null);
            } else {
                int fallbackSize = (int) (20 * scale);
                g2d.setColor(new Color(255, 100, 0, (int) (alpha * 200)));
                g2d.fillOval((int) x - fallbackSize / 2, (int) y - fallbackSize / 2, fallbackSize, fallbackSize);
                int innerSize = (int) (12 * scale);
                g2d.setColor(new Color(255, 200, 0, (int) (alpha * 150)));
                g2d.fillOval((int) x - innerSize / 2, (int) y - innerSize / 2, innerSize, innerSize);
            }

            g2d.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, 1.0f));
        } catch (Exception e) {
            System.err.println("Error drawing heat effect: " + e.getMessage());
        }
    }

    private void loadHeatImage() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/Skill/Heat.gif")) {
            if (is != null) {
                heatImage = ImageIO.read(is);
            }
        } catch (IOException e) {
            System.err.println("Failed to load heat effect image: " + e.getMessage());
        }
    }

    @Override
    public void collidedWith(Entity other) {
        // visual only
    }
}
