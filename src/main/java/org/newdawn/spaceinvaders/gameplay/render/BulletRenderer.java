package org.newdawn.spaceinvaders.gameplay.render;

import java.awt.*;

/**
 * 싱글/멀티 공통 총알 시각 효과 렌더러.
 * - 에일리언 에너지탄(원형 글로우)
 * - 스킬 드롭 아이콘(무적/관통/삼중)
 */
public final class BulletRenderer {
    private BulletRenderer() {}

    /**
     * 에일리언 탄환을 중심 좌표 기준으로 렌더
     */
    public static void drawAlienBullet(Graphics2D g2d, int centerX, int centerY) {
        int radius = 8; // 고정 반경

        // Outer glow
        g2d.setColor(new Color(255, 100, 100, 60));
        g2d.fillOval(centerX - radius - 3, centerY - radius - 3, (radius + 3) * 2, (radius + 3) * 2);

        // Main body
        g2d.setColor(new Color(255, 50, 50, 220));
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // Inner bright core
        g2d.setColor(new Color(255, 200, 200, 180));
        g2d.fillOval(centerX - radius + 2, centerY - radius + 2, (radius - 2) * 2, (radius - 2) * 2);

        // White hot center
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(centerX - radius + 4, centerY - radius + 4, (radius - 4) * 2, (radius - 4) * 2);

        // Trail effect
        g2d.setColor(new Color(255, 100, 100, 80));
        g2d.fillOval(centerX - radius + 1, centerY - radius + 6, (radius - 1) * 2, (radius - 1) * 2);
        g2d.setColor(new Color(255, 100, 100, 40));
        g2d.fillOval(centerX - radius + 2, centerY - radius + 10, (radius - 2) * 2, (radius - 2) * 2);
    }

    /**
     * 스킬 드롭(중심 좌표 기준)
     * @param skillType 0: 무적, 1: 관통, 2: 삼중샷
     */
    public static void drawSkillDrop(Graphics2D g2d, int centerX, int centerY, int skillType) {
        Stroke old = g2d.getStroke();
        final BasicStroke STROKE_2PX = new BasicStroke(2);

        if (skillType == 0) { // Invincible
            g2d.setColor(new Color(255, 215, 0, 200));
            g2d.fillOval(centerX - 8, centerY - 8, 16, 16);
            g2d.setColor(new Color(255, 255, 0, 150));
            g2d.fillOval(centerX - 6, centerY - 6, 12, 12);
            g2d.setColor(new Color(255, 140, 0));
            g2d.setStroke(STROKE_2PX);
            g2d.drawOval(centerX - 8, centerY - 8, 16, 16);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(STROKE_2PX);
            g2d.drawLine(centerX - 3, centerY, centerX + 3, centerY);
            g2d.drawLine(centerX, centerY - 3, centerX, centerY + 3);
        } else if (skillType == 1) { // Piercing
            g2d.setColor(new Color(255, 0, 0, 200));
            g2d.fillOval(centerX - 8, centerY - 8, 16, 16);
            g2d.setColor(new Color(255, 100, 100, 150));
            g2d.fillOval(centerX - 6, centerY - 6, 12, 12);
            g2d.setColor(new Color(180, 0, 0));
            g2d.setStroke(STROKE_2PX);
            g2d.drawOval(centerX - 8, centerY - 8, 16, 16);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(STROKE_2PX);
            g2d.drawLine(centerX, centerY - 4, centerX, centerY + 2);
            g2d.drawLine(centerX - 2, centerY, centerX, centerY + 2);
            g2d.drawLine(centerX + 2, centerY, centerX, centerY + 2);
        } else { // Triple shot
            g2d.setColor(new Color(0, 100, 255, 200));
            g2d.fillOval(centerX - 8, centerY - 8, 16, 16);
            g2d.setColor(new Color(100, 150, 255, 150));
            g2d.fillOval(centerX - 6, centerY - 6, 12, 12);
            g2d.setColor(new Color(0, 50, 150));
            g2d.setStroke(STROKE_2PX);
            g2d.drawOval(centerX - 8, centerY - 8, 16, 16);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(STROKE_2PX);
            g2d.fillOval(centerX - 4, centerY - 2, 3, 3);
            g2d.fillOval(centerX - 1, centerY - 2, 3, 3);
            g2d.fillOval(centerX + 2, centerY - 2, 3, 3);
        }

        // glow
        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.fillOval(centerX - 10, centerY - 10, 20, 20);
        g2d.setStroke(old);
    }
}
