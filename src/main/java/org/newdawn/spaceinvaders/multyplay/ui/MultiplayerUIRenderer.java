package org.newdawn.spaceinvaders.multyplay.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.gameplay.UIRenderer;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameCanvas;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerSkillManager;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;

/**
 * UI 렌더링을 담당하는 클래스
 * 게임 UI, 메시지 등을 그리는 역할
 */
public class MultiplayerUIRenderer {
    // 재사용 가능한 스트로크 (매 프레임 객체 생성 방지)
    private static final BasicStroke STROKE_2PX = new BasicStroke(2);
    private BufferedImage coinImage;

    public MultiplayerUIRenderer(MultiplayerGameCanvas game) {
        // MultiplayerGameCanvas reference not currently used, but kept for future extensibility
    }

    /**
     * 게임 UI 그리기 (HP, 스킬 포인트, 스탯 등)
     */
    public void drawGameUI(Graphics2D g, MultiplayerGameStateManager gameStateManager, MultiplayerSkillManager skillManager) {
        // Round display (top-left)
        g.setColor(Color.CYAN);
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 18));
        g.drawString("라운드: " + gameStateManager.getCurrentRound() + "/" + gameStateManager.getMaxRound(), 20, 25);

        // HP display
        g.setColor(Color.WHITE);
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 16));
        g.drawString("HP: " + gameStateManager.getCurrentHP() + "/" + gameStateManager.getMaxHP(), 20, 50);

        drawHPBar(g, gameStateManager);

        // Skill points & stats (mirror single-player layout)
        g.setColor(Color.YELLOW);
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 16));
        g.drawString("스킬 포인트: " + gameStateManager.getSkillPoints(), 20, 100);

        g.setColor(Color.WHITE);
        g.setFont(UIRenderer.getKostarFont(Font.PLAIN, 12));
        g.drawString("공격력: " + gameStateManager.getAttackPower(), 20, 120);
        g.drawString("공격속도: " + String.format("%.1f", gameStateManager.getAttackSpeed()) + "x", 20, 135);

        drawSkillBar(g, skillManager);
        drawSkillEffects(g, skillManager);

        drawPlayTimeAndCoins(g, gameStateManager);
    }

    /**
     * HP 바 그리기 (Hp.png 이미지 기반)
     */
    private void drawHPBar(Graphics2D g, MultiplayerGameStateManager gameStateManager) {
        int barWidth = 200;
        int barHeight = 20;
        int barX = 20;
        int barY = 60;
        
        // Load HP image
        BufferedImage hpImage = loadHPImage();
        
        if (hpImage != null) {
            // Calculate HP fill percentage
            double hpPercentage = (double) gameStateManager.getCurrentHP() / gameStateManager.getMaxHP();
            int hpWidth = (int) (hpPercentage * barWidth);
            
            // Draw HP bar background (empty part)
            g.setColor(new Color(50, 50, 50));
            g.fillRect(barX, barY, barWidth, barHeight);
            
            // Draw HP fill using Hp.png image
            if (hpWidth > 0) {
                // Scale the HP image to fit the HP bar width
                g.drawImage(hpImage, barX, barY, hpWidth, barHeight, null);
            }
            
            // Border
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawRect(barX, barY, barWidth, barHeight);
        } else {
            // Fallback: Draw simple colored HP bar if image fails to load
            g.setColor(new Color(50, 50, 50));
            g.fillRect(barX, barY, barWidth, barHeight);
            
            int hpWidth = (int) ((double) gameStateManager.getCurrentHP() / gameStateManager.getMaxHP() * barWidth);
            g.setColor(new Color(255, 0, 0));
            g.fillRect(barX, barY, hpWidth, barHeight);
            
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawRect(barX, barY, barWidth, barHeight);
        }
    }
    
    /**
     * HP 이미지 로드
     */
    private BufferedImage loadHPImage() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/ships/Hp.png");
            if (is != null) {
                BufferedImage image = ImageIO.read(is);
                is.close();
                return image;
            }
        } catch (Exception e) {
            System.err.println("Failed to load HP image: sprites/ships/Hp.png");
        }
        return null;
    }

    /**
     * 활성화된 스킬 효과 표시
     */
    private void drawSkillEffects(Graphics2D g, MultiplayerSkillManager skillManager) {
        int effectY = 200;

        if (skillManager.isInvincible()) {
            g.setColor(Color.YELLOW);
            g.setFont(UIRenderer.getKostarFont(Font.BOLD, 14));
            long remainingTime = skillManager.getRemainingTime(skillManager.getInvincibleEndTime());
            if (remainingTime > 0) {
                g.drawString("무적: " + remainingTime + "초", 20, effectY);
                effectY += 20;
            }
        }

        if (skillManager.hasTripleShot()) {
            g.setColor(Color.BLUE);
            g.setFont(UIRenderer.getKostarFont(Font.BOLD, 14));
            long remainingTime = skillManager.getRemainingTime(skillManager.getTripleShotEndTime());
            if (remainingTime > 0) {
                g.drawString("3줄공격: " + remainingTime + "초", 20, effectY);
                effectY += 20;
            }
        }
    }

    /**
     * 스킬 인벤토리 표시 (이미지 기반) - 화면 우하단에 배치
     */
    private void drawSkillBar(Graphics2D g, MultiplayerSkillManager skillManager) {
        int barWidth = 240;
        int barHeight = 35;
        int barX = 20;
        int barY = 145;

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(barX - 8, barY - 8, barWidth + 16, barHeight + 16, 10, 10);
        g.setColor(new Color(255, 255, 255, 100));
        g.drawRoundRect(barX - 8, barY - 8, barWidth + 16, barHeight + 16, 10, 10);

        int itemSize = 24;
        int itemSpacing = 70;
        int startX = barX + 10;
        int centerY = barY + (barHeight - itemSize) / 2;

        int[] skillTypes = {0, 2, 3};
        String[] skillNames = {"무적", "3줄공격", "미사일"};
        int[] counts = {
                skillManager.getInvincibleSkills(),
                skillManager.getTripleShotSkills(),
                skillManager.getMissileSkills()
        };

        for (int i = 0; i < skillTypes.length; i++) {
            int itemX = startX + (i * itemSpacing);
            drawSkillIconWithCount(g, skillTypes[i], itemX, centerY, itemSize, counts[i]);

            g.setFont(new Font("Arial", Font.BOLD, 11));
            String label = skillNames[i];
            int labelWidth = g.getFontMetrics().stringWidth(label);
            int labelX = itemX + (itemSize - labelWidth) / 2;
            int labelY = centerY + itemSize + 14;

            g.setColor(new Color(0, 0, 0, 200));
            g.drawString(label, labelX + 1, labelY + 1);
            g.setColor(Color.WHITE);
            g.drawString(label, labelX, labelY);
        }

        g.setColor(Color.CYAN);
        g.setFont(UIRenderer.getKostarFont(Font.PLAIN, 10));
        g.drawString("Q: 강화창", barX + 5, barY + barHeight + 18);
    }

    private BufferedImage getCoinImage() {
        if (coinImage != null) {
            return coinImage;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/star coin normal.png")) {
            if (is != null) {
                coinImage = ImageIO.read(is);
            }
        } catch (Exception ignored) {
        }
        return coinImage;
    }

    private void drawPlayTimeAndCoins(Graphics2D g, MultiplayerGameStateManager gameStateManager) {
        int screenWidth = 800;
        int panelWidth = 180;
        int panelHeight = 70;
        int startX = screenWidth - panelWidth - 20;
        int startY = 20;

        g.setColor(new Color(20, 20, 40, 130));
        g.fillRoundRect(startX, startY, panelWidth, panelHeight, 15, 15);
        g.setColor(new Color(10, 10, 25, 130));
        g.fillRoundRect(startX + 2, startY + 2, panelWidth - 4, panelHeight - 4, 13, 13);

        g.setColor(new Color(100, 150, 255, 50));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(startX, startY, panelWidth, panelHeight, 15, 15);
        g.setColor(new Color(150, 200, 255, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(startX + 1, startY + 1, panelWidth - 2, panelHeight - 2, 14, 14);

        g.setColor(new Color(100, 255, 255));
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("TIME: " + gameStateManager.getPlayTime(), startX + 15, startY + 25);

        int coins = gameStateManager.getEarnedCoins();
        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("COINS: " + coins, startX + 50, startY + 50);

        BufferedImage image = getCoinImage();
        int coinX = startX + 15;
        int coinY = startY + 34;
        if (image != null) {
            g.drawImage(image, coinX, coinY - 16, 24, 24, null);
        } else {
            g.setColor(Color.YELLOW);
            g.fillOval(coinX, coinY - 16, 24, 24);
            g.setColor(Color.ORANGE);
            g.setStroke(new BasicStroke(2));
            g.drawOval(coinX, coinY - 16, 24, 24);
        }
    }
    
    /**
     * 스킬 아이콘과 개수를 그리기
     */
    private void drawSkillIconWithCount(Graphics2D g, int skillType, int x, int y, int iconSize, int count) {
        // Load and draw skill icon
        BufferedImage skillImage = loadSkillImage(skillType);
        if (skillImage != null) {
            g.drawImage(skillImage, x, y, iconSize, iconSize, null);
        } else {
            // Fallback: draw colored rectangle
            g.setColor(getSkillColor(skillType));
            g.fillRect(x, y, iconSize, iconSize);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, iconSize, iconSize);
        }
        
        // Draw count
        g.setColor(Color.WHITE);
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 10));
        String countText = String.valueOf(count);
        FontMetrics fm = g.getFontMetrics();
        int textX = x + iconSize - fm.stringWidth(countText) - 2;
        int textY = y + iconSize - 2;
        
        // Draw count background
        g.setColor(Color.BLACK);
        g.fillRect(textX - 1, textY - fm.getHeight() + 2, fm.stringWidth(countText) + 2, fm.getHeight());
        
        // Draw count text
        g.setColor(Color.YELLOW);
        g.drawString(countText, textX, textY);
    }
    
    /**
     * 스킬 타입별 색상 반환 (fallback용)
     */
    private Color getSkillColor(int skillType) {
        switch (skillType) {
            case 0: return new Color(255, 215, 0); // Gold - Invincible
            case 2: return new Color(0, 100, 255); // Blue - Triple Shot
            case 3: return new Color(255, 165, 0); // Orange - Missile
            default: return Color.GRAY;
        }
    }
    
    /**
     * 스킬 이미지 로드
     */
    private BufferedImage loadSkillImage(int skillType) {
        String imagePath;
        switch (skillType) {
            case 0:
                imagePath = "sprites/Skill/1.png";
                break;
            case 2:
                imagePath = "sprites/Skill/3.png";
                break;
            case 3:
                imagePath = "sprites/Skill/4.png";
                break;
            default:
                imagePath = "sprites/Skill/1.png";
                break;
        }
        
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(imagePath);
            if (is != null) {
                BufferedImage image = ImageIO.read(is);
                is.close();
                return image;
            }
        } catch (Exception e) {
            System.err.println("Failed to load skill image: " + imagePath);
        }
        return null;
    }

    /**
     * 메시지 그리기 (게임 중간 메시지)
     */
    public void drawMessage(Graphics2D g, String message) {
        if (message != null && !message.isEmpty()) {
            g.setColor(Color.white);
            g.setFont(UIRenderer.getKostarFont(Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            int messageX = (800 - fm.stringWidth(message)) / 2;
            g.drawString(message, messageX, 250);

            g.setFont(UIRenderer.getKostarFont(Font.PLAIN, 14));
            fm = g.getFontMetrics();
            String pressKey = "Press any key";
            int pressKeyX = (800 - fm.stringWidth(pressKey)) / 2;
            g.drawString(pressKey, pressKeyX, 300);
        }
    }

    /**
     * FPS 표시 업데이트
     */
    public void updateFPSDisplay(String windowTitle, int fps) {
        // FPS는 Game 클래스에서 직접 처리
    }

    // ===== Overlays moved from mainmenu dependency =====
    public void drawPauseOverlay(Graphics2D g, int selectedIndex) {
        // 반투명 배경
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, 800, 600);

        // 제목
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 36));
        g.setColor(Color.WHITE);
        String title = "일시정지";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (800 - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 180);

        // 메뉴 항목
        String[] items = {"계속하기", "메인메뉴", "설정"};
        int y = 260;
        for (int i = 0; i < items.length; i++) {
            boolean sel = (i == selectedIndex);
            drawMenuButton(g, items[i], 325, y, 150, 40, sel);
            y += 60;
        }

        // 안내
        g.setFont(UIRenderer.getKostarFont(Font.PLAIN, 14));
        g.setColor(Color.YELLOW);
        String hint = "ESC: 메뉴 닫기";
        int hintX = (800 - g.getFontMetrics().stringWidth(hint)) / 2;
        g.drawString(hint, hintX, 520);
    }

    public void drawSkillOverlay(Graphics2D g,
                                 int skillPoints,
                                 int attackPower,
                                 double attackSpeed,
                                 int maxHP,
                                 int costAtk,
                                 int costAspd,
                                 int costHp,
                                 int selectedSkill,
                                 MultiplayerSkillManager skillManager) {
        // SkillMenuRenderer 사용
        org.newdawn.spaceinvaders.mainmenu.SkillMenuRenderer skillMenuRenderer = new org.newdawn.spaceinvaders.mainmenu.SkillMenuRenderer();
        skillMenuRenderer.drawSkillMenu(g, skillPoints, attackPower, attackSpeed, maxHP, costAtk, costAspd, costHp, selectedSkill, skillManager);
    }

    private void drawMenuButton(Graphics2D g, String text, int x, int y, int w, int h, boolean selected) {
        // 배경
        g.setColor(selected ? new Color(70, 130, 180) : new Color(25, 25, 112));
        g.fillRect(x, y, w, h);
        // 테두리
        g.setColor(new Color(100, 149, 237));
        g.setStroke(STROKE_2PX);
        g.drawRect(x, y, w, h);
        // 텍스트
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 16));
        g.setColor(Color.WHITE);
        int tx = x + (w - g.getFontMetrics().stringWidth(text)) / 2;
        int ty = y + (h + g.getFontMetrics().getAscent()) / 2 - 2;
        g.drawString(text, tx, ty);
    }

    private void drawCard(Graphics2D g, int x, int y, int w, int h, String title, String sub, boolean selected) {
        g.setColor(selected ? new Color(60, 60, 140) : new Color(40, 40, 100));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(120, 120, 200));
        g.setStroke(STROKE_2PX);
        g.drawRoundRect(x, y, w, h, 10, 10);

        g.setColor(Color.WHITE);
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 14));
        int tx = x + (w - g.getFontMetrics().stringWidth(title)) / 2;
        g.drawString(title, tx, y + 55);

        g.setFont(UIRenderer.getKostarFont(Font.PLAIN, 12));
        int sx = x + (w - g.getFontMetrics().stringWidth(sub)) / 2;
        g.setColor(Color.YELLOW);
        g.drawString(sub, sx, y + 80);
    }
}
