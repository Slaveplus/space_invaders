package org.newdawn.spaceinvaders.gameplay.sprite;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;

/**
 * A resource manager for sprites in the game. Its often quite important
 * how and where you get your game resources from. In most cases
 * it makes sense to have a central resource loader that goes away, gets
 * your resources and caches them for future use.
 * <p>
 * [singleton]
 * <p>
 * @author Kevin Glass
 */
public class SpriteStore {
	/** The single instance of this class */
	private static SpriteStore single = new SpriteStore();
	
	/**
	 * Get the single instance of this class 
	 * 
	 * @return The single instance of this class
	 */
	public static SpriteStore get() {
		return single;
	}
	
	/** The cached sprite map, from reference to sprite instance */
	private HashMap<String, Sprite> sprites = new HashMap<>();
	
	/**
	 * Retrieve a sprite from the store
	 * 
	 * @param ref The reference to the image to use for the sprite
	 * @return A sprite instance containing an accelerate image of the request reference
	 */
	public Sprite getSprite(String ref) {
		// if we've already got the sprite in the cache
		// then just return the existing version
		if (sprites.get(ref) != null) {
			return sprites.get(ref);
		}
		
		// otherwise, go away and grab the sprite from the resource
		// loader
		BufferedImage sourceImage = null;
		
		try {
			// The ClassLoader.getResource() ensures we get the sprite
			// from the appropriate place, this helps with deploying the game
			// with things like webstart. You could equally do a file look
			// up here.
			URL url = this.getClass().getClassLoader().getResource(ref);
			
			if (url == null) {
				fail("Can't find ref: "+ref);
			}
			
			// use ImageIO to read the image in
			sourceImage = ImageIO.read(url);
		} catch (IOException e) {
			fail("Failed to load: "+ref);
		}
		
		// 이미지 크기 조정
		BufferedImage resizedImage = resizeImageForGameplay(sourceImage, ref);
		
		// create an accelerated image of the right size to store our sprite in
		GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		Image image = gc.createCompatibleImage(resizedImage.getWidth(),resizedImage.getHeight(),Transparency.BITMASK);
		
		// draw our source image into the accelerated image
		image.getGraphics().drawImage(resizedImage,0,0,null);
		
		// create a sprite, add it the cache then return it
		Sprite sprite = new Sprite(image);
		sprites.put(ref, sprite);
		
		return sprite;
	}
	
	/**
	 * 게임플레이에 적합한 크기로 이미지를 리사이징
	 * 
	 * @param sourceImage 원본 이미지
	 * @param ref 이미지 참조 경로
	 * @return 리사이징된 이미지
	 */
	private BufferedImage resizeImageForGameplay(BufferedImage sourceImage, String ref) {
		int targetWidth, targetHeight;
		
		// 이미지 타입에 따라 다른 크기 적용
		if (ref.contains("ships/")) {
			// 우주선 이미지: 64x64로 리사이징
			targetWidth = 64;
			targetHeight = 64;
		} else if (ref.contains("weapons/")) {
			// 무기 이미지: 32x32로 리사이징
			targetWidth = 32;
			targetHeight = 32;
		} else if (ref.contains("shot.gif")) {
			// 기본 총알: 16x16로 리사이징
			targetWidth = 16;
			targetHeight = 16;
		} else if (ref.contains("alien")) {
			// 외계인: 48x48로 리사이징
			targetWidth = 48;
			targetHeight = 48;
		} else {
			// 기본값: 원본 크기 유지
			return sourceImage;
		}
		
		// 이미지가 이미 적절한 크기면 리사이징하지 않음
		if (sourceImage.getWidth() == targetWidth && sourceImage.getHeight() == targetHeight) {
			return sourceImage;
		}
		
		// 고품질 리사이징
		BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = resizedImage.createGraphics();
		
		// 고품질 렌더링 힌트 설정
		g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		
		// 이미지 그리기
		g2d.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
		g2d.dispose();
		
		// 이미지 리사이징 완료 (로그 제거)
		
		return resizedImage;
	}
	
	/**
	 * Utility method to handle resource loading failure
	 * 
	 * @param message The message to display on failure
	 */
	private void fail(String message) {
		// we're pretty dramatic here, if a resource isn't available
		// we dump the message and exit the game
		System.err.println(message);
		System.exit(0);
	}
}