package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.BossEntity;
import org.newdawn.spaceinvaders.common.entity.Entity;

/**
 * 멀티플레이용 4라운드 회복 공격.
 */
public class Round4HealAttack extends Entity {
    private final MultiplayerGameContext game;
    private final long healDuration = 3000;
    private final long startTime;
    private final int healWidth = 300;
    private final int healHeight = 300;
    private Image healImage;

    public Round4HealAttack(MultiplayerGameContext game, int x, int y) {
        super("sprites/Boss_Attack/4round.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        this.dx = 0;
        this.dy = 0;
        loadHealImage();
    }

    private void loadHealImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/4round.gif");
            if (imageUrl != null) {
                healImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                MediaTracker tracker = new MediaTracker(new java.awt.Canvas());
                tracker.addImage(healImage, 0);
                tracker.waitForAll();
                if (tracker.isErrorAny()) {
                    healImage = null;
                }
            }
        } catch (Exception e) {
            healImage = null;
        }
    }

    @Override
    public void move(long delta) {
        if (System.currentTimeMillis() - startTime > healDuration) {
            healBoss();
            game.removeEntity(this);
        }
    }

    private void healBoss() {
        for (Entity entity : game.getEntities()) {
            if (entity instanceof BossEntity) {
                BossEntity boss = (BossEntity) entity;
                boss.healToFull();
                break;
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
}
