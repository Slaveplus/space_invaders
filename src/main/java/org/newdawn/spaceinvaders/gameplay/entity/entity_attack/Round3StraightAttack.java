package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;

/**
 * Round 3 Boss Straight Attack - 3round4.gif를 사용한 일직선 대형 공격
 * 매우 크게 내려오는 강력한 공격
 */
public class Round3StraightAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The attack image */
    private java.awt.Image attackImage;
    /** Attack damage */
    private int damage = 3;
    /** Movement speed */
    private double moveSpeed = 120;
    /** Duration of the attack */
    private long attackDuration = 5000; // 5초
    /** Start time */
    private long startTime;
    /** Attack size */
    private int attackWidth = 600;
    private int attackHeight = 300;

    /**
     * Create a new straight attack entity
     * 
     * @param game The game in which this entity exists
     * @param x The initial x location of this entity
     * @param y The initial y location of this entity
     */
    public Round3StraightAttack(Game game, int x, int y) {
        super("sprites/Boss_Attack/3round4.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        
        // Move straight down
        this.dx = 0;
        this.dy = moveSpeed;
        
        loadAttackImage();
    }

    /**
     * Load the attack image
     */
    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/3round4.gif");
            if (imageUrl != null) {
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                System.out.println("🔴 Successfully loaded straight attack GIF: 3round4.gif via ClassLoader.getResource()");
            } else {
                System.err.println("🔴 Failed to find 3round4.gif resource.");
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage("src/main/resources/sprites/Boss_Attack/3round4.gif");
                System.out.println("🔴 Attempted direct loading of 3round4.gif: " + (attackImage != null));
            }
            // Ensure image is loaded before proceeding
            if (attackImage != null) {
                java.awt.MediaTracker mt = new java.awt.MediaTracker(new java.awt.Canvas());
                mt.addImage(attackImage, 0);
                mt.waitForAll();
                if (mt.isErrorAny()) {
                    System.err.println("🔴 Error loading 3round4.gif with MediaTracker.");
                    attackImage = null;
                } else {
                    System.out.println("🔴 Straight attack image dimensions (after MediaTracker): " + attackImage.getWidth(null) + "x" + attackImage.getHeight(null));
                }
            }
        } catch (Exception e) {
            System.err.println("🔴 Failed to load straight attack GIF image: " + e.getMessage());
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
        super.move(delta);
        
        // Check if attack duration has passed
        if (System.currentTimeMillis() - startTime > attackDuration) {
            game.removeEntity(this);
            return;
        }
        
        // Remove if out of screen bounds
        if (x < -attackWidth || x > game.getWidth() + attackWidth || y < -attackHeight || y > game.getHeight() + attackHeight) {
             game.removeEntity(this);
        }
    }

    /**
     * Draw this entity to the graphics context provided.
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        if (attackImage != null) {
            // Draw the attack image very large
            int drawX = (int)x - attackWidth / 2;
            int drawY = (int)y - attackHeight / 2;
            g.drawImage(attackImage, drawX, drawY, attackWidth, attackHeight, null);
            
            // Debug: Log straight attack position
            if (System.currentTimeMillis() % 1000 < 50) {
                System.out.println("🔴 Drawing MASSIVE straight attack at X:" + (int)x + " Y:" + (int)y + " Size:" + attackWidth + "x" + attackHeight);
            }
        } else {
            // Fallback: draw a large colored rectangle
            g.setColor(java.awt.Color.RED);
            g.fillRect((int)x - attackWidth/2, (int)y - attackHeight/2, attackWidth, attackHeight);
            g.setColor(java.awt.Color.DARK_GRAY);
            g.drawRect((int)x - attackWidth/2, (int)y - attackHeight/2, attackWidth, attackHeight);
        }
    }

    /**
     * Notification that this entity has collided with another.
     * 
     * @param other The other entity
     */
    public void collidedWith(Entity other) {
        // if we've hit the player's ship, damage it
        if (other instanceof org.newdawn.spaceinvaders.common.entity.ShipEntity) {
            // Check if player is invincible
            if (game.isPlayerInvincible()) {
                System.out.println("🛡️ Player is invincible, straight attack blocked");
                game.removeEntity(this);
                return;
            }
            
            // Check distance for more accurate collision detection
            double dx = this.x - other.getX();
            double dy = this.y - other.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            // Use collision radius that matches visual size (very large attack)
            double collisionRadius = Math.min(attackWidth, attackHeight) / 2.0; // 100 (half of smaller dimension)
            
            if (distance <= collisionRadius) {
                System.out.println("🔴 MASSIVE straight attack hit player ship! Distance: " + String.format("%.2f", distance) + 
                                 " Collision radius: " + collisionRadius + " Damage: " + damage);
                game.notifyPlayerDamaged(damage);
                game.removeEntity(this);
            } else {
                // Debug: log when collision check happens but no hit
                System.out.println("🔴 MASSIVE straight attack collision check - Distance: " + String.format("%.2f", distance) + 
                                 " > Collision radius: " + collisionRadius + " (No hit)");
            }
        }
    }
}
