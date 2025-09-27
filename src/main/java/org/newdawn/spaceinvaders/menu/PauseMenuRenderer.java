package org.newdawn.spaceinvaders.menu;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 일시정지 메뉴 UI 렌더링을 담당하는 클래스
 * MenuManager.java에서 일시정지 메뉴 관련 UI만 추출
 */
public class PauseMenuRenderer {
    // 별 필드 애니메이션
    private List<Star> stars;
    private int numStars = 150;
    private float galaxyRotation = 0;
    
    public PauseMenuRenderer() {
        initStars();
    }
    
    /**
     * 별 필드 초기화
     */
    private void initStars() {
        stars = new ArrayList<>();
        for (int i = 0; i < numStars; i++) {
            stars.add(new Star(
                (float) (Math.random() * 800),
                (float) (Math.random() * 600),
                (float) (Math.random() * 3 + 1),
                (float) (Math.random() * 0.02 + 0.01)
            ));
        }
    }
    
    /**
     * 일시정지 메뉴 그리기
     */
    public void drawPauseMenu(Graphics2D g, int selectedPauseMenuItem) {
        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Update animations (only galaxy rotation, no star twinkling)
        galaxyRotation += 0.005f;
        
        // Draw space background
        drawSpaceBackground(g);
        
        // Draw galaxy
        drawGalaxy(g);
        
        // Semi-transparent overlay for menu
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, 800, 600);
        
        // Title with shadow effect
        drawTitle(g);
        
        // Menu buttons
        String[] pauseMenuItems = {"계속하기", "메인메뉴", "설정"};
        int buttonY = 280;
        
        for (int i = 0; i < pauseMenuItems.length; i++) {
            boolean isSelected = (i == selectedPauseMenuItem);
            drawButton(g, pauseMenuItems[i], 325, buttonY, 150, 40, isSelected);
            buttonY += 65;
        }
        
        // Instructions
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        FontMetrics fm = g.getFontMetrics();
        String instruction = "ESC: 메뉴 닫기";
        int instX = (800 - fm.stringWidth(instruction)) / 2;
        g.drawString(instruction, instX, 520);
    }
    
    private void drawButton(Graphics2D g, String text, int x, int y, int width, int height, boolean isSelected) {
        // Button shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRect(x + 2, y + 2, width, height);
        
        // Button background
        if (isSelected) {
            g.setColor(new Color(70, 130, 180));
            g.fillRect(x, y, width, height);
            
            // Inner highlight
            g.setColor(new Color(100, 149, 237));
            g.fillRect(x, y, width, height / 2);
            
            // Glow effect
            g.setColor(new Color(100, 149, 237, 100));
            g.fillRect(x - 5, y - 2, width + 10, height + 4);
        } else {
            g.setColor(new Color(25, 25, 112));
            g.fillRect(x, y, width, height);
            
            // Subtle highlight
            g.setColor(new Color(45, 45, 132));
            g.fillRect(x, y, width, height / 3);
        }
        
        // Button border
        g.setColor(new Color(100, 149, 237));
        g.setStroke(new BasicStroke(2));
        g.drawRect(x, y, width, height);
        
        // Button text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (width - fm.stringWidth(text)) / 2;
        int textY = y + (height + fm.getAscent()) / 2 - 2;
        
        // Text shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(text, textX + 1, textY + 1);
        
        // Main text
        g.setColor(Color.WHITE);
        g.drawString(text, textX, textY);
    }
    
    private void drawSpaceBackground(Graphics2D g) {
        // Draw static stars (no twinkling effect)
        for (Star star : stars) {
            // Create different colored stars with fixed brightness
            Color starColor;
            if (star.size < 2) {
                starColor = new Color(255, 255, 255, 200); // White small stars
            } else if (star.size < 3) {
                starColor = new Color(200, 220, 255, 200); // Light blue medium stars
            } else {
                starColor = new Color(255, 255, 200, 200); // Yellow large stars
            }
            
            g.setColor(starColor);
            
            // Draw star
            g.fillOval((int) star.x, (int) star.y, (int) star.size, (int) star.size);
        }
    }
    
    private void drawGalaxy(Graphics2D g) {
        // Save current transform
        java.awt.geom.AffineTransform oldTransform = g.getTransform();
        
        // Translate to galaxy center
        g.translate(680, 120);
        g.rotate(galaxyRotation);
        
        // Draw galaxy core with gradient effect
        g.setColor(new Color(255, 255, 150));
        g.fillOval(-20, -20, 40, 40);
        
        // Draw inner core
        g.setColor(new Color(255, 255, 200));
        g.fillOval(-12, -12, 24, 24);
        
        // Draw brightest center
        g.setColor(new Color(255, 255, 255));
        g.fillOval(-6, -6, 12, 12);
        
        // Draw galaxy arms
        for (int arm = 0; arm < 3; arm++) {
            double armAngle = arm * Math.PI * 2 / 3;
            for (int r = 25; r < 120; r += 4) {
                double spiralAngle = armAngle + r * 0.08;
                double x = Math.cos(spiralAngle) * r;
                double y = Math.sin(spiralAngle) * r;
                
                int starSize = (int) (4 - r / 40);
                if (starSize < 1) starSize = 1;
                
                int alpha = (int) (200 - r / 2);
                if (alpha < 50) alpha = 50;
                
                g.setColor(new Color(180, 180, 255, alpha));
                g.fillOval((int) x - starSize/2, (int) y - starSize/2, starSize, starSize);
            }
        }
        
        // Restore transform
        g.setTransform(oldTransform);
    }
    
    private void drawTitle(Graphics2D g) {
        String title = "일시정지";
        
        // Draw title with shadow effect
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        int titleX = (800 - fm.stringWidth(title)) / 2;
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(title, titleX + 3, 203);
        
        // Main text
        g.setColor(Color.WHITE);
        g.drawString(title, titleX, 200);
    }
    
    /**
     * 별 필드를 위한 내부 클래스 (정적 별)
     */
    private static class Star {
        float x, y, size;
        
        Star(float x, float y, float size, float twinkle) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }
}
