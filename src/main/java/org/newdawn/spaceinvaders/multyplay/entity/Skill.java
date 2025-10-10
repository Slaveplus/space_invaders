package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;

import java.io.IOException;
import java.io.InputStream;

/**
 * 스킬 드랍 아이템을 관리하는 클래스
 */
public class Skill extends Entity {
    /** The game in which this skill exists */
    private MultiplayerGameContext game;
    /** True if this skill has been "used", i.e. collected by player */
    private boolean used = false;
    /** Skill type (0: Invincible, 1: Piercing, 2: Triple Shot) */
    private int skillType;
    /** Skill value/duration */
    private int skillValue;
    /** Movement speed */
    private double moveSpeed = 140;
    
    /** PNG skill icon */
    private BufferedImage skillIcon;
    
    /**
     * Create a new skill drop
     * 
     * @param game The game in which the skill has been created
     * @param sprite The sprite representing this skill
     * @param x The initial x location of the skill
     * @param y The initial y location of the skill
     * @param skillType The type of skill (0: Invincible, 1: Piercing, 2: Triple Shot)
     * @param skillValue The value/duration of the skill
     */
    public Skill(MultiplayerGameContext game, String sprite, int x, int y, int skillType, int skillValue) {
        super(sprite, x, y);
        
        this.game = game;
        this.skillType = skillType;
        this.skillValue = skillValue;
        
        // Load PNG skill icon
        loadSkillIcon();
        
        // Move downward in a straight line
        dx = 0;
        dy = moveSpeed;
    }
    
    /**
     * Request that this skill moved based on time elapsed
     * 
     * @param delta The time that has elapsed since last move
     */
    public void move(long delta) {
        // proceed with normal move
        super.move(delta);
        
        // if we've gone off the screen, remove ourself
        if (y > 700) {
            game.removeEntity(this);
        }
    }
    
    /**
     * Notification that this skill has collided with another entity
     * 
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        // prevents double collection, if we've already been collected,
        // don't collide
        if (used) {
            return;
        }
        
        // if we've hit the player ship, add to inventory
        if (other instanceof ShipEntity) {
            // remove the skill drop
            game.removeEntity(this);
            
            // add skill to inventory
            ShipEntity ship = (ShipEntity) other;
            game.addSkillToInventory(ship.getOwnerId(), skillType, skillValue);
            used = true;
        }
    }
    
    /**
     * Get the skill type
     * 
     * @return The skill type
     */
    public int getSkillType() {
        return skillType;
    }
    
    /**
     * Get the skill value
     * 
     * @return The skill value
     */
    public int getSkillValue() {
        return skillValue;
    }
    
    /**
     * Check if this skill has been used
     * 
     * @return True if the skill has been used
     */
    public boolean isUsed() {
        return used;
    }
    
    /**
     * Load the appropriate PNG skill icon based on skill type
     */
    private void loadSkillIcon() {
        String iconPath;
        switch (skillType) {
            case 0: // Attack Power
                iconPath = "sprites/Skill/Icon.1_45.png";
                break;
            case 1: // Attack Speed
                iconPath = "sprites/Skill/Icon.6_26.png";
                break;
            case 2: // HP Recovery
                iconPath = "sprites/Skill/Icon.7_11.png";
                break;
            case 3: // Missile
                iconPath = "sprites/Skill/Missile.png";
                break;
            default:
                iconPath = "sprites/Skill/Icon.1_45.png";
                break;
        }
        
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(iconPath);
            if (is != null) {
                skillIcon = ImageIO.read(is);
                is.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to load skill icon: " + iconPath);
            e.printStackTrace();
        }
    }
    
    /**
     * Draw the skill with PNG icons
     */
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        if (skillIcon != null) {
            // Draw the PNG skill icon directly
            g2d.drawImage(skillIcon, (int)x, (int)y, null);
        } else {
            // Fallback: Draw simple colored background if PNG failed to load
            g2d.setColor(Color.BLACK);
            g2d.fillRect((int)x, (int)y, 32, 32);
            g2d.setColor(Color.GRAY);
            g2d.drawRect((int)x, (int)y, 32, 32);
            
            // Draw skill type text as fallback
            g2d.setColor(Color.WHITE);
            g2d.drawString("S" + skillType, (int)x + 8, (int)y + 20);
        }
    }
    
}
