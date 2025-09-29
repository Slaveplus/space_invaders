package org.newdawn.spaceinvaders.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * An entity representing a shot fired by the player's ship
 * 
 * @author Kevin Glass
 */
public class ShotEntity extends Entity {
	/** The vertical speed at which the players shot moves */
	private double moveSpeed = -300;
	/** The game in which this entity exists */
	private Game game;
	/** True if this shot has been "used", i.e. its hit something */
	private boolean used = false;
	/** True if this is an alien shot (moves downward) */
	private boolean isAlienShot = false;
	/** True if this is a skill drop */
	private boolean isSkillDrop = false;
	/** Skill type for skill drops (0: Invincible, 1: Piercing) */
	private int skillType = -1;
	/** Skill value for skill drops */
	private int skillValue = 0;
	
	/**
	 * Create a new shot from the player
	 * 
	 * @param game The game in which the shot has been created
	 * @param sprite The sprite representing this shot
	 * @param x The initial x location of the shot
	 * @param y The initial y location of the shot
	 */
	public ShotEntity(Game game,String sprite,int x,int y) {
		super(sprite,x,y);
		
		this.game = game;
		
		dy = moveSpeed;
	}
	
	/**
	 * Create a new shot (player or alien)
	 * 
	 * @param game The game in which the shot has been created
	 * @param sprite The sprite representing this shot
	 * @param x The initial x location of the shot
	 * @param y The initial y location of the shot
	 * @param isAlienShot True if this is an alien shot
	 */
	public ShotEntity(Game game,String sprite,int x,int y,boolean isAlienShot) {
		super(sprite,x,y);
		
		this.game = game;
		this.isAlienShot = isAlienShot;
		
		if (isAlienShot) {
			dy = 300; // Move downward for alien shots
		} else {
			dy = moveSpeed; // Move upward for player shots
		}
	}
	
	/**
	 * Create a new shot (player, alien, or skill drop)
	 * 
	 * @param game The game in which the shot has been created
	 * @param sprite The sprite representing this shot
	 * @param x The initial x location of the shot
	 * @param y The initial y location of the shot
	 * @param isAlienShot True if this is an alien shot
	 * @param skillType Skill type for skill drops (0: Invincible, 1: Piercing)
	 * @param skillValue Skill value for skill drops
	 */
	public ShotEntity(Game game,String sprite,int x,int y,boolean isAlienShot,int skillType,int skillValue) {
		super(sprite,x,y);
		
		this.game = game;
		this.isAlienShot = isAlienShot;
		this.skillType = skillType;
		this.skillValue = skillValue;
		
		if (skillType >= 0) {
			// This is a skill drop
			isSkillDrop = true;
			dy = 150; // Skill drops move downward toward player
		} else if (isAlienShot) {
			dy = 300; // Alien shots move downward
		} else {
			dy = moveSpeed; // Player shots move upward (negative speed)
		}
	}

	/**
	 * Request that this shot moved based on time elapsed
	 * 
	 * @param delta The time that has elapsed since last move
	 */
	public void move(long delta) {
		// proceed with normal move
		super.move(delta);
		
		// if we shot off the screen, remove ourselfs
		if (isSkillDrop) {
			// Skill drops: remove when they go below screen
			if (y > 700) {
				game.removeEntity(this);
			}
		} else if (isAlienShot) {
			// Alien shots: remove when they go below screen
			if (y > 700) {
				game.removeEntity(this);
			}
		} else {
			// Player shots: remove when they go above screen
			if (y < -100) {
				game.removeEntity(this);
			}
		}
	}
	
