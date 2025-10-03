package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;

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
	/** Time of last collision */
	private long lastCollisionTime = 0;
	/** Minimum time between collision reactions (ms) */
	private static final long COLLISION_COOLDOWN = 500;
	
	/**
	 * Get the alien sprite path based on round number
	 * 
	 * @param round The round number
	 * @return The sprite path for the alien
	 */
	private static String getAlienSpriteForRound(int round) {
		switch (round) {
			case 1:
				return "sprites/Boss/1round_small.png";
			case 2:
				return "sprites/Boss/1round_small.png"; // alien2.gif 대신 사용
			case 3:
				return "sprites/Boss/1round_small.png"; // alien3.gif 대신 사용
			case 4:
				return "sprites/Boss/1round_small.png"; // alien.gif 대신 사용
			default:
				// For rounds 5 and above, use 1round_small.png
				return "sprites/Boss/1round_small.png";
		}
	}
	
	/**
	 * Create a new alien entity
	 * 
	 * @param game The game in which this entity is being created
	 * @param x The intial x location of this alien
	 * @param y The intial y location of this alient
	 */
	public AlienEntity(Game game,int x,int y) {
		// Get sprite based on round
		super(getAlienSpriteForRound(game.getCurrentRound()), x, y);
		
		this.game = game;
		
		// Apply round-based difficulty scaling with balanced progression
		int round = game.getCurrentRound();
		
		// setup the animation frames based on round
		frames[0] = sprite;
		frames[1] = sprite; // Same sprite for all frames
		frames[2] = sprite;
		frames[3] = sprite;
		
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
			firingInterval = 1000; // 1라운드: 1초 (2배 빨라짐)
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
		
		// 더 다양한 초기 이동 패턴 설정
		// 1. 좌우 방향은 랜덤하게
		// 2. 수직 방향도 약간의 랜덤 요소 추가
		// 3. 속도에도 약간의 랜덤 요소 추가
		
		double speedVariation = 0.8 + (Math.random() * 0.4); // 80% ~ 120% 속도 변화
		double actualMoveSpeed = moveSpeed * speedVariation;
		
		// 좌우 이동 설정
		if (movingRight) {
			dx = actualMoveSpeed;
		} else {
			dx = -actualMoveSpeed;
		}
		
		// 수직 이동도 약간 추가 (더 자연스러운 움직임을 위해)
		// 30% 확률로 위아래로도 움직임
		if (Math.random() < 0.3) {
			dy = (Math.random() - 0.5) * actualMoveSpeed * 0.3; // -15% ~ +15% 속도로 위아래 이동
		} else {
			dy = 0; // 대부분은 좌우로만 움직임
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
		
		// 주기적으로 방향을 바꾸는 로직 추가 (더 역동적인 움직임)
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastDirectionChange > directionChangeInterval) {
			// 20% 확률로 방향 변경
			if (Math.random() < 0.2) {
				changeDirection();
				// 10% 확률로 수직 방향도 변경
				if (Math.random() < 0.1) {
					changeVerticalDirection();
				}
				lastDirectionChange = currentTime;
			}
		}
		
		// Check screen boundaries and change direction (좌우로 맵 전체를 왕복)
		if (x < 10) {
			movingRight = true;
			dx = Math.abs(dx); // 현재 속도 유지하되 양수로
			x = 10; // Keep alien on screen
		} else if (x > 750) {
			movingRight = false;
			dx = -Math.abs(dx); // 현재 속도 유지하되 음수로
			x = 750;
		}
		
		// Y 위치 제한 (맵 절반 이상 내려오지 못하게)
		if (y < 50) {
			y = 50;
		} else if (y > 300) { // 맵 절반(300) 이상 내려오지 못하게 제한
			y = 300;
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
	 * Override getBounds to provide scaled collision bounds
	 * 
	 * @return The bounds of the alien entity
	 */
	@Override
	public java.awt.Rectangle getBounds() {
		// 1라운드는 45% 축소, 나머지는 75%로 설정
		double scale = (game.getCurrentRound() == 1) ? 0.45 : 0.75;
		int scaledWidth = (int)(sprite.getWidth() * scale);
		int scaledHeight = (int)(sprite.getHeight() * scale);
		
		// Center the collision box
		int centerX = (int)x - scaledWidth/2;
		int centerY = (int)y - scaledHeight/2;
		
		return new java.awt.Rectangle(centerX, centerY, scaledWidth, scaledHeight);
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
		// 속도 변화도 약간 추가 (90% ~ 110%)
		double speedVariation = 0.9 + (Math.random() * 0.2);
		dx = movingRight ? moveSpeed * speedVariation : -moveSpeed * speedVariation;
	}
	
	/**
	 * Change vertical movement direction
	 */
	private void changeVerticalDirection() {
		movingDown = !movingDown;
		// 수직 속도도 약간의 랜덤 요소 추가
		double verticalSpeedVariation = 0.8 + (Math.random() * 0.4);
		dy = movingDown ? moveSpeed * 0.3 * verticalSpeedVariation : -moveSpeed * 0.3 * verticalSpeedVariation;
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
<<<<<<< HEAD
=======
	 * Check if current round is a boss round
	 * 
	 * @return true if current round is a boss round
	 */
	private boolean isBossRound() {
		// 보스 라운드는 5의 배수 (5, 10, 15, ...)
		return game.getCurrentRound() % 5 == 0;
	}
	
	/**
	 * Handle collision with another alien entity
	 * 
	 * @param other The other alien entity
	 */
	private void handleAlienCollision(Entity other) {
		// 충돌 쿨다운 체크 (너무 빈번한 반응 방지)
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastCollisionTime < COLLISION_COOLDOWN) {
			return;
		}
		lastCollisionTime = currentTime;
		
		// 다른 적과의 거리 계산
		double dxToOther = other.getX() - x;
		double dyToOther = other.getY() - y;
		double distanceToOther = Math.sqrt(dxToOther * dxToOther + dyToOther * dyToOther);
		
		if (distanceToOther > 0) {
			// 상대방으로부터 멀어지는 방향으로 이동
			double moveAwayX = -dxToOther / distanceToOther;
			double moveAwayY = -dyToOther / distanceToOther;
			
			// 반대방향으로 이동 (속도는 현재 속도보다 약간 빠르게)
			this.dx = moveAwayX * moveSpeed * 1.2;
			this.dy = moveAwayY * moveSpeed * 0.4;
			
			// 이동 방향 플래그 업데이트
			movingRight = (this.dx > 0);
			movingDown = (this.dy > 0);
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
	 * Draw this alien with scaled size
	 * 
	 * @param g The graphics context on which to draw
	 */
	@Override
	public void draw(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		if (sprite != null) {
			// 1라운드는 30% 축소, 나머지는 75%로 설정
			double scale = (game.getCurrentRound() == 1) ? 0.45 : 0.75; // 75% * 0.6 = 45% (30% 축소)
			int scaledWidth = (int)(sprite.getWidth() * scale);
			int scaledHeight = (int)(sprite.getHeight() * scale);
			
			// 중앙 정렬을 위한 오프셋 계산
			int drawX = (int)x - scaledWidth/2;
			int drawY = (int)y - scaledHeight/2;
			
			g2d.drawImage(sprite.getImage(), drawX, drawY, 
						 drawX + scaledWidth, drawY + scaledHeight,
						 0, 0, sprite.getWidth(), sprite.getHeight(), null);
		}
	}
	
	/**
	 * Notification that this alien has collided with another entity
	 * 
	 * @param other The other entity
	 */
	public void collidedWith(Entity other) {
		// 보스와 충돌했을 때 반대방향으로 이동
		if (other instanceof BossEntity) {
			// 보스와의 거리 계산
			double dxToBoss = other.getX() - x;
			double dyToBoss = other.getY() - y;
			double distanceToBoss = Math.sqrt(dxToBoss * dxToBoss + dyToBoss * dyToBoss);
			
			if (distanceToBoss > 0) {
				// 보스로부터 멀어지는 방향으로 이동
				double moveAwayX = -dxToBoss / distanceToBoss;
				double moveAwayY = -dyToBoss / distanceToBoss;
				
				// 반대방향으로 이동 (속도 증가)
				this.dx = moveAwayX * moveSpeed * 1.5;
				this.dy = moveAwayY * moveSpeed * 0.3;
			}
		}
		// 보스가 아닌 라운드에서 다른 적들과 충돌했을 때 반대방향으로 이동
		else if (other instanceof AlienEntity && !isBossRound()) {
			handleAlienCollision(other);
		}
	}
}