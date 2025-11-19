package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;

/**
 * Round 2 Boss Machine Gun Attack - 기관총식 연발 공격
 * 2round1.gif를 사용하여 연속으로 빠르게 발사
 */
public class Round2MachineGunAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The attack image */
    private java.awt.Image attackImage;
    /** Attack damage */
    private int damage = 1;
    /** Movement speed */
    private double moveSpeed = 180;
    /** Target Y position (middle of map) */
    private double targetY = 300; // 맵 중간에서 사라짐
    /** Start time */
    private long startTime;

    /**
     * Create a new machine gun attack entity
     * 
     * @param game The game in which this entity exists
     * @param x The initial x location of this entity
     * @param y The initial y location of this entity
     */
    public Round2MachineGunAttack(Game game, int x, int y) {
        super("sprites/Boss_Attack/2round1.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        
        // Start from ceiling (Y=0) and move down
        this.x = x;
        this.y = 0; // 천장에서 시작
        
        // Move straight down towards middle of map
        this.dx = 0;
        this.dy = moveSpeed;
        
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
                System.out.println("🔫 Successfully loaded machine gun attack GIF: 2round1.gif via ClassLoader.getResource()");
            } else {
                System.err.println("🔫 Failed to find 2round1.gif resource.");
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage("src/main/resources/sprites/Boss_Attack/2round1.gif");
                System.out.println("🔫 Attempted direct loading of 2round1.gif: " + (attackImage != null));
            }
            // Ensure image is loaded before proceeding
            if (attackImage != null) {
                java.awt.MediaTracker mt = new java.awt.MediaTracker(new java.awt.Canvas());
                mt.addImage(attackImage, 0);
                mt.waitForAll();
                if (mt.isErrorAny()) {
                    System.err.println("🔫 Error loading 2round1.gif with MediaTracker.");
                    attackImage = null;
                } else {
                    System.out.println("🔫 Machine gun attack image dimensions (after MediaTracker): " + attackImage.getWidth(null) + "x" + attackImage.getHeight(null));
                }
            }
        } catch (Exception e) {
            System.err.println("🔫 Failed to load machine gun attack GIF image: " + e.getMessage());
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
        
        // Remove when reaching middle of map (targetY = 300)
        if (y >= targetY) {
            game.removeEntity(this);
            return;
        }
        
        // Remove if out of screen bounds horizontally
        if (x < -50 || x > game.getWidth() + 50) {
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
            // Draw the attack image at a smaller size for rapid fire
            g.drawImage(attackImage, (int)x - 15, (int)y - 15, 30, 30, null);
            
            // Debug: Log machine gun attack position
            if (System.currentTimeMillis() % 1000 < 50) {
                System.out.println("🔫 Drawing machine gun attack at X:" + (int)x + " Y:" + (int)y);
            }
        } else {
            // Fallback: draw a small colored circle
            g.setColor(java.awt.Color.YELLOW);
            g.fillOval((int)x - 10, (int)y - 10, 20, 20);
            g.setColor(java.awt.Color.ORANGE);
            g.drawOval((int)x - 10, (int)y - 10, 20, 20);
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
            System.out.println("🔫 Machine gun attack hit player ship!");
            game.removeEntity(this);
            game.notifyPlayerDamaged(damage);
        }
    }
}
