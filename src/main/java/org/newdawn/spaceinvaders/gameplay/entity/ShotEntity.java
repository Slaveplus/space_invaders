package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

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
	/** True if this shot has piercing ability */
	private boolean hasPiercing = false;
	
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
	 * @param isAlienShot True if this is an alien shot (moves downward)
	 */
	public ShotEntity(Game game,String sprite,int x,int y,boolean isAlienShot) {
		super(sprite,x,y);
		
		this.game = game;
		this.isAlienShot = isAlienShot;
		
		if (isAlienShot) {
			dy = moveSpeed;
		} else {
			dy = -moveSpeed;
		}
	}
	
	/**
	 * Create a new skill drop shot
	 * 
	 * @param game The game in which the shot has been created
	 * @param sprite The sprite representing this shot
	 * @param x The initial x location of the shot
	 * @param y The initial y location of the shot
	 * @param skillType The type of skill (0: Invincible, 1: Piercing)
	 * @param skillValue The value of the skill
	 */
	public ShotEntity(Game game,String sprite,int x,int y,int skillType,int skillValue) {
		super(sprite,x,y);
		
		this.game = game;
		this.isSkillDrop = true;
		this.skillType = skillType;
		this.skillValue = skillValue;
		
		dy = moveSpeed;
	}
	
	/**
	 * Request that this shot move itself based on an elapsed ammount of
	 * time
	 * 
	 * @param delta The time that has elapsed since last move (ms)
	 */
	public void move(long delta) {
		// proceed with normal move
		super.move(delta);
		
		// if we shot off the screen, remove ourselfs
		if (y < -100 || y > 600) {
			game.removeEntity(this);
		}
	}
	
	/**
	 * Notification that this shot has collided with another
	 * entity
	 * 
	 * @param other The other entity
	 */
	public void collidedWith(Entity other) {
		// prevents double kills, if we've already hit something,
		// don't collide
		if (used) {
			return;
		}
		
		// Debug: Log collision detection
		System.out.println("ShotEntity collidedWith called - other entity: " + other.getClass().getSimpleName());
		System.out.println("Full class name: " + other.getClass().getName());
		System.out.println("Is BossEntity? " + other.getClass().getSimpleName().equals("BossEntity"));
		
		// if we've hit an alien, kill it!
		if (other instanceof AlienEntity) {
			System.out.println("Shot hit AlienEntity! Removing entities...");
			
			// Create heat effect at impact point
			game.createHeatEffect((int)other.getX(), (int)other.getY(), 50.0);
			
			// remove the affected entities
			game.removeEntity(this);
			game.removeEntity(other);
			
			// notify the game that the alien has been killed
			game.notifyAlienKilled();
			used = true;
		}
		
		// if we've hit a boss, damage it!
		if (other.getClass().getSimpleName().equals("BossEntity")) {
			System.out.println("Shot hit BossEntity! Damaging boss...");
			
			// Create heat effect at impact point
			game.createHeatEffect((int)other.getX(), (int)other.getY(), 80.0);
			
			// remove the shot
			game.removeEntity(this);
			
			// damage the boss (assuming BossEntity has a takeDamage method)
			try {
				// Use reflection to call takeDamage method if it exists
				java.lang.reflect.Method takeDamageMethod = other.getClass().getMethod("takeDamage", int.class);
				takeDamageMethod.invoke(other, game.getPlayerAttackPower());
				System.out.println("Boss took " + game.getPlayerAttackPower() + " damage!");
			} catch (Exception e) {
				System.out.println("Could not damage boss: " + e.getMessage());
			}
			
			used = true;
		}
		
		// if we've hit the player's ship, damage it
		if (other instanceof ShipEntity) {
			// remove the shot
			game.removeEntity(this);
			
			// notify the game that the player has been damaged
			game.notifyPlayerDamaged(1);
			used = true;
		}
		
		// if this is a skill drop and we've hit the player's ship
		if (isSkillDrop && other instanceof ShipEntity) {
			// remove the skill drop
			game.removeEntity(this);
			
			// TODO: Implement skill collection logic
			System.out.println("Skill collected: Type=" + skillType + ", Value=" + skillValue);
			used = true;
		}
	}
	
	/**
	 * Draw this entity to the graphics context provided
	 * 
	 * @param g The graphics context on which to draw
	 */
	public void draw(Graphics g) {
		if (isSkillDrop) {
			// Draw skill drop with special color
			g.setColor(Color.CYAN);
			g.fillOval((int)x - 5, (int)y - 5, 10, 10);
			g.setColor(Color.WHITE);
			g.drawOval((int)x - 5, (int)y - 5, 10, 10);
		} else if (hasPiercing) {
			// Draw piercing shot with special color
			g.setColor(Color.YELLOW);
			g.fillRect((int)x - 2, (int)y - 10, 4, 20);
		} else {
			// Draw normal shot
			super.draw(g);
		}
	}
	
	/**
	 * Check if this shot has piercing ability
	 * 
	 * @return True if this shot can pierce through enemies
	 */
	public boolean hasPiercing() {
		return hasPiercing;
	}
	
	/**
	 * Set piercing ability for this shot
	 * 
	 * @param hasPiercing True if this shot can pierce through enemies
	 */
	public void setPiercing(boolean hasPiercing) {
		this.hasPiercing = hasPiercing;
	}
	
	/**
	 * Check if this is an alien shot
	 * 
	 * @return True if this is an alien shot
	 */
	public boolean isAlienShot() {
		return isAlienShot;
	}
	
	/**
	 * Check if this is a skill drop
	 * 
	 * @return True if this is a skill drop
	 */
	public boolean isSkillDrop() {
		return isSkillDrop;
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
}
