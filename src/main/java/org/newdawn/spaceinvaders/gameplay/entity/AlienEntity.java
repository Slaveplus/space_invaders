package org.newdawn.spaceinvaders.gameplay.entity;

import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.gameplay.sprite.Sprite;
import org.newdawn.spaceinvaders.gameplay.sprite.SpriteStore;

/**
 * An entity which represents one of our space invader aliens.
 * 
 * @author Kevin Glass
 */
public class AlienEntity extends Entity {
	/** The speed at which the alient moves horizontally */
	private double moveSpeed = 75;
	/** The game in which the entity exists */
	private Game game;
	/** The animation frames */
	private Sprite[] frames = new Sprite[4];
	/** The time since the last frame change took place */
	private long lastFrameChange;
	/** The frame duration in milliseconds, i.e. how long any given frame of animation lasts */
	private long frameDuration = 250;
	/** The current frame of animation being displayed */
	private int frameNumber;
	/** The time at which this alien last fired a shot */
	public long lastFire = 0;
	/** The interval between alien shots (ms) */
	public long firingInterval = 2000;
	/** The alien's current HP */
	private int currentHP = 2;
	/** The alien's maximum HP */
	private int maxHP = 2;
	/** Individual movement direction (true = right, false = left) */
	private boolean movingRight = Math.random() < 0.5;
	/** Individual vertical movement direction (true = down, false = up) */
	private boolean movingDown = Math.random() < 0.5;
	/** Time since last direction change */
	private long lastDirectionChange = 0;
	/** Minimum time between direction changes */
	private long directionChangeInterval = 2000;
	
	/**
	 * Create a new alien entity
	 * 
	 * @param game The game in which this entity is being created
	 * @param x The intial x location of this alien
	 * @param y The intial y location of this alient
	 */
	public AlienEntity(Game game,int x,int y) {
		super("sprites/alien.gif",x,y);
		
		// setup the animatin frames
		frames[0] = sprite;
		frames[1] = SpriteStore.get().getSprite("sprites/alien2.gif");
		frames[2] = sprite;
		frames[3] = SpriteStore.get().getSprite("sprites/alien3.gif");
		
		this.game = game;
		
		// Apply round-based difficulty scaling with balanced progression
		int round = game.getCurrentRound();
		
		// HP scaling: 2, 3, 4, 6, 8 for rounds 1-5 (increased to balance player attack power)
		maxHP = 2 + round + (round > 3 ? round - 3 : 0); // Extra HP for later rounds
		currentHP = maxHP;
		
		// Speed scaling: 75, 85, 95, 105, 115 for rounds 1-5 (more gradual increase)
		moveSpeed = 75 + (round * 10);
		
		// Firing interval scaling: 2500, 2000, 1500, 1000, 700, 500, 400 for rounds 1-7+
		// More aggressive progression with exponential decrease
		if (round <= 3) {
			firingInterval = 2500 - (round * 500); // 2500, 2000, 1500
		} else if (round <= 6) {
			firingInterval = 1000 - ((round - 3) * 100); // 1000, 900, 700
		} else {
			firingInterval = Math.max(300, 700 - ((round - 6) * 100)); // 600, 500, 400, 300...
		}
		
		// Add some randomness to firing intervals (±20% variation)
		double randomFactor = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
		firingInterval = (long)(firingInterval * randomFactor);
		
		// Set random initial movement direction
		if (movingRight) {
			dx = moveSpeed;
		} else {
			dx = -moveSpeed;
		}
		
		if (movingDown) {
			dy = moveSpeed * 0.3; // Slower vertical movement
		} else {
			dy = -moveSpeed * 0.3;
		}
	}

