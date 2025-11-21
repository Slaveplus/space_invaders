package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.util.List;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 라운드 3 끌어당기기 공격.
 */
public class Round3PullAttack extends BaseAttackEntity {
    private final long attackDuration = 6000;
    private final long startTime;
    private final double pullStrength = 0.3;
    private final double pullRange = 300.0;
    private final int attackWidth = 300;
    private final int attackHeight = 400;
    private Image attackImage;

    public Round3PullAttack(GameContext game, int x, int y) {
        super(game, "sprites/Boss_Attack/3round3.gif", x, y);
        this.startTime = System.currentTimeMillis();
        loadAttackImage();
        launchBlackHoleAttack();
    }

    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/3round3.gif");
            if (imageUrl != null) {
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                MediaTracker tracker = new MediaTracker(new java.awt.Canvas());
                tracker.addImage(attackImage, 0);
                tracker.waitForAll();
                if (tracker.isErrorAny()) {
                    attackImage = null;
                }
            }
        } catch (Exception e) {
            attackImage = null;
        }
    }

    @Override
    public void move(long delta) {
        if (System.currentTimeMillis() - startTime > attackDuration) {
            game.removeEntity(this);
            return;
        }
        pullPlayers();
    }

    private void pullPlayers() {
        List<Entity> entities = game.getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ShipEntity) {
                ShipEntity ship = (ShipEntity) entity;
                double dx = this.x - ship.getX();
                double dy = this.y - ship.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance <= pullRange && distance > 0) {
                    double pullX = (dx / distance) * pullStrength;
                    double pullY = (dy / distance) * pullStrength;
                    ship.pullTowards(pullX, pullY);
                }
            }
        }
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
            g2d.setColor(Color.BLUE);
            g2d.fillRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
            g2d.setColor(Color.CYAN);
            g2d.drawRect((int) x - attackWidth / 2, (int) y - attackHeight / 2, attackWidth, attackHeight);
        }
    }

    private void launchBlackHoleAttack() {
        game.addEntity(new Round3BlackHoleAttack(game, (int) this.x, (int) this.y));
        game.addEntity(new Round3BlackHoleAttack(game, (int) this.x - 100, (int) this.y));
        game.addEntity(new Round3BlackHoleAttack(game, (int) this.x + 100, (int) this.y));
    }

    @Override
    public void collidedWith(Entity other) {
        // pull only
    }

    @Override
    public java.awt.Rectangle getBounds() {
        return centeredBounds(attackWidth, attackHeight);
    }
}
