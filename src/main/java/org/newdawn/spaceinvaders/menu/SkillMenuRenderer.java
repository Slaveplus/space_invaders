package org.newdawn.spaceinvaders.menu;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * 스킬 메뉴 UI 렌더링을 담당하는 클래스
 * MenuManager.java에서 스킬 메뉴 관련 UI만 추출
 */
public class SkillMenuRenderer {
    
    /**
     * 스킬 메뉴 그리기
     */
    public void drawSkillMenu(Graphics2D g, int skillPoints, int attackPower, double attackSpeed, 
                             int maxHP, int attackPowerCost, int attackSpeedCost, int hpUpCost,
                             int selectedSkill) {
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
        
        for (int i = 0; i < 3; i++) {
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
}
