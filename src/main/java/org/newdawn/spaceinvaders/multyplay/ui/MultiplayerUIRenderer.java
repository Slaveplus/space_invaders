package org.newdawn.spaceinvaders.multyplay.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import org.newdawn.spaceinvaders.gameplay.UIRenderer;
import org.newdawn.spaceinvaders.gameplay.ui.HudContext;
import org.newdawn.spaceinvaders.gameplay.ui.SharedHudRenderer;
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
    private final SharedHudRenderer sharedHudRenderer = new SharedHudRenderer();

    public MultiplayerUIRenderer(MultiplayerGameCanvas game) {
        // MultiplayerGameCanvas reference not currently used, but kept for future extensibility
    }

    /**
     * 게임 UI 그리기 (HP, 스킬 포인트, 스탯 등)
     */
    public void drawGameUI(Graphics2D g, MultiplayerGameStateManager gameStateManager, MultiplayerSkillManager skillManager) {
        sharedHudRenderer.drawHud(g, new MultiplayerHudContext(gameStateManager, skillManager));
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

    private static final class MultiplayerHudContext implements HudContext {
        private final MultiplayerGameStateManager gameStateManager;
        private final MultiplayerSkillManager skillManager;

        private MultiplayerHudContext(MultiplayerGameStateManager gsm, MultiplayerSkillManager sm) {
            this.gameStateManager = gsm;
            this.skillManager = sm;
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
