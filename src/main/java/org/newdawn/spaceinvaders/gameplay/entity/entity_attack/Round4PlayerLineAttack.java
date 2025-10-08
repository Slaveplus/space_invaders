package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Round 4 Boss Player Line Attack - 4round3.gif를 사용한 플레이어 라인 공격
 * 플레이어 라인에서 생성되어 플레이어를 공격하며, 맞으면 플레이어 체력의 절반이 닳음
 */
public class Round4PlayerLineAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The attack image */
    private java.awt.Image attackImage;
    /** Duration of the attack */
    private long attackDuration = 4000; // 4초간 유지
    /** Start time */
    private long startTime;
    /** Attack size */
    private int attackWidth = 160;
    private int attackHeight = 160;
    /** Player line Y position */
    private int playerLineY;

    /**
     * Create a new player line attack entity
     * 
     * @param game The game in which this entity exists
     * @param x The initial x location of this entity (on player line)
     * @param y The initial y location of this entity (player line Y)
     */
    public Round4PlayerLineAttack(Game game, int x, int y) {
        super("sprites/Boss_Attack/4round3.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        this.playerLineY = y;
        
        // Attack starts stationary on player line
        this.dx = 0;
        this.dy = 0;
        
        // Load the attack image
        loadAttackImage();
    }

    /**
     * Load the attack image
     */
    private void loadAttackImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/4round3.gif");
            if (imageUrl != null) {
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                System.out.println("🟣 Successfully loaded 4round3 attack GIF: 4round3.gif via ClassLoader.getResource()");
            } else {
                System.err.println("🟣 Failed to find 4round3.gif resource.");
                attackImage = java.awt.Toolkit.getDefaultToolkit().createImage("src/main/resources/sprites/Boss_Attack/4round3.gif");
                System.out.println("🟣 Attempted direct loading of 4round3.gif: " + (attackImage != null));
            }
            // Ensure image is loaded before proceeding
            if (attackImage != null) {
                java.awt.MediaTracker mt = new java.awt.MediaTracker(new java.awt.Canvas());
                mt.addImage(attackImage, 0);
                mt.waitForAll();
                if (mt.isErrorAny()) {
                    System.err.println("🟣 Error loading 4round3.gif with MediaTracker.");
                    attackImage = null;
                } else {
                    System.out.println("🟣 4round3 attack image dimensions (after MediaTracker): " + attackImage.getWidth(null) + "x" + attackImage.getHeight(null));
                }
            }
        } catch (Exception e) {
            System.err.println("🟣 Failed to load 4round3 attack GIF image: " + e.getMessage());
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
        long elapsed = System.currentTimeMillis() - startTime;
        
        // Attack stays stationary on player line - no movement towards player
        // Just wait for the duration
        
        // Check if attack duration has expired
        if (elapsed > attackDuration) {
            System.out.println("🟣 4round3 attack completed after " + attackDuration + "ms");
            game.removeEntity(this);
            return;
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
                
                System.out.println("🟣 Drawing 4round3 attack at X:" + drawX + " Y:" + drawY + " Size:" + attackWidth + "x" + attackHeight);
            } else {
                // Fallback rendering if image failed to load
                g2d.setColor(Color.RED);
                g2d.fillRect((int)x - attackWidth / 2, (int)y - attackHeight / 2, attackWidth, attackHeight);
                g2d.setColor(Color.YELLOW);
                g2d.drawRect((int)x - attackWidth / 2, (int)y - attackHeight / 2, attackWidth, attackHeight);
                System.out.println("🟣 Drawing 4round3 attack fallback at X:" + (int)x + " Y:" + (int)y + " Size:" + attackWidth + "x" + attackHeight);
            }
        } catch (Exception e) {
            System.err.println("Error drawing 4round3 attack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Notification that this entity collided with another
     * 
     * @param other The entity with which we collided
     */
    public void collidedWith(Entity other) {
        // If we've hit the player's ship, deal moderate damage
        if (other instanceof org.newdawn.spaceinvaders.gameplay.entity.ShipEntity) {
            try {
                // Get player's current HP
                org.newdawn.spaceinvaders.gameplay.entity.ShipEntity player = (org.newdawn.spaceinvaders.gameplay.entity.ShipEntity) other;
                java.lang.reflect.Field hpField = player.getClass().getDeclaredField("currentHP");
                hpField.setAccessible(true);
                int currentHP = (Integer) hpField.get(player);
                
                // Calculate small fixed damage instead of percentage
                int smallDamage = 5; // 고정 5 데미지
                
                System.out.println("🟣 4round3 attack hit player! Small damage: " + smallDamage + " (from " + currentHP + " HP)");
                
                // Deal small damage
                game.notifyPlayerDamaged(smallDamage);
                game.removeEntity(this);
                
            } catch (Exception e) {
                System.err.println("🟣 Error dealing small damage: " + e.getMessage());
                // Fallback to tiny damage if reflection fails
                game.notifyPlayerDamaged(2);
                game.removeEntity(this);
            }
        }
    }
}

