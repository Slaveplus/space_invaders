package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;

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
	/** True if this is a near monster shot (needs smaller size) */
	private boolean isNearMonsterShot = false;
	
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
		
		// 스킬 드롭은 천천히 떨어지도록 속도 조정
		dy = 100; // 기본 moveSpeed 대신 100으로 고정 (천천히 떨어짐)
		
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
		
		// Debug skill drop movement (reduced for performance)
		// if (isSkillDrop) {
		//	System.out.println("🎁 Skill drop moving: y=" + y + ", delta=" + delta);
		// }
		
		// if we shot off the screen, remove ourselfs
		// 스킬 드롭은 더 오래 화면에 남아있도록 경계 조정
		if (y < -50 || y > 700) { // y > 600을 700으로 변경
			if (isSkillDrop) {
			}
			game.removeEntity(this);
		}
	}
	
	/**
	 * Load skill image for skill drop display
	 */
	private BufferedImage loadSkillImageForDrop(int skillType) {
		String imagePath;
		switch (skillType) {
			case 0: imagePath = "sprites/Skill/1.png"; break; // Invincible
			case 2: imagePath = "sprites/Skill/3.png"; break; // Triple Shot
			case 3: imagePath = "sprites/Skill/4.png"; break; // Missile
			default: imagePath = "sprites/Skill/1.png"; break;
		}
		
		try {
			return javax.imageio.ImageIO.read(getClass().getClassLoader().getResourceAsStream(imagePath));
		} catch (Exception e) {
			System.err.println("Failed to load skill image: " + imagePath);
			return null;
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
		
		// Prevent player shots from colliding with each other
		if (other instanceof ShotEntity && !isAlienShot && !((ShotEntity) other).isAlienShot) {
			return;
		}
		
		// Prevent player shots from hitting the player (only if this is actually a player shot)
		if (other instanceof ShipEntity && !isAlienShot && !isSkillDrop) {
			return;
		}
		
		// Debug: Log collision detection (reduced for performance)
		// System.out.println("ShotEntity collidedWith called - other entity: " + other.getClass().getSimpleName());
		
		// if we've hit an alien, kill it!
		if (other instanceof AlienEntity) {
			// System.out.println("Shot hit AlienEntity! Removing entities...");
			
			// Create heat effect at impact point
			game.createHeatEffect((int)other.getX(), (int)other.getY(), 50.0);
			
			// remove the affected entities
			game.removeEntity(this);
			game.removeEntity(other);
			
			// notify the game that the alien has been killed
			game.notifyAlienKilled();
			used = true;
		}
		
		// if we've hit a near monster, damage it! (but not if this is a near monster shot)
		if (other.getClass().getSimpleName().equals("NearEntity") && !isNearMonsterShot) {
			// System.out.println("🎯 Shot hit NearEntity! Damaging near monster...");
			
			// Create heat effect at impact point
			game.createHeatEffect((int)other.getX(), (int)other.getY(), 60.0);
			
			// remove the shot
			game.removeEntity(this);
			
			// damage the near monster
			try {
				// Cast to NearEntity and call takeDamage directly
				org.newdawn.spaceinvaders.gameplay.entity.NearEntity nearEntity = (org.newdawn.spaceinvaders.gameplay.entity.NearEntity) other;
				nearEntity.takeDamage(game.getPlayerAttackPower());
				// System.out.println("🎯 Near Monster took " + game.getPlayerAttackPower() + " damage!");
			} catch (Exception e) {
				System.out.println("🎯 Could not damage near monster: " + e.getMessage());
				e.printStackTrace();
			}
			
			used = true;
		}
		
		// if we've hit a boss, damage it!
		if (other.getClass().getSimpleName().equals("BossEntity")) {
			// System.out.println("Shot hit BossEntity! Damaging boss...");
			
			// Create heat effect at impact point
			game.createHeatEffect((int)other.getX(), (int)other.getY(), 80.0);
			
			// remove the shot
			game.removeEntity(this);
			
			// damage the boss (assuming BossEntity has a takeDamage method)
			try {
				// Use reflection to call takeDamage method if it exists
				java.lang.reflect.Method takeDamageMethod = other.getClass().getMethod("takeDamage", int.class);
				takeDamageMethod.invoke(other, game.getPlayerAttackPower());
				// System.out.println("Boss took " + game.getPlayerAttackPower() + " damage!");
			} catch (Exception e) {
				System.out.println("Could not damage boss: " + e.getMessage());
			}
			
			used = true;
		}
		
		// if we've hit the player's ship, damage it (but not if this is a player shot)
		if (other instanceof ShipEntity && isAlienShot) {
			
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
			// Draw skill drop with same style as in-game skill UI
			Graphics2D g2d = (Graphics2D) g;
			int itemSize = 24; // 스킬 드롭 크기
			int drawX = (int)x - itemSize / 2;
			int drawY = (int)y - itemSize / 2;
			
			// Load skill image based on skill type
			BufferedImage skillImage = loadSkillImageForDrop(skillType);
			if (skillImage != null) {
				// 진한 그림자 효과 (오른쪽 아래로 이동, 더 진한 색상)
				g2d.setColor(new Color(0, 0, 0, 200)); // 더 진한 그림자
				g2d.fillRect(drawX + 2, drawY + 2, itemSize, itemSize);
				
				// 추가 그림자 (더 깊이감)
				g2d.setColor(new Color(0, 0, 0, 150));
				g2d.fillRect(drawX + 1, drawY + 1, itemSize, itemSize);
				
				// 실제 스킬 이미지
				g2d.drawImage(skillImage, drawX, drawY, itemSize, itemSize, null);
				
				// 진한 테두리 효과 (선명도 향상)
				g2d.setColor(new Color(255, 255, 255, 200)); // 더 진한 테두리
				g2d.setStroke(new BasicStroke(2));
				g2d.drawRect(drawX, drawY, itemSize, itemSize);
				
				// 스킬 개수 표시 (스킬 드롭이므로 항상 1개)
				int circleX = drawX + itemSize - 4;
				int circleY = drawY + itemSize - 4;
				int circleRadius = 6;
				
				// 배경 원
				g2d.setColor(new Color(0, 0, 0, 180));
				g2d.fillOval(circleX - circleRadius, circleY - circleRadius, circleRadius * 2, circleRadius * 2);
				
				// 개수 텍스트
				g2d.setColor(Color.YELLOW);
				g2d.setFont(new Font("Arial", Font.BOLD, 8));
				String countText = "1";
				int textWidth = g2d.getFontMetrics().stringWidth(countText);
				int textHeight = g2d.getFontMetrics().getHeight();
				int textX = circleX - textWidth / 2;
				int textY = circleY + textHeight / 4;
				g2d.drawString(countText, textX, textY);
			} else {
				// Fallback: Draw simple colored circle
				g.setColor(Color.CYAN);
				g.fillOval(drawX, drawY, itemSize, itemSize);
				g.setColor(Color.WHITE);
				g.drawOval(drawX, drawY, itemSize, itemSize);
			}
		} else if (hasPiercing) {
			// Draw piercing shot with special color
			g.setColor(Color.YELLOW);
			g.fillRect((int)x - 2, (int)y - 10, 4, 20);
		} else if (isNearMonsterShot) {
			// Draw near monster shot with 50x50 size
			int smallWidth = 50;  // 50x50 크기로 키움
			int smallHeight = 50;
			sprite.draw(g, (int)x - smallWidth/2, (int)y - smallHeight/2, smallWidth, smallHeight);
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
	
	/**
	 * Set whether this is a near monster shot (for size adjustment)
	 * 
	 * @param isNearMonsterShot True if this is a near monster shot
	 */
	public void setNearMonsterShot(boolean isNearMonsterShot) {
		this.isNearMonsterShot = isNearMonsterShot;
	}
	
	/**
	 * Override getBounds to provide smaller hitbox for near monster shots
	 * 
	 * @return The bounds of the shot entity
	 */
	public java.awt.Rectangle getBounds() {
		if (isNearMonsterShot) {
			// Near monster shots have smaller hitbox (20x20) than display size (50x50)
			int hitboxSize = 20;
			return new java.awt.Rectangle((int)x - hitboxSize/2, (int)y - hitboxSize/2, hitboxSize, hitboxSize);
		} else {
			// Normal shots use default bounds
			return super.getBounds();
		}
	}
}
