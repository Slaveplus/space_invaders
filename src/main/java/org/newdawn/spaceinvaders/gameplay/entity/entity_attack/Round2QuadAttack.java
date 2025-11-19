package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;

/**
 * Round 2 Boss Quad Attack - 4갈래로 나가는 공격
 * 2round1.gif를 사용하여 4방향으로 동시에 발사
 */
public class Round2QuadAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The attack image */
    private java.awt.Image attackImage;
    /** Attack damage */
    private int damage = 2;
    /** Movement speed */
    private double moveSpeed = 150;
    /** Attack direction (0: Up, 1: Right, 2: Down, 3: Left) */
    private int direction;
    /** Duration of the attack */
    private long attackDuration = 3000; // 3초
    /** Start time */
    private long startTime;

    /**
     * Create a new quad attack entity
     * 
     * @param game The game in which this entity exists
     * @param x The initial x location of this entity
     * @param y The initial y location of this entity
     * @param direction The direction of the attack (0-3)
     */
    public Round2QuadAttack(Game game, int x, int y, int direction) {
        super("sprites/Boss_Attack/2round1.gif", x, y);
        this.game = game;
        this.direction = direction;
        this.startTime = System.currentTimeMillis();
        
        // Set movement based on direction - all towards player area with spread
        switch (direction) {
            case 0: // Center-left spread
                this.dx = -moveSpeed * 0.3; // Slightly left
                this.dy = moveSpeed * 0.9; // Mostly down
                break;
            case 1: // Center spread
                this.dx = 0;
                this.dy = moveSpeed; // Straight down
                break;
            case 2: // Center-right spread
                this.dx = moveSpeed * 0.3; // Slightly right
                this.dy = moveSpeed * 0.9; // Mostly down
                break;
            case 3: // Wide spread
                this.dx = moveSpeed * 0.5; // More right
                this.dy = moveSpeed * 0.8; // Down
                break;
        }
        
        loadAttackImage();
    }

    /**
     * Load the attack image
     */
    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/2round1.gif");
            if (imageUrl != null) {
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                System.out.println("🎯 Successfully loaded quad attack GIF: 2round1.gif via ClassLoader.getResource()");
            } else {
                System.err.println("🎯 Failed to find 2round1.gif resource.");
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage("src/main/resources/sprites/Boss_Attack/2round1.gif");
                System.out.println("🎯 Attempted direct loading of 2round1.gif: " + (attackImage != null));
            }
            // Ensure image is loaded before proceeding
            if (attackImage != null) {
                java.awt.MediaTracker mt = new java.awt.MediaTracker(new java.awt.Canvas());
                mt.addImage(attackImage, 0);
                mt.waitForAll();
                if (mt.isErrorAny()) {
                    System.err.println("🎯 Error loading 2round1.gif with MediaTracker.");
                    attackImage = null;
                } else {
                    System.out.println("🎯 Quad attack image dimensions (after MediaTracker): " + attackImage.getWidth(null) + "x" + attackImage.getHeight(null));
                }
            }
        } catch (Exception e) {
            System.err.println("🎯 Failed to load quad attack GIF image: " + e.getMessage());
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
        if (x < -50 || x > game.getWidth() + 50 || y < -50 || y > game.getHeight() + 50) {
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
            // Draw the attack image at a reasonable size
            g.drawImage(attackImage, (int)x - 25, (int)y - 25, 50, 50, null);
            
            // Debug: Log quad attack position
            if (System.currentTimeMillis() % 1000 < 50) {
                String[] directions = {"Up", "Right", "Down", "Left"};
                System.out.println("🎯 Drawing quad attack " + directions[direction] + " at X:" + (int)x + " Y:" + (int)y);
            }
        } else {
            // Fallback: draw a colored circle
            g.setColor(java.awt.Color.ORANGE);
            g.fillOval((int)x - 15, (int)y - 15, 30, 30);
            g.setColor(java.awt.Color.RED);
            g.drawOval((int)x - 15, (int)y - 15, 30, 30);
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
                System.out.println("🛡️ Player is invincible, quad attack blocked");
                game.removeEntity(this);
                return;
            }
            
            // Check distance for more accurate collision detection
            double dx = this.x - other.getX();
            double dy = this.y - other.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            // Use collision radius that matches visual size (50x50 image, so radius ~25)
            double collisionRadius = 25.0;
            
            if (distance <= collisionRadius) {
                System.out.println("🎯 Quad attack hit player ship! Distance: " + String.format("%.2f", distance) + 
                                 " Collision radius: " + collisionRadius + " Damage: " + damage);
                game.notifyPlayerDamaged(damage);
                game.removeEntity(this);
            } else {
                // Debug: log when collision check happens but no hit
                System.out.println("🎯 Quad attack collision check - Distance: " + String.format("%.2f", distance) + 
                                 " > Collision radius: " + collisionRadius + " (No hit)");
            }
        }
    }
}
