package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.common.sprite.Sprite;
import org.newdawn.spaceinvaders.common.sprite.SpriteStore;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * Ice ball attack entity using ice ball.gif sprite
 * 
 * @author Space Invaders Team
 */
public class IceBallAttack extends Entity {
	/** The game in which the entity exists */
	private Game game;
	/** Movement speed */
	private double moveSpeed = 150;
	/** Attack damage */
	private int damage = 1;
	/** Ice ball sprite image (animated GIF) */
	private Image iceBallImage;
	/** Scale factor */
	private double scale = 0.6;
	/** Direction X */
	private double dirX;
	/** Direction Y */
	private double dirY;
	
	/**
	 * Create a new ice ball attack
	 * 
	 * @param game The game in which the entity exists
	 * @param x The initial x location
	 * @param y The initial y location
	 * @param dirX Direction X (-1 to 1)
	 * @param dirY Direction Y (-1 to 1)
	 */
	public IceBallAttack(Game game, int x, int y, double dirX, double dirY) {
		super("sprites/Boss_Attack/ice ball.gif", x, y);
		this.game = game;
		this.dirX = dirX;
		this.dirY = dirY;
		this.dx = dirX * moveSpeed;
		this.dy = dirY * moveSpeed;
		loadIceBallImage();
	}
	
	/**
	 * Load the ice ball.gif animated image
	 */
	private void loadIceBallImage() {
		try {
			// Use Toolkit to load animated GIF properly
			java.net.URL imageURL = getClass().getClassLoader().getResource("sprites/Boss_Attack/ice ball.gif");
			if (imageURL != null) {
				iceBallImage = Toolkit.getDefaultToolkit().createImage(imageURL);
				System.out.println("Successfully loaded animated ice ball attack GIF: ice ball.gif");
			} else {
				System.err.println("Could not find ice ball.gif file");
			}
		} catch (Exception e) {
			System.err.println("Failed to load ice ball attack image: " + e.getMessage());
		}
	}
	
	/**
	 * Move the ice ball attack
	 */
	public void move(long delta) {
		// Move in specified direction
		x += (delta * dx) / 1000;
		y += (delta * dy) / 1000;
		
		// Remove if off screen
		if (y > 600 || y < -100 || x < -100 || x > 800) {
			System.out.println("Ice ball attack removed - went off screen at X:" + (int)x + " Y:" + (int)y);
			game.removeEntity(this);
		}
	}
	
	/**
	 * Draw the ice ball attack
	 */
	public void draw(Graphics g) {
		if (iceBallImage != null) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			
			// Scale the ice ball image
			int scaledWidth = (int)(iceBallImage.getWidth(null) * scale);
			int scaledHeight = (int)(iceBallImage.getHeight(null) * scale);
			
			// Center the image
			int drawX = (int)x - scaledWidth/2;
			int drawY = (int)y - scaledHeight/2;
			
			// 디버그: 그리기 정보 출력 (매번 출력) - 성능을 위해 주석 처리
			// System.out.println("Drawing ice ball attack at X:" + drawX + " Y:" + drawY + " W:" + scaledWidth + " H:" + scaledHeight);
			
			g2d.drawImage(iceBallImage, drawX, drawY, scaledWidth, scaledHeight, null);
		} else {
			// Fallback: draw a simple cyan circle
			// System.out.println("Ice ball image is null, drawing fallback cyan circle at X:" + (int)x + " Y:" + (int)y);
			g.setColor(java.awt.Color.CYAN);
			g.fillOval((int)x - 10, (int)y - 10, 20, 20);
		}
	}
	
	/**
	 * Get collision bounds
	 */
	@Override
	public Rectangle getBounds() {
		if (iceBallImage != null) {
			int scaledWidth = (int)(iceBallImage.getWidth(null) * scale);
			int scaledHeight = (int)(iceBallImage.getHeight(null) * scale);
			int drawX = (int)x - scaledWidth/2;
			int drawY = (int)y - scaledHeight/2;
			return new Rectangle(drawX, drawY, scaledWidth, scaledHeight);
		}
		return new Rectangle((int)x - 10, (int)y - 10, 20, 20);
	}
	
	/**
	 * Collision handler
	 */
	public void collidedWith(Entity other) {
		if (other instanceof ShipEntity) {
			// Hit the player
			System.out.println("Ice ball attack hit player!");
			game.notifyPlayerDamaged(damage);
			
			// Remove this ice ball attack
			game.removeEntity(this);
		}
	}
}
