package org.newdawn.spaceinvaders.gameplay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.io.InputStream;
import org.newdawn.spaceinvaders.common.util.FontConstants;

/**
 * UI 렌더링을 담당하는 클래스
 * 게임 UI, 메시지 등을 그리는 역할
 */
import org.newdawn.spaceinvaders.gameplay.ui.HudContext;
import org.newdawn.spaceinvaders.gameplay.ui.SharedHudRenderer;

public class UIRenderer {
    // Kostar 폰트 로드
    private static Font KOSTAR_FONT = null;
    
    private static final BasicStroke STROKE_2PX = new BasicStroke(2);
    
    static {
        loadKostarFont();
    }

    private final SharedHudRenderer sharedHudRenderer = new SharedHudRenderer();
    
    /**
     * Kostar 폰트 로드
     */
    private static void loadKostarFont() {
        try {
            InputStream fontStream = UIRenderer.class.getClassLoader().getResourceAsStream("fonts/Kostar.ttf");
            if (fontStream != null) {
                KOSTAR_FONT = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                fontStream.close();
                org.newdawn.spaceinvaders.common.util.Logger logger = 
                    org.newdawn.spaceinvaders.common.util.LoggerFactory.getLogger(UIRenderer.class);
                logger.info("Kostar 폰트 로드 성공");
            } else {
                org.newdawn.spaceinvaders.common.util.Logger logger = 
                    org.newdawn.spaceinvaders.common.util.LoggerFactory.getLogger(UIRenderer.class);
                logger.warn("Kostar 폰트 파일을 찾을 수 없습니다");
                KOSTAR_FONT = new Font(FontConstants.DEFAULT_FONT_NAME, Font.PLAIN, 12); // 폴백
            }
        } catch (Exception e) {
            org.newdawn.spaceinvaders.common.util.Logger logger = 
                org.newdawn.spaceinvaders.common.util.LoggerFactory.getLogger(UIRenderer.class);
            logger.error("Kostar 폰트 로드 실패: " + e.getMessage(), e);
            KOSTAR_FONT = new Font(FontConstants.DEFAULT_FONT_NAME, Font.PLAIN, 12); // 폴백
        }
    }
    
    /**
     * Kostar 폰트를 지정된 크기로 반환
     */
    public static Font getKostarFont(int size) {
        if (KOSTAR_FONT != null) {
            return KOSTAR_FONT.deriveFont(Font.PLAIN, size);
        }
        return new Font(FontConstants.DEFAULT_FONT_NAME, Font.PLAIN, size);
    }
    
    /**
     * Kostar 폰트를 지정된 크기와 스타일로 반환
     */
    public static Font getKostarFont(int style, int size) {
        if (KOSTAR_FONT != null) {
            return KOSTAR_FONT.deriveFont(style, size);
        }
        return new Font(FontConstants.DEFAULT_FONT_NAME, style, size);
    }
    
    public UIRenderer(Game game) {
        // Game reference not currently used, but kept for future extensibility
    }

    /**
     * 게임 UI 그리기 (HP, 스킬 포인트, 스탯 등)
     */
    public void drawGameUI(Graphics2D g, GameStateManager gameStateManager, SkillManager skillManager) {
        sharedHudRenderer.drawHud(g, new SinglePlayerHudContext(gameStateManager, skillManager));

        if (gameStateManager.isShowingSkillMenu()) {
            drawUpgradeStats(g, gameStateManager);
        }
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
        g.setFont(new Font(FontConstants.DEFAULT_FONT_NAME, Font.BOLD, 36));
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
        g.setFont(new Font(FontConstants.DEFAULT_FONT_NAME, Font.BOLD, 24));
        g.drawString("!", iconX + iconSize/2 - 6, iconY + iconSize - 8);
        
        // 메인 메시지
        g.setColor(Color.WHITE);
        g.setFont(new Font(FontConstants.DEFAULT_FONT_NAME, Font.BOLD, 20));
        String mainMessage = "곧 " + gameStateManager.getCurrentRound() + " 라운드가 시작됩니다";
        int messageX = iconX + iconSize + 20;
        int messageY = dialogY + 60;
        g.drawString(mainMessage, messageX, messageY);
        
        // 서브 메시지
        g.setColor(Color.WHITE);
        g.setFont(new Font(FontConstants.DEFAULT_FONT_NAME, Font.BOLD, 18));
        String subMessage = "대비하세요 !!";
        g.drawString(subMessage, messageX, messageY + 35);
        
        // 라운드 설명 텍스트
        g.setColor(Color.WHITE);
        g.setFont(new Font(FontConstants.DEFAULT_FONT_NAME, Font.PLAIN, 14));
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
        g.setFont(new Font(FontConstants.DEFAULT_FONT_NAME, Font.PLAIN, 12));
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
        g.setFont(new Font(FontConstants.DEFAULT_FONT_NAME, Font.BOLD, 20));
        g.drawString("!", iconX + iconSize/2 - 4, iconY + iconSize - 5);
        
        // 메인 메시지
        g.setColor(Color.WHITE);
        g.setFont(new Font(FontConstants.DEFAULT_FONT_NAME, Font.BOLD, 18));
        String mainMessage = "정말 그만두시겠습니까?";
        int messageX = iconX + iconSize + 20;
        int messageY = dialogY + 50;
        g.drawString(mainMessage, messageX, messageY);
        
        // 서브 메시지
        g.setColor(Color.WHITE);
        g.setFont(new Font(FontConstants.DEFAULT_FONT_NAME, Font.PLAIN, 14));
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

    private static final class SinglePlayerHudContext implements HudContext {
        private final GameStateManager gameStateManager;
        private final SkillManager skillManager;

        private SinglePlayerHudContext(GameStateManager gameStateManager, SkillManager skillManager) {
            this.gameStateManager = gameStateManager;
            this.skillManager = skillManager;
        }

        @Override
        public int getCurrentRound() {
            return gameStateManager.getCurrentRound();
        }

        @Override
        public int getMaxRound() {
            return gameStateManager.getMaxRound();
        }

        @Override
        public int getCurrentHp() {
            return gameStateManager.getCurrentHP();
        }

        @Override
        public int getMaxHp() {
            return gameStateManager.getMaxHP();
        }

        @Override
        public int getSkillPoints() {
            return gameStateManager.getSkillPoints();
        }

        @Override
        public int getAttackPower() {
            return gameStateManager.getAttackPower();
        }

        @Override
        public double getAttackSpeed() {
            return gameStateManager.getAttackSpeed();
        }

        @Override
        public int getInvincibleSkillCount() {
            return skillManager.getInvincibleSkills();
        }

        @Override
        public int getTripleShotSkillCount() {
            return skillManager.getTripleShotSkills();
        }

        @Override
        public int getMissileSkillCount() {
            return skillManager.getMissileSkills();
        }

        @Override
        public boolean isInvincibleActive() {
            return skillManager.isInvincible();
        }

        @Override
        public long getInvincibleRemainingMs() {
            return Math.max(0L, skillManager.getInvincibleEndTime() - System.currentTimeMillis());
        }

        @Override
        public boolean isTripleShotActive() {
            return skillManager.hasTripleShot();
        }

        @Override
        public long getTripleShotRemainingMs() {
            return Math.max(0L, skillManager.getTripleShotEndTime() - System.currentTimeMillis());
        }

        @Override
        public String getFormattedPlayTime() {
            return gameStateManager.getPlayTime();
        }

        @Override
        public int getEarnedCoins() {
            return gameStateManager.getEarnedCoins();
        }
    }
}
