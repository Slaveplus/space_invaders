package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Round 3 Boss Black Hole Attack - 3round.gif를 사용한 블랙홀 공격
 * 블랙홀 위치에서 시작하여 블랙홀 쪽으로 이동하는 공격
 */
public class Round3BlackHoleAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The attack image */
    private java.awt.Image attackImage;
    /** Damage dealt to player */
    private int damage = 3;
    /** Movement speed towards black hole */
    private int moveSpeed = 200;
    /** Duration of the attack */
    private long attackDuration = 3000; // 3초
    /** Start time */
    private long startTime;
    /** Attack size - 큰 공격 */
    private int attackWidth = 150;
    private int attackHeight = 150;
    /** Target black hole position */
    private double targetX;
    private double targetY;

    /**
     * Create a new black hole attack entity
     * 
     * @param game The game in which this entity exists
     * @param startX The initial x location of this entity
     * @param startY The initial y location of this entity
     */
    public Round3BlackHoleAttack(Game game, int startX, int startY) {
        super("sprites/Boss_Attack/3round.gif", startX, startY);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        
        // 블랙홀 위치 설정 (블랙홀 공격이 생성된 위치 = 블랙홀 중심)
        this.targetX = startX;
        this.targetY = startY;
        
        // 보스 위치에서 블랙홀 방향으로 이동하는 방향 계산
        // 보스는 일반적으로 화면 상단에 있음
        double bossX = 400; // 보스 X 위치 (화면 중앙)
        double bossY = 120; // 보스 Y 위치 (화면 상단)
        
        // 보스에서 블랙홀로의 방향 벡터
        double dx = targetX - bossX;
        double dy = targetY - bossY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // 정규화된 방향으로 이동 (보스에서 블랙홀 방향)
        this.dx = (dx / distance) * moveSpeed;
        this.dy = (dy / distance) * moveSpeed;
        
        // 시작 위치를 보스 위치로 설정
        this.x = bossX;
        this.y = bossY;
        
        // Load the attack image
        loadAttackImage();
    }

    /**
     * Load the attack image
     */
    private void loadAttackImage() {
        try {
            // Use ClassLoader to load the GIF image
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/3round.gif");
            if (imageUrl != null) {
                this.attackImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                System.out.println("🔴 Black hole attack GIF image loaded successfully");
            } else {
                System.err.println("🔴 Failed to find black hole attack GIF image: sprites/Boss_Attack/3round.gif");
            }
        } catch (Exception e) {
            System.err.println("🔴 Failed to load black hole attack GIF image: " + e.getMessage());
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
        // Move towards black hole
        super.move(delta);
        
        // Check if attack duration has expired
        if (System.currentTimeMillis() - startTime > attackDuration) {
            System.out.println("🔴 Black hole attack expired after " + attackDuration + "ms");
            game.removeEntity(this);
            return;
        }
        
        // Check if reached black hole (close enough)
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 80) {
            System.out.println("🔴 Black hole attack reached target at distance: " + String.format("%.2f", distance));
            game.removeEntity(this);
        }
        
        // Check if out of screen bounds
        if (x < -50 || x > 850 || y < -50 || y > 650) {
            System.out.println("🔴 Black hole attack out of bounds, removing");
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
            
            if (attackImage != null) {
                int drawX = (int)x - attackWidth / 2;
                int drawY = (int)y - attackHeight / 2;
                
                g2d.drawImage(attackImage, drawX, drawY, drawX + attackWidth, drawY + attackHeight, 
                             0, 0, attackImage.getWidth(null), attackImage.getHeight(null), null);
                
                System.out.println("🔴 Drawing small black hole attack from boss to black hole at X:" + drawX + " Y:" + drawY + " Size:" + attackWidth + "x" + attackHeight);
            } else {
                // Fallback rendering if image failed to load
                g2d.setColor(Color.BLUE);
                g2d.fillRect((int)x - attackWidth / 2, (int)y - attackHeight / 2, attackWidth, attackHeight);
                g2d.setColor(Color.MAGENTA);
                g2d.drawRect((int)x - attackWidth / 2, (int)y - attackHeight / 2, attackWidth, attackHeight);
                System.out.println("🔴 Drawing black hole attack fallback at X:" + (int)x + " Y:" + (int)y + " Size:" + attackWidth + "x" + attackHeight);
            }
        } catch (Exception e) {
            System.err.println("Error drawing black hole attack: " + e.getMessage());
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
            // Distance-based collision detection
            double dx = this.x - other.getX();
            double dy = this.y - other.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            double collisionRadius = Math.min(attackWidth, attackHeight) / 2.0;
            
            if (distance <= collisionRadius) {
                System.out.println("🔴 Black hole attack hit player ship! Distance: " + String.format("%.2f", distance) + 
                                 " Collision radius: " + collisionRadius + " Damage: " + damage);
                game.notifyPlayerDamaged(damage);
                game.removeEntity(this);
            } else {
                System.out.println("🔴 Black hole attack collision check - Distance: " + String.format("%.2f", distance) + 
                                 " > Collision radius: " + collisionRadius + " (No hit)");
            }
        }
    }
}
