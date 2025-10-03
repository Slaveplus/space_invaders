package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * 폭발 효과를 나타내는 엔티티
 */
public class ExplosionEntity extends Entity {
    /** The game in which this explosion exists */
    private Game game;
    /** Explosion duration in milliseconds */
    private long explosionDuration = 1000; // 1 second
    /** Time when explosion started */
    private long startTime;
    /** Maximum explosion radius */
    private double maxRadius;
    /** Current explosion radius */
    private double currentRadius;
    /** Damage dealt flag to prevent multiple damage */
    private boolean damageDealt = false;
    /** Last damage time */
    private long lastDamageTime = 0;
    /** Damage interval (ms) */
    private long damageInterval = 100; // 100ms마다 데미지
    /** Set of aliens that have been damaged */
    private java.util.Set<Entity> damagedEntities = new java.util.HashSet<>();
    /** Explosion image */
    private BufferedImage explosionImage;
    /** Explosion growth speed */
    // private double growthSpeed = 200; // pixels per second
    
    /**
     * Create a new explosion
     * 
     * @param game The game in which the explosion has been created
     * @param sprite The sprite representing this explosion
     * @param x The x location of the explosion
     * @param y The y location of the explosion
     * @param maxRadius The maximum radius of the explosion
     */
    public ExplosionEntity(Game game, String sprite, int x, int y, double maxRadius) {
        super(sprite, x, y);
        
        this.game = game;
        this.maxRadius = maxRadius;
        this.currentRadius = 0;
        this.startTime = System.currentTimeMillis();
        
        // Don't move
        dx = 0;
        dy = 0;
        
        // Load explosion image
        loadExplosionImage();
    }
    
    /**
     * Request that this explosion updated based on time elapsed
     * 
     * @param delta The time that has elapsed since last update
     */
    public void move(long delta) {
        try {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTime;
            
            // Calculate current radius based on time
            double progress = (double) elapsed / explosionDuration;
            
            if (progress >= 1.0) {
                // Explosion finished, remove it
                if (game != null) {
                    game.removeEntity(this);
                }
                return;
            }
            
            // Grow explosion radius over time
            currentRadius = maxRadius * progress;
            
            // Deal damage to enemies in explosion radius (continuously)
            if (currentRadius > maxRadius * 0.2 && currentTime - lastDamageTime >= damageInterval) {
                dealDamageToEnemies();
                lastDamageTime = currentTime;
            }
        } catch (Exception e) {
            System.err.println("Error in explosion move: " + e.getMessage());
            e.printStackTrace();
            // Try to remove explosion on error
            try {
                if (game != null) {
                    game.removeEntity(this);
                }
            } catch (Exception ex) {
                System.err.println("Error removing explosion: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Draw the explosion with scaling effect
     */
    @Override
    public void draw(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            
            // Enable anti-aliasing for smoother explosion
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw explosion image scaled to current radius
            if (explosionImage != null) {
                int drawSize = (int) (currentRadius * 2);
                int drawX = (int) x - drawSize / 2;
                int drawY = (int) y - drawSize / 2;
                
                // Draw the explosion image
                g2d.drawImage(explosionImage, drawX, drawY, drawSize, drawSize, null);
            }
        
            // Draw explosion ring effect
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new java.awt.BasicStroke(3.0f));
            g2d.drawOval((int)(x - currentRadius), (int)(y - currentRadius), 
                        (int)(currentRadius * 2), (int)(currentRadius * 2));
            
            // Draw inner ring
            g2d.setColor(Color.ORANGE);
            g2d.setStroke(new java.awt.BasicStroke(2.0f));
            double innerRadius = currentRadius * 0.7;
            g2d.drawOval((int)(x - innerRadius), (int)(y - innerRadius), 
                        (int)(innerRadius * 2), (int)(innerRadius * 2));
        } catch (Exception e) {
            System.err.println("Error drawing explosion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get current explosion radius
     * 
     * @return The current explosion radius
     */
    public double getCurrentRadius() {
        return currentRadius;
    }
    
    /**
     * Get maximum explosion radius
     * 
     * @return The maximum explosion radius
     */
    public double getMaxRadius() {
        return maxRadius;
    }
    
    /**
     * Check if explosion is finished
     * 
     * @return True if explosion is finished
     */
    public boolean isFinished() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - startTime) >= explosionDuration;
    }
    
    /**
     * Load explosion image
     */
    private void loadExplosionImage() {
        try {
            // Try to load explosion image
            URL url = getClass().getClassLoader().getResource("sprites/Skill/Explosion.png");
            if (url != null) {
                explosionImage = ImageIO.read(url);
                System.out.println("Successfully loaded explosion image: sprites/Skill/Explosion.png");
            } else {
                System.err.println("Cannot find explosion image: sprites/Skill/Explosion.png");
                explosionImage = null;
            }
        } catch (IOException e) {
            System.err.println("Failed to load explosion image: " + e.getMessage());
            e.printStackTrace();
            explosionImage = null;
        } catch (Exception e) {
            System.err.println("Unexpected error loading explosion image: " + e.getMessage());
            e.printStackTrace();
            explosionImage = null;
        }
    }
    
    /**
     * Deal damage to enemies in explosion radius
     */
    private void dealDamageToEnemies() {
        try {
            // Create a copy to avoid ConcurrentModificationException
            java.util.List<Object> entities = game.getEntities();
            java.util.List<AlienEntity> aliensToDamage = new java.util.ArrayList<>();
            
            // First collect all aliens in range
            for (Object obj : entities) {
                if (obj instanceof AlienEntity) {
                    AlienEntity alien = (AlienEntity) obj;
                    if (!damagedEntities.contains(alien)) {
                        double dxToEnemy = alien.getX() - x;
                        double dyToEnemy = alien.getY() - y;
                        double distanceToEnemy = Math.sqrt(dxToEnemy * dxToEnemy + dyToEnemy * dyToEnemy);
                        
                        if (distanceToEnemy <= currentRadius) {
                            aliensToDamage.add(alien);
                        }
                    }
                }
            }
            
            // Then damage all collected aliens
            for (AlienEntity alien : aliensToDamage) {
                if (alien != null) {
                    // Damage enemy (high damage for explosion)
                    alien.takeDamage(game.getPlayerAttackPower() * 3); // 3x damage
                    damagedEntities.add(alien); // Mark as damaged
                }
            }
        } catch (Exception e) {
            System.err.println("Error dealing explosion damage: " + e.getMessage());
        }
    }
    
    /**
     * Notification that this explosion has collided with another entity
     * 
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        // Explosions don't collide with other entities
        // They just damage enemies in their radius
    }
}
