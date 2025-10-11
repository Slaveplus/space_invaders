package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An entity representing a boss enemy
 * 
 * @author Space Invaders Team
 */
public class BossEntity extends Entity {
    /** The game in which this entity exists */
    private MultiplayerGameContext game;
    /** True if this boss has been "used", i.e. its hit something */
    private boolean used = false;
    /** Boss HP */
    private int currentHP;
    private int maxHP;
    /** Boss movement speed */
    private double moveSpeed = 50;
    /** Time since last shot */
    private long lastFire = 0;
    /** Firing interval */
    private long firingInterval = 1500;
    /** Movement direction */
    private boolean movingRight = true;
    /** Boss round number */
    private int round;
    /** Phase of the boss (1-3 based on HP) */
    private int phase = 1;
    /** Attack pattern counter (removed - now using random patterns) */
    /** Counter for fan attack animation */
    private double fanAngle = 0;
    
    /**
     * Get the boss sprite path based on round number
     * 
     * @param round The round number
     * @return The sprite path for the boss
     */
    private static String getBossSpriteForRound(int round) {
        switch (round) {
            case 1:
                return "sprites/Boss/1Boss.png";
            case 2:
                return "sprites/Boss/2Boss.png";
            case 3:
                return "sprites/Boss/3Boss.png";
            case 4:
                return "sprites/Boss/4Boss.png";
            default:
                return "sprites/Boss/5Boss.png";
        }
    }
    
    /**
     * Create a new boss entity
     * 
     * @param game The game in which the boss has been created
     * @param x The initial x location of the boss
     * @param y The initial y location of the boss
     * @param round The round number for scaling
     */
    public BossEntity(MultiplayerGameContext game, int x, int y, int round) {
        super(getBossSpriteForRound(round), x, y);
        
        this.game = game;
        this.round = round;
        
        // Scale boss stats based on round
        maxHP = 50 + (round * 30); // 80, 110, 140, 170, 200...
        currentHP = maxHP;
        
        // Scale movement speed
        moveSpeed = 50 + (round * 10);
        
        // Scale firing interval (slower for more strategic gameplay)
        firingInterval = Math.max(2000, 3000 - (round * 200)); // 더 느린 공격속도
        
        // Boss stays in center back - no movement
        dx = 0;
        dy = 0;
    }
    
