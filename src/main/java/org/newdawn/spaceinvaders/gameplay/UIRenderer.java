
package org.newdawn.spaceinvaders.gameplay;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.File;

/**
 * UI 렌더링을 담당하는 클래스
 * 게임 UI, 메시지 등을 그리는 역할
 */
public class UIRenderer {
    // Kostar 폰트 로드
    private static Font KOSTAR_FONT = null;
    
    // 재사용 가능한 폰트/스트로크 (매 프레임 객체 생성 방지)
    private static final Font FONT_TITLE_18_B = new Font("Arial", Font.BOLD, 18);
    private static final Font FONT_TEXT_16_B = new Font("Arial", Font.BOLD, 16);
    private static final Font FONT_TEXT_14_B = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_TEXT_12_P = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_TEXT_14_P = new Font("Arial", Font.PLAIN, 14);
    private static final Font FONT_TEXT_10_P = new Font("Arial", Font.PLAIN, 10);
    private static final BasicStroke STROKE_2PX = new BasicStroke(2);
    
    static {
        loadKostarFont();
    }
    
    /**
     * Kostar 폰트 로드
     */
    private static void loadKostarFont() {
        try {
            InputStream fontStream = UIRenderer.class.getClassLoader().getResourceAsStream("fonts/Kostar.ttf");
            if (fontStream != null) {
                KOSTAR_FONT = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                fontStream.close();
                System.out.println("✅ Kostar 폰트 로드 성공");
            } else {
                System.err.println("❌ Kostar 폰트 파일을 찾을 수 없습니다");
                KOSTAR_FONT = new Font("Arial", Font.PLAIN, 12); // 폴백
            }
        } catch (Exception e) {
            System.err.println("❌ Kostar 폰트 로드 실패: " + e.getMessage());
            KOSTAR_FONT = new Font("Arial", Font.PLAIN, 12); // 폴백
        }
    }
    
    /**
     * Kostar 폰트를 지정된 크기로 반환
     */
    public static Font getKostarFont(int size) {
        if (KOSTAR_FONT != null) {
            return KOSTAR_FONT.deriveFont(Font.PLAIN, size);
        }
        return new Font("Arial", Font.PLAIN, size);
    }
    
    /**
     * Kostar 폰트를 지정된 크기와 스타일로 반환
     */
    public static Font getKostarFont(int style, int size) {
        if (KOSTAR_FONT != null) {
            return KOSTAR_FONT.deriveFont(style, size);
        }
        return new Font("Arial", style, size);
    }
    
    // 이미지 캐싱
    private BufferedImage cachedCoinImage = null;

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
        g.setFont(getKostarFont(Font.BOLD, 16));
        g.drawString("HP: " + gameStateManager.getCurrentHP() + "/" + gameStateManager.getMaxHP(), 20, 50);

        // HP bar
        drawHPBar(g, gameStateManager);

        // Skill UI below HP bar (same size as HP bar)
        drawSkillUI(g, skillManager, gameStateManager);

        // Skill effects display
        drawSkillEffects(g, skillManager);

        // Q로 강화창이 열렸을 때만 스킬포인트와 스탯 표시
        if (gameStateManager.isShowingSkillMenu()) {
            drawUpgradeStats(g, gameStateManager);
        }
        
