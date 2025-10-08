package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * The entity that represents the players ship
 * 
 * @author Kevin Glass
 */
public class ShipEntity extends Entity {
	/** The game in which the ship exists */
	private Game game;
	
	/**
	 * Create a new entity to represent the players ship
	 *  
	 * @param game The game in which the ship is being created
	 * @param ref The reference to the sprite to show for the ship
	 * @param x The initial x location of the player's ship
	 * @param y The initial y location of the player's ship
	 */
	public ShipEntity(Game game,String ref,int x,int y) {
		super(ref,x,y);
		
		this.game = game;
	}
	
	/**
	 * Request that the ship move itself based on an elapsed ammount of
	 * time
	 * 
	 * @param delta The time that has elapsed since last move (ms)
	 */
	public void move(long delta) {
		// Apply magnetic field effects before normal movement
		applyMagneticFieldEffects();
		
		// if we're moving left and have reached the left hand side
		// of the screen, don't move
		if ((dx < 0) && (x < 10)) {
			return;
		}
		// if we're moving right and have reached the right hand side
		// of the screen, don't move
		if ((dx > 0) && (x > 750)) {
			return;
		}
		
		super.move(delta);
	}
	
	/**
	 * Apply magnetic field effects to the ship
	 */
	private void applyMagneticFieldEffects() {
		// Get all entities and check for magnetic fields
		java.util.List<Entity> entities = game.getEntities();
		
		for (Entity entity : entities) {
			if (entity.getClass().getSimpleName().equals("MagneticFieldEntity")) {
				org.newdawn.spaceinvaders.gameplay.entity.entity_attack.MagneticFieldEntity magneticField = 
					(org.newdawn.spaceinvaders.gameplay.entity.entity_attack.MagneticFieldEntity) entity;
				
				if (!magneticField.isActive()) continue;
				
				// Calculate distance to magnetic field center
				double dx_to_field = magneticField.getX() - this.x;
				double dy_to_field = magneticField.getY() - this.y;
				double distance = Math.sqrt(dx_to_field * dx_to_field + dy_to_field * dy_to_field);
				
				// Check if ship is within magnetic field range
				if (distance <= magneticField.getCurrentRadius() && distance > 0) {
					// Calculate force based on distance and field strength
					double force = magneticField.getFieldStrength() * (1.0 - distance / magneticField.getCurrentRadius());
					
					// Normalize direction vector
					double normalizedX = dx_to_field / distance;
					double normalizedY = dy_to_field / distance;
					
					// Apply magnetic force to ship velocity
					double magneticForceX = normalizedX * force * 0.5; // 0.5 is scaling factor for ship
					double magneticForceY = normalizedY * force * 0.5;
					
					// Update ship velocity
					this.dx += magneticForceX;
					this.dy += magneticForceY;
					
					// Limit maximum velocity to prevent ship from moving too fast
					double maxVelocity = 0.8;
					double currentSpeed = Math.sqrt(this.dx * this.dx + this.dy * this.dy);
					if (currentSpeed > maxVelocity) {
						this.dx = (this.dx / currentSpeed) * maxVelocity;
						this.dy = (this.dy / currentSpeed) * maxVelocity;
					}
				}
			}
		}
	}
	
	/**
	 * Draw this entity to the graphics context provided with invincibility effect
	 * 
	 * @param g The graphics context on which to draw
	 */
	@Override
	public void draw(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		
		// Check if player is invincible
		if (game.isPlayerInvincible()) {
			// Apply transparency effect for invincibility
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		}
		
		// Draw the ship
		sprite.draw(g2d, (int) x, (int) y);
		
		// Reset composite if it was changed
		if (game.isPlayerInvincible()) {
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		}
	}
	
	/**
	 * Pull the ship towards a specific direction
	 * 
	 * @param pullX The x-direction pull force
	 * @param pullY The y-direction pull force
	 */
	public void pullTowards(double pullX, double pullY) {
		// Apply pull force to ship position
		this.x += pullX;
		this.y += pullY;
		
		// Ensure ship stays within screen bounds
		if (this.x < 10) {
			this.x = 10;
		}
		if (this.x > 750) {
			this.x = 750;
		}
		if (this.y < 50) {
			this.y = 50;
		}
		if (this.y > 500) {
			this.y = 500;
		}
	}

	/**
	 * Notification that the player's ship has collided with something
	 * 
	 * @param other The entity with which the ship has collided
	 */
	public void collidedWith(Entity other) {
		// if its an alien, notify the game that the player
		// is dead
		if (other instanceof AlienEntity) {
			game.notifyDeath();
		}
	}
}