	/**
	 * Draw this entity with special coloring for alien shots
	 * 
	 * @param g The graphics context on which to draw
	 */
	public void draw(Graphics g) {
		if (isSkillDrop) {
			// Draw skill drop
			Graphics2D g2d = (Graphics2D) g;
			
			if (skillType == 0) { // Invincible skill
				// Golden shield-like appearance
				g2d.setColor(new Color(255, 215, 0, 200)); // Gold with transparency
				g2d.fillOval((int)x - 8, (int)y - 8, 16, 16);
				
				// Inner circle
				g2d.setColor(new Color(255, 255, 0, 150));
				g2d.fillOval((int)x - 6, (int)y - 6, 12, 12);
				
				// Border
				g2d.setColor(new Color(255, 140, 0));
				g2d.setStroke(new java.awt.BasicStroke(2));
				g2d.drawOval((int)x - 8, (int)y - 8, 16, 16);
				
				// Cross symbol for invincibility
				g2d.setColor(Color.WHITE);
				g2d.setStroke(new java.awt.BasicStroke(2));
				g2d.drawLine((int)x - 3, (int)y, (int)x + 3, (int)y);
				g2d.drawLine((int)x, (int)y - 3, (int)x, (int)y + 3);
				
			} else if (skillType == 1) { // Piercing skill
				// Red piercing arrow-like appearance
				g2d.setColor(new Color(255, 0, 0, 200)); // Red with transparency
				g2d.fillOval((int)x - 8, (int)y - 8, 16, 16);
				
				// Inner circle
				g2d.setColor(new Color(255, 100, 100, 150));
				g2d.fillOval((int)x - 6, (int)y - 6, 12, 12);
				
				// Border
				g2d.setColor(new Color(180, 0, 0));
				g2d.setStroke(new java.awt.BasicStroke(2));
				g2d.drawOval((int)x - 8, (int)y - 8, 16, 16);
				
				// Arrow symbol for piercing
				g2d.setColor(Color.WHITE);
				g2d.setStroke(new java.awt.BasicStroke(2));
				// Arrow pointing down
				g2d.drawLine((int)x, (int)y - 4, (int)x, (int)y + 2);
				g2d.drawLine((int)x - 2, (int)y, (int)x, (int)y + 2);
				g2d.drawLine((int)x + 2, (int)y, (int)x, (int)y + 2);
				
			} else if (skillType == 2) { // Triple shot skill
				// Blue triple shot appearance
				g2d.setColor(new Color(0, 100, 255, 200)); // Blue with transparency
				g2d.fillOval((int)x - 8, (int)y - 8, 16, 16);
				
				// Inner circle
				g2d.setColor(new Color(100, 150, 255, 150));
				g2d.fillOval((int)x - 6, (int)y - 6, 12, 12);
				
				// Border
				g2d.setColor(new Color(0, 50, 150));
				g2d.setStroke(new java.awt.BasicStroke(2));
				g2d.drawOval((int)x - 8, (int)y - 8, 16, 16);
				
				// Three dots symbol for triple shot
				g2d.setColor(Color.WHITE);
				g2d.setStroke(new java.awt.BasicStroke(2));
				g2d.fillOval((int)x - 4, (int)y - 2, 3, 3);
				g2d.fillOval((int)x - 1, (int)y - 2, 3, 3);
				g2d.fillOval((int)x + 2, (int)y - 2, 3, 3);
			}
			
			// Add a subtle glow effect
			g2d.setColor(new Color(255, 255, 255, 50));
			g2d.fillOval((int)x - 10, (int)y - 10, 20, 20);
			
		} else if (isAlienShot) {
			// Draw circular alien energy projectile
			Graphics2D g2d = (Graphics2D) g;
			
			// Calculate center position for perfect circle
			int centerX = (int) x + sprite.getWidth() / 2;
			int centerY = (int) y + sprite.getHeight() / 2;
			int radius = 8; // Fixed radius for perfect circle
			
			// Outer glow effect (larger circle)
			g2d.setColor(new Color(255, 100, 100, 60)); // Red glow
			g2d.fillOval(centerX - radius - 3, centerY - radius - 3, (radius + 3) * 2, (radius + 3) * 2);
			
			// Main projectile body (perfect circle)
			g2d.setColor(new Color(255, 50, 50, 220)); // Bright red core
			g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
			
			// Inner bright core (smaller circle)
			g2d.setColor(new Color(255, 200, 200, 180)); // Light red center
			g2d.fillOval(centerX - radius + 2, centerY - radius + 2, (radius - 2) * 2, (radius - 2) * 2);
			
			// Bright white center (smallest circle)
			g2d.setColor(new Color(255, 255, 255, 200)); // White hot center
			g2d.fillOval(centerX - radius + 4, centerY - radius + 4, (radius - 4) * 2, (radius - 4) * 2);
			
			// Add subtle energy trail effect (smaller circles behind)
			g2d.setColor(new Color(255, 100, 100, 80));
			g2d.fillOval(centerX - radius + 1, centerY - radius + 6, (radius - 1) * 2, (radius - 1) * 2);
			g2d.setColor(new Color(255, 100, 100, 40));
			g2d.fillOval(centerX - radius + 2, centerY - radius + 10, (radius - 2) * 2, (radius - 2) * 2);
		} else {
			// Draw normal player shot
			super.draw(g);
		}
	}
	
	/**
	 * Notification that this shot has collided with another
	 * entity
	 * 
	 * @parma other The other entity with which we've collided
	 */
	public void collidedWith(Entity other) {
		// prevents double kills, if we've already hit something,
		// don't collide
		if (used) {
			return;
		}
		
		if (isSkillDrop) {
			// Skill drop: if we've hit the player ship, add to inventory
			if (other instanceof ShipEntity) {
				// remove the skill drop
				game.removeEntity(this);
				
				// add skill to inventory instead of immediately activating
				game.addSkillToInventory(skillType, skillValue);
				used = true;
			}
		} else if (isAlienShot) {
			// Alien shot: if we've hit the player ship, damage it
			if (other instanceof ShipEntity) {
				// remove the shot
				game.removeEntity(this);
				
				// notify the game that the player has been hit
				game.notifyDeath();
				used = true;
			}
		} else {
			// Player shot: if we've hit an alien, damage it!
			if (other instanceof AlienEntity) {
				// damage the alien (using player's attack power)
				AlienEntity alien = (AlienEntity) other;
				alien.takeDamage(game.getPlayerAttackPower());
				
				// Check if player has piercing shots
				if (!game.hasPiercingShots()) {
					// remove the shot only if not piercing
					game.removeEntity(this);
					used = true;
				}
				// If piercing, the shot continues through aliens
			}
		}
	}
}