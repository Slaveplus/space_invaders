package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;

/**
 * An entity representing a shot fired by a boss enemy
 * Boss shots can move in any direction with custom speed
 * 
 * @author Space Invaders Team
 */
public class BossShotEntity extends Entity {
    /** The game in which this entity exists */
    private MultiplayerGameContext game;
    /** True if this shot has been "used", i.e. its hit something */
    private boolean used = false;
    /** The direction vector X component (normalized) */
    private double directionX;
    /** The direction vector Y component (normalized) */
    private double directionY;
    /** The speed of this shot */
    private double speed;
    /** The radius of this shot (for different sizes) */
    private int radius;
    /** True if this shot should split into smaller shots */
    private boolean canSplit;
    /** The Y position at which this shot should split */
    private double splitY;
    /** Number of smaller shots to create when splitting */
    private int splitCount;
    /** True if this shot has already split */
    private boolean hasSplit = false;
    
    /**
     * Create a new boss shot with specific direction and speed
     * 
     * @param game The game in which the shot has been created
     * @param x The initial x location of the shot
     * @param y The initial y location of the shot
     * @param directionX The X component of direction vector
     * @param directionY The Y component of direction vector
     * @param speed The speed of the shot in pixels per second
     */
    public BossShotEntity(MultiplayerGameContext game, int x, int y, double directionX, double directionY, double speed) {
        super("sprites/shot.gif", x, y);
        
        this.game = game;
        this.directionX = directionX;
        this.directionY = directionY;
        this.speed = speed;
        this.radius = 8; // Default radius
        this.canSplit = false;
        
        // Set the velocity based on direction and speed
        dx = directionX * speed;
        dy = directionY * speed;
    }
    
    /**
     * Create a new boss shot with splitting capability
     * 
     * @param game The game in which the shot has been created
     * @param x The initial x location of the shot
     * @param y The initial y location of the shot
     * @param directionX The X component of direction vector
     * @param directionY The Y component of direction vector
     * @param speed The speed of the shot in pixels per second
     * @param radius The radius of the shot
     * @param canSplit True if this shot should split
     * @param splitY The Y position at which to split
     * @param splitCount Number of smaller shots to create when splitting
     */
    public BossShotEntity(MultiplayerGameContext game, int x, int y, double directionX, double directionY, double speed,
                         int radius, boolean canSplit, double splitY, int splitCount) {
        super("sprites/shot.gif", x, y);
        
        this.game = game;
        this.directionX = directionX;
        this.directionY = directionY;
        this.speed = speed;
        this.radius = radius;
        this.canSplit = canSplit;
        this.splitY = splitY;
        this.splitCount = splitCount;
        
        // Set the velocity based on direction and speed
        dx = directionX * speed;
        dy = directionY * speed;
    }
    
    /**
     * Request that this shot moved based on time elapsed
     * 
     * @param delta The time that has elapsed since last move
     */
    public void move(long delta) {
        // Proceed with normal move
        super.move(delta);
        
        // Check if this shot should split
        if (canSplit && !hasSplit && y >= splitY) {
            split();
            hasSplit = true;
            // Remove this shot after splitting
            game.removeEntity(this);
            return;
        }
        
        // Remove shot if it goes off screen (any direction)
        if (y > 750 || y < -100 || x < -100 || x > 900) {
            game.removeEntity(this);
        }
    }
    
