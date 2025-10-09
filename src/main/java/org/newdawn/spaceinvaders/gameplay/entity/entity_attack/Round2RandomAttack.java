package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import java.awt.Graphics;
import java.awt.Image;

/**
 * 2라운드 보스의 베기 공격 (2round2.gif 사용)
 * 위에서부터 플레이어까지 촥하고 베는 방식
 */
public class Round2RandomAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The speed at which the entity moves (very fast for slash effect) */
    private double moveSpeed = 800; // 매우 빠른 베기 속도
    /** Damage dealt by this attack */
    private int damage = 2;
    /** Attack image (2round2.gif) */
    private Image attackImage;
    /** Target Y position (player line) */
    private double targetY = 500;
    /** Slash duration in milliseconds */
    private long slashDuration = 1600; // 1.6초간 지속
    /** Start time for slash effect */
    private long startTime;
    
    /**
     * Create a new slash attack
     * 
     * @param game The game in which this entity exists
     * @param x The initial x location of this entity
     * @param y The initial y location of this entity
     */
    public Round2RandomAttack(Game game, int x, int y) {
        super("sprites/Boss_Attack/2round2.gif", x, y);
        this.game = game;
        
        // Fixed position from boss line to player line - NO MOVEMENT
        this.x = x; // 랜덤 X 위치
        this.y = 150; // 보스 바로 아래 라인에서 시작 (고정 위치)
        
        // NO MOVEMENT - this is a static slash from boss line to player line
        dy = 0; // 이동하지 않음 - 고정된 베기
        dx = 0;
        
        // Record start time for slash duration
        this.startTime = System.currentTimeMillis();
        
        // Load attack image
        loadAttackImage();
        
        System.out.println("⚔️ Round 2 STATIC Slash Attack created at X:" + x + " Y:" + 150 + " (Boss line to Player line - NO MOVEMENT)");
    }
    
    /**
     * Load the attack image
     */
    private void loadAttackImage() {
        try {
            // Use Toolkit to load animated GIF properly (same as IceAttack)
            java.net.URL imageURL = getClass().getClassLoader().getResource("sprites/Boss_Attack/2round2.gif");
            if (imageURL != null) {
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageURL);
                System.out.println("⚔️ Successfully loaded animated slash attack GIF: 2round2.gif");
            } else {
                System.err.println("⚔️ Could not find 2round2.gif file");
            }
            
            if (attackImage != null) {
                System.out.println("⚔️ Slash attack image dimensions: " + attackImage.getWidth(null) + "x" + attackImage.getHeight(null));
                System.out.println("⚔️ Slash attack image class: " + attackImage.getClass().getSimpleName());
            }
        } catch (Exception e) {
            System.err.println("⚔️ Failed to load slash attack GIF image: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Request that this entity move itself based on a certain amount
     * of time having passed.
     * 
     * @param delta The amount of time that has passed in milliseconds
     */
    public void move(long delta) {
        // NO MOVEMENT - this is a static slash attack
        // Do NOT call super.move(delta) - we want to stay in fixed position
        
        // Slash duration check - remove after 0.5 seconds
        if (System.currentTimeMillis() - startTime > slashDuration) {
            game.removeEntity(this);
            return;
        }
        
        // Static slash - no movement, no screen boundary checks
    }
    
    /**
     * Draw this entity to the graphics context provided.
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        if (attackImage != null) {
            // 보스 라인(Y:150)부터 플레이어 라인(Y:550)까지 완전히 덮는 거대한 베기
            int drawX = (int)x - 150; // 중심에서 좌우로 150픽셀씩 (히트범위 더 축소)
            int drawY = 150; // 보스 바로 아래 라인에서 시작
            int drawWidth = 300; // 가로 300픽셀 (히트범위 더 축소)
            int drawHeight = 400; // 세로 400픽셀 (보스 라인부터 플레이어 라인까지)
            
            g.drawImage(attackImage, drawX, drawY, drawWidth, drawHeight, null);
            
            // Debug: Log slash attack position (less frequent to avoid spam)
            if (System.currentTimeMillis() % 500 < 50) { // 0.5초마다 한 번만 출력
                System.out.println("⚔️ Drawing MASSIVE STATIC slash from Boss line to Player line at X:" + (int)x + " Y:150 Size: " + drawWidth + "x" + drawHeight);
            }
        } else {
            // Fallback: draw a massive static slash rectangle if image fails to load
            g.setColor(java.awt.Color.YELLOW);
            g.fillRect((int)x - 350, 150, 700, 400);
        }
    }
    
    /**
     * Notification that this entity has collided with another.
     * 
     * @param other The other entity
     */
    public void collidedWith(Entity other) {
        // if we've hit the player's ship, damage it
        if (other instanceof org.newdawn.spaceinvaders.gameplay.entity.ShipEntity) {
            // Check if player is invincible
            if (game.isPlayerInvincible()) {
                System.out.println("🛡️ Player is invincible, random slash attack blocked");
                return;
            }
            
            // Check if player is within the static slash area
            // Static slash covers from Y:150 to Y:550, X: (x-150) to (x+150) - 히트범위 더 축소
            double playerX = other.getX();
            double playerY = other.getY();
            
            // Check if player is within the static slash rectangle
            boolean inSlashArea = (playerX >= (this.x - 150)) && (playerX <= (this.x + 150)) && 
                                 (playerY >= 150) && (playerY <= 550);
            
            // Always log collision check for debugging
            System.out.println("⚔️ STATIC slash collision check - Player X:" + String.format("%.2f", playerX) + 
                             " Y:" + String.format("%.2f", playerY) + " In slash area: " + inSlashArea);
            System.out.println("⚔️ Slash area bounds - X: " + (this.x - 150) + " to " + (this.x + 150) + ", Y: 150 to 550");
            
            if (inSlashArea) {
                System.out.println("⚔️ MASSIVE STATIC MASSIVE slash attack hit player ship! Damage: " + damage);
                game.notifyPlayerDamaged(damage);
                // Don't remove entity immediately - let it persist for the full duration
            }
        }
    }
}
