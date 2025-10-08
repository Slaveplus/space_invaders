package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.io.InputStream;

import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.entity.ShipEntity;

/**
 * Ice attack entity using ice.gif sprite
 * 
 * @author Space Invaders Team
 */
public class IceAttack extends Entity {
	/** The game in which the entity exists */
	private Game game;
	/** Movement speed downward */
	private double moveSpeed = 200;
	/** Attack damage */
	private int damage = 3;
	/** Ice sprite image (animated GIF) */
	private Image iceImage;
	/** Scale factor */
	private double scale = 0.8;
	
	/**
	 * Create a new ice attack
	 * 
	 * @param game The game in which the entity exists
	 * @param x The initial x location
	 * @param y The initial y location
	 */
	public IceAttack(Game game, int x, int y) {
		super("sprites/Boss_Attack/ice.gif", x, y);
		this.game = game;
		this.dy = moveSpeed; // Move downward
		loadIceImage();
	}
	
	/**
	 * Load the ice.gif animated image
	 */
	private void loadIceImage() {
		try {
			// Use Toolkit to load animated GIF properly
			java.net.URL imageURL = getClass().getClassLoader().getResource("sprites/Boss_Attack/ice.gif");
			if (imageURL != null) {
				iceImage = Toolkit.getDefaultToolkit().createImage(imageURL);
				System.out.println("Successfully loaded animated ice attack GIF: ice.gif");
			} else {
				System.err.println("Could not find ice.gif file");
			}
		} catch (Exception e) {
			System.err.println("Failed to load ice attack image: " + e.getMessage());
		}
	}
	
	/**
	 * Move the ice attack
	 */
	public void move(long delta) {
		// Move downward
		y += (delta * dy) / 1000;
		
		// 디버그: 이동 정보 출력
		if (Math.random() < 0.05) { // 5% 확률로 출력
			System.out.println("Ice attack moving to X:" + (int)x + " Y:" + (int)y);
		}
		
		// Remove if off screen
		if (y > 600) {
			System.out.println("Ice attack removed - went off screen at Y:" + (int)y);
			game.removeEntity(this);
		}
	}
	
	/**
	 * Draw the ice attack
	 */
	public void draw(Graphics g) {
		if (iceImage != null) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			
		// Scale the ice image
		int scaledWidth = (int)(iceImage.getWidth(null) * scale);
		int scaledHeight = (int)(iceImage.getHeight(null) * scale);
			
			// Center the image
			int drawX = (int)x - scaledWidth/2;
			int drawY = (int)y - scaledHeight/2;
			
			// 디버그: 그리기 정보 출력 (매번 출력)
			System.out.println("Drawing ice attack at X:" + drawX + " Y:" + drawY + " W:" + scaledWidth + " H:" + scaledHeight);
			
			g2d.drawImage(iceImage, drawX, drawY, scaledWidth, scaledHeight, null);
		} else {
			// Fallback: draw a simple blue rectangle
			System.out.println("Ice image is null, drawing fallback blue circle at X:" + (int)x + " Y:" + (int)y);
			g.setColor(java.awt.Color.CYAN);
			g.fillOval((int)x - 10, (int)y - 10, 20, 20);
		}
	}
	
	/**
	 * Get collision bounds
	 */
	@Override
	public Rectangle getBounds() {
		if (iceImage != null) {
			int scaledWidth = (int)(iceImage.getWidth(null) * scale);
			int scaledHeight = (int)(iceImage.getHeight(null) * scale);
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
			System.out.println("Ice attack hit player!");
			game.notifyPlayerDamaged(damage);
			
			// Remove this ice attack
			game.removeEntity(this);
		}
	}
}
