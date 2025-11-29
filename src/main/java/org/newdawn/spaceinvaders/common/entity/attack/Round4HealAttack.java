package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 라운드 4 보스 회복 공격.
 */
public class Round4HealAttack extends BaseAttackEntity {
    private final long healDuration = 3000;
    private final long startTime;
    private final int healWidth = 300;
    private final int healHeight = 300;
    private Image healImage;

    public Round4HealAttack(GameContext game, int x, int y) {
        super(game, "sprites/Boss_Attack/4round.gif", x, y);
        this.startTime = System.currentTimeMillis();
        loadHealImage();
    }

    private void loadHealImage() {
        healImage = loadImageWithMediaTracker("sprites/Boss_Attack/4round.gif");
    }

    @Override
    public void move(long delta) {
        if (checkAndRemoveIfExpired(startTime, healDuration)) {
            healBossToFull();
        }
    }

    private void healBossToFull() {
        for (Entity entity : game.getEntities()) {
            if (!"BossEntity".equals(entity.getClass().getSimpleName())) {
                continue;
            }
            try {
                entity.getClass().getMethod("healToFull").invoke(entity);
                return;
            } catch (Exception ignore) {
                try {
                    java.lang.reflect.Field hpField = entity.getClass().getDeclaredField("currentHP");
                    java.lang.reflect.Field maxHpField = entity.getClass().getDeclaredField("maxHP");
                    hpField.setAccessible(true);
                    maxHpField.setAccessible(true);
                    hpField.set(entity, maxHpField.get(entity));
                    return;
                } catch (Exception ignored) {
                    // fall back to no-op
                }
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (healImage != null) {
            int drawX = (int) x - healWidth / 2;
            int drawY = (int) y - healHeight / 2;
            g2d.drawImage(healImage, drawX, drawY, drawX + healWidth, drawY + healHeight,
                    0, 0, healImage.getWidth(null), healImage.getHeight(null), null);
        } else {
            g2d.setColor(Color.GREEN);
            g2d.fillRect((int) x - healWidth / 2, (int) y - healHeight / 2, healWidth, healHeight);
            g2d.setColor(Color.YELLOW);
            g2d.drawRect((int) x - healWidth / 2, (int) y - healHeight / 2, healWidth, healHeight);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        // no-op
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(healWidth, healHeight);
    }
}
