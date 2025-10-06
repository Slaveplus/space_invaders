package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import org.newdawn.spaceinvaders.multyplay.core.MultiGameRuntime; // 멀티 런타임 인터페이스
import org.newdawn.spaceinvaders.gameplay.sprite.Sprite;

public class AlienEntity extends Entity {
    private double moveSpeed = 75; private MultiGameRuntime game; private Sprite[] frames = new Sprite[4];
    private long lastFrameChange; private long frameDuration = 250; private int frameNumber; public long lastFire = 0; public long firingInterval = 2000;
    private int currentHP = 2; private int maxHP = 2; private boolean movingRight = Math.random() < 0.5; private boolean movingDown = Math.random() < 0.5; private long lastDirectionChange = 0; private long directionChangeInterval = 2000; private long lastCollisionTime = 0; private static final long COLLISION_COOLDOWN = 500;

    private static String getAlienSpriteForRound(int round) { return "sprites/Boss/1round_small.png"; }

    public AlienEntity(MultiGameRuntime game,int x,int y) {
        super(getAlienSpriteForRound(game.getCurrentRound()), x, y); this.game = game; int round = game.getCurrentRound(); frames[0] = sprite; frames[1] = sprite; frames[2] = sprite; frames[3] = sprite;
        switch (round) { case 1: maxHP = 1; break; case 2: maxHP = 4; break; case 3: maxHP = 10; break; case 4: maxHP = 18; break; case 5: maxHP = 30; break; default: maxHP = 30 + ((round - 5) * 15); }
        currentHP = maxHP; moveSpeed = 75 + (round * 10);
        if (round == 1) { firingInterval = 1000; } else if (round <= 3) { firingInterval = 2000 - (round * 500); } else if (round <= 6) { firingInterval = 700 - ((round - 3) * 100); } else { firingInterval = Math.max(200, 400 - ((round - 6) * 50)); }
        double randomFactor = 0.8 + (Math.random() * 0.4); firingInterval = (long)(firingInterval * randomFactor);
        double speedVariation = 0.8 + (Math.random() * 0.4); double actualMoveSpeed = moveSpeed * speedVariation; dx = movingRight ? actualMoveSpeed : -actualMoveSpeed;
        if (Math.random() < 0.3) { dy = (Math.random() - 0.5) * actualMoveSpeed * 0.3; } else { dy = 0; }
    }

