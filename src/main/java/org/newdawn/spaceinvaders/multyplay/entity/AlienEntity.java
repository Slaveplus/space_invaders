package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.sprite.Sprite;

/**
 * 스페이스 인베이더 외계인 중 하나를 나타내는 엔티티입니다.
 * 
 * @author Kevin Glass
 */
public class AlienEntity extends Entity {
	/** 외계인이 수평으로 이동하는 속도 */
	private double moveSpeed = 75;
	/** 변화가 있는 현재 수평 속도 */
	private double horizontalSpeed;
	/** 엔티티가 존재하는 게임 */
	private MultiplayerGameContext game;
	/** 애니메이션 프레임 */
	private Sprite[] frames = new Sprite[4];
	/** 마지막 프레임 변경 이후의 시간 */
	private long lastFrameChange;
	/** 프레임 지속 시간 (밀리초), 즉 애니메이션의 특정 프레임이 지속되는 시간 */
	private long frameDuration = 250;
	/** 현재 표시되고 있는 애니메이션 프레임 */
	private int frameNumber;
	/** 이 외계인이 마지막으로 발사한 시간 */
	public long lastFire = 0;
	/** 외계인 발사 간격 (ms) */
	public long firingInterval = 2000;
	/** 외계인의 현재 HP */
	private int currentHP = 2;
	/** 외계인의 최대 HP */
	private int maxHP = 2;
	/** 개별 이동 방향 (true = 오른쪽, false = 왼쪽) */
	private boolean movingRight = Math.random() < 0.5;
	/** 개별 수직 이동 방향 (true = 아래, false = 위) */
	private boolean movingDown = Math.random() < 0.5;
	/** 마지막 방향 변경 이후의 시간 */
	private long lastDirectionChange = 0;
	/** 방향 변경 간 최소 시간 */
	private long directionChangeInterval = 2000;
	/** 마지막 충돌 시간 */
	private long lastCollisionTime = 0;
	/** 충돌 반응 간 최소 시간 (ms) */
	private static final long COLLISION_COOLDOWN = 500;
	
	/**
	 * 라운드 번호를 기반으로 외계인 스프라이트 경로를 가져옵니다
	 * 
	 * @param round 라운드 번호
	 * @return 외계인의 스프라이트 경로
	 */
	private static String getAlienSpriteForRound(int round) {
		switch (round) {
			case 1:
				return "sprites/Boss/1near.png";
			case 2:
				return "sprites/Boss/2near.png";
			case 3:
				return "sprites/Boss/3near.png";
			case 4:
				return "sprites/Boss/4near.png";
			case 5:
				return "sprites/Boss/5near.png";
			default:
				// For rounds 6 and above, reuse the late-round near sprite
				return "sprites/Boss/5near.png";
		}
	}
	
	/**
	 * 새로운 외계인 엔티티를 생성합니다
	 * 
	 * @param game 이 엔티티가 생성되는 게임
	 * @param x 이 외계인의 초기 x 위치
	 * @param y 이 외계인의 초기 y 위치
	 */
	public AlienEntity(MultiplayerGameContext game,int x,int y) {
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
		horizontalSpeed = actualMoveSpeed;
		dx = movingRight ? horizontalSpeed : -horizontalSpeed;
		
		// 수직 이동도 약간 추가 (더 자연스러운 움직임을 위해)
		// 30% 확률로 위아래로도 움직임
		if (Math.random() < 0.3) {
			dy = (Math.random() - 0.5) * actualMoveSpeed * 0.3; // -15% ~ +15% 속도로 위아래 이동
		} else {
			dy = 0; // 대부분은 좌우로만 움직임
		}

		directionChangeInterval = 800 + (long) (Math.random() * 1800);
		lastDirectionChange = System.currentTimeMillis();
		dx = movingRight ? horizontalSpeed : -horizontalSpeed;
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
			shuffleHorizontalDirection(currentTime);
		}
		
		double deltaSeconds = delta * 0.001;
		if (movingRight) {
			x += horizontalSpeed * deltaSeconds;
			if (x >= 750) {
				x = 745;
				movingRight = false;
				horizontalSpeed = moveSpeed * (0.6 + Math.random() * 0.8);
				if (Math.random() < 0.4) {
					changeVerticalDirection();
				}
				directionChangeInterval = 600 + (long) (Math.random() * 1200);
				lastDirectionChange = currentTime;
			}
		} else {
			x -= horizontalSpeed * deltaSeconds;
			if (x <= 10) {
				x = 15;
				movingRight = true;
				horizontalSpeed = moveSpeed * (0.6 + Math.random() * 0.8);
				if (Math.random() < 0.4) {
					changeVerticalDirection();
				}
				directionChangeInterval = 600 + (long) (Math.random() * 1200);
				lastDirectionChange = currentTime;
			}
		}
		dx = movingRight ? horizontalSpeed : -horizontalSpeed;