	/**
	 * Request that this alien moved based on time elapsed
	 * 
	 * @param delta The time that has elapsed since last move
	 */
	public void move(long delta) {
		// since the move tells us how much time has passed
		// by we can use it to drive the animation, however
		// its the not the prettiest solution
		lastFrameChange += delta;
		
		// if we need to change the frame, update the frame number
		// and flip over the sprite in use
		if (lastFrameChange > frameDuration) {
			// reset our frame change time counter
			lastFrameChange = 0;
			
			// update the frame
			frameNumber++;
			if (frameNumber >= frames.length) {
				frameNumber = 0;
			}
			
			sprite = frames[frameNumber];
		}
		
		// Update direction change timer
		lastDirectionChange += delta;
		
		// Check screen boundaries and change direction
		if (x < 10) {
			changeDirection();
			x = 10; // Keep alien on screen
		} else if (x > 750) {
			changeDirection();
			x = 750;
		}
		
		if (y < 50) {
			changeVerticalDirection();
			y = 50;
		} else if (y > 450) {
			changeVerticalDirection();
			y = 450;
		}
		
		// Random direction changes for more chaotic movement
		if (lastDirectionChange > directionChangeInterval) {
			if (Math.random() < 0.1) { // 10% chance to change direction randomly
				changeDirection();
			}
			lastDirectionChange = 0;
		}
		
		// proceed with normal move
		super.move(delta);
	}
	
	/**
	 * Update the game logic related to aliens
	 */
	public void doLogic() {
		// Individual aliens no longer need group logic
		// Each alien handles its own movement independently
	}
	
	/**
	 * Attempt to fire a shot from this alien
	 */
	public void tryToFire() {
		// check that we have waited long enough to fire
		if (System.currentTimeMillis() - lastFire < firingInterval) {
			return;
		}
		
		int round = game.getCurrentRound();
		boolean shouldFire = false;
		
		// Progressive firing conditions based on round
		if (round <= 2) {
			// Early rounds: Fire from medium range (350 pixels vertically) - increased from 200
			if (Math.abs(y - 550) < 350) {
				shouldFire = Math.random() < 0.6; // 60% chance when in range
			}
		} else if (round <= 4) {
			// Mid rounds: Fire from long range (450 pixels vertically) - increased from 300
			if (Math.abs(y - 550) < 450) {
				// Add some randomness to make it less predictable
				shouldFire = Math.random() < 0.8; // 80% chance when in range
			}
		} else {
			// Later rounds: More aggressive firing from longer range
			if (Math.abs(y - 550) < 500) { // increased from 400
				// Higher chance to fire and can fire from further away
				shouldFire = Math.random() < 0.9; // 90% chance when in range
			}
			// Very close aliens (within 150 pixels) always fire
			if (Math.abs(y - 550) < 150) {
				shouldFire = true;
			}
		}
		
		// Additional firing conditions for higher rounds
		if (round >= 5) {
			// High rounds: Chance to fire even when not perfectly aligned
			if (Math.random() < 0.05) { // 5% chance per frame regardless of position
				shouldFire = true;
			}
		}
		
		if (shouldFire) {
			lastFire = System.currentTimeMillis();
			game.addAimedAlienShot((int)(x + 10), (int)(y + 30), (int)x);
		}
	}
	
	/**
	 * Take damage from a player shot
	 * 
	 * @param damage The amount of damage to take
	 */
	public void takeDamage(int damage) {
		currentHP -= damage;
		if (currentHP <= 0) {
			// Alien is destroyed
			game.removeEntity(this);
			game.notifyAlienKilled();
		}
	}
	
	/**
	 * Get the alien's current HP
	 * 
	 * @return The current HP
	 */
	public int getCurrentHP() {
		return currentHP;
	}
	
	/**
	 * Get the alien's maximum HP
	 * 
	 * @return The maximum HP
	 */
	public int getMaxHP() {
		return maxHP;
	}
	
	/**
	 * Change horizontal movement direction
	 */
	private void changeDirection() {
		movingRight = !movingRight;
		dx = movingRight ? moveSpeed : -moveSpeed;
	}
	
	/**
	 * Change vertical movement direction
	 */
	private void changeVerticalDirection() {
		movingDown = !movingDown;
		dy = movingDown ? moveSpeed * 0.3 : -moveSpeed * 0.3;
	}
	
	/**
	 * Called when this alien collides with another alien
	 */
	public void collideWithAlien() {
		changeDirection();
		// Also randomly change vertical direction sometimes
		if (Math.random() < 0.3) {
			changeVerticalDirection();
		}
	}
	
	/**
	 * Notification that this alien has collided with another entity
	 * 
	 * @param other The other entity
	 */
	public void collidedWith(Entity other) {
		// Handle collision with another alien
		if (other instanceof AlienEntity) {
			collideWithAlien();
		}
	}
}