package org.newdawn.spaceinvaders.mainmenu;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerSkillManager;
import org.newdawn.spaceinvaders.gameplay.SkillManager;
import java.io.IOException;
import java.io.InputStream;

/**
 * 스킬 메뉴 UI 렌더링을 담당하는 클래스
 * MenuManager.java에서 스킬 메뉴 관련 UI만 추출
 */
public class SkillMenuRenderer {
    private static final String DEFAULT_FONT_NAME = "Arial";
    private static final String NEXT_LEVEL_TEXT = "다음 단계";
    
    /**
     * 스킬 메뉴 그리기
     */
    public void drawSkillMenu(Graphics2D g, int skillPoints, int attackPower, double attackSpeed, 
                             int maxHP, int attackPowerCost, int attackSpeedCost, int hpUpCost,
                             int selectedSkill, MultiplayerSkillManager skillManager) {
        // Enable anti-aliasing for smoother shapes
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // No background - completely transparent
        
        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font(DEFAULT_FONT_NAME, Font.BOLD, 36));
        FontMetrics fm = g.getFontMetrics();
        String title = "강화 선택";
        int titleX = (800 - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 60);
        
        // Skill points display (bottom center, small text)
        g.setFont(new Font(DEFAULT_FONT_NAME, Font.PLAIN, 14));
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
            
            // Panel background using Force images based on affordability
            boolean canAfford = getCanAfford(i, attackPowerCost, attackSpeedCost, hpUpCost, skillPoints);
            BufferedImage panelBackground = loadPanelBackground(canAfford);
            if (panelBackground != null) {
                g.drawImage(panelBackground, panelX, panelY, panelWidth, panelHeight, null);
            }
            
            // Draw border based on skill enhancement status
            drawSkillBorder(g, panelX, panelY, panelWidth, panelHeight, isSelected, canAfford, i, skillManager);
            
            // Skill icon
            drawSkillIcon(g, i, panelX, panelY, panelWidth);
            
            // Skill name
            g.setColor(Color.WHITE);
            g.setFont(new Font(DEFAULT_FONT_NAME, Font.BOLD, 16));
            fm = g.getFontMetrics();
            int nameX = panelX + (panelWidth - fm.stringWidth(skillNames[i])) / 2;
            g.drawString(skillNames[i], nameX, panelY + 105);
            
            // Skill description
            g.setFont(new Font(DEFAULT_FONT_NAME, Font.PLAIN, 12));
            fm = g.getFontMetrics();
            int descX = panelX + (panelWidth - fm.stringWidth(skillDescriptions[i])) / 2;
            g.drawString(skillDescriptions[i], descX, panelY + 125);
            
