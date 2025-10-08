package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Round 4 Boss Green Sphere Attack - 플레이어를 향해 날아가는 초록색 구체
 */
public class Round4GreenSphereAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** Damage dealt to player */
    private int damage = 2;
    /** Movement speed */
    private double moveSpeed = 200;
    /** Attack duration */
    private long attackDuration = 5000; // 5초간 유지
    /** Start time */
    private long startTime;
    /** Green sphere size */
    private int sphereSize = 8; // 히트범위 대폭 축소: 30 → 20 → 15 → 8

    /**
     * Create a new green sphere attack entity
     * 
     * @param game The game in which this entity exists
     * @param x The initial x location of this entity
     * @param y The initial y location of this entity
     * @param directionX Direction X component (normalized)
     * @param directionY Direction Y component (normalized)
     */
    public Round4GreenSphereAttack(Game game, int x, int y, double directionX, double directionY) {
        super("sprites/Skill/Heat.gif", x, y); // Use existing image instead of non-existent green_sphere.png
        this.game = game;
        this.startTime = System.currentTimeMillis();
        
        // Set movement direction towards player
        this.dx = directionX * moveSpeed;
        this.dy = directionY * moveSpeed;
        
        System.out.println("🟣 Green Sphere Attack created - Direction: (" + String.format("%.2f", directionX) + 
                         ", " + String.format("%.2f", directionY) + ") Speed: " + moveSpeed);
    }

    /**
     * Request that this entity move itself based on a certain amount
     * of time having passed.
     * 
     * @param delta The amount of time that has passed in milliseconds
     */
    public void move(long delta) {
        // Move towards player
        super.move(delta);
        
        // Check if attack duration has expired
        if (System.currentTimeMillis() - startTime > attackDuration) {
            System.out.println("🟣 Green sphere attack expired after " + attackDuration + "ms");
            game.removeEntity(this);
            return;
        }
        
        // Check if out of screen bounds
        if (x < -50 || x > 850 || y < -50 || y > 650) {
            System.out.println("🟣 Green sphere attack out of bounds, removing");
            game.removeEntity(this);
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
            
            // Draw green sphere (fallback rendering since we don't have green_sphere.png)
            g2d.setColor(Color.GREEN);
            g2d.fillOval((int)x - sphereSize/2, (int)y - sphereSize/2, sphereSize, sphereSize);
            
            // Add a bright green outline for better visibility
            g2d.setColor(Color.WHITE);
            g2d.drawOval((int)x - sphereSize/2, (int)y - sphereSize/2, sphereSize, sphereSize);
            
            // Add inner glow effect
            g2d.setColor(new Color(0, 255, 0, 100));
            g2d.fillOval((int)x - sphereSize/4, (int)y - sphereSize/4, sphereSize/2, sphereSize/2);
            
            System.out.println("🟣 Drawing green sphere at X:" + (int)x + " Y:" + (int)y + " Size:" + sphereSize);
            
        } catch (Exception e) {
            System.err.println("Error drawing green sphere: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Notification that this entity collided with another
     * 
     * @param other The entity with which we collided
     */
    public void collidedWith(Entity other) {
        // if we've hit the player's ship, damage it
        if (other instanceof org.newdawn.spaceinvaders.gameplay.entity.ShipEntity) {
            System.out.println("🟣 Green sphere hit player ship! Damage: " + damage);
            game.notifyPlayerDamaged(damage);
            game.removeEntity(this);
        }
    }
}
