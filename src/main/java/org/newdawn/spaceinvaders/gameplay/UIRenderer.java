
package org.newdawn.spaceinvaders.gameplay;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;

/**
 * UI 렌더링을 담당하는 클래스
 * 게임 UI, 메시지 등을 그리는 역할
 */
public class UIRenderer {
    // 재사용 가능한 폰트/스트로크 (매 프레임 객체 생성 방지)
    private static final Font FONT_TITLE_18_B = new Font("Arial", Font.BOLD, 18);
    private static final Font FONT_TEXT_16_B = new Font("Arial", Font.BOLD, 16);
    private static final Font FONT_TEXT_14_B = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_TEXT_12_P = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_TEXT_14_P = new Font("Arial", Font.PLAIN, 14);
    private static final BasicStroke STROKE_2PX = new BasicStroke(2);

    public UIRenderer(Game game) {
        // Game reference not currently used, but kept for future extensibility
    }

    /**
     * 게임 UI 그리기 (HP, 스킬 포인트, 스탯 등)
     */
    public void drawGameUI(Graphics2D g, GameStateManager gameStateManager, SkillManager skillManager) {
        // Round display
        g.setColor(Color.CYAN);
        g.setFont(FONT_TITLE_18_B);
        g.drawString("라운드: " + gameStateManager.getCurrentRound() + "/" + gameStateManager.getMaxRound(), 20, 25);

        // HP display
        g.setColor(Color.WHITE);
        g.setFont(FONT_TEXT_16_B);
        g.drawString("HP: " + gameStateManager.getCurrentHP() + "/" + gameStateManager.getMaxHP(), 20, 50);

        // HP bar
        drawHPBar(g, gameStateManager);

        // Skill points display
        g.setColor(Color.YELLOW);
        g.setFont(FONT_TEXT_16_B);
        g.drawString("스킬 포인트: " + gameStateManager.getSkillPoints(), 20, 100);

        // Stats display
        g.setColor(Color.WHITE);
        g.setFont(FONT_TEXT_12_P);
        g.drawString("공격력: " + gameStateManager.getAttackPower(), 20, 120);
        g.drawString("공격속도: " + String.format("%.1f", gameStateManager.getAttackSpeed()) + "x", 20, 135);

        // Skill effects display
        drawSkillEffects(g, skillManager);

        // Skill inventory display
        drawSkillInventory(g, skillManager);
        
        // Instructions moved to drawSkillInventory to avoid overlap
    }

