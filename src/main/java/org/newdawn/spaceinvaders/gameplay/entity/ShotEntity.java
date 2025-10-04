package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.gameplay.core.Rules;
import org.newdawn.spaceinvaders.gameplay.render.BulletRenderer;

import java.io.InputStream;

/**
 * An entity representing a shot fired by the player's ship
 * 
 * @author Kevin Glass
 */
public class ShotEntity extends Entity {
	/** The vertical speed at which the players shot moves */
	private double moveSpeed = Rules.BULLET_PLAYER_SPEED_Y;
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

	// Reusable strokes to avoid per-frame allocations
	// kept for backward compatibility; BulletRenderer uses its own strokes
	// private static final BasicStroke STROKE_2PX = new BasicStroke(2);

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
			dy = Rules.BULLET_ENEMY_SPEED_Y; // Move downward for alien shots
		} else {
			dy = moveSpeed; // Move upward for player shots
		}
	}
	
	/**
	 * Create a new shot with piercing ability
	 *
	 * @param game The game in which the shot has been created
	 * @param sprite The sprite representing this shot
	 * @param x The initial x location of the shot
	 * @param y The initial y location of the shot
	 * @param isAlienShot True if this is an alien shot
	 * @param hasPiercing True if this shot has piercing ability
	 */
	public ShotEntity(Game game,String sprite,int x,int y,boolean isAlienShot,boolean hasPiercing) {
		super(sprite,x,y);

		this.game = game;
		this.isAlienShot = isAlienShot;
		this.hasPiercing = hasPiercing;

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
			// This is a skill drop - move straight down
			isSkillDrop = true;

			// Move straight down at constant speed
			dx = 0; // No horizontal movement
			dy = 150; // Move downward at 150 pixels per second
		} else if (isAlienShot) {
			dy = Rules.BULLET_ENEMY_SPEED_Y; // Alien shots move downward
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
			// Draw skill drop using PNG images
			Graphics2D g2d = (Graphics2D) g;
			BulletRenderer.drawSkillDrop(g2d, (int) x, (int) y, skillType < 0 ? 0 : skillType);

			// Load and draw the appropriate PNG image based on skill type
			BufferedImage skillImage = loadSkillImage(skillType);
			if (skillImage != null) {
				// Draw the PNG skill image, scaled to 32x32
				int imageSize = 32;
				int drawX = (int)x - imageSize/2;
				int drawY = (int)y - imageSize/2;
				g2d.drawImage(skillImage, drawX, drawY, imageSize, imageSize, null);
			} else {
				// Fallback: Draw simple colored background if PNG failed to load
				g2d.setColor(Color.BLACK);
				g2d.fillRect((int)x - 16, (int)y - 16, 32, 32);
				g2d.setColor(Color.GRAY);
				g2d.drawRect((int)x - 16, (int)y - 16, 32, 32);

				// Draw skill type text as fallback
				g2d.setColor(Color.WHITE);
				g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
				g2d.drawString("S" + skillType, (int)x - 8, (int)y + 4);
			}

			// Add a subtle glow effect
			g2d.setColor(new Color(255, 255, 255, 50));
			g2d.fillOval((int)x - 18, (int)y - 18, 36, 36);

		} else if (isAlienShot) {
			Graphics2D g2d = (Graphics2D) g;
			int w = (sprite != null ? sprite.getWidth() : 16);
			int h = (sprite != null ? sprite.getHeight() : 16);
			int centerX = (int) x + w / 2;
			int centerY = (int) y + h / 2;
			BulletRenderer.drawAlienBullet(g2d, centerX, centerY);
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
				System.out.println("Skill drop collected! Type: " + skillType + ", Value: " + skillValue);
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
				
				// Create heat effect at hit location
				HeatEffectEntity heatEffect = new HeatEffectEntity(game, (int)x, (int)y);
				game.addEntity(heatEffect);

				// Check if this shot has piercing ability or player has piercing shots
				if (!hasPiercing && !game.hasPiercingShots()) {
					// remove the shot only if not piercing
					game.removeEntity(this);
					used = true;
				}
				// If piercing, the shot continues through aliens
			} else if (other instanceof BossEntity) {
				// Player shot: if we've hit a boss, damage it!
				BossEntity boss = (BossEntity) other;
				boss.takeDamage(game.getPlayerAttackPower());

				// Create heat effect at hit location
				HeatEffectEntity heatEffect = new HeatEffectEntity(game, (int)x, (int)y);
				game.addEntity(heatEffect);

				// Boss shots are always destroyed on hit (no piercing through boss)
				game.removeEntity(this);
				used = true;
			}
		}
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
	 * Load skill image based on skill type
	 *
	 * @param skillType The skill type (0-3)
	 * @return BufferedImage of the skill icon
	 */
	private BufferedImage loadSkillImage(int skillType) {
		String imagePath;
		switch (skillType) {
			case 0: // Attack Power
				imagePath = "sprites/Skill/1.png";
				break;
			case 1: // Attack Speed
				imagePath = "sprites/Skill/2.png";
				break;
			case 2: // HP Recovery
				imagePath = "sprites/Skill/3.png";
				break;
			case 3: // Missile
				imagePath = "sprites/Skill/4.png";
				break;
			default:
				imagePath = "sprites/Skill/1.png";
				break;
		}

		try {
			InputStream is = getClass().getClassLoader().getResourceAsStream(imagePath);
			if (is != null) {
				BufferedImage image = ImageIO.read(is);
				is.close();
				return image;
			}
		} catch (Exception e) {
			System.err.println("Failed to load skill image: " + imagePath);
		}
		return null;
	}

}