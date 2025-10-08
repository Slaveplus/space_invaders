package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 2라운드 보스 1단계 공격 (2round3.gif)
 * 보스에서 직선으로 맵 중간까지 이동한 후 2단계 공격으로 전환
 */
public class Round2Phase1Attack extends Entity {
    /** The game in which the entity exists */
    private Game game;
    /** Movement speed */
    private double moveSpeed = 250;
    /** Attack damage */
    private int damage = 2;
    /** Projectile sprite image (2round3.gif) */
    private Image projectileImage;
    /** Target Y position (map middle) */
    private double targetY;
    /** Whether phase 1 is complete */
    private boolean phase1Complete = false;
    
    /**
     * Create a new phase 1 attack
     * 
     * @param game The game in which the entity exists
     * @param x The initial x location of the projectile
     * @param y The initial y location of the projectile
     */
    public Round2Phase1Attack(Game game, int x, int y) {
        super("sprites/Boss_Attack/2round3.gif", x, y);
        this.game = game;
        
        // 프로젝타일은 아래쪽으로 이동
        dy = moveSpeed;
        dx = 0;
        
        // 맵 중간까지 이동 (Y: 400 정도로 더 멀리)
        targetY = 400;
        
        // 이미지 로드
        try {
            projectileImage = org.newdawn.spaceinvaders.gameplay.sprite.SpriteStore.get().getSprite("sprites/Boss_Attack/2round3.gif").getImage();
        } catch (Exception e) {
            System.err.println("Failed to load projectile image: " + e.getMessage());
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
        
        // Check if reached target position
        if (y >= targetY && !phase1Complete) {
            phase1Complete = true;
            // Create phase 2 attack (2round1.gif spreading in all directions)
            createPhase2Attack();
            // Remove this projectile
            game.removeEntity(this);
        }
        
        // if we shot off the screen, remove ourselves
        if (y > 600 || y < -100) {
            game.removeEntity(this);
        }
    }
    
    /**
     * Create phase 2 attack - 2round1.gif spreading in all directions
     */
    private void createPhase2Attack() {
        try {
            // 8방향으로 2round1.gif 발사
            double[] angles = {0, Math.PI/4, Math.PI/2, 3*Math.PI/4, Math.PI, 5*Math.PI/4, 3*Math.PI/2, 7*Math.PI/4};
            
            for (double angle : angles) {
                Round2Phase2Attack phase2Attack = new Round2Phase2Attack(game, (int)x, (int)y, angle);
                game.addEntity(phase2Attack);
            }
            
            System.out.println("💥 Round 2 Phase 1 Complete! Phase 2 attack launched in 8 directions!");
            
        } catch (Exception e) {
            System.err.println("Error creating phase 2 attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Draw this entity to the graphics context provided
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        if (projectileImage != null) {
            g.drawImage(projectileImage, (int)x - 40, (int)y - 40, 80, 80, null);
            
            // Debug: Log projectile position
            System.out.println("🎯 Drawing phase 1 projectile at X:" + (int)x + " Y:" + (int)y + " (Target: " + (int)targetY + ")");
        } else {
            // Fallback: draw a simple rectangle if image fails to load
            g.setColor(java.awt.Color.ORANGE);
            g.fillRect((int)x - 40, (int)y - 40, 80, 80);
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
            game.removeEntity(this);
            game.notifyPlayerDamaged(damage);
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
     * Check if phase 1 is complete
     * 
     * @return True if phase 1 is complete
     */
    public boolean isPhase1Complete() {
        return phase1Complete;
    }
}