    /**
     * Request that this boss moved based on time elapsed
     * 
     * @param delta The time that has elapsed since last move
     */
    public void move(long delta) {
        try {
            // Boss stays stationary in center back
            dx = 0;
            dy = 0;
            
            // Try to fire
            tryToFire();
        } catch (Exception e) {
            System.err.println("Error in boss move: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Attempt to fire a shot from this boss
     */
    public void tryToFire() {
        try {
            // check that we have waited long enough to fire
            if (System.currentTimeMillis() - lastFire < firingInterval) {
                return;
            }
            
            lastFire = System.currentTimeMillis();
            
            // Get player position for targeted attacks
            Entity targetShip = game.getShip(null);
            double playerX = targetShip != null ? targetShip.getX() + 15 : x;
            double playerY = targetShip != null ? targetShip.getY() : y + 200;
            double bossFireX = x;
            double bossFireY = y + 75; // Fire from bottom of boss
            
            // Randomly select from 5 different attack patterns
            int randomPattern = (int)(Math.random() * 5);
            switch (randomPattern) {
                case 0:
                    // Pattern 1: 일직선 4발 발사 (4 shots in a straight line)
                    fireLinearPattern(bossFireX, bossFireY);
                    break;
                case 1:
                    // Pattern 2: 플레이어 방향 발사 (Aimed shots at player)
                    fireAimedPattern(bossFireX, bossFireY, playerX, playerY);
                    break;
                case 2:
                    // Pattern 3: 부채꼴 형태로 채찍 휘두르듯 발사 (Fan/whip pattern)
                    fireFanPattern(bossFireX, bossFireY);
                    break;
                case 3:
                    // Pattern 4: 원 형태로 발사 (Circular pattern)
                    fireCircularPattern(bossFireX, bossFireY);
                    break;
                case 4:
                    // Pattern 5: 큰 구체 발사 후 분열 (Large splitting shot)
                    fireSplittingPattern(bossFireX, bossFireY);
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Error in boss firing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Pattern 1: Fire 4 shots in a straight vertical line
     */
    private void fireLinearPattern(double fireX, double fireY) {
        int spacing = 40;
        for (int i = 0; i < 4; i++) {
            int offsetX = (i - 1) * spacing - spacing / 2; // Center the pattern
            createDirectionalShot(fireX + offsetX, fireY, 0, 1, 300);
        }
    }
    
    /**
     * Pattern 2: Fire multiple shots aimed at player position
     */
    private void fireAimedPattern(double fireX, double fireY, double playerX, double playerY) {
        // Calculate direction to player
        double dx = playerX - fireX;
        double dy = playerY - fireY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            // Normalize direction
            double dirX = dx / distance;
            double dirY = dy / distance;
            
            // Fire multiple shots with slight spread
            for (int i = -1; i <= 1; i++) {
                double spreadAngle = i * 0.2; // 약간의 각도 차이
                double spreadDirX = dirX * Math.cos(spreadAngle) - dirY * Math.sin(spreadAngle);
                double spreadDirY = dirX * Math.sin(spreadAngle) + dirY * Math.cos(spreadAngle);
                
                createDirectionalShot(fireX, fireY, spreadDirX, spreadDirY, 350);
            }
        }
    }
    
    /**
     * Pattern 3: Fire shots in a fan/whip pattern (sweeping from left to right)
     */
    private void fireFanPattern(double fireX, double fireY) {
        // Fire 5 shots in a fan pattern
        int numShots = 5;
        double startAngle = Math.PI / 2 - Math.PI / 6; // Start from left (60 degrees left of down)
        double endAngle = Math.PI / 2 + Math.PI / 6;   // End at right (60 degrees right of down)
        
        for (int i = 0; i < numShots; i++) {
            double angle = startAngle + (endAngle - startAngle) * i / (numShots - 1);
            double dirX = Math.cos(angle);
            double dirY = Math.sin(angle);
            
            createDirectionalShot(fireX, fireY, dirX, dirY, 300);
        }
    }
    
    /**
     * Pattern 4: Fire shots in a circular pattern (all directions)
     */
    private void fireCircularPattern(double fireX, double fireY) {
        // Fire 8 shots in a circle
        int numShots = 8;
        for (int i = 0; i < numShots; i++) {
            double angle = 2 * Math.PI * i / numShots;
            double dirX = Math.cos(angle);
            double dirY = Math.sin(angle);
            
            createDirectionalShot(fireX, fireY, dirX, dirY, 250);
        }
    }
    
    /**
     * Pattern 5: Fire a large splitting shot that explodes in the middle of the screen
     */
    private void fireSplittingPattern(double fireX, double fireY) {
        try {
            // Create a large shot that will split at screen center (Y = 350)
            double splitYPosition = 350; // Middle of screen
            int largeRadius = 16; // Large size (2x normal)
            int splitCount = 12; // Split into 12 smaller shots
            double speed = 200; // Moderate speed
            
            // Fire the large splitting shot downward
            BossShotEntity largeSplittingShot = new BossShotEntity(
                game,
                (int)fireX,
                (int)fireY,
                0, // Direction X (straight down)
                1, // Direction Y (straight down)
                speed,
                largeRadius,
                true, // Can split
                splitYPosition,
                splitCount
            );
            
            game.addEntity(largeSplittingShot);
            
        } catch (Exception e) {
            System.err.println("Error creating splitting shot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create a boss shot with specific direction and speed
     */
    private void createDirectionalShot(double startX, double startY, double dirX, double dirY, double speed) {
        try {
            BossShotEntity shot = new BossShotEntity(game, (int)startX, (int)startY, dirX, dirY, speed);
            game.addEntity(shot);
        } catch (Exception e) {
            System.err.println("Error creating directional shot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Take damage from a player shot
     * 
     * @param damage The amount of damage to take
     */
    public void takeDamage(int damage, String playerId) {
        try {
            currentHP -= damage;
            
            // Update phase based on HP
            int newPhase;
            if (currentHP > maxHP * 0.66) {
                newPhase = 1;
            } else if (currentHP > maxHP * 0.33) {
                newPhase = 2;
            } else {
                newPhase = 3;
            }
            
            if (newPhase != phase) {
                phase = newPhase;
                System.out.println("Boss Phase " + phase + " activated!");
            }
            
            if (currentHP <= 0) {
                // Boss defeated
                createBossExplosion();
                game.addScore(playerId, 1000 * round);
                game.addSkillPoints(playerId, 5 * round);
                game.notifyBossDefeated(playerId);
                game.removeEntity(this);
                used = true;
            }
        } catch (Exception e) {
            System.err.println("Error in boss takeDamage: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create explosion effect when boss is defeated
     */
    private void createBossExplosion() {
        try {
            // Create multiple explosions for dramatic effect
            for (int i = 0; i < 5; i++) {
                int explosionX = (int)(x + (Math.random() - 0.5) * 100);
                int explosionY = (int)(y + (Math.random() - 0.5) * 100);
                game.createExplosion(explosionX, explosionY, 80.0);
            }
        } catch (Exception e) {
            System.err.println("Error creating boss explosion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Draw this boss with health bar
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        try {
            // Draw the boss sprite with very large size (6x scale)
            Graphics2D g2d = (Graphics2D) g;
            if (sprite != null) {
                // Draw boss sprite at 6x size, perfectly centered
                int bossWidth = 300;  // 6x size (더욱 크게)
                int bossHeight = 300; // 6x size (더욱 크게)
                int drawX = (int)x - bossWidth/2; // Center horizontally
                int drawY = (int)y - bossHeight/2; // Center vertically
                
                g2d.drawImage(sprite.getImage(), drawX, drawY, drawX + bossWidth, drawY + bossHeight, 
                             0, 0, sprite.getWidth(), sprite.getHeight(), null);
            }
            
            // Phase indicator (above boss)
            g2d.setColor(Color.YELLOW);
            g2d.setFont(g2d.getFont().deriveFont(16f));
            g2d.drawString("Phase " + phase, (int)x - 25, (int)y - 170);
            
            // Draw health bar (much smaller, below boss)
            g2d.setColor(Color.RED);
            g2d.fillRect((int)x - 50, (int)y + 170, 100, 8);
            
            // Health bar foreground
            g2d.setColor(Color.GREEN);
            int healthWidth = (int)(100 * ((double)currentHP / maxHP));
            g2d.fillRect((int)x - 50, (int)y + 170, healthWidth, 8);
            
            // Health bar border
            g2d.setColor(Color.WHITE);
            g2d.drawRect((int)x - 50, (int)y + 170, 100, 8);
        } catch (Exception e) {
            System.err.println("Error drawing boss: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Notification that this boss has collided with another entity
     * 
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        // Boss doesn't take collision damage from shots
        // Shot damage is handled in ShotEntity.collidedWith()
        
        // Alien과 충돌했을 때 Alien이 반대방향으로 이동하도록 함
        // (AlienEntity.collidedWith에서 처리됨)
    }
    
    /**
     * Get the boss's current HP
     * 
     * @return The current HP
     */
    public int getCurrentHP() {
        return currentHP;
    }
    
    /**
     * Get the boss's maximum HP
     * 
     * @return The maximum HP
     */
    public int getMaxHP() {
        return maxHP;
    }
    
    /**
     * Get the boss's current phase
     * 
     * @return The current phase (1-3)
     */
    public int getPhase() {
        return phase;
    }
    
    /**
     * Override getBounds to provide larger hitbox for the bigger boss
     * 
     * @return The bounds of the boss entity
     */
    public java.awt.Rectangle getBounds() {
        // Return bounds matching the visual size (6x scale = 300x300)
        return new java.awt.Rectangle((int)x - 150, (int)y - 150, 300, 300);
    }

    @Override
    protected String snapshotMetadata() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("hp", Integer.toString(currentHP));
        map.put("max", Integer.toString(maxHP));
        map.put("phase", Integer.toString(phase));
        return MetadataCodec.encode(map);
    }

    @Override
    protected void applySnapshotMetadata(String metadata) {
        Map<String, String> map = MetadataCodec.decode(metadata);
        if (map.isEmpty()) {
            return;
        }
        try {
            currentHP = Integer.parseInt(map.getOrDefault("hp", Integer.toString(currentHP)));
        } catch (NumberFormatException ignore) {
            // keep previous value
        }
        try {
            maxHP = Integer.parseInt(map.getOrDefault("max", Integer.toString(maxHP)));
        } catch (NumberFormatException ignore) {
            // keep previous value
        }
        try {
            phase = Integer.parseInt(map.getOrDefault("phase", Integer.toString(phase)));
        } catch (NumberFormatException ignore) {
            // keep previous value
        }
    }
}
