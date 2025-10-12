package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.attack.MagneticFieldEntity;

/**
 * The entity that represents the players ship
 * 
 * @author Kevin Glass
 */
public class ShipEntity extends Entity {
	/** The game in which the ship exists */
	private MultiplayerGameContext game;
	
	/**
	 * Create a new entity to represent the players ship
	 *  
	 * @param game The game in which the ship is being created
	 * @param ref The reference to the sprite to show for the ship
	 * @param x The initial x location of the player's ship
	 * @param y The initial y location of the player's ship
	 */
	public ShipEntity(MultiplayerGameContext game,String ref,int x,int y) {
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

	@Override
	public void draw(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		
		// Check if player is invincible
		if (game.isPlayerInvincible(getOwnerId())) {
			// Apply transparency effect for invincibility
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		}
		
		// Draw the ship
		sprite.draw(g2d, (int) x, (int) y);
		
		// Reset composite if it was changed
		if (game.isPlayerInvincible(getOwnerId())) {
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		}
	}

	private void applyMagneticFieldEffects() {
		List<Entity> entities = game.getEntities();
		for (Entity entity : entities) {
			if (!(entity instanceof MagneticFieldEntity)) {
				continue;
			}
			MagneticFieldEntity field = (MagneticFieldEntity) entity;
			if (!field.isActive()) {
				continue;
			}

			double dxToField = field.getX() - this.x;
			double dyToField = field.getY() - this.y;
			double distance = Math.sqrt(dxToField * dxToField + dyToField * dyToField);
			if (distance <= 0 || distance > field.getCurrentRadius()) {
				continue;
			}

			double force = field.getFieldStrength() * (1.0 - distance / field.getCurrentRadius());
			double normalizedX = dxToField / distance;
			double normalizedY = dyToField / distance;

			this.dx += normalizedX * force * 0.5;
			this.dy += normalizedY * force * 0.5;

			double maxVelocity = 0.8;
			double currentSpeed = Math.sqrt(this.dx * this.dx + this.dy * this.dy);
			if (currentSpeed > maxVelocity) {
				this.dx = (this.dx / currentSpeed) * maxVelocity;
				this.dy = (this.dy / currentSpeed) * maxVelocity;
			}
		}
	}

	public void pullTowards(double pullX, double pullY) {
		this.x += pullX;
		this.y += pullY;

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
			game.notifyDeath(getOwnerId());
		}
	}

	@Override
	public Rectangle getBounds() {
		int width = sprite != null ? sprite.getWidth() : 33;
		int height = sprite != null ? sprite.getHeight() : 23;
		int hitboxWidth = (int) Math.max(10, width * 0.8);
		int hitboxHeight = (int) Math.max(10, height * 0.8);
		int offsetX = (width - hitboxWidth) / 2;
		int offsetY = (height - hitboxHeight) / 2;
		return new Rectangle((int) x + offsetX, (int) y + offsetY, hitboxWidth, hitboxHeight);
	}
}