        // 우측 상단에 플레이 시간과 획득 코인 표시
        drawPlayTimeAndCoins(g, gameStateManager);
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
            g.setFont(getKostarFont(Font.BOLD, 14));
            long remainingTime = skillManager.getRemainingTime(skillManager.getInvincibleEndTime());
            if (remainingTime > 0) {
                g.drawString("무적: " + remainingTime + "초", 20, effectY);
                effectY += 20;
            }
        }

        // 관통 스킬 제거됨

        if (skillManager.hasTripleShot()) {
            g.setColor(Color.BLUE);
            g.setFont(getKostarFont(Font.BOLD, 14));
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
        
        // Skill 1: Invincible (무적)
        drawSkillIconWithCount(g, 0, startX, startY + 5, iconSize, skillManager.getInvincibleSkills());
        
        // Skill 2: Triple Shot (3줄공격)
        drawSkillIconWithCount(g, 2, startX + spacing, startY + 5, iconSize, skillManager.getTripleShotSkills());
        
        // Skill 3: Missile (미사일)
        drawSkillIconWithCount(g, 3, startX + spacing * 2, startY + 5, iconSize, skillManager.getMissileSkills());
        
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
            case 0: return new Color(255, 215, 0); // Gold (무적)
            case 2: return new Color(0, 100, 255); // Blue (3줄공격)
            case 3: return new Color(255, 165, 0); // Orange (미사일)
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
            // 게임 클리어 메시지인지 확인
            if (message.startsWith("경")) {
                drawGameClearMessage(g, message);
            } else {
                drawGameOverMessage(g, message);
            }
        }
    }
    
    /**
     * 게임 오버 메시지 그리기 (첫 번째 이미지 스타일)
     */
    private void drawGameOverMessage(Graphics2D g, String message) {
        // 반투명 배경
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, 800, 600);
        
        // 메시지 창 (첫 번째 이미지 스타일)
        int windowWidth = 400;
        int windowHeight = 150;
        int windowX = (800 - windowWidth) / 2;
        int windowY = (600 - windowHeight) / 2;
        
        // 창 배경 (연한 파란색)
        g.setColor(new Color(224, 235, 245)); // 연한 파란색
        g.fillRect(windowX, windowY, windowWidth, windowHeight);
        
        // 창 테두리 (진한 파란색)
        g.setColor(new Color(25, 118, 210)); // 진한 파란색
        g.setStroke(new BasicStroke(4));
        g.drawRect(windowX, windowY, windowWidth, windowHeight);
        
        // 메시지 텍스트 (Kostar 폰트, 손글씨체)
        g.setColor(Color.BLACK);
        g.setFont(getKostarFont(Font.BOLD, 24));
        String[] lines = message.split("\n");
        
        int textY = windowY + 60;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                FontMetrics fm = g.getFontMetrics();
                int textX = windowX + (windowWidth - fm.stringWidth(line)) / 2;
                g.drawString(line, textX, textY);
                textY += 35;
            } else {
                textY += 15; // 빈 줄은 더 작은 간격
            }
        }
        
        // 안내 텍스트
        g.setColor(new Color(100, 100, 100));
        g.setFont(getKostarFont(16));
        String pressKey = "아무 키나 눌러서 다시 시작";
        FontMetrics fm = g.getFontMetrics();
        int pressKeyX = windowX + (windowWidth - fm.stringWidth(pressKey)) / 2;
        g.drawString(pressKey, pressKeyX, windowY + windowHeight - 20);
    }
    
    /**
     * 게임 클리어 메시지 그리기 (두 번째 이미지 스타일)
     */
    private void drawGameClearMessage(Graphics2D g, String message) {
        // 반투명 배경
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, 800, 600);
        
        // 메시지 창 (첨부 이미지 스타일)
        int windowWidth = 400;
        int windowHeight = 250;
        int windowX = (800 - windowWidth) / 2;
        int windowY = (600 - windowHeight) / 2;
        
        // 그라데이션 배경을 위한 색상 (스톱워치 box와 동일)
        Color bgColor1 = new Color(20, 20, 40, 130); // 어두운 보라색
        Color bgColor2 = new Color(10, 10, 25, 130);  // 더 어두운 보라색
        
        // 배경 그라데이션 효과
        g.setColor(bgColor1);
        g.fillRoundRect(windowX, windowY, windowWidth, windowHeight, 15, 15);
        
        // 내부 그라데이션 효과
        g.setColor(bgColor2);
        g.fillRoundRect(windowX + 2, windowY + 2, windowWidth - 4, windowHeight - 4, 13, 13);
        
        // 외곽 테두리 (글로우 효과)
        g.setColor(new Color(100, 150, 255, 50));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(windowX, windowY, windowWidth, windowHeight, 15, 15);
        
        // 내부 테두리
        g.setColor(new Color(150, 200, 255, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(windowX + 1, windowY + 1, windowWidth - 2, windowHeight - 2, 14, 14);
        
        // 경축 텍스트 (큰 글씨, 중앙정렬)
        g.setColor(Color.WHITE);
        g.setFont(getKostarFont(Font.BOLD, 28));
        String[] lines = message.split("\n");
        String title = "경축"; // "경축"만 표시
        
        FontMetrics titleFm = g.getFontMetrics();
        int titleX = windowX + (windowWidth - titleFm.stringWidth(title)) / 2;
        int titleY = windowY + 50;
        g.drawString(title, titleX, titleY);
        
        // 정보 텍스트 (걸린시간, 획득코인, 중앙정렬)
        g.setFont(getKostarFont(16));
        g.setColor(Color.WHITE);
        int infoY = windowY + 100;
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) {
                FontMetrics infoFm = g.getFontMetrics();
                int infoX = windowX + (windowWidth - infoFm.stringWidth(lines[i])) / 2;
                g.drawString(lines[i], infoX, infoY);
                infoY += 30;
            }
        }
        
        // 안내 텍스트 (하단 중앙)
        g.setColor(Color.WHITE);
        g.setFont(getKostarFont(14));
        String pressKey = "아무키나 눌러서 메인화면 이동";
        FontMetrics fm = g.getFontMetrics();
        int pressKeyX = windowX + (windowWidth - fm.stringWidth(pressKey)) / 2;
        int pressKeyY = windowY + windowHeight - 20;
        g.drawString(pressKey, pressKeyX, pressKeyY);
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
        String[] items = {"계속하기", "그만두기"};
        int y = 260;
        for (int i = 0; i < items.length; i++) {
            boolean sel = (i == selectedIndex);
            drawMenuButton(g, items[i], 325, y, 150, 40, sel);
            y += 60;
        }
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
                                 SkillManager skillManager) {
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
        g.setFont(getKostarFont(Font.BOLD, 16));
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
        g.setFont(getKostarFont(Font.BOLD, 14));
        int tx = x + (w - g.getFontMetrics().stringWidth(title)) / 2;
        g.drawString(title, tx, y + 55);

        g.setFont(getKostarFont(12));
        int sx = x + (w - g.getFontMetrics().stringWidth(sub)) / 2;
        g.setColor(Color.YELLOW);
        g.drawString(sub, sx, y + 80);
    }

    /**
     * 스킬 UI 그리기 (HP바 아래, HP바와 같은 크기)
     */
    private void drawSkillUI(Graphics2D g, SkillManager skillManager, GameStateManager gameStateManager) {
        int barWidth = 240; // 스킬 UI 너비를 늘림 (200 → 240)
        int barHeight = 35; // 스킬 UI 높이를 늘림 (20 → 35)
        int barX = 20;
        int barY = 85; // HP바 아래 (HP바 Y=60, 높이=20이므로 60+20+5=85)
        
        // 반투명 상자 제거 - 그림자 효과만 사용
        
        // 스킬 아이템들 표시 (item_box.png 기반)
        drawSkillItems(g, skillManager, barX, barY, barWidth, barHeight);
        
        // Q키 안내를 스킬 UI 아래로 이동
        g.setColor(Color.CYAN);
        g.setFont(getKostarFont(10));
        g.drawString("Q: 강화창", barX + 5, barY + barHeight + 15);
    }

    /**
     * 스킬 아이템들 그리기 (item_box.png 기반)
     */
    private void drawSkillItems(Graphics2D g, SkillManager skillManager, int x, int y, int width, int height) {
        // 스킬 아이템 크기를 키우고 간격 조정
        int itemSize = 18; // 스킬 아이템 크기를 키움 (12 → 18)
        int itemSpacing = 35; // 아이템 간격을 늘림 (20 → 35)
        int startX = x + 10;
        int centerY = y + (height - itemSize) / 2;
        
        // 각 스킬 아이템 표시 (실제 스킬 이미지 사용)
        int[] skillTypes = {0, 2, 3}; // 무적, 3줄공격, 미사일
        String[] skillNames = {"무적", "3줄공격", "미사일"};
        
        for (int i = 0; i < 3; i++) {
            int itemX = startX + (i * itemSpacing);
            
            // 실제 스킬 이미지 사용 (진한 그림자 효과로 선명도 향상)
            BufferedImage skillImage = loadSkillImage(skillTypes[i]);
            if (skillImage != null) {
                // 진한 그림자 효과 (오른쪽 아래로 이동, 더 진한 색상)
                g.setColor(new Color(0, 0, 0, 200)); // 더 진한 그림자
                g.fillRect(itemX + 2, centerY + 2, itemSize, itemSize);
                
                // 추가 그림자 (더 깊이감)
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(itemX + 1, centerY + 1, itemSize, itemSize);
                
                // 실제 스킬 이미지
                g.drawImage(skillImage, itemX, centerY, itemSize, itemSize, null);
                
                // 진한 테두리 효과 (선명도 향상)
                g.setColor(new Color(255, 255, 255, 200)); // 더 진한 테두리
                g.setStroke(new BasicStroke(2));
                g.drawRect(itemX, centerY, itemSize, itemSize);
            }
            
            // 스킬 개수 표시 (더 명확하게)
            int skillCount = getSkillCount(skillManager, skillTypes[i]);
            
            // 개수 배경 (반투명 원형)
            int circleX = itemX + itemSize - 6;
            int circleY = centerY + itemSize - 6;
            int circleRadius = 8;
            
            if (skillCount > 0) {
                // 배경 원
                g.setColor(new Color(0, 0, 0, 180));
                g.fillOval(circleX - circleRadius, circleY - circleRadius, circleRadius * 2, circleRadius * 2);
                
                // 개수 텍스트
                g.setColor(Color.YELLOW);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                String countText = String.valueOf(skillCount);
                int textWidth = g.getFontMetrics().stringWidth(countText);
                int textHeight = g.getFontMetrics().getHeight();
                int textX = circleX - textWidth / 2;
                int textY = circleY + textHeight / 4;
                g.drawString(countText, textX, textY);
            } else {
                // 개수가 0일 때는 빈 원만 표시
                g.setColor(new Color(100, 100, 100, 100));
                g.fillOval(circleX - circleRadius, circleY - circleRadius, circleRadius * 2, circleRadius * 2);
                g.setColor(new Color(150, 150, 150));
                g.drawOval(circleX - circleRadius, circleY - circleRadius, circleRadius * 2, circleRadius * 2);
            }
            
            // 스킬 이름 표시 (아래쪽에 작게, 진한 그림자 효과로 선명도 향상)
            g.setFont(new Font("Arial", Font.BOLD, 10)); // 크기도 조금 키움
            String skillName = skillNames[i];
            int nameWidth = g.getFontMetrics().stringWidth(skillName);
            int nameX = itemX + (itemSize - nameWidth) / 2;
            int nameY = centerY + itemSize + 12;
            
            // 진한 그림자 효과 (여러 겹으로)
            g.setColor(new Color(0, 0, 0, 220)); // 매우 진한 그림자
            g.drawString(skillName, nameX + 2, nameY + 2);
            g.setColor(new Color(0, 0, 0, 180)); // 중간 그림자
            g.drawString(skillName, nameX + 1, nameY + 1);
            
            // 실제 텍스트 (더 진한 흰색)
            g.setColor(new Color(255, 255, 255, 255)); // 완전 불투명 흰색
            g.drawString(skillName, nameX, nameY);
        }
    }

    /**
     * 스킬 개수 반환
     */
    private int getSkillCount(SkillManager skillManager, int skillType) {
        switch (skillType) {
            case 0: return skillManager.getInvincibleSkills();
            case 2: return skillManager.getTripleShotSkills();
            case 3: return skillManager.getMissileSkills();
            default: return 0;
        }
    }

    /**
     * item_box.png 이미지 로드
     */
    private BufferedImage loadItemBoxImage() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/item_box.png");
            if (is != null) {
                BufferedImage image = ImageIO.read(is);
                is.close();
                return image;
            }
        } catch (Exception e) {
            System.err.println("Failed to load item box image: sprites/item_box.png");
        }
        return null;
    }

    /**
     * 강화창에서만 표시되는 스킬포인트와 스탯
     */
    private void drawUpgradeStats(Graphics2D g, GameStateManager gameStateManager) {
        int startY = 120;
        
        // 스킬 포인트 표시
        g.setColor(Color.YELLOW);
        g.setFont(getKostarFont(Font.BOLD, 16));
        g.drawString("스킬 포인트: " + gameStateManager.getSkillPoints(), 20, startY);

        // 공격력과 공격속도 표시
        g.setColor(Color.WHITE);
        g.setFont(getKostarFont(12));
        g.drawString("공격력: " + gameStateManager.getAttackPower(), 20, startY + 25);
        g.drawString("공격속도: " + String.format("%.1f", gameStateManager.getAttackSpeed()) + "x", 20, startY + 40);
    }
    
    /**
     * 우측 상단에 플레이 시간과 획득 코인 표시
     */
    private void drawPlayTimeAndCoins(Graphics2D g, GameStateManager gameStateManager) {
        int screenWidth = 800;
        int panelWidth = 180;
        int panelHeight = 70;
        int startX = screenWidth - panelWidth - 20; // 우측에서 20px 떨어진 위치
        int startY = 20;
        
        // 그라데이션 배경을 위한 색상
        Color bgColor1 = new Color(20, 20, 40, 130); // 어두운 보라색
        Color bgColor2 = new Color(10, 10, 25, 130);  // 더 어두운 보라색
        
        // 배경 그라데이션 효과
        g.setColor(bgColor1);
        g.fillRoundRect(startX, startY, panelWidth, panelHeight, 15, 15);
        
        // 내부 그라데이션 효과
        g.setColor(bgColor2);
        g.fillRoundRect(startX + 2, startY + 2, panelWidth - 4, panelHeight - 4, 13, 13);
        
        // 외곽 테두리 (글로우 효과)
        g.setColor(new Color(100, 150, 255, 50));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(startX, startY, panelWidth, panelHeight, 15, 15);
        
        // 내부 테두리
        g.setColor(new Color(150, 200, 255, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(startX + 1, startY + 1, panelWidth - 2, panelHeight - 2, 14, 14);
        
        // 플레이 시간 표시
        g.setColor(new Color(100, 255, 255)); // 밝은 청록색
        g.setFont(new Font("Arial", Font.BOLD, 16));
        String playTime = gameStateManager.getPlayTime();
        g.drawString("TIME: " + playTime, startX + 15, startY + 25);
        
        // 획득 코인 표시
        g.setColor(new Color(255, 215, 0)); // 골드 색상
        g.setFont(new Font("Arial", Font.BOLD, 16));
        String earnedCoins = String.valueOf(gameStateManager.getEarnedCoins());
        g.drawString("COINS: " + earnedCoins, startX + 15, startY + 50);
        
        // 코인 아이콘 표시 (크기 조정)
        BufferedImage coinImage = loadCoinImage();
        if (coinImage != null) {
            int iconSize = 24; // 크기 증가
            int iconX = startX + panelWidth - iconSize - 15;
            int iconY = startY + 35; // 코인 텍스트와 정렬
            
            // 코인 아이콘 배경 (원형)
            g.setColor(new Color(255, 215, 0, 50));
            g.fillOval(iconX - 2, iconY - 2, iconSize + 4, iconSize + 4);
            
            // 코인 아이콘 테두리
            g.setColor(new Color(255, 215, 0, 150));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(iconX - 2, iconY - 2, iconSize + 4, iconSize + 4);
            
            // 코인 이미지 그리기
            g.drawImage(coinImage, iconX, iconY, iconSize, iconSize, null);
        }
    }
    
    /**
     * 코인 이미지 로드
     */
    private BufferedImage loadCoinImage() {
        if (cachedCoinImage == null) {
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/star coin normal.png");
                if (is != null) {
                    cachedCoinImage = ImageIO.read(is);
                    is.close();
                }
            } catch (Exception e) {
                System.err.println("Failed to load coin image: sprites/star coin normal.png");
            }
        }
        return cachedCoinImage;
    }
    
    /**
     * 라운드 설명 창 그리기 (상점 경고창 스타일)
     */
    public void drawRoundInfoOverlay(Graphics2D g, GameStateManager gameStateManager) {
        // 전체 화면 어둡게 처리
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, 800, 600);
        
        // 경고창 크기 계산
        int dialogWidth = 500;
        int dialogHeight = 250;
        int dialogX = (800 - dialogWidth) / 2;
        int dialogY = (600 - dialogHeight) / 2;
        
        // 경고창 배경 (상점 스타일 적용)
        g.setColor(new Color(220, 50, 50, 240));
        g.fillRect(dialogX, dialogY, dialogWidth, dialogHeight);
        
        // 경고창 테두리
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRect(dialogX, dialogY, dialogWidth, dialogHeight);
        g.setStroke(new BasicStroke(1)); // 기본 스트로크로 복원
        
        // 경고 아이콘 (삼각형 + 느낌표)
        int iconX = dialogX + 30;
        int iconY = dialogY + 30;
        int iconSize = 40;
        
        // 삼각형 경고 아이콘
        g.setColor(Color.YELLOW);
        int[] triangleX = {iconX + iconSize/2, iconX, iconX + iconSize};
        int[] triangleY = {iconY, iconY + iconSize, iconY + iconSize};
        g.fillPolygon(triangleX, triangleY, 3);
        
        // 삼각형 테두리
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(triangleX, triangleY, 3);
        g.setStroke(new BasicStroke(1f));
        
        // 느낌표
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("!", iconX + iconSize/2 - 6, iconY + iconSize - 8);
        
        // 메인 메시지
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        String mainMessage = "곧 " + gameStateManager.getCurrentRound() + " 라운드가 시작됩니다";
        int messageX = iconX + iconSize + 20;
        int messageY = dialogY + 60;
        g.drawString(mainMessage, messageX, messageY);
        
        // 서브 메시지
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String subMessage = "대비하세요 !!";
        g.drawString(subMessage, messageX, messageY + 35);
        
        // 라운드 설명 텍스트
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        String description = gameStateManager.getRoundDescription(gameStateManager.getCurrentRound());
        String[] lines = description.split("\n");
        
        int textY = messageY + 70;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                g.drawString(line, messageX, textY);
                textY += 18;
            } else {
                textY += 8; // 빈 줄은 더 작은 간격
            }
        }
        
        // 자동 닫기 안내 텍스트
        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        String autoCloseText = "이 경고창은 7초 후 자동으로 닫힙니다";
        FontMetrics fm = g.getFontMetrics();
        int autoCloseX = dialogX + (dialogWidth - fm.stringWidth(autoCloseText)) / 2;
        g.drawString(autoCloseText, autoCloseX, dialogY + dialogHeight - 20);
    }
    
    /**
     * 그만두기 확인 창 그리기
     */
    public void drawQuitConfirmOverlay(Graphics2D g, GameStateManager gameStateManager) {
        // 전체 화면 어둡게 처리
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, 800, 600);
        
        // 경고창 크기 계산
        int dialogWidth = 450;
        int dialogHeight = 200;
        int dialogX = (800 - dialogWidth) / 2;
        int dialogY = (600 - dialogHeight) / 2;
        
        // 경고창 배경 (상점 스타일 적용)
        g.setColor(new Color(220, 50, 50, 240));
        g.fillRect(dialogX, dialogY, dialogWidth, dialogHeight);
        
        // 경고창 테두리
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRect(dialogX, dialogY, dialogWidth, dialogHeight);
        g.setStroke(new BasicStroke(1)); // 기본 스트로크로 복원
        
        // 경고 아이콘 (삼각형 + 느낌표)
        int iconX = dialogX + 30;
        int iconY = dialogY + 30;
        int iconSize = 30;
        
        // 삼각형 경고 아이콘
        g.setColor(Color.YELLOW);
        int[] triangleX = {iconX + iconSize/2, iconX, iconX + iconSize};
        int[] triangleY = {iconY, iconY + iconSize, iconY + iconSize};
        g.fillPolygon(triangleX, triangleY, 3);
        
        // 느낌표
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("!", iconX + iconSize/2 - 4, iconY + iconSize - 5);
        
        // 메인 메시지
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String mainMessage = "정말 그만두시겠습니까?";
        int messageX = iconX + iconSize + 20;
        int messageY = dialogY + 50;
        g.drawString(mainMessage, messageX, messageY);
        
        // 서브 메시지
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        String subMessage = "게임을 그만두면 획득한 코인이 저장됩니다.";
        g.drawString(subMessage, messageX, messageY + 25);
        
        // 버튼들
        int buttonWidth = 100;
        int buttonHeight = 35;
        int buttonSpacing = 20;
        int totalButtonWidth = buttonWidth * 2 + buttonSpacing;
        int startButtonX = dialogX + (dialogWidth - totalButtonWidth) / 2;
        int buttonY = dialogY + dialogHeight - 60;
        
        // 아니요 버튼
        boolean noSelected = (gameStateManager.getSelectedQuitOption() == 0);
        drawConfirmButton(g, "아니요", startButtonX, buttonY, buttonWidth, buttonHeight, noSelected);
        
        // 예 버튼
        boolean yesSelected = (gameStateManager.getSelectedQuitOption() == 1);
        drawConfirmButton(g, "예", startButtonX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight, yesSelected);
    }
    
    /**
     * 확인 창 버튼 그리기
     */
    private void drawConfirmButton(Graphics2D g, String text, int x, int y, int width, int height, boolean selected) {
        // 버튼 배경
        g.setColor(selected ? new Color(70, 130, 180) : new Color(50, 50, 100));
        g.fillRoundRect(x, y, width, height, 10, 10);
        
        // 버튼 테두리
        g.setColor(selected ? new Color(100, 149, 237) : new Color(80, 80, 120));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x, y, width, height, 10, 10);
        
        // 버튼 텍스트
        g.setColor(Color.WHITE);
        g.setFont(getKostarFont(Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (width - fm.stringWidth(text)) / 2;
        int textY = y + (height + fm.getAscent()) / 2 - 2;
        g.drawString(text, textX, textY);
    }
}