    public void move(long delta) {
        lastFrameChange += delta; if (lastFrameChange > frameDuration) { lastFrameChange = 0; frameNumber++; if (frameNumber >= frames.length) { frameNumber = 0; } sprite = frames[frameNumber]; }
        long currentTime = System.currentTimeMillis(); if (currentTime - lastDirectionChange > directionChangeInterval) { if (Math.random() < 0.2) { changeDirection(); if (Math.random() < 0.1) { changeVerticalDirection(); } lastDirectionChange = currentTime; } }
        if (x < 10) { movingRight = true; dx = Math.abs(dx); x = 10; } else if (x > 750) { movingRight = false; dx = -Math.abs(dx); x = 750; }
        if (y < 50) { y = 50; } else if (y > 300) { y = 300; }
        super.move(delta);
    }
    public void doLogic() { }
    public void tryToFire() {
        if (System.currentTimeMillis() - lastFire < firingInterval) { return; }
        int round = game.getCurrentRound(); boolean shouldFire;
        if (round <= 2) { shouldFire = Math.random() < 0.3; } else if (round <= 4) { shouldFire = Math.random() < 0.5; } else if (round <= 6) { shouldFire = Math.random() < 0.7; } else { shouldFire = Math.random() < 0.85; }
        if (shouldFire) { lastFire = System.currentTimeMillis(); game.addAimedAlienShot((int)(x + 10), (int)(y + 30), (int)x); }
    }
    public void takeDamage(int damage) { currentHP -= damage; if (currentHP <= 0) { game.removeEntity(this); game.notifyAlienKilled(); } }
    public int getCurrentHP() { return currentHP; }
    @Override public java.awt.Rectangle getBounds() { double scale = (game.getCurrentRound() == 1) ? 0.45 : 0.75; int scaledWidth = (int)(sprite.getWidth() * scale); int scaledHeight = (int)(sprite.getHeight() * scale); int centerX = (int)x - scaledWidth/2; int centerY = (int)y - scaledHeight/2; return new java.awt.Rectangle(centerX, centerY, scaledWidth, scaledHeight); }
    public int getMaxHP() { return maxHP; }
    private void changeDirection() { movingRight = !movingRight; double speedVariation = 0.9 + (Math.random() * 0.2); dx = movingRight ? moveSpeed * speedVariation : -moveSpeed * speedVariation; }
    private void changeVerticalDirection() { movingDown = !movingDown; double verticalSpeedVariation = 0.8 + (Math.random() * 0.4); dy = movingDown ? moveSpeed * 0.3 * verticalSpeedVariation : -moveSpeed * 0.3 * verticalSpeedVariation; }
    private boolean isBossRound() { return game.getCurrentRound() % 5 == 0; }
    private void handleAlienCollision(Entity other) { long currentTime = System.currentTimeMillis(); if (currentTime - lastCollisionTime < COLLISION_COOLDOWN) { return; } lastCollisionTime = currentTime; double dxToOther = other.getX() - x; double dyToOther = other.getY() - y; double distanceToOther = Math.sqrt(dxToOther * dxToOther + dyToOther * dyToOther); if (distanceToOther > 0) { double moveAwayX = -dxToOther / distanceToOther; double moveAwayY = -dyToOther / distanceToOther; this.dx = moveAwayX * moveSpeed * 1.2; this.dy = moveAwayY * moveSpeed * 0.4; movingRight = (this.dx > 0); movingDown = (this.dy > 0); } }
    private void avoidPlayer() { try { int playerX = game.getShipX(); int playerY = game.getShipY(); double dxToPlayer = playerX - x; double dyToPlayer = playerY - y; double distanceToPlayer = Math.sqrt(dxToPlayer * dxToPlayer + dyToPlayer * dyToPlayer); double minDistance = 120; if (distanceToPlayer < minDistance && distanceToPlayer > 0) { double moveAwayX = -dxToPlayer / distanceToPlayer; double moveAwayY = -dyToPlayer / distanceToPlayer; this.dx = moveAwayX * moveSpeed * 1.5; this.dy = moveAwayY * moveSpeed * 0.5; } } catch (Exception e) { } }
    @Override public void draw(Graphics g) { Graphics2D g2d = (Graphics2D) g; if (sprite != null) { double scale = (game.getCurrentRound() == 1) ? 0.45 : 0.75; int scaledWidth = (int)(sprite.getWidth() * scale); int scaledHeight = (int)(sprite.getHeight() * scale); int drawX = (int)x - scaledWidth/2; int drawY = (int)y - scaledHeight/2; g2d.drawImage(sprite.getImage(), drawX, drawY, drawX + scaledWidth, drawY + scaledHeight, 0, 0, sprite.getWidth(), sprite.getHeight(), null); } }
    public void collidedWith(Entity other) { if (other instanceof BossEntity) { double dxToBoss = other.getX() - x; double dyToBoss = other.getY() - y; double distanceToBoss = Math.sqrt(dxToBoss * dxToBoss + dyToBoss * dyToBoss); if (distanceToBoss > 0) { double moveAwayX = -dxToBoss / distanceToBoss; double moveAwayY = -dyToBoss / distanceToBoss; this.dx = moveAwayX * moveSpeed * 1.5; this.dy = moveAwayY * moveSpeed * 0.3; } } else if (other instanceof AlienEntity && !isBossRound()) { handleAlienCollision(other); } }
}
