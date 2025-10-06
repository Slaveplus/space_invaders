package org.newdawn.spaceinvaders.multyplay.ui;

import java.awt.*;
import org.newdawn.spaceinvaders.multyplay.core.MultiGameController;

/**
 * 멀티플레이 UI 렌더러 (싱글 UIRenderer 포크)
 * - 현재 MVP: 단일 플레이어 HUD
 * - 향후: 다중 플레이어 (슬롯 0~3) HP/스킬/이펙트 분할 표시
 */
public class MultiUIRenderer {
    private static final Font FONT_TITLE_18_B = new Font("Arial", Font.BOLD, 18);
    private static final Font FONT_TEXT_16_B = new Font("Arial", Font.BOLD, 16);

    private final MultiGameController controller;

    public MultiUIRenderer(MultiGameController controller){ this.controller = controller; }

    public void drawHUD(Graphics2D g){
        g.setColor(Color.CYAN);
        g.setFont(FONT_TITLE_18_B);
        g.drawString("Round: "+ controller.getCurrentRound(), 20, 25);

        g.setColor(Color.WHITE); g.setFont(FONT_TEXT_16_B);
        g.drawString("Entities: "+ controller.getEntities().size(), 20, 50);

        String msg = controller.getMessage();
        if(msg != null && !msg.isEmpty()){
            g.setColor(Color.WHITE);
            g.setFont(FONT_TEXT_16_B);
            FontMetrics fm = g.getFontMetrics();
            int x = (800 - fm.stringWidth(msg))/2;
            g.drawString(msg, x, 300);
        }

        // 향후: 각 플레이어 슬롯별 HP/Skill Points/Effects
    }
}
