package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameCanvas;

/**
 * Floating coin popup for multiplayer reward feedback.
 */
public class CoinDisplayEntity extends Entity {

    private static final double RISE_SPEED = 50.0;
    private static final long DISPLAY_DURATION_MS = 2000L;
    private static final long FADE_START_MS = 1500L;

    private final MultiplayerGameCanvas game;
    private final int coinAmount;
    private final long creationTime;
    private BufferedImage coinImage;

    public CoinDisplayEntity(MultiplayerGameCanvas game, int x, int y, int coinAmount) {
        super("sprites/star coin normal.png", x, y);
        this.game = game;
        this.coinAmount = coinAmount;
        this.creationTime = System.currentTimeMillis();
        setVerticalMovement(-RISE_SPEED);
        setHorizontalMovement(0);
        loadCoinImage();
    }

    private void loadCoinImage() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/star coin normal.png")) {
            if (is != null) {
                coinImage = ImageIO.read(is);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        long elapsed = System.currentTimeMillis() - creationTime;
        if (elapsed > DISPLAY_DURATION_MS) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        long elapsed = System.currentTimeMillis() - creationTime;

        float alpha = 1.0f;
        if (elapsed > FADE_START_MS) {
            alpha = 1.0f - (float) (elapsed - FADE_START_MS) / (DISPLAY_DURATION_MS - FADE_START_MS);
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        }

        AlphaComposite originalComposite = (AlphaComposite) g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        if (coinImage != null) {
            g2d.drawImage(coinImage, (int) x, (int) y, 20, 20, null);
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int) x, (int) y, 20, 20);
            g2d.setColor(Color.ORANGE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval((int) x, (int) y, 20, 20);
        }

        g2d.setComposite(originalComposite);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics metrics = g2d.getFontMetrics();
        String text = "+" + coinAmount;
        int textX = (int) x + 24;
        int textY = (int) y + metrics.getAscent();

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, textX + 1, textY + 1);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, textX, textY);
    }

    @Override
    public void collidedWith(Entity other) {
        // purely visual
    }
}
