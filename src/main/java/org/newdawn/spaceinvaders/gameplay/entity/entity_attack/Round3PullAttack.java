package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Round 3 Boss Pull Attack - 3round3.gif를 사용한 끌어당기기 공격
 * 플레이어 라인에 생성되어 플레이어를 끌어당기는 공격 (데미지 없음)
 */
public class Round3PullAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The attack image */
    private java.awt.Image attackImage;
    /** Pull strength */
    private double pullStrength = 0.3;
    /** Duration of the attack */
    private long attackDuration = 6000; // 6초
    /** Start time */
    private long startTime;
    /** Attack size */
    private int attackWidth = 300; // 150 → 300 (2배)
    private int attackHeight = 400; // 200 → 400 (2배)
    /** Pull range */
    private double pullRange = 300.0;

    /**
     * Create a new pull attack entity
     * 
     * @param game The game in which this entity exists
     * @param x The initial x location of this entity
     * @param y The initial y location of this entity
     */
    public Round3PullAttack(Game game, int x, int y) {
        super("sprites/Boss_Attack/3round3.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        
        // No movement - stays in place
        this.dx = 0;
        this.dy = 0;
        
        // Load the attack image
        loadAttackImage();
        
        // 블랙홀 생성 시 3round.gif 공격을 블랙홀 쪽으로 발사
        launchBlackHoleAttack();
    }

    /**
     * Load the attack image
     */
    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/3round3.gif");
            if (imageUrl != null) {
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                System.out.println("🔴 Successfully loaded pull attack GIF: 3round3.gif via ClassLoader.getResource()");
            } else {
                System.err.println("🔴 Failed to find 3round3.gif resource.");
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage("src/main/resources/sprites/Boss_Attack/3round3.gif");
                System.out.println("🔴 Attempted direct loading of 3round3.gif: " + (attackImage != null));
            }
            // Ensure image is loaded before proceeding
            if (attackImage != null) {
                java.awt.MediaTracker mt = new java.awt.MediaTracker(new java.awt.Canvas());
                mt.addImage(attackImage, 0);
                mt.waitForAll();
                if (mt.isErrorAny()) {
                    System.err.println("🔴 Error loading 3round3.gif with MediaTracker.");
                    attackImage = null;
                } else {
                    System.out.println("🔴 Pull attack image dimensions (after MediaTracker): " + attackImage.getWidth(null) + "x" + attackImage.getHeight(null));
                }
            }
        } catch (Exception e) {
            System.err.println("🔴 Failed to load pull attack GIF image: " + e.getMessage());
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
        // Check if attack duration has expired
        if (System.currentTimeMillis() - startTime > attackDuration) {
            System.out.println("🔴 Pull attack expired after " + attackDuration + "ms");
            game.removeEntity(this);
            return;
        }
        
        // Pull player towards this attack
        pullPlayer();
    }

    /**
     * Pull the player towards this attack
     */
    private void pullPlayer() {
        try {
            // Get player entity
            org.newdawn.spaceinvaders.gameplay.entity.ShipEntity player = game.getPlayerShip();
            if (player == null) {
                return;
            }
            
            double playerX = player.getX();
            double playerY = player.getY();
            
            // Calculate distance to player
            double dx = this.x - playerX;
            double dy = this.y - playerY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            // Only pull if player is within range
            if (distance <= pullRange && distance > 0) {
                // Calculate pull direction
                double pullX = (dx / distance) * pullStrength;
                double pullY = (dy / distance) * pullStrength;
                
                // Apply pull to player
                player.pullTowards(pullX, pullY);
                
                System.out.println("🔴 Pull attack pulling player - Distance: " + String.format("%.2f", distance) + 
                                 " Pull strength: " + pullStrength + " Direction: (" + String.format("%.2f", pullX) + ", " + String.format("%.2f", pullY) + ")");
            }
        } catch (Exception e) {
            System.err.println("Error in pull attack: " + e.getMessage());
            e.printStackTrace();
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
                
                System.out.println("🔴 Drawing pull attack at X:" + drawX + " Y:" + drawY + " Size:" + attackWidth + "x" + attackHeight);
            } else {
                // Fallback rendering if image failed to load
                g2d.setColor(Color.BLUE);
                g2d.fillRect((int)x - attackWidth / 2, (int)y - attackHeight / 2, attackWidth, attackHeight);
                g2d.setColor(Color.CYAN);
                g2d.drawRect((int)x - attackWidth / 2, (int)y - attackHeight / 2, attackWidth, attackHeight);
                System.out.println("🔴 Drawing pull attack fallback at X:" + (int)x + " Y:" + (int)y + " Size:" + attackWidth + "x" + attackHeight);
            }
            
        } catch (Exception e) {
            System.err.println("Error drawing pull attack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Launch black hole attack using 3round.gif
     */
    private void launchBlackHoleAttack() {
        try {
            // 3round.gif를 사용한 3갈래 블랙홀 공격 생성
            // 블랙홀 위치에서 시작하여 블랙홀 쪽으로 이동하는 공격
            
            // 중앙 공격
            Round3BlackHoleAttack centerAttack = new Round3BlackHoleAttack(game, (int)this.x, (int)this.y);
            game.addEntity(centerAttack);
            
            // 왼쪽 공격 (블랙홀에서 왼쪽으로 100픽셀 떨어진 위치)
            Round3BlackHoleAttack leftAttack = new Round3BlackHoleAttack(game, (int)this.x - 100, (int)this.y);
            game.addEntity(leftAttack);
            
            // 오른쪽 공격 (블랙홀에서 오른쪽으로 100픽셀 떨어진 위치)
            Round3BlackHoleAttack rightAttack = new Round3BlackHoleAttack(game, (int)this.x + 100, (int)this.y);
            game.addEntity(rightAttack);
            
            System.out.println("🔴 Triple 3round.gif attacks launched from boss towards black hole area at X:" + (int)this.x + " Y:" + (int)this.y);
            
        } catch (Exception e) {
            System.err.println("Error launching black hole attack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Notification that this entity collided with another
     * 
     * @param other The entity with which we collided
     */
    public void collidedWith(Entity other) {
        // Pull attack doesn't deal damage, only pulls
        // No collision handling needed
    }
}
