package org.newdawn.spaceinvaders.gameplay.util;

import java.awt.*;

/**
 * 공통 그래픽 유틸리티 함수 모음
 */
public final class GraphicsUtils {
    private GraphicsUtils() {}

    public static void drawCenteredString(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (GameViewport.WIDTH - fm.stringWidth(text)) / 2; // 고정 폭 기준
        g.drawString(text, x, y);
    }

    public static void fillTransparentRect(Graphics2D g, int x, int y, int w, int h, int alpha) {
        Color old = g.getColor();
        g.setColor(new Color(0, 0, 0, alpha));
        g.fillRect(x, y, w, h);
        g.setColor(old);
    }
}
