package org.newdawn.spaceinvaders.gameplay.entity;

import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Near Entity - 보스 전에 나타나는 중간 보스 몬스터
 * 1near.png, 2near.png, 3near.png, 4near.png를 사용
 */
public class NearEntity extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The near monster image */
    private java.awt.Image nearImage;
    /** Current HP */
    private int currentHP;
    /** Maximum HP */
    private int maxHP;
    /** Movement speed */
    private double moveSpeed = 50;
    /** Movement direction */
    private boolean movingRight = true;
    /** Near monster round number */
    private int round;
    /** Near monster size */
    private int nearWidth = 120;
    private int nearHeight = 120;
    /** Time since last shot */
    private long lastShot = 0;
    /** Shot interval */
    private long shotInterval = 2000; // 2초마다 공격
    /** Monster ID for unique timing */
    private int monsterId;

    /**
     * Get the near sprite path based on round number
     * 
     * @param round The round number (1,3,5,7 = near rounds before boss rounds 1,2,3,4)
     * @return The sprite path for the near monster
     */
    private static String getNearSpriteForRound(int round) {
        switch (round) {
            case 1:
                return "sprites/Boss/1near.png"; // 1라운드 보스 전
            case 3:
                return "sprites/Boss/2near.png"; // 2라운드 보스 전
            case 5:
                return "sprites/Boss/3near.png"; // 3라운드 보스 전
            case 7:
                return "sprites/Boss/4near.png"; // 4라운드 보스 전
            default:
                return "sprites/Boss/1near.png";
        }
    }

    /**
     * Get the boss round number from near round number
     * 
     * @param nearRound The near round number (1,3,5,7)
     * @return The corresponding boss round number (1,2,3,4)
     */
    private static int getBossRoundFromNearRound(int nearRound) {
        switch (nearRound) {
            case 1:
                return 1; // 1라운드 보스 전
            case 3:
                return 2; // 2라운드 보스 전
            case 5:
                return 3; // 3라운드 보스 전
            case 7:
                return 4; // 4라운드 보스 전
            default:
                return 1;
        }
    }

    /**
     * Create a new near entity
     * 
     * @param game The game in which the near monster has been created
     * @param x The initial x location of the near monster
     * @param y The initial y location of the near monster
     * @param round The round number for scaling
     * @param monsterId Unique ID for this monster (0-5)
     */
    public NearEntity(Game game, int x, int y, int round, int monsterId) {
        super(getNearSpriteForRound(round), x, y);
        
        this.game = game;
        this.round = round;
        this.monsterId = monsterId;
        
        // Scale near monster stats based on boss round (1,2,3,4)
        int bossRound = getBossRoundFromNearRound(round);
        maxHP = 20 + (bossRound * 15); // 35, 50, 65, 80
        currentHP = maxHP;
        
        // Scale movement speed
        moveSpeed = 50 + (bossRound * 10);
        
        // Set unique shot interval based on monster ID
        // Each monster has different attack timing: 3000, 3500, 4000, 4500, 5000, 5500 ms (slower attacks)
        shotInterval = 3000 + (monsterId * 500);
        
        // Load the near image
        loadNearImage();
        
        System.out.println("Near Monster " + monsterId + " spawned! Round " + round + ", HP: " + currentHP + "/" + maxHP + ", Attack Interval: " + shotInterval + "ms");
    }

    /**
     * Load the near image
     */
    private void loadNearImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource(getNearSpriteForRound(round));
            if (imageUrl != null) {
                nearImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                System.out.println("✅ Successfully loaded near monster image: " + getNearSpriteForRound(round));
            } else {
                System.err.println("❌ Failed to find near monster image: " + getNearSpriteForRound(round));
                nearImage = java.awt.Toolkit.getDefaultToolkit().createImage("src/main/resources/" + getNearSpriteForRound(round));
                System.out.println("✅ Attempted direct loading: " + (nearImage != null));
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to load near monster image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Request that this near monster moved based on time elapsed
     * 
     * @param delta The time that has elapsed since last move
     */
    public void move(long delta) {
        try {
            // Store old position
            double oldX = x;
            
            // Move left and right
            if (movingRight) {
                x += moveSpeed * delta / 1000.0;
                if (x > 700) {
                    movingRight = false;
                }
            } else {
                x -= moveSpeed * delta / 1000.0;
                if (x < 100) {
                    movingRight = true;
                }
            }
            
            // Check collision with other near monsters
            checkNearMonsterCollision();
            
            // Try to shoot
            tryShoot();
            
        } catch (Exception e) {
            System.err.println("Error in near monster move: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check collision with other near monsters
     */
    private void checkNearMonsterCollision() {
        try {
            for (Entity entity : game.getGameStateManager().getEntities()) {
                if (entity != this && entity.getClass().getSimpleName().equals("NearEntity")) {
                    NearEntity otherNear = (NearEntity) entity;
                    
                    // Calculate distance between monsters
                    double dx = this.x - otherNear.x;
                    double dy = this.y - otherNear.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    // Collision detection (monster size is 120x120, so radius is 60)
                    double collisionDistance = 120; // Monster width/height
                    
                    if (distance < collisionDistance) {
                        // Push monsters apart
                        double pushForce = (collisionDistance - distance) / 2;
                        double pushX = (dx / distance) * pushForce;
                        double pushY = (dy / distance) * pushForce;
                        
                        // Apply push force
                        this.x += pushX;
                        this.y += pushY;
                        otherNear.x -= pushX;
                        otherNear.y -= pushY;
                        
                        // Keep monsters within screen bounds
                        this.x = Math.max(60, Math.min(740, this.x));
                        this.y = Math.max(60, Math.min(140, this.y));
                        otherNear.x = Math.max(60, Math.min(740, otherNear.x));
                        otherNear.y = Math.max(60, Math.min(140, otherNear.y));
                        
                        // Change direction if colliding
                        this.movingRight = !this.movingRight;
                        otherNear.movingRight = !otherNear.movingRight;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in near monster collision check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Try to shoot at player
     */
    private void tryShoot() {
        try {
            // Check if 3 seconds have passed since game start
            if (!game.canEnemiesAttack()) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            
            // Check if enough time has passed since last shot
            if (currentTime - lastShot < shotInterval) {
                return;
            }
            
            // Create shot moving towards player with round-specific sprite
            String shotSprite = getShotSpriteForRound(round);
            ShotEntity shot = new ShotEntity(game, shotSprite, (int)x, (int)y + nearHeight/2);
            shot.setVerticalMovement(300); // Move down at 300 pixels/sec
            shot.setNearMonsterShot(true); // Mark as near monster shot for smaller size
            
            game.addEntity(shot);
            
            // Update last shot time
            lastShot = currentTime;
            
        } catch (Exception e) {
            System.err.println("Error in near monster shoot: " + e.getMessage());
            lastShot = System.currentTimeMillis();
        }
    }

    /**
     * Take damage from a player shot
     * 
     * @param damage The amount of damage to take
     */
    public void takeDamage(int damage) {
        try {
            currentHP -= damage;
            System.out.println("🎯 Near Monster " + monsterId + " took " + damage + " damage! HP: " + currentHP + "/" + maxHP);
            
            if (currentHP <= 0) {
                System.out.println("🎯 Near Monster " + monsterId + " DEFEATED!");
                
                // Near monster defeated (no explosion)
                int bossRound = getBossRoundFromNearRound(round);
                game.addScore(100 * bossRound);
                game.addSkillPoints(2 + bossRound); // 3, 4, 5, 6 skill points
                
                // Random chance to drop skill or skill points
                double dropChance = Math.random();
                System.out.println("🎁 Drop chance: " + dropChance + " (need <0.2 for skill, <0.5 for points)");
                
                if (dropChance < 0.2) { // 20% chance to drop skill
                    System.out.println("🎁 SKILL DROP TRIGGERED! Calling dropRandomSkill...");
                    game.dropRandomSkill((int)x, (int)y);
                } else if (dropChance < 0.5) { // 30% chance to drop skill points
                    System.out.println("🎁 SKILL POINTS DROP TRIGGERED! Calling dropRandomSkillPoints...");
                    game.dropRandomSkillPoints((int)x, (int)y);
                } else {
                    System.out.println("🎁 No drop this time (chance: " + dropChance + ")");
                }
                
                System.out.println("🎯 Removing Near Monster " + monsterId + " from game...");
                game.removeEntity(this);
                
                // Check if all near monsters are defeated
                System.out.println("🎯 Calling checkAllNearMonstersDefeated()...");
                game.checkAllNearMonstersDefeated();
            }
        } catch (Exception e) {
            System.err.println("Error in near monster takeDamage: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Draw this near monster with health bar
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            
            if (nearImage != null) {
                int drawX = (int)x - nearWidth / 2;
                int drawY = (int)y - nearHeight / 2;
                
                g2d.drawImage(nearImage, drawX, drawY, drawX + nearWidth, drawY + nearHeight, 
                             0, 0, nearImage.getWidth(null), nearImage.getHeight(null), null);
            } else {
                // Fallback rendering if image failed to load
                g2d.setColor(Color.ORANGE);
                g2d.fillRect((int)x - nearWidth / 2, (int)y - nearHeight / 2, nearWidth, nearHeight);
                g2d.setColor(Color.YELLOW);
                g2d.drawRect((int)x - nearWidth / 2, (int)y - nearHeight / 2, nearWidth, nearHeight);
            }
            
            // Draw health bar (above near monster)
            g2d.setColor(Color.RED);
            g2d.fillRect((int)x - 30, (int)y - nearHeight/2 - 15, 60, 6);
            
            // Health bar foreground
            g2d.setColor(Color.GREEN);
            int healthWidth = (int)(60 * ((double)currentHP / maxHP));
            g2d.fillRect((int)x - 30, (int)y - nearHeight/2 - 15, healthWidth, 6);
            
            // Health bar border
            g2d.setColor(Color.WHITE);
            g2d.drawRect((int)x - 30, (int)y - nearHeight/2 - 15, 60, 6);
            
        } catch (Exception e) {
            System.err.println("Error drawing near monster: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Notification that this near monster has collided with another entity
     * 
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        // Near monster doesn't take collision damage from shots
        // Shot damage is handled in ShotEntity.collidedWith()
    }

    /**
     * Get the near monster's current HP
     * 
     * @return The current HP
     */
    public int getCurrentHP() {
        return currentHP;
    }

    /**
     * Get the near monster's maximum HP
     * 
     * @return The maximum HP
     */
    public int getMaxHP() {
        return maxHP;
    }

    /**
     * Get the near monster's round number
     * 
     * @return The round number
     */
    public int getRound() {
        return round;
    }

    /**
     * Override getBounds to provide hitbox for the near monster
     * 
     * @return The bounds of the near monster entity
     */
    public java.awt.Rectangle getBounds() {
        return new java.awt.Rectangle((int)x - nearWidth/2, (int)y - nearHeight/2, nearWidth, nearHeight);
    }
    
    /**
     * Get the shot sprite for the given near round
     * 
     * @param nearRound The near round number (1,3,5,7)
     * @return The sprite path for the shot
     */
    private String getShotSpriteForRound(int nearRound) {
        switch (nearRound) {
            case 1: return "sprites/Boss_Attack/ice ball.gif"; // 1라운드: ice ball.gif
            case 3: return "sprites/shot.gif";                 // 3라운드: 검정색 구체 (기본 shot.gif)
            case 5: return "sprites/shot.gif";                 // 5라운드: 검정색 구체 (기본 shot.gif)
            case 7: return "sprites/Boss_Attack/5round1.gif";  // 7라운드: 5round1.gif
            default: return "sprites/shot.gif"; // Default fallback
        }
    }
}