    /**
     * HP 바 그리기 (Hp.png 이미지 기반)
     */
    private void drawHPBar(Graphics2D g, GameStateManager gameStateManager) {
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
    private void drawSkillEffects(Graphics2D g, SkillManager skillManager) {
        int effectY = 160;

        if (skillManager.isInvincible()) {
            g.setColor(Color.YELLOW);
            g.setFont(FONT_TEXT_14_B);
            long remainingTime = skillManager.getRemainingTime(skillManager.getInvincibleEndTime());
            if (remainingTime > 0) {
                g.drawString("무적: " + remainingTime + "초", 20, effectY);
                effectY += 20;
            }
        }

        if (skillManager.hasPiercing()) {
            g.setColor(Color.RED);
            g.setFont(FONT_TEXT_14_B);
            long remainingTime = skillManager.getRemainingTime(skillManager.getPiercingEndTime());
            if (remainingTime > 0) {
                g.drawString("관통: " + remainingTime + "초", 20, effectY);
                effectY += 20;
            }
        }

        if (skillManager.hasTripleShot()) {
            g.setColor(Color.BLUE);
            g.setFont(FONT_TEXT_14_B);
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
    private void drawSkillInventory(Graphics2D g, SkillManager skillManager) {
        // 화면 우하단에 인벤토리 배치 (맵을 덜 가리도록)
        int screenWidth = 800;
        int screenHeight = 600;
        int iconSize = 48; // 아이콘 크기를 더 키움
        int spacing = 60; // 아이콘 간격을 줄임
        int inventoryWidth = spacing * 4 + iconSize; // 전체 너비 계산
        int inventoryHeight = iconSize + 30; // 높이 계산
        int startX = screenWidth - inventoryWidth - 15; // 우측에서 15px 떨어진 위치
        int startY = screenHeight - inventoryHeight - 15; // 하단에서 15px 떨어진 위치
        
        // 반투명 배경 그리기 (가독성 향상) - 크기 줄임
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(startX - 8, startY - 8, inventoryWidth + 16, inventoryHeight + 16);
        
        // 테두리 그리기 - 크기 줄임
        g.setColor(new Color(255, 255, 255, 100));
        g.drawRect(startX - 8, startY - 8, inventoryWidth + 16, inventoryHeight + 16);
        
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("스킬:", startX, startY - 5);
        
        // Skill 1: Attack Power (무적)
        drawSkillIconWithCount(g, 0, startX, startY + 5, iconSize, skillManager.getInvincibleSkills());
        
        // Skill 2: Attack Speed (관통)
        drawSkillIconWithCount(g, 1, startX + spacing, startY + 5, iconSize, skillManager.getPiercingSkills());
        
        // Skill 3: HP Recovery (3줄공격)
        drawSkillIconWithCount(g, 2, startX + spacing * 2, startY + 5, iconSize, skillManager.getTripleShotSkills());
        
        // Skill 4: Missile (미사일)
        drawSkillIconWithCount(g, 3, startX + spacing * 3, startY + 5, iconSize, skillManager.getMissileSkills());
        
        // Instructions (위쪽으로 이동)
        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("Q: 강화", startX, startY - 25);
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
        g.setFont(new Font("Arial", Font.BOLD, 10));
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
            case 0: return new Color(255, 215, 0); // Gold
            case 1: return new Color(255, 0, 0);   // Red
            case 2: return new Color(0, 100, 255); // Blue
            case 3: return new Color(255, 165, 0); // Orange
            default: return Color.GRAY;
        }
    }
    
    /**
     * 스킬 이미지 로드
     */
    private BufferedImage loadSkillImage(int skillType) {
        String imagePath;
        switch (skillType) {
            case 0: imagePath = "sprites/Skill/1.png"; break;
            case 1: imagePath = "sprites/Skill/2.png"; break;
            case 2: imagePath = "sprites/Skill/3.png"; break;
            case 3: imagePath = "sprites/Skill/4.png"; break;
            default: imagePath = "sprites/Skill/1.png"; break;
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
            g.setFont(FONT_TITLE_18_B);
            FontMetrics fm = g.getFontMetrics();
            int messageX = (800 - fm.stringWidth(message)) / 2;
            g.drawString(message, messageX, 250);

            g.setFont(FONT_TEXT_14_P);
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
        g.setFont(new Font("Arial", Font.BOLD, 36));
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
        g.setFont(FONT_TEXT_14_P);
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
                                 int selectedSkill) {
        // 반투명 배경
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, 800, 600);

        // 제목
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        String title = "스킬 업그레이드";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (800 - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 140);

        // 포인트
        g.setFont(FONT_TEXT_16_B);
        g.setColor(Color.YELLOW);
        g.drawString("스킬 포인트: " + skillPoints, 300, 180);

        // 카드 형태 버튼 3개
        String[] names = {"공격력 +1", "공격속도 +0.2", "최대HP +3 & 풀회복"};
        String[] costs = {"필요: " + costAtk, "필요: " + costAspd, "필요: " + costHp};
        int startX = 120;
        for (int i = 0; i < 3; i++) {
            boolean sel = (i == selectedSkill);
            int x = startX + i * 200;
            drawCard(g, x, 220, 180, 140, names[i], costs[i], sel);
        }

        // 힌트
        g.setFont(FONT_TEXT_12_P);
        g.setColor(Color.CYAN);
        String hint = "좌/우로 이동, Enter/Space 선택, Q 닫기";
        int hintX = (800 - g.getFontMetrics().stringWidth(hint)) / 2;
        g.drawString(hint, hintX, 410);
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
        g.setFont(FONT_TEXT_16_B);
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
        g.setFont(FONT_TEXT_14_B);
        int tx = x + (w - g.getFontMetrics().stringWidth(title)) / 2;
        g.drawString(title, tx, y + 55);

        g.setFont(FONT_TEXT_12_P);
        int sx = x + (w - g.getFontMetrics().stringWidth(sub)) / 2;
        g.setColor(Color.YELLOW);
        g.drawString(sub, sx, y + 80);
    }
}
