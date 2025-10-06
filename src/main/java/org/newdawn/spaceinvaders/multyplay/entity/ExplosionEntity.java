package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.newdawn.spaceinvaders.multyplay.core.MultiGameRuntime;

public class ExplosionEntity extends Entity {
    private MultiGameRuntime game;
    private long explosionDuration = 1000;
    private long startTime;
    private double maxRadius;
    private double currentRadius;
    private long lastDamageTime = 0;
    private long damageInterval = 100;
    private Set<Entity> damaged = new HashSet<>();
    private BufferedImage explosionImage;

    public ExplosionEntity(MultiGameRuntime game, String sprite, int x, int y, double maxRadius) {
        super(sprite, x, y);
        this.game = game;
        this.maxRadius = maxRadius;
        this.currentRadius = 0;
        this.startTime = System.currentTimeMillis();
        dx = 0; dy = 0;
        loadImage();
    }

    public void move(long delta) {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        double progress = (double) elapsed / explosionDuration;
        if (progress >= 1.0) { game.removeEntity(this); return; }
        currentRadius = maxRadius * progress;
        if (currentRadius > maxRadius * 0.2 && now - lastDamageTime >= damageInterval) {
            damageEnemies();
            lastDamageTime = now;
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (explosionImage != null) {
            int size = (int) (currentRadius * 2);
            int dxDraw = (int) x - size / 2;
            int dyDraw = (int) y - size / 2;
            g2d.drawImage(explosionImage, dxDraw, dyDraw, size, size, null);
        }
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawOval((int) (x - currentRadius), (int) (y - currentRadius), (int) (currentRadius * 2), (int) (currentRadius * 2));
        g2d.setColor(Color.ORANGE);
        g2d.setStroke(new BasicStroke(2f));
        double inner = currentRadius * 0.7;
        g2d.drawOval((int) (x - inner), (int) (y - inner), (int) (inner * 2), (int) (inner * 2));
    }

    private void loadImage() {
        try {
            URL url = getClass().getClassLoader().getResource("sprites/Skill/Explosion.png");
            if (url != null) explosionImage = ImageIO.read(url);
        } catch (Exception e) { explosionImage = null; }
    }

    private void damageEnemies() {
        List<Entity> list = game.getEntities();
        List<AlienEntity> aliens = new ArrayList<>();
        for (Entity e : list) {
            if (e instanceof AlienEntity && !damaged.contains(e)) {
                double dxVal = e.getX() - x;
                double dyVal = e.getY() - y;
                double dist = Math.sqrt(dxVal * dxVal + dyVal * dyVal);
                if (dist <= currentRadius) aliens.add((AlienEntity) e);
            }
        }
        for (AlienEntity a : aliens) {
            a.takeDamage(game.getPlayerAttackPower() * 3);
            damaged.add(a);
        }
    }

    public boolean isFinished() { long now = System.currentTimeMillis(); return (now - startTime) >= explosionDuration; }
    public void collidedWith(Entity other) { }
}
