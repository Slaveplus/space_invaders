package org.newdawn.spaceinvaders.common.entity.ui;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 공용 코인 획득 표시 엔티티.
 *
 * 플레이 모드에 상관없이 동일한 비주얼 연출을 제공한다.
 */
public class CoinDisplayEntity extends Entity {
    private static final double RISE_SPEED = 50.0;
    private static final long DISPLAY_DURATION_MS = 2_000L;
    private static final long FADE_START_MS = 1_500L;

    private final GameContext game;
    private final int coinAmount;
    private final long creationTime;
    private final BufferedImage coinImage;

    public CoinDisplayEntity(GameContext game, int x, int y, int coinAmount) {
        super(SpriteConstants.STAR_COIN_NORMAL_PNG, x, y);
        this.game = game;
        this.coinAmount = coinAmount;
        this.creationTime = System.currentTimeMillis();
        this.coinImage = loadCoinImage();
        setVerticalMovement(-RISE_SPEED);
        setHorizontalMovement(0);
    }

    private BufferedImage loadCoinImage() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SpriteConstants.STAR_COIN_NORMAL_PNG)) {
            return is != null ? ImageIO.read(is) : null;
        } catch (IOException e) {
            org.newdawn.spaceinvaders.common.util.Logger logger = 
                org.newdawn.spaceinvaders.common.util.LoggerFactory.getLogger(CoinDisplayEntity.class);
            logger.error("Failed to load coin sprite: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (isExpired()) {
            game.removeEntity(this);
        }
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > DISPLAY_DURATION_MS;
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
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

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

        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics metrics = g2d.getFontMetrics();
        int textX = (int) x + 24;
        int textY = (int) y + metrics.getAscent();
        String text = "+" + coinAmount;

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, textX + 1, textY + 1);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, textX, textY);
    }

    @Override
    public void collidedWith(Entity other) {
        // purely visual, no collisions
    }
}
