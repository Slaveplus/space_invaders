package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * An entity representing a missile fired by the player
 * 
 * @author Space Invaders Team
 */
public class MissileEntity extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** Target coordinates */
    private double targetX;
    private double targetY;
    /** Missile speed */
    private double speed = 400;
    /** True if this missile has been "used", i.e. its hit something */
    private boolean used = false;
    
    /**
     * Create a new missile
     * 
     * @param game The game in which the missile has been created
     * @param sprite The sprite representing this missile
     * @param x The initial x location of the missile
     * @param y The initial y location of the missile
     * @param targetX The target x location
     * @param targetY The target y location
     */
    public MissileEntity(Game game, String sprite, int x, int y, double targetX, double targetY) {
        super(sprite, x, y);
        
        this.game = game;
        this.targetX = targetX;
        this.targetY = targetY;
        
        // Calculate direction to target
        double dxToTarget = targetX - x;
        double dyToTarget = targetY - y;
        double distance = Math.sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget);
        
        // Normalize and scale to desired speed
        if (distance > 0) {
            dx = (dxToTarget / distance) * speed;
            dy = (dyToTarget / distance) * speed;
        } else {
            dx = 0;
            dy = -speed; // Fallback to upward movement
        }
    }
    
    /**
     * Request that this missile moved based on time elapsed
     * 
     * @param delta The time that has elapsed since last move
     */
    public void move(long delta) {
        // proceed with normal move
        super.move(delta);
        
        // Check if missile has reached target area or gone off screen
        double distanceToTarget = Math.sqrt((targetX - x) * (targetX - x) + (targetY - y) * (targetY - y));
        
        if (distanceToTarget < 30 || y < -100 || y > 700 || x < -100 || x > 800) {
            // Create explosion at target location
            ExplosionEntity explosion = new ExplosionEntity(game, "sprites/Skill/Explosion.png", 
                    (int)targetX - 25, (int)targetY - 25, 100.0);
            game.addEntity(explosion);
            
            // Remove missile
            game.removeEntity(this);
        }
    }
    
    /**
     * Draw this missile with scaled size
     * 
     * @param g The graphics context on which to draw
     */
    @Override
    public void draw(Graphics g) {
        // Scale down the missile to 50% of original size
        int originalWidth = sprite.getWidth();
        int originalHeight = sprite.getHeight();
        int scaledWidth = originalWidth / 2;
        int scaledHeight = originalHeight / 2;
        
        // Center the scaled missile
        int drawX = (int) x - (scaledWidth - originalWidth) / 2;
        int drawY = (int) y - (scaledHeight - originalHeight) / 2;
        
        // Enable smooth scaling
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Draw scaled missile
        g2d.drawImage(sprite.getImage(), drawX, drawY, scaledWidth, scaledHeight, null);
    }
    
    /**
     * Notification that this missile has collided with another entity
     * 
     * @param other The other entity
     */
    public void collidedWith(Entity other) {
        // prevents double kills, if we've already hit something,
        // don't collide
        if (used) {
            return;
        }
        
        // if we've hit an alien, kill it!
        if (other instanceof AlienEntity) {
            // remove the affected entities
            game.removeEntity(this);
            game.removeEntity(other);
            
            // notify the game that the alien has been killed
            game.notifyAlienKilled();
            used = true;
        }
        
        // if we've hit a boss, damage it
        if (other instanceof BossEntity) {
            BossEntity boss = (BossEntity) other;
            boss.takeDamage(50); // Missile deals 50 damage
            
            // remove the missile
            game.removeEntity(this);
            used = true;
        }
    }
}
