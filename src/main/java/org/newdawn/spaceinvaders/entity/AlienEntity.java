package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.sprite.Sprite;
import org.newdawn.spaceinvaders.sprite.SpriteStore;

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
		
		// HP scaling: 1, 4, 10, 18, 30 for rounds 1-5 (더 도전적인 체력 증가)
		switch (round) {
			case 1: maxHP = 1; break;
			case 2: maxHP = 4; break;
			case 3: maxHP = 10; break;
			case 4: maxHP = 18; break;
			case 5: maxHP = 30; break;
			default: maxHP = 30 + ((round - 5) * 15); // 6라운드+: 45, 60, 75...
		}
		currentHP = maxHP;
		
		// Speed scaling: 75, 85, 95, 105, 115 for rounds 1-5 (more gradual increase)
		moveSpeed = 75 + (round * 10);
		
		// Firing interval scaling: 2000, 1500, 1000, 700, 500, 400, 300 for rounds 1-7+
		// More aggressive progression with faster decrease
		if (round == 1) {
			firingInterval = 2000; // 1라운드: 2초
		} else if (round <= 3) {
			firingInterval = 2000 - (round * 500); // 1500, 1000
		} else if (round <= 6) {
			firingInterval = 700 - ((round - 3) * 100); // 600, 500, 400
		} else {
			firingInterval = Math.max(200, 400 - ((round - 6) * 50)); // 350, 300, 250, 200...
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
		
		// Check distance to player and avoid getting too close
		avoidPlayer();
		
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
		
		// 모든 라운드에서 전체 범위에서 공격 가능 (거리 제한 없음)
		// 라운드별로 공격 확률만 차등 적용
		if (round <= 2) {
			// 초기 라운드: 낮은 공격 확률
			shouldFire = Math.random() < 0.3; // 30% 확률
		} else if (round <= 4) {
			// 중간 라운드: 중간 공격 확률
			shouldFire = Math.random() < 0.5; // 50% 확률
		} else if (round <= 6) {
			// 고급 라운드: 높은 공격 확률
			shouldFire = Math.random() < 0.7; // 70% 확률
		} else {
			// 최고 라운드: 매우 높은 공격 확률
			shouldFire = Math.random() < 0.85; // 85% 확률
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
			// 7% 확률로 스킬 드랍
			if (Math.random() < 0.07) {
				// 랜덤 스킬 타입 선택 (0: 공격력, 1: 공격속도, 2: 체력회복, 3: 미사일)
				int skillType = (int)(Math.random() * 4);
				// 스킬 지속 시간 설정
				int skillValue = 10000; // 10초
				
				// 스킬 드랍 생성 (플레이어 쪽으로 이동)
				game.createSkillDrop((int)x, (int)y, skillType, skillValue);
			}
			
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
	 * Avoid getting too close to the player
	 */
	private void avoidPlayer() {
		try {
			// Get player position
			int playerX = game.getShipX();
			int playerY = game.getShipY();
			
			// Calculate distance to player
			double dxToPlayer = playerX - x;
			double dyToPlayer = playerY - y;
			double distanceToPlayer = Math.sqrt(dxToPlayer * dxToPlayer + dyToPlayer * dyToPlayer);
			
			// Minimum safe distance (120 pixels) - 더 넉넉한 거리
			double minDistance = 120;
			
			// If too close to player, move away
			if (distanceToPlayer < minDistance && distanceToPlayer > 0) {
				// Calculate direction away from player
				double moveAwayX = -dxToPlayer / distanceToPlayer;
				double moveAwayY = -dyToPlayer / distanceToPlayer;
				
				// Apply movement away from player (override current movement)
				this.dx = moveAwayX * moveSpeed * 1.5; // Move away faster
				this.dy = moveAwayY * moveSpeed * 0.5;
			}
		} catch (Exception e) {
			// Ignore errors in player avoidance
		}
	}
	
	/**
	 * Notification that this alien has collided with another entity
	 * 
	 * @param other The other entity
	 */
	public void collidedWith(Entity other) {
		// 적들끼리는 통과되도록 충돌 처리하지 않음
		// (AlienEntity와의 충돌은 무시)
	}
}