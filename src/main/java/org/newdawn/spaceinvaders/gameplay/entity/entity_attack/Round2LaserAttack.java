package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Image;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 2라운드 보스 레이저 공격
 * 2round.gif를 90도 회전하여 레이저처럼 발사
 */
public class Round2LaserAttack extends Entity {
    /** The game in which the entity exists */
    private Game game;
    /** Movement speed */
    private double moveSpeed = 300;
    /** Attack damage */
    private int damage = 3;
    /** Laser sprite image (2round.gif) */
    private Image laserImage;
    /** Rotation angle in radians (90 degrees = π/2) */
    private double rotationAngle = Math.PI / 2;
    /** Laser length (height for vertical laser) - from boss to bottom of screen */
    private double laserLength = 700; // 보스부터 맵 끝까지 더 길게
    /** Laser width */
    private double laserWidth = 300; // 더 넓게
    /** Start time for laser duration */
    private long startTime;
    
    /**
     * Create a new laser attack
     * 
     * @param game The game in which the entity exists
     * @param x The initial x location of the laser
     * @param y The initial y location of the laser
     */
    public Round2LaserAttack(Game game, int x, int y) {
        super("sprites/Boss_Attack/2round.gif", x, y);
        this.game = game;
        
        // 레이저는 보스 위치에서 시작해서 아래쪽으로 뻗어나감
        dy = 0; // 이동하지 않음
        dx = 0;
        
        // 시작 시간 기록
        this.startTime = System.currentTimeMillis();
        
        // 레이저를 보스 위치에서 시작하도록 Y 좌표 조정
        // 레이저가 아래쪽으로 뻗어나가므로 시작 Y는 보스 위치 그대로 사용
        
            // 이미지 로드 - IceAttack과 같은 방식으로 GIF 로딩
            try {
                // Use Toolkit to load animated GIF properly (same as IceAttack)
                java.net.URL imageURL = getClass().getClassLoader().getResource("sprites/Boss_Attack/2round.gif");
                if (imageURL != null) {
                    laserImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageURL);
                    System.out.println("🔵 Successfully loaded animated laser attack GIF: 2round.gif");
                } else {
                    System.err.println("🔵 Could not find 2round.gif file");
                }
                
                if (laserImage != null) {
                    System.out.println("🔵 Laser image dimensions: " + laserImage.getWidth(null) + "x" + laserImage.getHeight(null));
                    System.out.println("🔵 Laser image class: " + laserImage.getClass().getSimpleName());
                }
            } catch (Exception e) {
                System.err.println("🔵 Failed to load laser GIF image: " + e.getMessage());
                e.printStackTrace();
            }
    }
    
    /**
     * Request that this laser move itself based on an elapsed amount of time
     * 
     * @param delta The time that has elapsed since last move (ms)
     */
    public void move(long delta) {
        // 레이저는 이동하지 않고 고정된 위치에서 지속적으로 발사
        // super.move(delta)를 호출하지 않음
        
        // 레이저 지속 시간 후 제거 (3초)
        if (System.currentTimeMillis() - startTime > 3000) {
            game.removeEntity(this);
        }
    }
    
    /**
     * Draw this entity to the graphics context provided
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        // Debug: Log laser position and image status
        System.out.println("🔵 Drawing laser attack at X:" + (int)x + " Y:" + (int)y + " W:" + (int)laserLength + " H:" + (int)laserWidth);
        System.out.println("🔵 Laser image loaded: " + (laserImage != null));
        
        if (laserImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            
            // Save the original transform
            AffineTransform originalTransform = g2d.getTransform();
            
            // Move to the center of the laser for rotation
            g2d.translate(x + laserLength / 2.0, y + laserWidth / 2.0);
            
            // Rotate 90 degrees (π/2 radians)
            g2d.rotate(rotationAngle);
            
            // Draw the rotated image centered
            g2d.drawImage(laserImage, 
                -(int)(laserLength / 2), 
                -(int)(laserWidth / 2), 
                (int)laserLength, 
                (int)laserWidth, 
                null);
            
            // Restore the original transform
            g2d.setTransform(originalTransform);
            
            System.out.println("🔵 Laser image drawn successfully");
        } else {
            // Fallback: draw a bright cyan rectangle if image fails to load
            g.setColor(java.awt.Color.CYAN);
            g.fillRect((int)x, (int)y, (int)laserLength, (int)laserWidth);
            
            // Also draw a bright border to make it more visible
            g.setColor(java.awt.Color.BLUE);
            g.drawRect((int)x, (int)y, (int)laserLength, (int)laserWidth);
            
            System.out.println("🔵 Laser image is null, using fallback rectangle");
        }
    }
    
    /**
     * Notification that this laser has collided with another entity
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
     * Get the damage this laser deals
     * 
     * @return The damage amount
     */
    public int getDamage() {
        return damage;
    }
    
    /**
     * Set the damage this laser deals
     * 
     * @param damage The damage amount
     */
    public void setDamage(int damage) {
        this.damage = damage;
    }
}
