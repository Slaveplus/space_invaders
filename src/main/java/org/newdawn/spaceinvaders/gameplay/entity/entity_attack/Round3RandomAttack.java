package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Round 3 Boss Random Attack - 3round2.gif를 사용한 랜덤 위치 공격
 * 화면 상단에서 랜덤한 위치로 발사되는 공격
 */
public class Round3RandomAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The attack image */
    private java.awt.Image attackImage;
    /** Damage dealt to player */
    private int damage = 2;
    /** Movement speed */
    private int moveSpeed = 300; // 빠른 레이저 속도
    /** Duration of the attack */
    private long attackDuration = 3000; // 3초
    /** Start time */
    private long startTime;
    /** Attack size - 매우 큰 레이저 */
    private int attackWidth = 400; // 100 → 400 (4배)
    private int attackHeight = 1400; // 1200 → 1400 (더 긴 레이저)
    /** Delay before damage is applied */
    private long damageDelay = 500; // 0.5초 지연
    /** Track if damage has been applied */
    private boolean damageApplied = false;

    /**
     * Create a new random laser attack entity
     * 
     * @param game The game in which this entity exists
     * @param x The initial x location of this entity
     * @param y The initial y location of this entity
     */
    public Round3RandomAttack(Game game, int x, int y) {
        super("sprites/Boss_Attack/3round2.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        
        // 레이저는 정지 상태로 시작 (연속 빔)
        this.dx = 0;
        this.dy = 0;
        
        // Load the attack image
        loadAttackImage();
    }

    /**
     * Load the attack image
     */
    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/3round2.gif");
            if (imageUrl != null) {
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                System.out.println("🔴 Successfully loaded random attack GIF: 3round2.gif via ClassLoader.getResource()");
            } else {
                System.err.println("🔴 Failed to find 3round2.gif resource.");
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage("src/main/resources/sprites/Boss_Attack/3round2.gif");
                System.out.println("🔴 Attempted direct loading of 3round2.gif: " + (attackImage != null));
            }
            // Ensure image is loaded before proceeding
            if (attackImage != null) {
                java.awt.MediaTracker mt = new java.awt.MediaTracker(new java.awt.Canvas());
                mt.addImage(attackImage, 0);
                mt.waitForAll();
                if (mt.isErrorAny()) {
                    System.err.println("🔴 Error loading 3round2.gif with MediaTracker.");
                    attackImage = null;
                } else {
                    System.out.println("🔴 Random attack image dimensions (after MediaTracker): " + attackImage.getWidth(null) + "x" + attackImage.getHeight(null));
                }
            }
        } catch (Exception e) {
            System.err.println("🔴 Failed to load random attack GIF image: " + e.getMessage());
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
        // 레이저는 움직이지 않음 (정지된 빔)
        
        // Check if attack duration has expired
        if (System.currentTimeMillis() - startTime > attackDuration) {
            System.out.println("🔴 Random laser attack expired after " + attackDuration + "ms");
            game.removeEntity(this);
            return;
        }
    }

    /**
     * Draw this entity to the graphics context provided
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            
            if (attackImage != null) {
                int drawX = (int)x - attackWidth / 2;
                int drawY = (int)y - attackHeight / 2;
                
                g2d.drawImage(attackImage, drawX, drawY, drawX + attackWidth, drawY + attackHeight, 
                             0, 0, attackImage.getWidth(null), attackImage.getHeight(null), null);
                
                System.out.println("🔴 Drawing MASSIVE random laser at X:" + drawX + " Y:" + drawY + " Size:" + attackWidth + "x" + attackHeight);
            } else {
                // Fallback rendering if image failed to load
                g2d.setColor(Color.RED);
                g2d.fillRect((int)x - attackWidth / 2, (int)y - attackHeight / 2, attackWidth, attackHeight);
                g2d.setColor(Color.YELLOW);
                g2d.drawRect((int)x - attackWidth / 2, (int)y - attackHeight / 2, attackWidth, attackHeight);
                System.out.println("🔴 Drawing MASSIVE random laser fallback at X:" + (int)x + " Y:" + (int)y + " Size:" + attackWidth + "x" + attackHeight);
            }
        } catch (Exception e) {
            System.err.println("Error drawing random laser: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Notification that this entity collided with another
     * 
     * @param other The entity with which we collided
     */
    public void collidedWith(Entity other) {
        // if we've hit the player's ship, check for delayed damage
        if (other instanceof org.newdawn.spaceinvaders.common.entity.ShipEntity) {
            // Check if player is invincible
            if (game.isPlayerInvincible()) {
                System.out.println("🛡️ Player is invincible, random laser attack blocked");
                return;
            }
            
            // Rectangle-based collision detection for laser
            double playerX = other.getX();
            double playerY = other.getY();
            
            // Check if player is within the massive laser area
            boolean inLaserArea = (playerX >= (this.x - attackWidth / 2)) && (playerX <= (this.x + attackWidth / 2)) && 
                                 (playerY >= (this.y - attackHeight / 2)) && (playerY <= (this.y + attackHeight / 2));
            
            if (inLaserArea) {
                // Check if enough time has passed for damage delay
                long elapsedTime = System.currentTimeMillis() - startTime;
                
                if (!damageApplied && elapsedTime >= damageDelay) {
                    System.out.println("🔴 MASSIVE random laser DELAYED damage applied! Player X:" + String.format("%.2f", playerX) + 
                                     " Y:" + String.format("%.2f", playerY) + " Damage: " + damage + " (after " + damageDelay + "ms delay)");
                    game.notifyPlayerDamaged(damage);
                    damageApplied = true; // 한 번만 데미지 적용
                } else if (!damageApplied) {
                    // Show warning that damage is coming
                    System.out.println("🔴 MASSIVE random laser WARNING! Player in danger zone! Damage coming in " + 
                                     (damageDelay - elapsedTime) + "ms");
                }
                // 레이저는 지속되므로 제거하지 않음
            }
        }
    }
}
