package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 플레이어의 우주선을 나타내는 엔티티
 * 
 * @author Kevin Glass
 */
public class ShipEntity extends Entity {
	/** 우주선이 존재하는 게임 */
	private Game game;
	
	/**
	 * 플레이어의 우주선을 나타내는 새로운 엔티티를 생성합니다
	 *  
	 * @param game 우주선이 생성되는 게임
	 * @param ref 우주선에 표시할 스프라이트의 참조
	 * @param x 플레이어 우주선의 초기 x 위치
	 * @param y 플레이어 우주선의 초기 y 위치
	 */
	public ShipEntity(Game game,String ref,int x,int y) {
		super(ref,x,y);
		
		this.game = game;
	}
	
	/**
	 * 경과된 시간을 기반으로 우주선이 스스로 이동하도록 요청합니다
	 * 
	 * @param delta 마지막 이동 이후 경과된 시간 (ms)
	 */
	public void move(long delta) {
		// 일반 이동 전에 자기장 효과 적용
		applyMagneticFieldEffects();
		
		// 왼쪽으로 이동 중이고 화면 왼쪽 끝에 도달했다면 이동하지 않음
		if ((dx < 0) && (x < 10)) {
			return;
		}
		// 오른쪽으로 이동 중이고 화면 오른쪽 끝에 도달했다면 이동하지 않음
		if ((dx > 0) && (x > 750)) {
			return;
		}
		
		super.move(delta);
	}
	
	/**
	 * 우주선에 자기장 효과를 적용합니다
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