		// Y축 움직임 (작은 범위 내에서만)
		if (!movingDown) {
			y -= moveSpeed * delta * 0.0005;
			if (y <= 80) {
				movingDown = true;
			}
		} else {
			y += moveSpeed * delta * 0.0005;
			if (y >= 180) {
				movingDown = false;
			}
		}

		// Y 위치 제한 (맵 절반 이상 내려오지 못하게)
		if (y < 50) {
			y = 50;
		} else if (y > 300) {
			y = 300;
		}
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
			game.removeEntity(this);
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
	 * Draw this alien with scaled size to match single-player visuals.
	 *
	 * @param g The graphics context on which to draw
	 */
	@Override
	public void draw(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		if (sprite != null) {
			double scale = (game.getCurrentRound() == 1) ? 0.18 : 0.75;
			int scaledWidth = (int) (sprite.getWidth() * scale);
			int scaledHeight = (int) (sprite.getHeight() * scale);
			int drawX = (int) Math.round(x) - scaledWidth / 2;
			int drawY = (int) Math.round(y) - scaledHeight / 2;

			g2d.drawImage(
					sprite.getImage(),
					drawX,
					drawY,
					drawX + scaledWidth,
					drawY + scaledHeight,
					0,
					0,
					sprite.getWidth(),
					sprite.getHeight(),
					null);
		}
	}

	/**
	 * Override getBounds to match the scaled visual hitbox.
	 *
	 * @return The bounds of the alien entity
	 */
	@Override
	public Rectangle getBounds() {
		if (sprite != null) {
			double scale = (game.getCurrentRound() == 1) ? 0.18 : 0.75;
			int scaledWidth = (int) (sprite.getWidth() * scale);
			int scaledHeight = (int) (sprite.getHeight() * scale);
			int drawX = (int) Math.round(x) - scaledWidth / 2;
			int drawY = (int) Math.round(y) - scaledHeight / 2;

			int hitboxWidth = (int) Math.max(4, scaledWidth * 0.05);
			int hitboxHeight = (int) Math.max(4, scaledHeight * 0.03);
			int hitboxX = drawX + (scaledWidth - hitboxWidth) / 2;
			int hitboxY = drawY + (scaledHeight - hitboxHeight) / 2;
			return new Rectangle(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
		}
		return super.getBounds();
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
		horizontalSpeed = moveSpeed * (0.6 + Math.random() * 0.8);
		dx = movingRight ? horizontalSpeed : -horizontalSpeed;
	}

	private void shuffleHorizontalDirection(long currentTime) {
		movingRight = Math.random() < 0.5;
		horizontalSpeed = moveSpeed * (0.6 + Math.random() * 0.8);
		if (Math.random() < 0.3) {
			changeVerticalDirection();
		}
		directionChangeInterval = 800 + (long) (Math.random() * 1800);
		lastDirectionChange = currentTime;
		dx = movingRight ? horizontalSpeed : -horizontalSpeed;
	}

	private void syncHorizontalFromDx() {
		double newSpeed = Math.abs(dx);
		if (newSpeed > 0.01) {
			horizontalSpeed = Math.max(moveSpeed * 0.5, Math.min(moveSpeed * 1.5, newSpeed));
			movingRight = dx > 0;
			dx = movingRight ? horizontalSpeed : -horizontalSpeed;
		}
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
			syncHorizontalFromDx();
			
			// 이동 방향 플래그 업데이트
			movingDown = (this.dy > 0);
		}
	}
	
	/**
	 * Avoid getting too close to the player
	 */
	@SuppressWarnings("unused")
	private void avoidPlayer() {
		try {
			// Get local player ID and position
			String localPlayerId = game.getGameStateManager().getLocalPlayerId();
			if (localPlayerId == null) return;
			
			int playerX = game.getShipX(localPlayerId);
			int playerY = game.getShipY(localPlayerId);
			
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
				syncHorizontalFromDx();
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
				syncHorizontalFromDx();
			}
		}
		// 보스가 아닌 라운드에서 다른 적들과 충돌했을 때 반대방향으로 이동
		else if (other instanceof AlienEntity && !isBossRound()) {
			handleAlienCollision(other);
		}
	}
}
