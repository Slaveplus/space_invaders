package org.newdawn.spaceinvaders.gameplay.ui;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.newdawn.spaceinvaders.gameplay.UIRenderer;

/**
 * 싱글플레이어와 멀티플레이어 모드 모두에서 공유되는 공통 게임플레이 HUD를 렌더링합니다.
 */
public final class SharedHudRenderer {

    private BufferedImage cachedHpImage;
    private BufferedImage cachedCoinImage;

    public void drawHud(Graphics2D g, HudContext context) {
        if (context == null) {
            return;
        }
        drawRoundAndHp(g, context);
        drawSkillStats(g, context);
        drawSkillInventory(g, context);
        drawSkillEffects(g, context);
        drawPlayTimeAndCoins(g, context);
    }

    private void drawRoundAndHp(Graphics2D g, HudContext context) {
        g.setColor(Color.CYAN);
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 18));
        g.drawString("라운드: " + context.getCurrentRound() + "/" + context.getMaxRound(), 20, 25);

        g.setColor(Color.WHITE);
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 16));
        g.drawString("HP: " + context.getCurrentHp() + "/" + context.getMaxHp(), 20, 50);
        drawHpBar(g, context);
    }

    private void drawHpBar(Graphics2D g, HudContext context) {
        int barWidth = 200;
        int barHeight = 20;
        int barX = 20;
        int barY = 60;

        BufferedImage hp = getHpImage();
        if (hp != null) {
            double hpRatio = context.getMaxHp() > 0
                    ? (double) context.getCurrentHp() / context.getMaxHp()
                    : 0;
            int fillWidth = (int) Math.round(barWidth * Math.max(0.0, Math.min(1.0, hpRatio)));

            g.setColor(new Color(50, 50, 50));
            g.fillRect(barX, barY, barWidth, barHeight);

            if (fillWidth > 0) {
                g.drawImage(hp, barX, barY, barX + fillWidth, barY + barHeight,
                        0, 0, hp.getWidth(), hp.getHeight(), null);
            }
        } else {
            double hpRatio = context.getMaxHp() > 0
                    ? (double) context.getCurrentHp() / context.getMaxHp()
                    : 0;
            int fillWidth = (int) Math.round(barWidth * Math.max(0.0, Math.min(1.0, hpRatio)));
            g.setColor(new Color(50, 50, 50));
            g.fillRect(barX, barY, barWidth, barHeight);
            g.setColor(new Color(255, 0, 0));
            g.fillRect(barX, barY, fillWidth, barHeight);
        }

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawRect(barX, barY, barWidth, barHeight);
    }

    private void drawSkillStats(Graphics2D g, HudContext context) {
        g.setColor(Color.YELLOW);
        g.setFont(UIRenderer.getKostarFont(Font.BOLD, 16));
        g.drawString("스킬 포인트: " + context.getSkillPoints(), 20, 100);

        g.setColor(Color.WHITE);
        g.setFont(UIRenderer.getKostarFont(Font.PLAIN, 12));
        g.drawString("공격력: " + context.getAttackPower(), 20, 120);
        g.drawString("공격속도: " + String.format("%.1f", context.getAttackSpeed()) + "x", 20, 135);
    }

    private void drawSkillInventory(Graphics2D g, HudContext context) {
        int barWidth = 240;
        int barHeight = 35;
        int barX = 20;
        int barY = 145;

        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(barX - 6, barY - 6, barWidth + 12, barHeight + 12, 10, 10);
        g.setColor(new Color(255, 255, 255, 90));
        g.drawRoundRect(barX - 6, barY - 6, barWidth + 12, barHeight + 12, 10, 10);

        int itemSize = 18;
        int itemSpacing = 35;
        int startX = barX + 10;
        int centerY = barY + (barHeight - itemSize) / 2;

        int[] skillTypes = {0, 2, 3};
        String[] skillNames = {"무적", "3줄공격", "미사일"};
        int[] counts = {
                context.getInvincibleSkillCount(),
                context.getTripleShotSkillCount(),
                context.getMissileSkillCount()
        };

        for (int i = 0; i < skillTypes.length; i++) {
            int drawX = startX + (i * itemSpacing);
            drawSkillIconSlot(g, skillTypes[i], counts[i], skillNames[i], drawX, centerY, itemSize);
        }

        g.setColor(Color.CYAN);
        g.setFont(UIRenderer.getKostarFont(Font.PLAIN, 10));
        g.drawString("Q: 강화창", barX + 5, barY + barHeight + 16);
    }

    private void drawSkillEffects(Graphics2D g, HudContext context) {
        int effectY = 160;
        if (context.isInvincibleActive()) {
            g.setColor(Color.YELLOW);
            g.setFont(UIRenderer.getKostarFont(Font.BOLD, 14));
            long remaining = Math.max(0L, context.getInvincibleRemainingMs() / 1000);
            if (remaining > 0) {
                g.drawString("무적: " + remaining + "초", 20, effectY);
                effectY += 20;
            }
        }

        if (context.isTripleShotActive()) {
            g.setColor(Color.BLUE);
            g.setFont(UIRenderer.getKostarFont(Font.BOLD, 14));
            long remaining = Math.max(0L, context.getTripleShotRemainingMs() / 1000);
            if (remaining > 0) {
                g.drawString("3줄공격: " + remaining + "초", 20, effectY);
                effectY += 20;
            }
        }
    }

    private void drawPlayTimeAndCoins(Graphics2D g, HudContext context) {
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
        g.drawString("TIME: " + context.getFormattedPlayTime(), startX + 15, startY + 25);

        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("COINS: " + context.getEarnedCoins(), startX + 50, startY + 50);

        BufferedImage coin = getCoinImage();
        int coinX = startX + 15;
        int coinTop = startY + 28;
        int coinSize = 24;
        if (coin != null) {
            g.drawImage(coin, coinX, coinTop, coinSize, coinSize, null);
        } else {
            g.setColor(Color.YELLOW);
            g.fillOval(coinX, coinTop, coinSize, coinSize);
            g.setColor(Color.ORANGE);
            g.setStroke(new BasicStroke(2));
            g.drawOval(coinX, coinTop, coinSize, coinSize);
        }
    }

    private void drawSkillIconSlot(Graphics2D g, int skillType, int count, String label,
                                   int x, int y, int size) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(x + 2, y + 2, size, size);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x + 1, y + 1, size, size);

        BufferedImage icon = loadSkillImage(skillType);
        if (icon != null) {
            g.drawImage(icon, x, y, size, size, null);
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(2));
            g.drawRect(x, y, size, size);
        }

        drawSkillCount(g, count, x + size - 6, y + size - 6);

        g.setFont(new Font("Arial", Font.BOLD, 10));
        int labelWidth = g.getFontMetrics().stringWidth(label);
        int labelX = x + (size - labelWidth) / 2;
        int labelY = y + size + 12;
        g.setColor(new Color(0, 0, 0, 220));
        g.drawString(label, labelX + 1, labelY + 1);
        g.setColor(Color.WHITE);
        g.drawString(label, labelX, labelY);
    }

    private void drawSkillCount(Graphics2D g, int count, int centerX, int centerY) {
        int radius = 8;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        String text = Integer.toString(count);
        int width = g.getFontMetrics().stringWidth(text);
        int height = g.getFontMetrics().getHeight();
        g.drawString(text, centerX - width / 2, centerY + height / 4);
    }

    private BufferedImage getHpImage() {
        if (cachedHpImage != null) {
            return cachedHpImage;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SpriteConstants.SHIPS_HP_PNG)) {
            if (is != null) {
                cachedHpImage = ImageIO.read(is);
            }
        } catch (Exception ignored) {
        }
        return cachedHpImage;
    }

    private BufferedImage getCoinImage() {
        if (cachedCoinImage != null) {
            return cachedCoinImage;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SpriteConstants.STAR_COIN_NORMAL_PNG)) {
            if (is != null) {
                cachedCoinImage = ImageIO.read(is);
            }
        } catch (Exception ignored) {
        }
        return cachedCoinImage;
    }

    private BufferedImage loadSkillImage(int skillType) {
        String path;
        switch (skillType) {
            case 0:
                path = SpriteConstants.SKILL_1_PNG;
                break;
            case 2:
                path = SpriteConstants.SKILL_3_PNG;
                break;
            case 3:
                path = SpriteConstants.SKILL_4_PNG;
                break;
            default:
                path = SpriteConstants.SKILL_1_PNG;
                break;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
