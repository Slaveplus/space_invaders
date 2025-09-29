
package org.newdawn.spaceinvaders.gameplay;

import org.newdawn.spaceinvaders.Game;

import java.awt.*;

/**
 * UI 렌더링을 담당하는 클래스
 * 게임 UI, 메시지 등을 그리는 역할
 */
public class UIRenderer {
    
    public UIRenderer(Game game) {
        // Game reference not currently used, but kept for future extensibility
    }
    
    /**
     * 게임 UI 그리기 (HP, 스킬 포인트, 스탯 등)
     */
    public void drawGameUI(Graphics2D g, GameStateManager gameStateManager, SkillManager skillManager) {
        // Round display
        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("라운드: " + gameStateManager.getCurrentRound() + "/" + gameStateManager.getMaxRound(), 20, 25);
        
        // HP display
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("HP: " + gameStateManager.getCurrentHP() + "/" + gameStateManager.getMaxHP(), 20, 50);
        
        // HP bar
        drawHPBar(g, gameStateManager);
        
        // Skill points display
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("스킬 포인트: " + gameStateManager.getSkillPoints(), 20, 100);
        
        // Stats display
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("공격력: " + gameStateManager.getAttackPower(), 20, 120);
        g.drawString("공격속도: " + String.format("%.1f", gameStateManager.getAttackSpeed()) + "x", 20, 135);
        
        // Skill effects display
        drawSkillEffects(g, skillManager);
        
        // Skill inventory display
        drawSkillInventory(g, skillManager);
        
        // Instructions moved to drawSkillInventory to avoid overlap
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
        g.setStroke(new BasicStroke(2));
        g.drawRect(barX, barY, barWidth, barHeight);
    }
    
    /**
     * 활성화된 스킬 효과 표시
     */
    private void drawSkillEffects(Graphics2D g, SkillManager skillManager) {
        int effectY = 160;
        
        if (skillManager.isInvincible()) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            long remainingTime = skillManager.getRemainingTime(skillManager.getInvincibleEndTime());
            if (remainingTime > 0) {
                g.drawString("무적: " + remainingTime + "초", 20, effectY);
                effectY += 20;
            }
        }
        
        if (skillManager.hasPiercing()) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            long remainingTime = skillManager.getRemainingTime(skillManager.getPiercingEndTime());
            if (remainingTime > 0) {
                g.drawString("관통: " + remainingTime + "초", 20, effectY);
                effectY += 20;
            }
        }
        
        if (skillManager.hasTripleShot()) {
            g.setColor(Color.BLUE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
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
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("스킬:", 20, effectY);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("1: 무적 (" + skillManager.getInvincibleSkills() + "개)", 20, effectY + 15);
        g.drawString("2: 관통 (" + skillManager.getPiercingSkills() + "개)", 20, effectY + 30);
        g.drawString("3: 3줄공격 (" + skillManager.getTripleShotSkills() + "개)", 20, effectY + 45);
        g.drawString("4: 미사일 (" + skillManager.getMissileSkills() + "개)", 20, effectY + 60);
        
        // Instructions (moved here to avoid overlap)
        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Q: 강화", 20, effectY + 85);
    }
    
    /**
     * 메시지 그리기 (게임 중간 메시지)
     */
    public void drawMessage(Graphics2D g, String message) {
        if (message != null && !message.isEmpty()) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            int messageX = (800 - fm.stringWidth(message)) / 2;
            g.drawString(message, messageX, 250);
            
            g.setFont(new Font("Arial", Font.PLAIN, 14));
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
}
