package org.newdawn.spaceinvaders.gameplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.attack.IceAttack;
import org.newdawn.spaceinvaders.common.entity.effect.ExplosionEntity;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.common.sprite.Sprite;

/**
 * 최소한의 히트박스로 수평 이동하는 간단한 사이드 몬스터
 * 
 * @author Space Invaders Team
 */
public class SideMonster extends Entity {
	/** 엔티티가 존재하는 게임 */
	private Game game;
	/** 이동 속도 */
	private double moveSpeed = 50;
	/** 방향 (true = 오른쪽, false = 왼쪽) */
	private boolean movingRight = Math.random() < 0.5;
	/** 현재 HP */
	private int currentHP = 20;
	/** 최대 HP */
	private int maxHP = 20;
	/** 스프라이트 선택을 위한 라운드 번호 */
	private int round;
	/** 마지막 아이스 공격 이후의 시간 */
	private long lastIceAttack = 0;
	/** 아이스 공격 간격 */
	private long iceAttackInterval = 4000; // 4초마다 아이스 공격
	
	/**
	 * 새로운 사이드 몬스터를 생성합니다
	 * 
	 * @param game 엔티티가 존재하는 게임
	 * @param x 초기 x 위치
	 * @param y 초기 y 위치
	 * @param round 라운드 번호
	 */
	public SideMonster(Game game, int x, int y, int round) {
		super(getSideMonsterSpriteForRound(round), x, y);
		this.game = game;
		this.round = round;
		this.maxHP = 20; // Fixed HP for all rounds
		this.currentHP = maxHP;
	}
	
	/**
	 * Get the side monster sprite path based on round number
	 */
	private static String getSideMonsterSpriteForRound(int round) {
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
				return "sprites/Boss/1near.png";
		}
	}
	
	/**
	 * Move the side monster
	 */
	public void move(long delta) {
		// Simple horizontal movement
		if (movingRight) {
			x += moveSpeed * delta * 0.001;
			if (x >= 750) {
				movingRight = false;
				x = 745;
			}
		} else {
			x -= moveSpeed * delta * 0.001;
			if (x <= 10) {
				movingRight = true;
				x = 15;
			}
		}
		
		// Keep Y position stable
		if (y < 100) y = 100;
		if (y > 140) y = 140;
		
		// Try ice attack
		tryIceAttack();
		
		// Check collision with boss and reverse direction
		checkBossCollision();
	}
	
	/**
	 * Check collision with boss and reverse direction if needed
	 */
	private void checkBossCollision() {
		// Find boss entity
		for (Entity entity : game.getEntities()) {
		if (entity instanceof org.newdawn.spaceinvaders.common.entity.boss.BossEntity) {
			org.newdawn.spaceinvaders.common.entity.boss.BossEntity boss = (org.newdawn.spaceinvaders.common.entity.boss.BossEntity) entity;
				
				// Check if we're close to boss (within 150 pixels)
				double distanceToBoss = Math.sqrt(Math.pow(x - boss.getX(), 2) + Math.pow(y - boss.getY(), 2));
				
				if (distanceToBoss < 150) {
					// Reverse direction
					movingRight = !movingRight;
					
					// Move away from boss
					if (x < boss.getX()) {
						x -= 20; // Move left
					} else {
						x += 20; // Move right
					}
					
					System.out.println("Side monster reversed direction due to boss proximity!");
					break;
				}
			}
		}
	}
	
	/**
	 * Draw the side monster with scaling
	 */
	public void draw(Graphics g) {
		if (sprite != null) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			
			// Scale down to 18% for round 1, 75% for others
			double scale = 0.18; // 모든 라운드에서 1라운드와 동일한 크기
			int scaledWidth = (int)(sprite.getWidth() * scale);
			int scaledHeight = (int)(sprite.getHeight() * scale);
			
			// Center the sprite
			int drawX = (int)x - scaledWidth/2;
			int drawY = (int)y - scaledHeight/2;
			
			g2d.drawImage(sprite.getImage(), drawX, drawY, scaledWidth, scaledHeight, null);
		}
	}
	
	/**
	 * Get collision bounds - very small hitbox
	 */
	@Override
	public Rectangle getBounds() {
		if (sprite != null) {
			// Scale calculation
			double scale = 0.18; // 모든 라운드에서 1라운드와 동일한 크기
			int scaledWidth = (int)(sprite.getWidth() * scale);
			int scaledHeight = (int)(sprite.getHeight() * scale);
			
			// Center calculation
			int drawX = (int)x - scaledWidth/2;
			int drawY = (int)y - scaledHeight/2;
			
			// Hitbox - 60% of scaled size (much larger for easier hitting)
			int hitboxWidth = Math.max(1, (int)(scaledWidth * 0.60));
			int hitboxHeight = Math.max(1, (int)(scaledHeight * 0.60));
			int hitboxX = drawX + (scaledWidth - hitboxWidth) / 2;
			int hitboxY = drawY + (scaledHeight - hitboxHeight) / 2;
			
			// 디버그: 히트박스 정보 출력
			if (Math.random() < 0.005) { // 0.5% 확률로 출력
				System.out.println("SideMonster hitbox - X:" + hitboxX + " Y:" + hitboxY + " W:" + hitboxWidth + " H:" + hitboxHeight + " (60% of scaled size)");
			}
			
			return new Rectangle(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
		}
		return super.getBounds();
	}
	
	/**
	 * Take damage
	 */
	public void takeDamage(int damage) {
		System.out.println("SideMonster took " + damage + " damage! HP: " + currentHP + " -> " + (currentHP - damage));
		currentHP -= damage;
		if (currentHP <= 0) {
			System.out.println("SideMonster destroyed!");
			// Create explosion
			ExplosionEntity explosion = new ExplosionEntity(game, "sprites/Skill/Explosion.png", (int)x, (int)y, 1000);
			game.addEntity(explosion);
			
			// Remove this entity
			game.removeEntity(this);
			
			// Notify game
			game.notifyAlienKilled();
		}
	}
	
	/**
	 * Get current HP
	 */
	public int getCurrentHP() {
		return currentHP;
	}
	
	/**
	 * Get maximum HP
	 */
	public int getMaxHP() {
		return maxHP;
	}
	
	/**
	 * Try to perform ice attack
	 */
	public void tryIceAttack() {
		try {
			// Check if 3 seconds have passed since game start
			if (!game.canEnemiesAttack()) {
				return;
			}
			
			long currentTime = System.currentTimeMillis();
			
			// Check if enough time has passed since last ice attack
			if (currentTime - lastIceAttack < iceAttackInterval) {
				return;
			}
			
			// Create new ice attack using ice.gif (from side monster position)
			IceAttack iceAttack = new IceAttack(game, (int) x, (int) y + 80);
			
			// Add to game entities
			game.addEntity(iceAttack);
			
			// Update last ice attack time
			lastIceAttack = currentTime;
			
			System.out.println("Side Monster performed ice attack with ice.gif!");
			
		} catch (Exception e) {
			System.err.println("Error in side monster ice attack: " + e.getMessage());
			// 게임이 크래시되지 않도록 예외를 잡고 계속 진행
			lastIceAttack = System.currentTimeMillis(); // 다음 공격 시간 업데이트
		}
	}
	
	/**
	 * Collision handler
	 */
	public void collidedWith(Entity other) {
		// Simple collision - no complex behavior needed
	}
}
