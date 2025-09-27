package org.newdawn.spaceinvaders;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * 메뉴 시스템을 관리하는 클래스
 * 메인 메뉴, 일시정지 메뉴, 스킬 메뉴 등을 관리
 */
public class MenuManager {
    // 메뉴 상태
    private boolean showMenu = true;
    private boolean showPauseMenu = false;
    private boolean showSkillMenu = false;
    
    // 메인 메뉴
    private int selectedMenuItem = 0; // 0: NEW GAME, 1: LEADERBOARD, 2: SETTINGS
    private final int MENU_ITEMS = 3;
    
    // 일시정지 메뉴
    private int selectedPauseMenuItem = 0; // 0: CONTINUE, 1: MAIN MENU, 2: SETTINGS
    private final int PAUSE_MENU_ITEMS = 3;
    
    // 스킬 메뉴
    private int selectedSkill = 0; // 0: Attack Power, 1: Attack Speed, 2: HP Up & Heal
    private final int SKILL_COUNT = 3;
    
    // 배경 애니메이션
    private List<Star> stars;
    private int numStars = 200;
    private float galaxyRotation = 0;
    private float earthRotation = 0;
    
    public MenuManager() {
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
     * 메뉴 업데이트 (애니메이션)
     */
    public void update() {
        galaxyRotation += 0.005f;
        earthRotation += 0.01f;
        
        // Update stars
        for (Star star : stars) {
            star.update();
        }
    }
    
    /**
     * 메인 메뉴 그리기
     */
    public void drawMainMenu(Graphics2D g) {
        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw space background
        drawSpaceBackground(g);
        
        // Draw galaxy
        drawGalaxy(g);
        
        // Draw title
        drawTitle(g);
        
        // Draw menu buttons
        drawMenuButtons(g);
        
        // Draw spaceship icon
        drawSpaceshipIcon(g);
        
        // Draw Earth
        drawEarth(g);
    }
    
    /**
     * 일시정지 메뉴 그리기
     */
    public void drawPauseMenu(Graphics2D g) {
        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Semi-transparent overlay
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, 800, 600);
        
        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        String title = "일시정지";
        int titleX = (800 - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 200);
        
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
        fm = g.getFontMetrics();
        String instruction = "ESC: 메뉴 닫기";
        int instX = (800 - fm.stringWidth(instruction)) / 2;
        g.drawString(instruction, instX, 520);
    }
    
    /**
     * 스킬 메뉴 그리기
     */
    public void drawSkillMenu(Graphics2D g, int skillPoints, int attackPower, double attackSpeed, 
                             int maxHP, int attackPowerCost, int attackSpeedCost, int hpUpCost) {
        // Enable anti-aliasing for smoother shapes
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Dark background
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, 800, 600);
        
        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics fm = g.getFontMetrics();
        String title = "강화 선택";
        int titleX = (800 - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 60);
        
        // Skill points display (bottom center, small text)
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        fm = g.getFontMetrics();
        String pointsText = "강화 포인트: " + skillPoints;
        int pointsX = (800 - fm.stringWidth(pointsText)) / 2;
        g.drawString(pointsText, pointsX, 450);
        
        // Skill panels
        String[] skillNames = {"공격력 증가", "공격속도 증가", "HP 증가 & 회복"};
        String[] skillDescriptions = {
            "공격력이 20% 증가",
            "발사 속도가 빨라집니다", 
            "최대 HP 3 증가하고 즉시 회복됩니다"
        };
        
        int panelWidth = 180;
        int panelHeight = 280;
        int startX = (800 - (panelWidth * 3 + 40 * 2)) / 2;
        int panelY = 100;
        
