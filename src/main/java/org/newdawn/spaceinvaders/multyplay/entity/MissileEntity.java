package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;

/**
 * 플레이어가 발사한 미사일을 나타내는 엔티티
 * 
 * @author Space Invaders Team
 */
public class MissileEntity extends Entity {
    /** 이 엔티티가 존재하는 게임 */
    private MultiplayerGameContext game;
    /** 목표 좌표 */
    private double targetX;
    private double targetY;
    /** 미사일 속도 */
    private double speed = 400;
    /** 이 미사일이 "사용됨"인지 여부, 즉 무언가에 맞았는지 */
    private boolean used = false;
    
    /**
     * 새로운 미사일을 생성합니다
     * 
     * @param game 미사일이 생성된 게임
     * @param sprite 이 미사일을 나타내는 스프라이트
     * @param x 미사일의 초기 x 위치
     * @param y 미사일의 초기 y 위치
     * @param targetX 목표 x 위치
     * @param targetY 목표 y 위치
     */
    public MissileEntity(MultiplayerGameContext game, String sprite, int x, int y, double targetX, double targetY) {
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
     * 경과된 시간을 기반으로 이 미사일이 이동하도록 요청합니다
     * 
     * @param delta 마지막 이동 이후 경과된 시간
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
            explosion.setOwnerId(getOwnerId());
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
            AlienEntity alien = (AlienEntity) other;
            String ownerId = getOwnerId();
            int damage = Math.max(alien.getCurrentHP(), game.getPlayerAttackPower(ownerId) * 4);
            alien.takeDamage(damage);
            if (alien.getCurrentHP() <= 0) {
                game.notifyAlienKilled(ownerId, other.getX(), other.getY());
            }
            game.removeEntity(this);
            used = true;
        }

        if (other instanceof BossEntity) {
            BossEntity boss = (BossEntity) other;
            String ownerId = getOwnerId();
            boss.takeDamage(game.getPlayerAttackPower(ownerId), ownerId);
            game.removeEntity(this);
            used = true;
        }
    }
}