            // Current level/effect display
            drawSkillStats(g, i, panelX, panelY, panelWidth, attackPower, attackSpeed, maxHP, 
                          attackPowerCost, attackSpeedCost, hpUpCost, skillPoints);
        }
        
        // Instructions
        g.setColor(Color.WHITE);
        g.setFont(new Font(DEFAULT_FONT_NAME, Font.PLAIN, 16));
        fm = g.getFontMetrics();
        String instruction = "Q로 닫기";
        int instX = (800 - fm.stringWidth(instruction)) / 2;
        g.drawString(instruction, instX, 480);
    }
    
    private void drawSkillIcon(Graphics2D g, int skillType, int panelX, int panelY, int panelWidth) {
        int iconSize = 50;
        int iconX = panelX + (panelWidth - iconSize) / 2;
        int iconY = panelY + 25;
        
        // Load and draw PNG skill icon
        BufferedImage skillIcon = loadSkillIcon(skillType);
        if (skillIcon != null) {
            // Draw the PNG icon, scaled to fit the icon size
            g.drawImage(skillIcon, iconX, iconY, iconSize, iconSize, null);
        } else {
            // Fallback to original drawn icons if PNG loading fails
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
                default: // Default case to handle unexpected skillType
                    // Optionally draw a placeholder or log a warning
                    g.setColor(Color.RED);
                    g.drawRect(iconX, iconY, iconSize, iconSize);
                    g.drawString("?", iconX + iconSize / 2, iconY + iconSize / 2);
                    break;
            }
        }
    }
    
    /**
     * Check if skill can be afforded
     */
    private boolean getCanAfford(int skillType, int attackPowerCost, int attackSpeedCost, int hpUpCost, int skillPoints) {
        switch (skillType) {
            case 0: return skillPoints >= attackPowerCost;
            case 1: return skillPoints >= attackSpeedCost;
            case 2: return skillPoints >= hpUpCost;
            default: return false;
        }
    }
    
    private boolean isSkillEnhanced(org.newdawn.spaceinvaders.gameplay.SkillManager skillManager, int skillIndex) {
        if (skillManager == null) {
            return false;
        }
        switch (skillIndex) {
            case 0: // 공격력
                return skillManager.getAttackPowerLevel() > 0;
            case 1: // 공격속도
                return skillManager.getAttackSpeedLevel() > 0;
            case 2: // HP 증가
                return skillManager.getHpUpLevel() > 0;
            default:
                return false;
        }
    }

    /**
     * Draw skill border based on affordability
     */
    private void drawSkillBorder(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight, boolean isSelected, boolean canAfford, int skillIndex, org.newdawn.spaceinvaders.gameplay.SkillManager skillManager) {
        BufferedImage borderImage;
        
        // 스킬 강화 상태 확인
        boolean isEnhanced = isSkillEnhanced(skillManager, skillIndex);
        
        if (isEnhanced) {
            // 스킬이 강화되었으면 Force True.png 사용
            borderImage = loadForceTrueImage();
        } else {
            // 스킬이 강화되지 않았으면 Force Select.png 사용
            borderImage = loadForceSelectImage();
        }
        
        if (borderImage != null) {
            drawBorderImageFrame(g, borderImage, panelX, panelY, panelWidth, panelHeight, isSelected);
        } else {
            // Fallback to simple colored border if image fails to load
            drawFallbackBorder(g, panelX, panelY, panelWidth, panelHeight, isSelected, canAfford);
        }
    }
    
    private void drawBorderImageFrame(Graphics2D g, BufferedImage borderImage, int panelX, int panelY, int panelWidth, int panelHeight, boolean isSelected) {
        int borderThickness = isSelected ? 8 : 4;
        
        // Top border
        g.drawImage(borderImage, panelX - borderThickness, panelY - borderThickness, 
                   panelX + panelWidth + borderThickness, panelY, 
                   0, 0, borderImage.getWidth(), borderImage.getHeight() / 4, null);
        
        // Bottom border
        g.drawImage(borderImage, panelX - borderThickness, panelY + panelHeight, 
                   panelX + panelWidth + borderThickness, panelY + panelHeight + borderThickness, 
                   0, borderImage.getHeight() * 3 / 4, borderImage.getWidth(), borderImage.getHeight(), null);
        
        // Left border
        g.drawImage(borderImage, panelX - borderThickness, panelY - borderThickness, 
                   panelX, panelY + panelHeight + borderThickness, 
                   0, 0, borderImage.getWidth() / 4, borderImage.getHeight(), null);
        
        // Right border
        g.drawImage(borderImage, panelX + panelWidth, panelY - borderThickness, 
                   panelX + panelWidth + borderThickness, panelY + panelHeight + borderThickness, 
                   borderImage.getWidth() * 3 / 4, 0, borderImage.getWidth(), borderImage.getHeight(), null);
    }
    
    private void drawFallbackBorder(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight, boolean isSelected, boolean canAfford) {
        Color borderColor = isSelected ? Color.YELLOW : (canAfford ? Color.GREEN : Color.RED);
        g.setColor(borderColor);
        g.setStroke(new BasicStroke(isSelected ? 4 : 2));
        g.drawRect(panelX, panelY, panelWidth, panelHeight);
    }

    /* ================= Overloads for single-player SkillManager ================= */
    /** Single-player 버전을 위한 오버로드 */
    public void drawSkillMenu(Graphics2D g, int skillPoints, int attackPower, double attackSpeed,
                              int maxHP, int attackPowerCost, int attackSpeedCost, int hpUpCost,
                              int selectedSkill, SkillManager skillManager) {
        // MultiplayerSkillManager 시그니처와 동일하게 래핑 (기능 현재 동일)
        drawSkillMenu(g, skillPoints, attackPower, attackSpeed, maxHP,
                attackPowerCost, attackSpeedCost, hpUpCost, selectedSkill,
                (MultiplayerSkillManager) null);
        // 싱글 플레이어 강화 상태 테두리 재그리기 위해 개별 루프 수행 (skillManager 값 활용)
        int panelWidth = 180;
        int panelHeight = 280;
        int startX = (800 - (panelWidth * 3 + 40 * 2)) / 2;
        int panelY = 100;
        for (int i=0;i<3;i++) {
            int panelX = startX + i * (panelWidth + 40);
            boolean isSelected = (i == selectedSkill);
            boolean canAfford = true; // 단순 처리: 비용 가능 여부는 외부 호출자가 이미 표시
            drawSkillBorder(g, panelX, panelY, panelWidth, panelHeight, isSelected, canAfford, i, skillManager);
        }
    }

    /** 호환성: MultiplayerSkillManager 오버로드 내부 구현 공유 (null 처리) */
    private void drawSkillBorder(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight, boolean isSelected, boolean canAfford, int skillIndex, MultiplayerSkillManager mpSkillManager) {
        // Multiplayer 전용 상태 판단: 현재는 동일 로직 없으므로 SkillManager null 전달
        drawSkillBorder(g, panelX, panelY, panelWidth, panelHeight, isSelected, canAfford, skillIndex, (SkillManager) null);
    }
    
    /**
     * Load panel background image based on affordability
     */
    private BufferedImage loadPanelBackground(boolean canAfford) {
        String imagePath;
        if (canAfford) {
            // Use Force True.png when affordable
            imagePath = "sprites/Force/Force True.png";
        } else {
            // Use Force Select.png when not affordable
            imagePath = "sprites/Force/Force Select.png";
        }
        
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(imagePath);
            if (is != null) {
                BufferedImage image = ImageIO.read(is);
                is.close();
                System.out.println("Successfully loaded panel background: " + imagePath);
                return image;
            }
        } catch (IOException e) {
            System.err.println("Failed to load panel background: " + imagePath);
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Load Force True image for affordable skills
     */
    private BufferedImage loadForceTrueImage() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/Force/Force True.png");
            if (is != null) {
                BufferedImage image = ImageIO.read(is);
                is.close();
                System.out.println("Successfully loaded Force True image");
                return image;
            }
        } catch (IOException e) {
            System.err.println("Failed to load Force True image: sprites/Force/Force True.png");
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Draw Force Select border around skill panel (legacy method)
     */
    @SuppressWarnings("unused")
    private void drawForceSelectBorder(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight, boolean isSelected) {
        BufferedImage forceSelectImage = loadForceSelectImage();
        
        if (forceSelectImage != null) {
            // Create border by drawing the image as a frame around the panel
            int borderThickness = isSelected ? 8 : 4;
            
            // Top border
            g.drawImage(forceSelectImage, panelX - borderThickness, panelY - borderThickness, 
                       panelX + panelWidth + borderThickness, panelY, 
                       0, 0, forceSelectImage.getWidth(), forceSelectImage.getHeight() / 4, null);
            
            // Bottom border
            g.drawImage(forceSelectImage, panelX - borderThickness, panelY + panelHeight, 
                       panelX + panelWidth + borderThickness, panelY + panelHeight + borderThickness, 
                       0, forceSelectImage.getHeight() * 3 / 4, forceSelectImage.getWidth(), forceSelectImage.getHeight(), null);
            
            // Left border
            g.drawImage(forceSelectImage, panelX - borderThickness, panelY - borderThickness, 
                       panelX, panelY + panelHeight + borderThickness, 
                       0, 0, forceSelectImage.getWidth() / 4, forceSelectImage.getHeight(), null);
            
            // Right border
            g.drawImage(forceSelectImage, panelX + panelWidth, panelY - borderThickness, 
                       panelX + panelWidth + borderThickness, panelY + panelHeight + borderThickness, 
                       forceSelectImage.getWidth() * 3 / 4, 0, forceSelectImage.getWidth(), forceSelectImage.getHeight(), null);
        } else {
            // Fallback to simple colored border if image fails to load
            Color borderColor = isSelected ? Color.YELLOW : Color.WHITE;
            g.setColor(borderColor);
            g.setStroke(new BasicStroke(isSelected ? 4 : 2));
            g.drawRect(panelX, panelY, panelWidth, panelHeight);
        }
    }
    
    /**
     * Load Force Select background image
     */
    private BufferedImage loadForceSelectImage() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/Force/Force Select.png");
            if (is != null) {
                BufferedImage image = ImageIO.read(is);
                is.close();
                System.out.println("Successfully loaded Force Select background image");
                return image;
            }
        } catch (IOException e) {
            System.err.println("Failed to load Force Select background image: sprites/Force/Force Select.png");
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Load PNG skill icon based on skill type
     */
    private BufferedImage loadSkillIcon(int skillType) {
        String iconPath;
        switch (skillType) {
            case 0: // Attack Power
                iconPath = "sprites/Skill/Icon.6_26.png";
                break;
            case 1: // Attack Speed
                iconPath = "sprites/Skill/Icon.1_45.png";
                break;
            case 2: // HP Recovery
                iconPath = "sprites/Skill/Icon.7_11.png";
                break;
            default:
                iconPath = "sprites/Skill/Icon.6_26.png";
                break;
        }
        
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(iconPath);
            if (is != null) {
                BufferedImage icon = ImageIO.read(is);
                is.close();
                return icon;
            }
        } catch (IOException e) {
            System.err.println("Failed to load skill icon: " + iconPath);
            e.printStackTrace();
        }
        return null;
    }
    
    private void drawSkillStats(Graphics2D g, int skillType, int panelX, int panelY, int panelWidth,
                               int attackPower, double attackSpeed, int maxHP,
                               int attackPowerCost, int attackSpeedCost, int hpUpCost, int skillPoints) {
        g.setFont(new Font(DEFAULT_FONT_NAME, Font.PLAIN, 13));
        FontMetrics fm = g.getFontMetrics();
        
    String currentText = "";
    String nextText = "";
    String costText = "";
        
        switch (skillType) {
            case 0: 
                currentText = "현재: " + (100 + attackPower * 20) + "%";
                nextText = NEXT_LEVEL_TEXT + ": " + (100 + (attackPower + 1) * 20) + "%";
                costText = "비용: " + attackPowerCost + "포인트";
                break;
            case 1: 
                currentText = "현재: " + String.format("%.1f", attackSpeed) + "x";
                nextText = NEXT_LEVEL_TEXT + ": " + String.format("%.1f", attackSpeed + 0.2) + "x";
                costText = "비용: " + attackSpeedCost + "포인트";
                break;
                        case 2:
                            currentText = "현재: " + maxHP + " HP";
                            nextText = NEXT_LEVEL_TEXT + ": " + (maxHP + 3) + " HP";
                            costText = "비용: " + hpUpCost + "포인트";
                            break;
                        default:
                            // Handle unexpected skillType gracefully
                            currentText = "현재: N/A";
                            nextText = "다음 단계: N/A";
                            costText = "비용: N/A";
                            break;
                    }        
        // Current level
        int currentX = panelX + (panelWidth - fm.stringWidth(currentText)) / 2;
        g.drawString(currentText, currentX, panelY + 150);
        
        // Next level
        int nextX = panelX + (panelWidth - fm.stringWidth(nextText)) / 2;
        g.drawString(nextText, nextX, panelY + 170);
        
        // Cost
        g.setFont(new Font(DEFAULT_FONT_NAME, Font.PLAIN, 12));
        fm = g.getFontMetrics();
        int costX = panelX + (panelWidth - fm.stringWidth(costText)) / 2;
        g.drawString(costText, costX, panelY + 195);
        
        // Note: Removed insufficient points warning - now using visual border indication instead
    }
}