        for (int i = 0; i < SKILL_COUNT; i++) {
            int panelX = startX + i * (panelWidth + 40);
            boolean isSelected = (i == selectedSkill);
            
            // Panel background with different colors
            Color panelColor;
            Color borderColor;
            switch (i) {
                case 0: // Attack Power - Red theme
                    panelColor = isSelected ? new Color(200, 50, 50, 180) : new Color(100, 30, 30, 150);
                    borderColor = isSelected ? new Color(255, 100, 100) : new Color(150, 50, 50);
                    break;
                case 1: // Attack Speed - Blue theme
                    panelColor = isSelected ? new Color(50, 100, 200, 180) : new Color(30, 60, 120, 150);
                    borderColor = isSelected ? new Color(100, 150, 255) : new Color(60, 100, 180);
                    break;
                case 2: // HP Up - Green theme
                    panelColor = isSelected ? new Color(50, 150, 50, 180) : new Color(30, 100, 30, 150);
                    borderColor = isSelected ? new Color(100, 200, 100) : new Color(60, 150, 60);
                    break;
                default:
                    panelColor = new Color(50, 50, 100, 150);
                    borderColor = new Color(100, 149, 237);
            }
            
            g.setColor(panelColor);
            g.fillRect(panelX, panelY, panelWidth, panelHeight);
            
            // Panel border with glow effect for selected
            if (isSelected) {
                g.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 100));
                g.setStroke(new BasicStroke(4));
                g.drawRect(panelX - 2, panelY - 2, panelWidth + 4, panelHeight + 4);
            }
            g.setColor(borderColor);
            g.setStroke(new BasicStroke(2));
            g.drawRect(panelX, panelY, panelWidth, panelHeight);
            
            // Skill icon
            drawSkillIcon(g, i, panelX, panelY, panelWidth);
            
            // Skill name
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            fm = g.getFontMetrics();
            int nameX = panelX + (panelWidth - fm.stringWidth(skillNames[i])) / 2;
            g.drawString(skillNames[i], nameX, panelY + 105);
            
            // Skill description
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            fm = g.getFontMetrics();
            int descX = panelX + (panelWidth - fm.stringWidth(skillDescriptions[i])) / 2;
            g.drawString(skillDescriptions[i], descX, panelY + 125);
            
            // Current level/effect display
            drawSkillStats(g, i, panelX, panelY, panelWidth, attackPower, attackSpeed, maxHP, 
                          attackPowerCost, attackSpeedCost, hpUpCost, skillPoints);
        }
        
        // Instructions
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        fm = g.getFontMetrics();
        String instruction = "Q로 닫기";
        int instX = (800 - fm.stringWidth(instruction)) / 2;
        g.drawString(instruction, instX, 480);
    }
    
    private void drawSpaceBackground(Graphics2D g) {
        // Update and draw stars
        for (Star star : stars) {
            // Create different colored stars
            int alpha = (int) (star.brightness * 255);
            Color starColor;
            if (star.size < 2) {
                starColor = new Color(255, 255, 255, alpha); // White small stars
            } else if (star.size < 3) {
                starColor = new Color(200, 220, 255, alpha); // Light blue medium stars
            } else {
                starColor = new Color(255, 255, 200, alpha); // Yellow large stars
            }
            
            g.setColor(starColor);
            
            // Draw star with slight glow effect
            g.fillOval((int) star.x, (int) star.y, (int) star.size, (int) star.size);
            
            // Add glow for brighter stars
            if (star.brightness > 0.8f) {
                g.setColor(new Color(starColor.getRed(), starColor.getGreen(), starColor.getBlue(), alpha / 3));
                g.fillOval((int) star.x - 1, (int) star.y - 1, (int) star.size + 2, (int) star.size + 2);
            }
        }
    }
    
    private void drawGalaxy(Graphics2D g) {
        // Save current transform
        AffineTransform oldTransform = g.getTransform();
        
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
        String title1 = "SPACE";
        String title2 = "INVADER";
        
        // Draw "SPACE"
        g.setFont(new Font("Arial", Font.BOLD, 52));
        FontMetrics fm = g.getFontMetrics();
        int x1 = (800 - fm.stringWidth(title1)) / 2;
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(title1, x1 + 3, 103);
        
        // Main text
        g.setColor(Color.WHITE);
        g.drawString(title1, x1, 100);
        
        // Draw "INVADER"
        g.setFont(new Font("Arial", Font.BOLD, 64));
        fm = g.getFontMetrics();
        int x2 = (800 - fm.stringWidth(title2)) / 2;
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(title2, x2 + 4, 164);
        
        // Main text
        g.setColor(Color.WHITE);
        g.drawString(title2, x2, 160);
    }
    
    private void drawMenuButtons(Graphics2D g) {
        String[] menuItems = {"NEW GAME", "LEADERBOARD", "SETTINGS"};
        int buttonY = 280;
        
        for (int i = 0; i < menuItems.length; i++) {
            boolean isSelected = (i == selectedMenuItem);
            drawButton(g, menuItems[i], 325, buttonY, 150, 40, isSelected);
            buttonY += 65;
        }
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
    
    private void drawSpaceshipIcon(Graphics2D g) {
        int shipX = 380;
        int shipY = 480;
        
        // Spaceship body
        g.setColor(new Color(35, 35, 122));
        int[] shipXPoints = {shipX, shipX + 40, shipX + 20};
        int[] shipYPoints = {shipY + 30, shipY + 30, shipY};
        g.fillPolygon(shipXPoints, shipYPoints, 3);
        
        // Cockpit
        g.setColor(new Color(100, 149, 237, 200));
        g.fillOval(shipX + 13, shipY + 8, 14, 12);
    }
    
    private void drawEarth(Graphics2D g) {
        AffineTransform oldTransform = g.getTransform();
        
        g.translate(680, 520);
        g.rotate(earthRotation);
        
        // Draw Earth
        g.setColor(new Color(30, 144, 255));
        g.fillOval(-40, -40, 80, 80);
        
        // Draw continents
        g.setColor(new Color(34, 139, 34));
        g.fillOval(-20, -20, 40, 40);
        
        g.setTransform(oldTransform);
    }
    
    private void drawSkillIcon(Graphics2D g, int skillType, int panelX, int panelY, int panelWidth) {
        int iconSize = 50;
        int iconX = panelX + (panelWidth - iconSize) / 2;
        int iconY = panelY + 25;
        
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        
        switch (skillType) {
            case 0: // Attack Power - Sword
                int centerX = iconX + iconSize/2;
                g.fillRect(centerX - 3, iconY + 5, 6, iconSize - 25);
                int[] tipX = {centerX - 4, centerX + 4, centerX};
                int[] tipY = {iconY + 5, iconY + 5, iconY + 1};
                g.fillPolygon(tipX, tipY, 3);
                g.fillRect(centerX - 6, iconY + iconSize - 25, 12, 3);
                g.fillRect(centerX - 4, iconY + iconSize - 22, 8, 12);
                break;
            case 1: // Attack Speed - Arrow
                g.setStroke(new BasicStroke(3));
                g.drawLine(iconX + 8, iconY + iconSize/2, iconX + iconSize - 8, iconY + iconSize/2);
                int ahx = iconX + iconSize - 8;
                int ahy = iconY + iconSize/2;
                int[] arrowHeadX = {ahx, ahx + 6, ahx};
                int[] arrowHeadY = {ahy - 4, ahy, ahy + 4};
                g.fillPolygon(arrowHeadX, arrowHeadY, 3);
                break;
            case 2: // Heart
                int heartX = iconX + 11;
                int heartY = iconY + 12;
                g.fillOval(heartX, heartY, 16, 16);
                g.fillOval(heartX + 16, heartY, 16, 16);
                int[] xPoints = {heartX - 1, heartX + 33, heartX + 16};
                int[] yPoints = {heartY + 10, heartY + 10, heartY + 30};
                g.fillPolygon(xPoints, yPoints, 3);
                break;
        }
    }
    
    private void drawSkillStats(Graphics2D g, int skillType, int panelX, int panelY, int panelWidth,
                               int attackPower, double attackSpeed, int maxHP,
                               int attackPowerCost, int attackSpeedCost, int hpUpCost, int skillPoints) {
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        FontMetrics fm = g.getFontMetrics();
        
        String currentText = "";
        String nextText = "";
        String costText = "";
        boolean canAfford = false;
        
        switch (skillType) {
            case 0: 
                currentText = "현재: " + (100 + attackPower * 20) + "%";
                nextText = "다음 단계: " + (100 + (attackPower + 1) * 20) + "%";
                costText = "비용: " + attackPowerCost + "포인트";
                canAfford = skillPoints >= attackPowerCost;
                break;
            case 1: 
                currentText = "현재: " + String.format("%.1f", attackSpeed) + "x";
                nextText = "다음 단계: " + String.format("%.1f", attackSpeed + 0.2) + "x";
                costText = "비용: " + attackSpeedCost + "포인트";
                canAfford = skillPoints >= attackSpeedCost;
                break;
            case 2: 
                currentText = "현재: " + maxHP + " HP";
                nextText = "다음 단계: " + (maxHP + 3) + " HP";
                costText = "비용: " + hpUpCost + "포인트";
                canAfford = skillPoints >= hpUpCost;
                break;
        }
        
        // Current level
        int currentX = panelX + (panelWidth - fm.stringWidth(currentText)) / 2;
        g.drawString(currentText, currentX, panelY + 150);
        
        // Next level
        int nextX = panelX + (panelWidth - fm.stringWidth(nextText)) / 2;
        g.drawString(nextText, nextX, panelY + 170);
        
        // Cost
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        fm = g.getFontMetrics();
        int costX = panelX + (panelWidth - fm.stringWidth(costText)) / 2;
        g.drawString(costText, costX, panelY + 195);
        
        // Insufficient points warning
        if (!canAfford) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            fm = g.getFontMetrics();
            String warning = "포인트 부족";
            int warningX = panelX + (panelWidth - fm.stringWidth(warning)) / 2;
            g.drawString(warning, warningX, panelY + 215);
        }
    }
    
    // Getters and Setters
    public boolean isShowMenu() { return showMenu; }
    public void setShowMenu(boolean showMenu) { this.showMenu = showMenu; }
    
    public boolean isShowPauseMenu() { return showPauseMenu; }
    public void setShowPauseMenu(boolean showPauseMenu) { this.showPauseMenu = showPauseMenu; }
    
    public boolean isShowSkillMenu() { return showSkillMenu; }
    public void setShowSkillMenu(boolean showSkillMenu) { this.showSkillMenu = showSkillMenu; }
    
    public int getSelectedMenuItem() { return selectedMenuItem; }
    public void setSelectedMenuItem(int selectedMenuItem) { this.selectedMenuItem = selectedMenuItem; }
    
    public int getSelectedPauseMenuItem() { return selectedPauseMenuItem; }
    public void setSelectedPauseMenuItem(int selectedPauseMenuItem) { this.selectedPauseMenuItem = selectedPauseMenuItem; }
    
    public int getSelectedSkill() { return selectedSkill; }
    public void setSelectedSkill(int selectedSkill) { this.selectedSkill = selectedSkill; }
    
    public int getMenuItems() { return MENU_ITEMS; }
    public int getPauseMenuItems() { return PAUSE_MENU_ITEMS; }
    public int getSkillCount() { return SKILL_COUNT; }
    
    /**
     * 별 필드 애니메이션을 위한 내부 클래스
     */
    private static class Star {
        float x, y, size, brightness, twinkle;
        
        Star(float x, float y, float size, float twinkle) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.twinkle = twinkle;
            this.brightness = (float) Math.random();
        }
        
        void update() {
            brightness += twinkle;
            if (brightness > 1.0f) {
                brightness = 1.0f;
                twinkle = -twinkle;
            } else if (brightness < 0.3f) {
                brightness = 0.3f;
                twinkle = -twinkle;
            }
        }
    }
}