    /**
     * Split this shot into multiple smaller shots
     */
    private void split() {
        try {
            // 폭발 효과 제거 - 단순히 분열만 수행
            
            // Create smaller shots in all directions
            for (int i = 0; i < splitCount; i++) {
                double angle = 2 * Math.PI * i / splitCount;
                double newDirX = Math.cos(angle);
                double newDirY = Math.sin(angle);
                
                // Create smaller shot (radius is half of the original)
                BossShotEntity smallShot = new BossShotEntity(
                    game, 
                    (int)x, 
                    (int)y, 
                    newDirX, 
                    newDirY, 
                    speed * 0.8 // Slightly slower than parent
                );
                smallShot.radius = Math.max(4, radius / 2); // Half size, minimum 4
                game.addEntity(smallShot);
            }
        } catch (Exception e) {
            System.err.println("Error splitting boss shot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Draw this boss shot with special visual effects
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        // Draw circular boss energy projectile
        Graphics2D g2d = (Graphics2D) g;
        
        // Calculate center position for perfect circle
        int centerX = (int) x + sprite.getWidth() / 2;
        int centerY = (int) y + sprite.getHeight() / 2;
        
        // Color intensity changes based on size (bigger = more intense for splitting shots)
        int glowAlpha = canSplit ? 100 : 60;
        int coreAlpha = canSplit ? 240 : 220;
        
        // Outer glow effect (larger circle)
        g2d.setColor(new Color(255, 100, 100, glowAlpha)); // Red glow
        g2d.fillOval(centerX - radius - 3, centerY - radius - 3, (radius + 3) * 2, (radius + 3) * 2);
        
        // Main projectile body (perfect circle)
        g2d.setColor(new Color(255, 50, 50, coreAlpha)); // Bright red core
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        
        // Inner bright core (smaller circle) - only if radius is large enough
        if (radius > 4) {
            g2d.setColor(new Color(255, 200, 200, 180)); // Light red center
            g2d.fillOval(centerX - radius + 2, centerY - radius + 2, (radius - 2) * 2, (radius - 2) * 2);
        }
        
        // Bright white center (smallest circle) - only if radius is large enough
        if (radius > 6) {
            g2d.setColor(new Color(255, 255, 255, 200)); // White hot center
            g2d.fillOval(centerX - radius + 4, centerY - radius + 4, (radius - 4) * 2, (radius - 4) * 2);
        }
        
        // Add subtle energy trail effect in the opposite direction of movement
        if (speed > 0 && radius > 4) {
            // Calculate trail position (opposite to movement direction)
            int trailDistance = Math.max(8, radius);
            int trailX = centerX - (int)(directionX * trailDistance);
            int trailY = centerY - (int)(directionY * trailDistance);
            
            g2d.setColor(new Color(255, 100, 100, 80));
            int trailRadius = Math.max(3, radius - 1);
            g2d.fillOval(trailX - trailRadius, trailY - trailRadius, trailRadius * 2, trailRadius * 2);
            
            trailX = centerX - (int)(directionX * trailDistance * 1.5);
            trailY = centerY - (int)(directionY * trailDistance * 1.5);
            
            g2d.setColor(new Color(255, 100, 100, 40));
            trailRadius = Math.max(2, radius - 2);
            g2d.fillOval(trailX - trailRadius, trailY - trailRadius, trailRadius * 2, trailRadius * 2);
        }
    }
    
    /**
     * Notification that this shot has collided with another entity
     * 
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        // Prevents double kills, if we've already hit something, don't collide
        if (used) {
            return;
        }
        
        // Boss shot: if we've hit the player ship, damage it
        if (other instanceof ShipEntity) {
            // Remove the shot
            game.removeEntity(this);
            
            // Notify the game that the player has been hit
            game.notifyDeath();
            used = true;
        }
    }
    
    /**
     * Get the direction X component
     * 
     * @return The X component of the direction vector
     */
    public double getDirectionX() {
        return directionX;
    }
    
    /**
     * Get the direction Y component
     * 
     * @return The Y component of the direction vector
     */
    public double getDirectionY() {
        return directionY;
    }
    
    /**
     * Get the speed of this shot
     * 
     * @return The speed in pixels per second
     */
    public double getSpeed() {
        return speed;
    }
    
    /**
     * Check if this shot has been used (hit something)
     * 
     * @return True if this shot has been used
     */
    public boolean isUsed() {
        return used;
    }
}

