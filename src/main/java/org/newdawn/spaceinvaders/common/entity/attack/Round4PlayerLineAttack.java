package org.newdawn.spaceinvaders.common.entity.attack;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 4 플레이어 라인 공격.
 */
public class Round4PlayerLineAttack extends BaseAttackEntity {
    private final long attackDuration = 4000;
    private final long startTime;
    private final int attackWidth = 160;
    private final int attackHeight = 160;
    private Image attackImage;

    public Round4PlayerLineAttack(GameContext game, int x, int y) {
        super(game, SpriteConstants.BOSS_ATTACK_4ROUND3_GIF, x, y);
        this.startTime = System.currentTimeMillis();
        loadAttackImage();
    }

    private void loadAttackImage() {
        attackImage = loadImageWithMediaTracker(SpriteConstants.BOSS_ATTACK_4ROUND3_GIF);
    }

    @Override
    public void move(long delta) {
        checkAndRemoveIfExpired(startTime, attackDuration);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (attackImage != null) {
            int drawX = (int) x - attackWidth / 2;
            int drawY = (int) y - attackHeight / 2;
            g2d.drawImage(attackImage, drawX, drawY, drawX + attackWidth, drawY + attackHeight,
                    0, 0, attackImage.getWidth(null), attackImage.getHeight(null), null);
        } else {
            g2d.setColor(Color.RED);
            g2d.fillRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
            g2d.setColor(Color.YELLOW);
            g2d.drawRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        handleShipCollision(other, 5);
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(attackWidth, attackHeight);
    }
}
