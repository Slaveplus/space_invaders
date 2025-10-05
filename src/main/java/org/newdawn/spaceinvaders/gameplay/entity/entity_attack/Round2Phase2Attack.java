package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import java.awt.Graphics;
import java.awt.Image;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 2라운드 보스 2단계 공격 (2round1.gif)
 * 1단계 공격이 맵 중간에 도달한 후 사방으로 퍼지는 공격
 */
public class Round2Phase2Attack extends Entity {
    /** The game in which the entity exists */
    private Game game;
    /** Movement speed */
    private double moveSpeed = 200;
    /** Attack damage */
    private int damage = 2;
    /** Projectile sprite image (2round1.gif) */
    private Image projectileImage;
    /** Movement angle */
    private double angle;
    
    /**
     * Create a new phase 2 attack
     * 
     * @param game The game in which the entity exists
     * @param x The initial x location of the projectile
     * @param y The initial y location of the projectile
     * @param angle The movement angle in radians
     */
    public Round2Phase2Attack(Game game, int x, int y, double angle) {
        super("sprites/Boss_Attack/2round1.gif", x, y);
        this.game = game;
        this.angle = angle;
        
        // 각도에 따라 이동 방향 설정
        dx = Math.cos(angle) * moveSpeed;
        dy = Math.sin(angle) * moveSpeed;
        
        // 이미지 로드
        try {
            projectileImage = org.newdawn.spaceinvaders.gameplay.sprite.SpriteStore.get().getSprite("sprites/Boss_Attack/2round1.gif").getImage();
        } catch (Exception e) {
            System.err.println("Failed to load phase 2 projectile image: " + e.getMessage());
        }
    }
    
    /**
     * Request that this projectile move itself based on an elapsed amount of time
     * 
     * @param delta The time that has elapsed since last move (ms)
     */
    public void move(long delta) {
        // proceed with normal move
        super.move(delta);
        
        // if we shot off the screen, remove ourselves
        if (y > 600 || y < -100 || x < -100 || x > 800) {
            game.removeEntity(this);
        }
    }
    
    /**
     * Draw this entity to the graphics context provided
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        if (projectileImage != null) {
            g.drawImage(projectileImage, (int)x - 25, (int)y - 25, 50, 50, null);
            
            // Debug: Log phase 2 projectile position
            System.out.println("💥 Drawing phase 2 projectile at X:" + (int)x + " Y:" + (int)y + " Angle:" + String.format("%.2f", angle));
        } else {
            // Fallback: draw a simple rectangle if image fails to load
            g.setColor(java.awt.Color.RED);
            g.fillRect((int)x - 25, (int)y - 25, 50, 50);
        }
    }
    
    /**
     * Notification that this projectile has collided with another entity
     * 
     * @param other The other entity
     */
    public void collidedWith(Entity other) {
        // if we've hit the player's ship, damage it
        if (other instanceof org.newdawn.spaceinvaders.gameplay.entity.ShipEntity) {
            // Debug: Check actual collision distance
            double distance = Math.sqrt(
                Math.pow(this.x - other.getX(), 2) + Math.pow(this.y - other.getY(), 2)
            );
            double projectileRadius = 25; // Half of 50x50 size
            double shipRadius = 20; // Approximate ship collision radius
            
            System.out.println("💥 Phase 2 collision check - Distance: " + String.format("%.2f", distance) + 
                             " Projectile radius: " + projectileRadius + " Ship radius: " + shipRadius);
            
            if (distance <= (projectileRadius + shipRadius)) {
                System.out.println("💥 Phase 2 projectile hit player ship!");
                game.removeEntity(this);
                game.notifyPlayerDamaged(damage);
            } else {
                System.out.println("💥 Phase 2 projectile collision detected but distance too far, ignoring");
            }
        }
    }
    
    /**
     * Get the damage this projectile deals
     * 
     * @return The damage amount
     */
    public int getDamage() {
        return damage;
    }
    
    /**
     * Get the movement angle
     * 
     * @return The angle in radians
     */
    public double getAngle() {
        return angle;
    }
}
