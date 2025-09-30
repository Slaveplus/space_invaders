
package org.newdawn.spaceinvaders.gameplay;

import java.awt.*;

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

        // Instructions
        g.setColor(Color.CYAN);
        g.setFont(FONT_TEXT_12_P);
        g.drawString("Q: 스킬 메뉴", 20, 280);
    }

    /**
     * HP 바 그리기
     */
    private void drawHPBar(Graphics2D g, GameStateManager gameStateManager) {
        int barWidth = 200;
        int barHeight = 20;
        int barX = 20;
        int barY = 60;

        // Background
        g.setColor(new Color(50, 50, 50));
        g.fillRect(barX, barY, barWidth, barHeight);

        // HP fill
        int hpWidth = (int) ((double) gameStateManager.getCurrentHP() / gameStateManager.getMaxHP() * barWidth);
        g.setColor(new Color(255, 0, 0));
        g.fillRect(barX, barY, hpWidth, barHeight);

        // Border
        g.setColor(Color.WHITE);
        g.setStroke(STROKE_2PX);
        g.drawRect(barX, barY, barWidth, barHeight);
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
     * 스킬 인벤토리 표시
     */
    private void drawSkillInventory(Graphics2D g, SkillManager skillManager) {
        int effectY = 220;

        g.setColor(Color.YELLOW);
        g.setFont(FONT_TEXT_14_B);
        g.drawString("스킬:", 20, effectY);

        g.setColor(Color.WHITE);
        g.setFont(FONT_TEXT_12_P);
        g.drawString("1: 무적 (" + skillManager.getInvincibleSkills() + "개)", 20, effectY + 15);
        g.drawString("2: 관통 (" + skillManager.getPiercingSkills() + "개)", 20, effectY + 30);
        g.drawString("3: 3줄공격 (" + skillManager.getTripleShotSkills() + "개)", 20, effectY + 45);
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
