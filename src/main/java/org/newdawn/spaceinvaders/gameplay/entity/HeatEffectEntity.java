package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.gameplay.Game;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * 플레이어 공격이 적에 맞을 때 나타나는 Heat 효과 엔티티
 */
public class HeatEffectEntity extends Entity {
    /** 이 히트 효과가 존재하는 게임 */
    private Game game;
    /** 히트 효과 지속 시간 (밀리초) */
    private long heatDuration = 800; // 0.8초
    /** 히트 효과가 시작된 시간 */
    private long startTime;
    /** 히트 효과 이미지 */
    private BufferedImage heatImage;
    /** 페이딩 효과를 위한 현재 알파 값 */
    private float alpha = 1.0f;
    /** 크기 스케일링 팩터 */
    private float scale = 1.0f;
    
    /**
     * 새로운 히트 효과를 생성합니다
     * 
     * @param game 히트 효과가 생성된 게임
     * @param x 히트 효과의 x 위치
     * @param y 히트 효과의 y 위치
     */
    public HeatEffectEntity(Game game, int x, int y) {
        super("sprites/Skill/Heat.gif", x, y);
        
        this.game = game;
        this.startTime = System.currentTimeMillis();
        
        // Don't move
        dx = 0;
        dy = 0;
        
        // Load heat effect image
        loadHeatImage();
    }
    
    /**
     * Request that this heat effect updated based on time elapsed
     * 
     * @param delta The time that has elapsed since last update
     */
    public void move(long delta) {
        try {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTime;
            
            // Calculate progress (0.0 to 1.0)
            double progress = (double) elapsed / heatDuration;
            
            if (progress >= 1.0) {
                // Heat effect finished, remove it
                if (game != null) {
                    game.removeEntity(this);
                }
                return;
            }
            
            // Fade out effect (alpha decreases over time)
            alpha = (float) (1.0 - progress);
            
            // Scale effect (grow then shrink with larger size)
            if (progress < 0.3) {
                // Grow phase (first 30% of duration)
                scale = 1.5f + (float) (progress * 1.0); // Grow to 2.5x
            } else {
                // Shrink phase (remaining 70% of duration)
                float shrinkProgress = (float) ((progress - 0.3) / 0.7);
                scale = 2.5f - (float) (shrinkProgress * 1.0); // Shrink from 2.5x to 1.5x
            }
        } catch (Exception e) {
            System.err.println("Error in heat effect move: " + e.getMessage());
            e.printStackTrace();
            // Try to remove heat effect on error
            try {
                if (game != null) {
                    game.removeEntity(this);
                }
            } catch (Exception ex) {
                System.err.println("Error removing heat effect: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Draw the heat effect with scaling and fading
     */
    @Override
    public void draw(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            
            // Enable anti-aliasing for smoother effect
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Set alpha for fading effect
            g2d.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, alpha));
            
            // Draw heat effect image scaled
            if (heatImage != null) {
                int originalWidth = heatImage.getWidth();
                int originalHeight = heatImage.getHeight();
                
                int scaledWidth = (int) (originalWidth * scale);
                int scaledHeight = (int) (originalHeight * scale);
                
                // Center the effect
                int drawX = (int) x - scaledWidth / 2;
                int drawY = (int) y - scaledHeight / 2;
                
                // Draw the heat effect image
                g2d.drawImage(heatImage, drawX, drawY, scaledWidth, scaledHeight, null);
            } else {
                // Fallback: draw a simple heat effect if image fails to load
                int fallbackSize = (int) (20 * scale);
                g2d.setColor(new java.awt.Color(255, 100, 0, (int)(alpha * 200)));
                g2d.fillOval((int)x - fallbackSize/2, (int)y - fallbackSize/2, fallbackSize, fallbackSize);
                
                int innerSize = (int) (12 * scale);
                g2d.setColor(new java.awt.Color(255, 200, 0, (int)(alpha * 150)));
                g2d.fillOval((int)x - innerSize/2, (int)y - innerSize/2, innerSize, innerSize);
            }
            
            // Reset alpha
            g2d.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, 1.0f));
        } catch (Exception e) {
            System.err.println("Error drawing heat effect: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if heat effect is finished
     * 
     * @return True if heat effect is finished
     */
    public boolean isFinished() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - startTime) >= heatDuration;
    }
    
    /**
     * Load heat effect image
     */
    private void loadHeatImage() {
        try {
            // Try to load heat effect image
            URL url = getClass().getClassLoader().getResource("sprites/Skill/Heat.gif");
            if (url != null) {
                heatImage = ImageIO.read(url);
                System.out.println("Successfully loaded heat effect image: sprites/Skill/Heat.gif");
            } else {
                System.err.println("Cannot find heat effect image: sprites/Skill/Heat.gif");
                heatImage = null;
            }
        } catch (IOException e) {
            System.err.println("Failed to load heat effect image: " + e.getMessage());
            e.printStackTrace();
            heatImage = null;
        } catch (Exception e) {
            System.err.println("Unexpected error loading heat effect image: " + e.getMessage());
            e.printStackTrace();
            heatImage = null;
        }
    }
    
    /**
     * Notification that this heat effect has collided with another entity
     * 
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        // Heat effects don't collide with other entities
        // They are just visual effects
    }
}
